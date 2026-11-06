package com.docfold.scanner.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/** A question Docfold shows itself, as opposed to Android's own permission prompts. */
enum class AccessPrompt { CameraWhy, CameraBlocked, SaveFiles }

/** The storage permission this Android version has, or null from Android 13, which has none for saving files. */
fun storagePermission(sdk: Int = Build.VERSION.SDK_INT): String? = when {
    sdk <= Build.VERSION_CODES.P -> Manifest.permission.WRITE_EXTERNAL_STORAGE
    sdk <= Build.VERSION_CODES.S_V2 -> Manifest.permission.READ_EXTERNAL_STORAGE
    else -> null
}

/**
 * Asks for the camera and for saving to the phone the first time each is needed. The camera is Android's own prompt
 * on every version. Saving is Android's own prompt up to Android 12; from Android 13 there is no such permission, so
 * Docfold asks itself and remembers the answer. Saying no is never a dead end: a PDF can still go through the system
 * "Save as" screen, and photos and PDFs can still be imported.
 */
class Access internal constructor(private val activity: Activity, private val vm: AppViewModel) {
    var prompt by mutableStateOf<AccessPrompt?>(null)
        private set

    internal lateinit var cameraRequest: ManagedActivityResultLauncher<String, Boolean>
    internal lateinit var storageRequest: ManagedActivityResultLauncher<String, Boolean>
    private var afterCamera: (() -> Unit)? = null
    private var afterSaving: (() -> Unit)? = null
    private var insteadOfSaving: (() -> Unit)? = null

    private fun granted(permission: String) = ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
    private val cameraRationale get() = ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)

    val cameraAllowed get() = granted(Manifest.permission.CAMERA)
    val savingAllowed get() = storagePermission()?.let(::granted) ?: vm.savingAllowed

    /** Runs [then] once the camera is allowed. After a first refusal it explains why before asking again. */
    fun camera(then: () -> Unit) {
        if (cameraAllowed) return then()
        afterCamera = then
        if (cameraRationale) prompt = AccessPrompt.CameraWhy else cameraRequest.launch(Manifest.permission.CAMERA)
    }

    internal fun onCameraAnswer(ok: Boolean) {
        val then = afterCamera
        afterCamera = null
        when {
            ok -> then?.invoke()
            cameraRationale -> vm.notify("Camera is off, so scanning can't start. You can still import photos or a PDF.")
            else -> prompt = AccessPrompt.CameraBlocked
        }
    }

    /** Runs [allowed] if files may be saved to the phone, [declined] if the user would rather not. */
    fun saving(allowed: () -> Unit, declined: () -> Unit) {
        if (savingAllowed) return allowed()
        afterSaving = allowed
        insteadOfSaving = declined
        val permission = storagePermission()
        if (permission != null) storageRequest.launch(permission) else prompt = AccessPrompt.SaveFiles
    }

    internal fun onSavingAnswer(ok: Boolean) {
        val next = if (ok) afterSaving else insteadOfSaving
        afterSaving = null
        insteadOfSaving = null
        next?.invoke()
    }

    /** The main button on Docfold's own question. */
    fun accept() {
        when (prompt) {
            AccessPrompt.CameraWhy -> cameraRequest.launch(Manifest.permission.CAMERA)
            AccessPrompt.CameraBlocked -> openAppSettings()
            AccessPrompt.SaveFiles -> { vm.allowSaving(true); onSavingAnswer(true) }
            null -> Unit
        }
        prompt = null
    }

    /** "Not now" on Docfold's own question. */
    fun decline() {
        if (prompt == AccessPrompt.SaveFiles) onSavingAnswer(false)
        close()
    }

    /** The sheet was swiped away: nothing was decided, so nothing happens. */
    fun close() {
        afterCamera = null
        afterSaving = null
        insteadOfSaving = null
        prompt = null
    }

    fun openAppSettings() {
        activity.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", activity.packageName, null)))
    }
}

val LocalAccess = staticCompositionLocalOf<Access> { error("No Access provided") }

@Composable
fun rememberAccess(vm: AppViewModel, activity: Activity): Access {
    val access = remember { Access(activity, vm) }
    access.cameraRequest = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission(), access::onCameraAnswer)
    access.storageRequest = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission(), access::onSavingAnswer)
    return access
}
