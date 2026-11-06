package com.docfold.scanner.ui.screens

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.clickable
import androidx.compose.ui.unit.dp
import com.docfold.scanner.ui.theme.DocIcons
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatSize(bytes: Long): String = when {
    bytes >= 1_000_000 -> String.format(Locale.getDefault(), "%.1f MB", bytes / 1_000_000.0).replace(Regex("[.,]0 MB$"), " MB")
    bytes >= 1_000 -> "${bytes / 1_000} KB"
    else -> "$bytes B"
}

/** "TODAY 4:42 PM", "YESTERDAY", "28 OCT", "28 OCT 2025" — postal-date style. */
fun postalDate(ms: Long, withTime: Boolean = true): String {
    val now = System.currentTimeMillis()
    val time = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(ms))
    val day = when {
        DateUtils.isToday(ms) -> if (withTime) "Today $time" else "Today"
        DateUtils.isToday(ms + DateUtils.DAY_IN_MILLIS) -> "Yesterday"
        SimpleDateFormat("yyyy", Locale.ROOT).format(Date(ms)) == SimpleDateFormat("yyyy", Locale.ROOT).format(Date(now)) ->
            SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(ms))
        else -> SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(ms))
    }
    return day.uppercase(Locale.getDefault())
}

fun plural(n: Int, one: String, many: String = one + "s") = "$n ${if (n == 1) one else many}"

/** Text buttons that aren't the next step stay in ink: airmail red is kept for "do this". */
@Composable
fun quietTextButton() = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)

/** The hairline between a bottom bar and the sheet above it. */
@Composable
fun Modifier.topHairline(): Modifier {
    val line = MaterialTheme.colorScheme.outlineVariant
    return drawWithContent {
        drawContent()
        drawLine(line, Offset.Zero, Offset(size.width, 0f), 2.dp.toPx())
    }
}

/** The "add pages" chooser: scanner first, then photos, then a PDF. Each choice says what it does. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPagesSheet(onDismiss: () -> Unit, onScan: () -> Unit, onPhotos: () -> Unit, onPdf: () -> Unit, onCamera: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = 20.dp).navigationBarsPadding().padding(bottom = 16.dp)) {
            Text("Add pages", style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(12.dp))
            SheetChoice(DocIcons.Scan, "Scan with camera", "Edges and shadows are fixed for you", MaterialTheme.colorScheme.primary) { onDismiss(); onScan() }
            SheetChoice(DocIcons.Photo, "Choose photos", "Pick pictures of documents from your gallery", MaterialTheme.colorScheme.secondary) { onDismiss(); onPhotos() }
            SheetChoice(DocIcons.Pdf, "Import a PDF", "Add its pages — also how you merge PDFs", MaterialTheme.colorScheme.tertiary) { onDismiss(); onPdf() }
            SheetChoice(DocIcons.Camera, "Basic camera", "Works offline on any phone — you set the corners", MaterialTheme.colorScheme.onSurfaceVariant) { onDismiss(); onCamera() }
        }
    }
}

/** Picks an existing folder, none, or a new one by typing its name. Folders need no separate creating step. */
@Composable
fun FolderDialog(folders: List<String>, current: String, onDismiss: () -> Unit, onMove: (String) -> Unit) {
    var choice by remember { mutableStateOf(current) }
    var typed by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Move to folder") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                (listOf("") + folders).forEach { folder ->
                    val on = folder == choice && typed.isBlank()
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(on, role = Role.RadioButton) { choice = folder; typed = "" },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(on, onClick = null)
                        Spacer(Modifier.width(12.dp))
                        Text(folder.ifEmpty { "No folder" }, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(typed, { typed = it.take(40) }, label = { Text("New folder name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { TextButton({ onMove(typed.trim().ifEmpty { choice }) }) { Text("Move") } },
        dismissButton = { TextButton(onDismiss) { Text("Cancel") } },
    )
}

@Composable
fun SheetChoice(icon: ImageVector, title: String, detail: String, tint: Color, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable(role = Role.Button, onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(color = tint.copy(alpha = 0.12f), shape = MaterialTheme.shapes.medium, modifier = Modifier.size(48.dp)) {
            Icon(icon, null, tint = tint, modifier = Modifier.padding(12.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
