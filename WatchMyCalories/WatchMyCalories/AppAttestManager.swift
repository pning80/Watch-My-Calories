import Foundation
import DeviceCheck
import CryptoKit
import os

/// Manages Apple App Attest key generation, attestation, and per-request assertions.
///
/// On a real device, this generates a key pair in the Secure Enclave, attests it with Apple,
/// and registers the public key with the backend. Subsequent requests are signed with assertions
/// that the backend verifies. On simulator or unsupported devices, this is a no-op and the caller
/// falls back to legacy `x-backend-key` authentication.
final class AppAttestManager {

    static let shared = AppAttestManager()
    private static let logger = Logger(subsystem: Bundle.main.bundleIdentifier ?? "WatchMyCalories", category: "AppAttest")

    /// Whether App Attest is available on this device.
    var isSupported: Bool {
        #if targetEnvironment(simulator)
        return false
        #else
        return DCAppAttestService.shared.isSupported
        #endif
    }

    // MARK: - Keychain Constants

    private let keychainService = "com.pning80.WatchMyCalories.AppAttest"
    private let keychainKeyIDAccount = "attestKeyID"
    private let keychainAttestedAccount = "attestCompleted"

    // MARK: - State

    private let lock = NSLock()
    private var attestationInProgress: Task<Void, Error>?

    private init() {}

    // MARK: - Public API

    /// Ensures the device has a valid attested key. Idempotent — no-op if already attested.
    /// Generates a new key and attests it if needed.
    func ensureAttested() async throws {
        guard isSupported else { return }

        // Fast path: already attested
        if isAttested() { return }

        // Serialize concurrent attestation attempts
        let task: Task<Void, Error> = lock.withLock {
            if let existing = attestationInProgress {
                return existing
            }
            let newTask = Task {
                defer {
                    self.lock.withLock { self.attestationInProgress = nil }
                }
                try await self.performAttestation()
            }
            attestationInProgress = newTask
            return newTask
        }

        try await task.value
    }

    /// Generates assertion headers for the given request body.
    /// Returns empty dictionary if App Attest is not supported or not attested.
    func assertionHeaders(for body: Data) async throws -> [String: String] {
        guard isSupported, let keyID = loadKeyID(), isAttested() else {
            return [:]
        }

        let clientDataHash = Data(SHA256.hash(data: body))
        let service = DCAppAttestService.shared

        do {
            let assertion = try await service.generateAssertion(keyID, clientDataHash: clientDataHash)
            return [
                "X-App-Attest-Assertion": assertion.base64EncodedString(),
                "X-App-Attest-Key-ID": keyID
            ]
        } catch {
            // Key may be gone from the Secure Enclave (app reinstall, device restore)
            // while keychain still says "attested" — clear state and re-attest
            handleAttestationRejected()
            try await ensureAttested()

            guard let newKeyID = loadKeyID(), isAttested() else {
                return [:]
            }
            let newAssertion = try await service.generateAssertion(newKeyID, clientDataHash: clientDataHash)
            return [
                "X-App-Attest-Assertion": newAssertion.base64EncodedString(),
                "X-App-Attest-Key-ID": newKeyID
            ]
        }
    }

    /// Clears local attestation state, forcing re-attestation on next call.
    /// Call this when the backend returns 401 for an attested request.
    func handleAttestationRejected() {
        deleteKeychainItem(account: keychainKeyIDAccount)
        deleteKeychainItem(account: keychainAttestedAccount)
    }

    // MARK: - Attestation Flow

    private func performAttestation() async throws {
        let service = DCAppAttestService.shared

        // Step 1: Generate a new key (or reuse existing un-attested key)
        var keyID: String
        let existingKeyID = loadKeyID()
        if let existingKeyID, !isAttested() {
            keyID = existingKeyID
        } else {
            keyID = try await service.generateKey()
            saveKeyID(keyID)
        }

        // Step 2: Fetch a one-time challenge from the backend
        var challenge = try await fetchChallenge()

        // Step 3: Attest the key with Apple
        var challengeHash = Data(SHA256.hash(data: Data(challenge.utf8)))
        let attestation: Data
        do {
            attestation = try await service.attestKey(keyID, clientDataHash: challengeHash)
        } catch {
            // Key may be permanently invalid (e.g. already attested) — clear it
            deleteKeychainItem(account: keychainKeyIDAccount)

            if existingKeyID != nil {
                // Stale key — generate a fresh one and retry once
                let freshKeyID = try await service.generateKey()
                saveKeyID(freshKeyID)
                challenge = try await fetchChallenge()
                challengeHash = Data(SHA256.hash(data: Data(challenge.utf8)))
                attestation = try await service.attestKey(freshKeyID, clientDataHash: challengeHash)
                keyID = freshKeyID
            } else {
                throw error
            }
        }

        // Step 4: Register the attested key with the backend
        do {
            try await registerAttestation(keyID: keyID, attestation: attestation, challenge: challenge)
        } catch {
            // Apple already attested this key but backend rejected it — key is now unusable, clear it
            deleteKeychainItem(account: keychainKeyIDAccount)
            throw error
        }

        // Mark as attested
        saveAttested(true)
    }

