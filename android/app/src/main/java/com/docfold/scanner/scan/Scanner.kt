package com.docfold.scanner.scan

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.docfold.scanner.imaging.DeviceProfile
import com.google.mlkit.vision.documentscanner.GmsDocumentScanner
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult

/**
 * Google's on-device document scanner (edge detection, auto-capture, perspective fix, clean-up),
 * delivered by Play services so it adds almost nothing to the app's size and needs no camera permission.
 * Phones it doesn't support (under ~1.7 GB RAM or without Play services) fall back to the system camera
 * plus Docfold's own corner crop.
 */
object Scanner {
    private val client: GmsDocumentScanner by lazy {
        GmsDocumentScanning.getClient(
            GmsDocumentScannerOptions.Builder()
                .setGalleryImportAllowed(true)
                .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
                .setScannerMode(
                    if (DeviceProfile.lowRam) GmsDocumentScannerOptions.SCANNER_MODE_BASE_WITH_FILTER
                    else GmsDocumentScannerOptions.SCANNER_MODE_FULL
                )
                .build()
        )
    }

    fun start(activity: Activity, launcher: ActivityResultLauncher<IntentSenderRequest>, onUnavailable: (Exception) -> Unit) {
        client.getStartScanIntent(activity)
            .addOnSuccessListener { launcher.launch(IntentSenderRequest.Builder(it).build()) }
            .addOnFailureListener(onUnavailable)
    }

    fun pages(resultCode: Int, data: Intent?): List<Uri> {
        if (resultCode != Activity.RESULT_OK) return emptyList()
        return GmsDocumentScanningResult.fromActivityResultIntent(data)?.pages?.map { it.imageUri }.orEmpty()
    }
}
