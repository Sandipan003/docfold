package com.docfold.scanner.ocr

import android.content.Context
import android.graphics.Bitmap
import com.docfold.scanner.pdf.TextLine
import com.google.android.gms.common.moduleinstall.InstallStatusListener
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.android.gms.common.moduleinstall.ModuleInstallStatusUpdate
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume

/** On-device text recognition (Latin scripts). The model is fetched once by Play services on first use. */
object Ocr {
    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    /** Makes sure the recogniser model is on the phone. Returns false when it can't be downloaded (offline). */
    suspend fun ensureReady(context: Context): Boolean {
        val client = ModuleInstall.getClient(context)
        if (runCatching { client.areModulesAvailable(recognizer).await().areModulesAvailable() }.getOrDefault(false)) return true
        return suspendCancellableCoroutine { cont ->
            lateinit var listener: InstallStatusListener
            fun finish(ok: Boolean) {
                client.unregisterListener(listener)
                if (cont.isActive) cont.resume(ok)
            }
            listener = InstallStatusListener { update ->
                when (update.installState) {
                    ModuleInstallStatusUpdate.InstallState.STATE_COMPLETED -> finish(true)
                    ModuleInstallStatusUpdate.InstallState.STATE_FAILED,
                    ModuleInstallStatusUpdate.InstallState.STATE_CANCELED -> finish(false)
                    else -> Unit
                }
            }
            client.installModules(ModuleInstallRequest.newBuilder().addApi(recognizer).setListener(listener).build())
                .addOnSuccessListener { if (it.areModulesAlreadyInstalled()) finish(true) }
                .addOnFailureListener { finish(false) }
        }
    }

    private suspend fun run(bitmap: Bitmap): Text = recognizer.process(InputImage.fromBitmap(bitmap, 0)).await()

    suspend fun lines(bitmap: Bitmap): List<TextLine> =
        run(bitmap).textBlocks.flatMap { it.lines }.mapNotNull { l -> l.boundingBox?.let { TextLine(l.text, it) } }

    suspend fun text(bitmap: Bitmap): String = run(bitmap).text
}
