package com.docfold.scanner.ui.screens

import android.content.Intent
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.docfold.scanner.data.Doc
import com.docfold.scanner.data.Page
import com.docfold.scanner.imaging.FormImage
import com.docfold.scanner.ui.AppViewModel
import com.docfold.scanner.ui.theme.DocIcons
import com.docfold.scanner.ui.theme.Mono
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** Starting points only: every form sets its own numbers, so the fields stay editable. */
private enum class FormSize(val label: String, val width: Int, val height: Int, val minKb: Int, val maxKb: Int) {
    Photo("Photo", 200, 230, 20, 50),
    Signature("Signature", 140, 60, 10, 20),
}

/** Turns a page into the exact image an online form asks for: fixed pixels, under a file size. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormImageSheet(vm: AppViewModel, doc: Doc, page: Page, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var widthText by rememberSaveable { mutableStateOf(FormSize.Photo.width.toString()) }
    var heightText by rememberSaveable { mutableStateOf(FormSize.Photo.height.toString()) }
    var minText by rememberSaveable { mutableStateOf(FormSize.Photo.minKb.toString()) }
    var maxText by rememberSaveable { mutableStateOf(FormSize.Photo.maxKb.toString()) }
    val width = widthText.toIntOrNull()?.takeIf { it in 16..4000 }
    val height = heightText.toIntOrNull()?.takeIf { it in 16..4000 }
    val maxKb = maxText.toLongOrNull()?.takeIf { it in 1..20_000 }
    val minKb = if (minText.isBlank()) 0L else minText.toLongOrNull()?.takeIf { it <= (maxKb ?: 0) }
    val valid = width != null && height != null && minKb != null && maxKb != null
    var result by remember { mutableStateOf<FormImage.Result?>(null) }
    LaunchedEffect(page.id, page.rev, page.rotation, width, height, minKb, maxKb) {
        result = null
        if (width == null || height == null || minKb == null || maxKb == null) return@LaunchedEffect
        delay(250) // the fields change per keystroke
        result = withContext(Dispatchers.IO) {
            runCatching { FormImage.render(vm.store.pageFile(doc.id, page.id), page.rotation, width, height, minKb * 1000, maxKb * 1000) }.getOrNull()
        }
    }
    val preview = remember(result) { result?.bytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() } }
    val saveFile = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/jpeg")) { uri ->
        val bytes = result?.bytes
        if (uri != null && bytes != null) { vm.saveBytes(uri, bytes); onDismiss() }
    }
    fun share(bytes: ByteArray, name: String) = vm.shareBytes(name, bytes) { uri ->
        val send = Intent(Intent.ACTION_SEND).setType("image/jpeg").putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(send, "Send ${doc.name}"))
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).navigationBarsPadding().padding(bottom = 16.dp)) {
            Text("Resize for a form", style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(8.dp))
            Text(
                "Type the size your form asks for. Crop the page first, so only the photo or signature shows.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FormSize.entries.forEach { s ->
                    val on = width == s.width && height == s.height && minKb == s.minKb.toLong() && maxKb == s.maxKb.toLong()
                    FilterChip(on, {
                        widthText = s.width.toString(); heightText = s.height.toString(); minText = s.minKb.toString(); maxText = s.maxKb.toString()
                    }, { Text(s.label) }, leadingIcon = checkIf(on))
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Number("Width px", widthText, Modifier.weight(1f)) { widthText = it }
                Number("Height px", heightText, Modifier.weight(1f)) { heightText = it }
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Number("Min KB", minText, Modifier.weight(1f)) { minText = it }
                Number("Max KB", maxText, Modifier.weight(1f)) { maxText = it }
            }
            Spacer(Modifier.height(16.dp))
            Box(Modifier.fillMaxWidth().heightIn(min = 150.dp), contentAlignment = Alignment.Center) {
                preview?.let {
                    Image(
                        it, "Preview of the resized image", contentScale = ContentScale.Fit,
                        modifier = Modifier.heightIn(max = 190.dp).border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)),
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            val r = result
            Text(
                when {
                    !valid -> "Type a width, height and size, with Min KB below Max KB."
                    r == null -> "Measuring…"
                    !r.fits -> "Smallest is ${formatSize(r.bytes.size.toLong())}. Allow more KB, or a smaller picture."
                    else -> "$width × $height px · ${formatSize(r.bytes.size.toLong())}"
                },
                style = Mono.medium,
                color = if (r != null && !r.fits) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            val done = r != null && width != null && height != null
            Button({ if (done) saveFile.launch(vm.formImageName(doc, width!!, height!!)) }, Modifier.fillMaxWidth().height(58.dp), enabled = done) {
                Icon(DocIcons.Download, null); Spacer(Modifier.width(10.dp)); Text("Save image", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton({ if (done) share(r!!.bytes, vm.formImageName(doc, width!!, height!!)) }, Modifier.fillMaxWidth().height(54.dp), enabled = done) {
                Icon(DocIcons.Share, null); Spacer(Modifier.width(10.dp)); Text("Share", maxLines = 1)
            }
        }
    }
}

@Composable
private fun Number(label: String, value: String, modifier: Modifier, onChange: (String) -> Unit) {
    OutlinedTextField(
        value, { onChange(it.filter(Char::isDigit).take(5)) }, label = { Text(label, maxLines = 1) }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = modifier,
    )
}
