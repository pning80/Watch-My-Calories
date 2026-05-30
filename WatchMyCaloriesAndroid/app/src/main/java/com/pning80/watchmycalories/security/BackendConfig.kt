package com.pning80.watchmycalories.security

import com.pning80.watchmycalories.BuildConfig

/**
 * Cloud Run backend configuration (PORTING_CRITERIA.md T1.5).
 *
 * The Android client no longer holds a Gemini API key — all Gemini requests
 * are proxied through the backend. Production builds use Play Integrity
 * attestation (T1.8); dev builds may fall back to a legacy `x-backend-key`
 * for emulators (mirrors the iOS App Attest simulator fallback).
 */
object BackendConfig {

    // Remote endpoint — mirrors iOS BackendConfig.swift. The same dev backend
    // serves both platforms; the X-App-Platform header dispatches verification.
    val baseURL: String = if (BuildConfig.DEBUG) {
        "https://watchmycalories-backend-dev-657698311127.us-central1.run.app"
    } else {
        "https://watchmycalories-backend-657698311127.us-central1.run.app"
    }

    // Dev-only legacy x-backend-key. Used only when Play Integrity is unsupported
    // or fails in debug builds, and only against the dev backend.
    // XOR-obfuscated to match iOS.
    private val debugObfuscatedKey = byteArrayOf(
        0x15.toByte(), 0xb5.toByte(), 0x84.toByte(), 0xea.toByte(), 0x85.toByte(), 0x3c.toByte(), 0x0b.toByte(), 0x94.toByte(),
        0xee.toByte(), 0x72.toByte(), 0x13.toByte(), 0x74.toByte(), 0x1e.toByte(), 0x7f.toByte(), 0x0d.toByte(), 0x3e.toByte(),
        0x12.toByte(), 0x6a.toByte(), 0x1f.toByte(), 0xe3.toByte(), 0xce.toByte(), 0x32.toByte(), 0xde.toByte(), 0x53.toByte(),
        0x86.toByte(), 0x5f.toByte(), 0x7f.toByte(), 0x0f.toByte(), 0xee.toByte(), 0x48.toByte(), 0x3e.toByte(), 0x30.toByte(),
        0x57.toByte(), 0x5a.toByte(), 0x40.toByte(), 0xd0.toByte(), 0x07.toByte(), 0x8a.toByte(), 0x3c.toByte(), 0xa6.toByte(),
        0xdf.toByte(), 0x4d.toByte(), 0x74.toByte(), 0x75.toByte(), 0x88.toByte(), 0x64.toByte(), 0xa6.toByte(), 0xc7.toByte(),
        0x49.toByte(), 0x77.toByte(), 0x12.toByte(), 0xda.toByte(), 0x76.toByte(), 0xb8.toByte(), 0xaf.toByte(), 0xf5.toByte(),
        0xd2.toByte(), 0x1e.toByte(), 0x80.toByte(), 0xeb.toByte(), 0x06.toByte(), 0x6f.toByte(), 0x4b.toByte(), 0xea.toByte()
    )

    private val debugKeyMask = byteArrayOf(
        0x22.toByte(), 0xd0.toByte(), 0xb0.toByte(), 0x89.toByte(), 0xe3.toByte(), 0x5f.toByte(), 0x3c.toByte(), 0xa3.toByte(),
        0xde.toByte(), 0x17.toByte(), 0x71.toByte(), 0x45.toByte(), 0x2b.toByte(), 0x4d.toByte(), 0x6c.toByte(), 0x58.toByte(),
        0x23.toByte(), 0x58.toByte(), 0x2d.toByte(), 0x80.toByte(), 0xaa.toByte(), 0x04.toByte(), 0xef.toByte(), 0x32.toByte(),
        0xbe.toByte(), 0x6a.toByte(), 0x47.toByte(), 0x6c.toByte(), 0x8d.toByte(), 0x70.toByte(), 0x07.toByte(), 0x03.toByte(),
        0x63.toByte(), 0x6c.toByte(), 0x70.toByte(), 0xb5.toByte(), 0x64.toByte(), 0xbe.toByte(), 0x0b.toByte(), 0x94.toByte(),
        0xed.toByte(), 0x2e.toByte(), 0x17.toByte(), 0x14.toByte(), 0xed.toByte(), 0x07.toByte(), 0x90.toByte(), 0xff.toByte(),
        0x2f.toByte(), 0x43.toByte(), 0x25.toByte(), 0xbe.toByte(), 0x42.toByte(), 0xd9.toByte(), 0x96.toByte(), 0xc4.toByte(),
        0xb1.toByte(), 0x26.toByte(), 0xe2.toByte(), 0xdb.toByte(), 0x35.toByte(), 0x5d.toByte(), 0x79.toByte(), 0xd2.toByte()
    )

    val devLegacyKey: String?
        get() = if (BuildConfig.DEBUG) {
            val decryptedBytes = ByteArray(debugObfuscatedKey.size)
            for (i in debugObfuscatedKey.indices) {
                decryptedBytes[i] = (debugObfuscatedKey[i].toInt() xor debugKeyMask[i].toInt()).toByte()
            }
            String(decryptedBytes, Charsets.UTF_8)
        } else {
            null
        }
}
