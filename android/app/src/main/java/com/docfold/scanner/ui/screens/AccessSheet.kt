package com.docfold.scanner.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.docfold.scanner.ui.Access
import com.docfold.scanner.ui.AccessPrompt
import com.docfold.scanner.ui.theme.DocIcons

private class Ask(val icon: ImageVector, val tint: Color, val title: String, val body: String, val button: String)

/** Docfold's own questions about access. Android's prompts appear on their own and need nothing here. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccessSheet(access: Access) {
    val prompt = access.prompt ?: return
    val red = MaterialTheme.colorScheme.primary
    val ask = when (prompt) {
        AccessPrompt.CameraWhy -> Ask(DocIcons.Camera, red, "Allow the camera to scan", "Docfold uses the camera only to scan your pages. Your scans stay on this phone.", "Allow camera")
        AccessPrompt.CameraBlocked -> Ask(DocIcons.Camera, red, "Turn on the camera", "To scan, open Settings, tap Permissions, then allow Camera. You can still import photos or a PDF.", "Open settings")
        AccessPrompt.SaveFiles -> Ask(
            DocIcons.Download, MaterialTheme.colorScheme.secondary, "Save files to this phone?",
            "PDFs go to Downloads › Docfold and page images to Pictures › Docfold, where your Files and Gallery apps can find them. Nothing is uploaded.", "Allow",
        )
    }
    ModalBottomSheet(onDismissRequest = access::close, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = 20.dp).navigationBarsPadding().padding(bottom = 16.dp)) {
            Surface(color = ask.tint.copy(alpha = 0.12f), shape = MaterialTheme.shapes.medium, modifier = Modifier.size(56.dp)) {
                Icon(ask.icon, null, tint = ask.tint, modifier = Modifier.padding(14.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(ask.title, style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(10.dp))
            Text(ask.body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))
            Button(access::accept, Modifier.fillMaxWidth().height(58.dp)) {
                Icon(ask.icon, null, Modifier.size(22.dp)); Spacer(Modifier.width(10.dp)); Text(ask.button, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(access::decline, Modifier.fillMaxWidth().height(54.dp)) { Text("Not now") }
        }
    }
}
