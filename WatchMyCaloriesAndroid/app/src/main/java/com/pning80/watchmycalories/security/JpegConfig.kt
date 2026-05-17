package com.pning80.watchmycalories.security

/**
 * JPEG encode quality used everywhere we write a JPEG (camera capture
 * persistence + Gemini request payload). Matches iOS's 0.8 quality value
 * from `CameraManager.swift` / `EstimationReviewView.swift`, scaled to
 * Android's 0..100 range. See PORTING_CRITERIA.md T1.4.
 *
 * If iOS ever changes its encode quality, change this value to match.
 */
object JpegConfig {
    const val QUALITY = 80
}
