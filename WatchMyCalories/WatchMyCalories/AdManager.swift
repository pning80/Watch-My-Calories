import Foundation
import GoogleMobileAds
import os
import UserMessagingPlatform

final class AdManager: ObservableObject {
    static let shared = AdManager()

    private static let logger = Logger(subsystem: "com.pning80.WatchMyCalories", category: "AdManager")

    @Published var canRequestAds: Bool = false
    @Published var isPrivacyOptionsRequired: Bool = false

    static var isUITestingMode: Bool {
        WatchMyCaloriesApp.isUITesting
    }

    // MARK: - Ad Unit IDs

    #if DEBUG
    static let bannerAdUnitID = "ca-app-pub-3940256099942544/2934735716"
    static let nativeAdUnitID = "ca-app-pub-3940256099942544/3986624511"
    #else
    static let bannerAdUnitID = Bundle.main.infoDictionary?["AdMobBannerAdUnitID"] as? String ?? ""
    static let nativeAdUnitID = Bundle.main.infoDictionary?["AdMobNativeAdUnitID"] as? String ?? ""
    #endif

    private init() {
        guard !Self.isUITestingMode else { return }
    }

    // MARK: - UMP Consent

    func gatherConsent() async {
        guard !Self.isUITestingMode else { return }

        let params = RequestParameters()
        params.isTaggedForUnderAgeOfConsent = false

        #if targetEnvironment(simulator)
        if CommandLine.arguments.contains("-UMPGeographyEEA") {
            let debugSettings = DebugSettings()
            debugSettings.geography = .EEA
            params.debugSettings = debugSettings
        }
        #endif

        do {
            try await ConsentInformation.shared.requestConsentInfoUpdate(with: params)

            let rootVC: UIViewController? = await MainActor.run {
                UIApplication.shared.connectedScenes
                    .compactMap { $0 as? UIWindowScene }
                    .first?.windows.first?.rootViewController
            }

            guard let rootVC else { return }

            try await MainActor.run {
                try ConsentForm.loadAndPresentIfRequired(from: rootVC)
            }
        } catch {
            Self.logger.error("Consent error: \(error.localizedDescription)")
        }

        await MainActor.run {
            isPrivacyOptionsRequired = ConsentInformation.shared.privacyOptionsRequirementStatus == .required
        }

        let canRequest = ConsentInformation.shared.canRequestAds
        if canRequest {
            startSDK()
        }
    }

    // MARK: - Privacy Options

    @MainActor
    func presentPrivacyOptionsForm() async {
        guard let windowScene = UIApplication.shared.connectedScenes
            .compactMap({ $0 as? UIWindowScene }).first,
            let rootVC = windowScene.windows.first?.rootViewController else { return }

        do {
            try await ConsentForm.presentPrivacyOptionsForm(from: rootVC)

            isPrivacyOptionsRequired = ConsentInformation.shared.privacyOptionsRequirementStatus == .required
            let canRequest = ConsentInformation.shared.canRequestAds
            canRequestAds = canRequest
            if canRequest {
                startSDK()
            }
        } catch {
            Self.logger.error("Privacy options error: \(error.localizedDescription)")
        }
    }

    // MARK: - Enable Ads

    /// Gathers UMP consent and starts the SDK (non-personalized ads only).
    func enableAds() async {
        guard !Self.isUITestingMode else { return }
        await gatherConsent()
    }

    // MARK: - SDK Start

    private func startSDK() {
        guard !Self.isUITestingMode else { return }
        MobileAds.shared.start { _ in
            Task { @MainActor in
                self.canRequestAds = true
            }
        }
    }
}
