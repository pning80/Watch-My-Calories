package com.pning80.watchmycalories.security

object BackendConfig {
    
    // Remote endpoint mirroring iOS backend config
    val baseURL = "https://watchmycalories-backend-dev-657698311127.us-central1.run.app"

    // Placeholder until remote key vault is implemented or using a buildconfig field
    const val GEMINI_API_KEY = "YOUR_API_KEY"
}
