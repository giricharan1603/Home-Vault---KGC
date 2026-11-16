package com.homevault.app.scanner

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executors

object ReceiptOcrHelper {
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    @OptIn(ExperimentalGetImage::class)
    fun captureAndRecognize(
        imageCapture: ImageCapture,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        imageCapture.takePicture(cameraExecutor, object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(imageProxy: ImageProxy) {
                val mediaImage = imageProxy.image
                if (mediaImage == null) {
                    imageProxy.close()
                    onError("Camera frame was empty.")
                    return
                }

                val inputImage = InputImage.fromMediaImage(
                    mediaImage,
                    imageProxy.imageInfo.rotationDegrees
                )

                recognizer.process(inputImage)
                    .addOnSuccessListener { visionText ->
                        onSuccess(visionText.text)
                    }
                    .addOnFailureListener { e ->
                        onError(e.localizedMessage ?: "Text recognition failed.")
                    }
                    .addOnCompleteListener {
                        // Guaranteed buffer release to prevent CameraX pipeline freeze
                        imageProxy.close()
                    }
            }

            override fun onError(exception: ImageCaptureException) {
                onError(exception.localizedMessage ?: "Failed to capture image.")
            }
        })
    }

    suspend fun recognizeFromSandboxedFile(context: Context, file: File): String =
        withContext(Dispatchers.IO) {
            val image = InputImage.fromFilePath(context, Uri.fromFile(file))
            recognizer.process(image).await().text
        }
}