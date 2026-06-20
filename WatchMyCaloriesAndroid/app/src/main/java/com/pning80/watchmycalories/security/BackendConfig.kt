package com.pning80.watchmycalories.security

import com.pning80.watchmycalories.BuildConfig

/**
 * Cloud Run backend configuration (PORTING_CRITERIA.md T1.5).
 *
 * The Android client no longer holds a Gemini API key — all Gemini requests
 * are proxied through the backend. Production builds use Play Integrity
 * attestation (T1.8); dev builds may fall back to a legacy `x-backend-key`
 * for emulators (mirrors the iOS App Attest simulator fallback).
 *
 * The dev legacy key is **not** hardcoded. The single source of truth is
 * `Backend/.env.dev` (`APP_BACKEND_API_KEY`); `scripts/sync-dev-backend-key.sh`
 * copies it into `local.properties`, which `build.gradle.kts` surfaces as
 * `BuildConfig.APP_BACKEND_API_KEY`. Absent it, debug builds simply have no
 * legacy key (and must rely on Play Integrity).
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
    // or fails in debug builds, and only against the dev backend. Null in release
    // and whenever the key wasn't provided via local.properties.
    val devLegacyKey: String?
        get() = BuildConfig.APP_BACKEND_API_KEY.takeIf {
            BuildConfig.DEBUG && it.isNotBlank()
        }
}