    // MARK: - Backend Communication

    private func fetchChallenge() async throws -> String {
        let urlString = "\(BackendConfig.baseURL)/attest/challenge"
        guard let url = URL(string: urlString) else {
            throw AppAttestError.invalidURL
        }

        let (data, response) = try await URLSession.shared.data(from: url)
        guard let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 200 else {
            throw AppAttestError.challengeFetchFailed
        }

        guard let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let challenge = json["challenge"] as? String else {
            throw AppAttestError.invalidChallengeResponse
        }

        return challenge
    }

    private func registerAttestation(keyID: String, attestation: Data, challenge: String) async throws {
        let urlString = "\(BackendConfig.baseURL)/attest/verify"
        guard let url = URL(string: urlString) else {
            throw AppAttestError.invalidURL
        }

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.addValue("application/json", forHTTPHeaderField: "Content-Type")

        let body: [String: String] = [
            "keyID": keyID,
            "attestation": attestation.base64EncodedString(),
            "challenge": challenge
        ]
        request.httpBody = try JSONSerialization.data(withJSONObject: body)

        let (data, response) = try await URLSession.shared.data(for: request)
        guard let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 200 else {
            let errorMsg = String(data: data, encoding: .utf8) ?? "Unknown error"
            throw AppAttestError.attestationVerifyFailed(errorMsg)
        }
    }

    // MARK: - Keychain Helpers

    func isAttested() -> Bool {
        return loadKeychainString(account: keychainAttestedAccount) == "true"
    }

    private func saveAttested(_ value: Bool) {
        saveKeychainString(value ? "true" : "false", account: keychainAttestedAccount)
    }

    private func loadKeyID() -> String? {
        return loadKeychainString(account: keychainKeyIDAccount)
    }

    private func saveKeyID(_ keyID: String) {
        saveKeychainString(keyID, account: keychainKeyIDAccount)
    }

    private func saveKeychainString(_ value: String, account: String) {
        guard let data = value.data(using: .utf8) else {
            Self.logger.error("Keychain save failed: could not encode value for account '\(account)'")
            return
        }
        deleteKeychainItem(account: account)

        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: keychainService,
            kSecAttrAccount as String: account,
            kSecValueData as String: data,
            kSecAttrAccessible as String: kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
        ]
        let status = SecItemAdd(query as CFDictionary, nil)
        if status != errSecSuccess {
            Self.logger.error("Keychain SecItemAdd failed for account '\(account)': OSStatus \(status)")
        }
    }

    private func loadKeychainString(account: String) -> String? {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: keychainService,
            kSecAttrAccount as String: account,
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne
        ]

        var result: AnyObject?
        let status = SecItemCopyMatching(query as CFDictionary, &result)
        guard status == errSecSuccess, let data = result as? Data else { return nil }
        return String(data: data, encoding: .utf8)
    }

    private func deleteKeychainItem(account: String) {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: keychainService,
            kSecAttrAccount as String: account
        ]
        let status = SecItemDelete(query as CFDictionary)
        if status != errSecSuccess && status != errSecItemNotFound {
            Self.logger.error("Keychain SecItemDelete failed for account '\(account)': OSStatus \(status)")
        }
    }
}

// MARK: - Errors

enum AppAttestError: Error, LocalizedError {
    case invalidURL
    case challengeFetchFailed
    case invalidChallengeResponse
    case attestationVerifyFailed(String)

    var errorDescription: String? {
        switch self {
        case .invalidURL:
            return "Invalid App Attest URL."
        case .challengeFetchFailed:
            return "Failed to fetch attestation challenge from server."
        case .invalidChallengeResponse:
            return "Invalid challenge response from server."
        case .attestationVerifyFailed(let msg):
            return "Attestation verification failed: \(msg)"
        }
    }
}
