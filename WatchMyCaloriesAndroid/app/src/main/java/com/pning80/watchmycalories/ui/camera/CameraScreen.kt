package com.pning80.watchmycalories.ui.camera

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.provider.Settings
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import android.view.HapticFeedbackConstants
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.pning80.watchmycalories.utils.AccessibilityTags
import java.util.concurrent.Executor

/**
 * Capture mode for [CameraScreen]. Mirrors iOS's split between food capture
 * (multi-photo bundle) and menu capture (single OCR-friendly frame).
 */
enum class CaptureMode { Food, Menu }

@Composable
fun CameraScreen(
    onPhotosCaptured: (List<Bitmap>) -> Unit,
    captureMode: CaptureMode = CaptureMode.Food,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val view = LocalView.current
    
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }

    if (hasCameraPermission) {
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }

                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        // Bind after the PreviewView is laid out so its ViewPort
                        // (size + FILL_CENTER scale type) is available.
                        previewView.post {
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }

                            imageCapture = ImageCapture.Builder().build()

                            // Group Preview + ImageCapture under the preview's ViewPort
                            // so the captured frame is cropped to exactly what's visible
                            // on screen (WYSIWYG). Without this, ImageCapture returns the
                            // full 4:3 sensor frame while the preview center-crops to fill
                            // the tall screen — so the review showed a much wider shot than
                            // the user framed. The resulting crop arrives as image.cropRect.
                            val groupBuilder = UseCaseGroup.Builder()
                                .addUseCase(preview)
                                .addUseCase(imageCapture!!)
                            previewView.viewPort?.let { groupBuilder.setViewPort(it) }

                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    groupBuilder.build()
                                )
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                }
            )

            // Top + bottom scrim gradients so the status bar and shutter stay
            // legible over any frame — mirrors iOS CameraView.swift:91-99
            // (top 60%→clear 100pt, bottom clear→80% 160pt).
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent)
                            )
                        )
                )
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                            )
                        )
                )
            }

            // Shutter — white ring + mint inner fill, mirroring iOS
            // CameraView.swift:109-117 (80pt white-stroke ring, 68pt cwPrimary
            // inner). Replaces the generic flat-white circle.
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .imePadding()
                    .padding(bottom = 80.dp)
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .padding(6.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .testTag(AccessibilityTags.Camera.CAPTURE_BUTTON)
                    .clickable {
                        // Mirror iOS UIImpactFeedbackGenerator(.heavy) on capture.
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        val executor = ContextCompat.getMainExecutor(context)
                        imageCapture?.takePicture(executor, object : ImageCapture.OnImageCapturedCallback() {
                            override fun onCaptureSuccess(image: ImageProxy) {
                                val buffer = image.planes[0].buffer
                                val bytes = ByteArray(buffer.capacity())
                                buffer.get(bytes)
                                val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, null)
                                // CameraX delivers the full sensor JPEG plus (a) a cropRect
                                // from the ViewPort marking the on-screen region and (b) the
                                // clockwise rotation to bring it upright. The bitmap is consumed
                                // in-memory (cameraReview → analysis) with no EXIF round-trip,
                                // so we must apply both here, in sensor space then rotation —
                                // otherwise the photo is wider than framed and/or sideways.
                                val crop = image.cropRect
                                val rotation = image.imageInfo.rotationDegrees
                                image.close()

                                val framed = if (
                                    crop.left >= 0 && crop.top >= 0 &&
                                    crop.width() in 1..decoded.width &&
                                    crop.height() in 1..decoded.height &&
                                    (crop.width() != decoded.width || crop.height() != decoded.height)
                                ) {
                                    Bitmap.createBitmap(decoded, crop.left, crop.top, crop.width(), crop.height())
                                } else {
                                    decoded
                                }

                                val bitmap = if (rotation != 0) {
                                    val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
                                    Bitmap.createBitmap(
                                        framed, 0, 0, framed.width, framed.height, matrix, true
                                    )
                                } else {
                                    framed
                                }
                                // Single-shot for BOTH modes — mirrors iOS, which
                                // captures one photo per session and routes straight
                                // to review (CameraView.swift onChange of
                                // capturedImages.count). Food mode previously
                                // accumulated a bundle behind an "Analyze N" button;
                                // that was an Android-only divergence (D-003) and is
                                // removed for parity.
                                onPhotosCaptured(listOf(bitmap))
                            }
                            override fun onError(exception: ImageCaptureException) {
                                exception.printStackTrace()
                            }
                        })
                    },
            )

            if (captureMode == CaptureMode.Menu) {
                // OCR-friendly hint overlay (D-003b — single-photo menu capture).
                Text(
                    "Align the menu within the frame",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 80.dp)
                        .background(Color.Black.copy(alpha = 0.45f))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Camera Permission Required",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Watch My Calories needs access to your camera to capture food photos and analyze their calorie content.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { permissionLauncher.launch(android.Manifest.permission.CAMERA) }
            ) {
                Text("Grant Camera Permission")
            }
            // Deep-link to system app settings — covers the "Don't ask again"
            // case where the in-app request above is a silent no-op. iOS only
            // offers Open Settings (CameraView.swift:182-188); Android keeps
            // both since a first denial can still be re-prompted in-app.
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = {
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null),
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                }
            ) {
                Text("Open Settings")
            }
        }
    }
}
