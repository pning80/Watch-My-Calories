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

    /**
     * Dev-only legacy backend key. Used only when Play Integrity is unsupported
     * (e.g., on an emulator without Play Services) and only against the dev
     * backend. Sourced from local.properties via BuildConfig — never committed.
     * Returns null in release builds; in that case the only auth path is
     * Play Integrity (T1.8).
     */
    val devLegacyKey: String?
        get() = if (BuildConfig.DEBUG) BuildConfig.APP_BACKEND_API_KEY.ifBlank { null } else null
}
