package com.docfold.scanner.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.docfold.scanner.data.Doc
import com.docfold.scanner.pdf.ExportOptions
import com.docfold.scanner.pdf.Layout
import com.docfold.scanner.pdf.Margin
import com.docfold.scanner.pdf.PaperSize
import com.docfold.scanner.pdf.Quality
import com.docfold.scanner.ui.AppViewModel
import com.docfold.scanner.ui.theme.DocIcons
import com.docfold.scanner.ui.theme.Mono

/** File size choices, named the way upload portals phrase their limits. */
private enum class SizeChoice(val label: String, val limit: Long?, val quality: Quality?) {
    Original("Original", null, Quality.Original),
    Standard("Standard", null, Quality.Balanced),
    Under2("Under 2 MB", 2_000_000, null),
    Under1("Under 1 MB", 1_000_000, null),
    Under500("Under 500 KB", 500_000, null),
    Under200("Under 200 KB", 200_000, null),
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExportSheet(vm: AppViewModel, doc: Doc, onDismiss: () -> Unit, onSave: (ExportOptions) -> Unit, onShare: (ExportOptions) -> Unit) {
    var name by rememberSaveable { mutableStateOf(doc.name) }
    var size by rememberSaveable { mutableStateOf(SizeChoice.Standard) }
    var paper by rememberSaveable { mutableStateOf(PaperSize.Auto) }
    var layout by rememberSaveable { mutableStateOf(Layout.OnePerPage) }
    var margin by rememberSaveable { mutableStateOf(Margin.None) }
    var searchable by rememberSaveable { mutableStateOf(false) }
    var protect by rememberSaveable { mutableStateOf(false) }
    var password by rememberSaveable { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var more by rememberSaveable { mutableStateOf(false) }

    // Live estimate: re-measured whenever the size choice changes.
    var quality by remember { mutableStateOf(size.quality ?: Quality.Balanced) }
    var estimate by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(size, doc.pages) {
        estimate = null
        val limit = size.limit
        if (limit == null) {
            quality = size.quality ?: Quality.Balanced
            estimate = vm.exporter.estimate(doc, quality)
        } else {
            val (q, e) = vm.exporter.qualityFor(doc, limit)
            quality = q; estimate = e
        }
    }

    fun options() = ExportOptions(
        paper = paper, margin = margin, quality = quality, layout = layout,
        password = password.takeIf { protect && it.isNotEmpty() }, searchable = searchable,
    )
    fun commitName() { if (name.isNotBlank() && name != doc.name) vm.rename(doc.id, name) }
    val ready = !protect || password.length >= 4

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).navigationBarsPadding().imePadding().padding(bottom = 16.dp),
        ) {
            Text("Save as PDF", style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(name, { name = it }, label = { Text("File name") }, singleLine = true, modifier = Modifier.fillMaxWidth())

            Spacer(Modifier.height(20.dp))
            Text("File size", style = MaterialTheme.typography.titleMedium)
            Text("Upload sites often set a limit. Pick it and Docfold fits the PDF under it.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SizeChoice.entries.forEach { c -> FilterChip(size == c, { size = c }, { Text(c.label) }, leadingIcon = checkIf(size == c)) }
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val e = estimate
                val limit = size.limit
                val fits = e != null && (limit == null || e <= limit)
                Text(
                    when {
                        e == null -> "Measuring…"
                        limit != null && !fits -> "Smallest possible is about ${formatSize(e)}. Try removing pages."
                        limit != null -> "About ${formatSize(e)} · fits under ${formatSize(limit)}"
                        else -> "About ${formatSize(e)} · ${plural(doc.pages.size, "page")} · ${quality.label} quality"
                    },
                    style = Mono.medium,
                    color = if (e != null && limit != null && !fits) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            TextButton({ more = !more }, Modifier.padding(vertical = 4.dp)) {
                Text(if (more) "Fewer options" else "More options: page size, ID card, password, text")
                Spacer(Modifier.width(6.dp))
                Icon(DocIcons.ChevronDown, null, Modifier.size(18.dp))
            }
            AnimatedVisibility(more) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    OptionRow("Page size") { PaperSize.entries.forEach { FilterChip(paper == it, { paper = it }, { Text(it.label) }, leadingIcon = checkIf(paper == it)) } }
                    OptionRow("Layout", "ID card prints front and back at real card size on one sheet.") {
                        Layout.entries.forEach { FilterChip(layout == it, { layout = it }, { Text(it.label) }, leadingIcon = checkIf(layout == it)) }
                    }
                    OptionRow("Margins") { Margin.entries.forEach { FilterChip(margin == it, { margin = it }, { Text(it.label) }, leadingIcon = checkIf(margin == it)) } }
                    SwitchRow("Searchable text", "Lets you search and copy words in the PDF. Takes a little longer.", searchable) { searchable = it }
                    SwitchRow("Password", "Anyone opening the PDF must type it. AES-256.", protect) { protect = it }
                    AnimatedVisibility(protect) {
                        OutlinedTextField(
                            password, { password = it }, label = { Text("Password (at least 4 characters)") }, singleLine = true,
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = { TextButton({ showPassword = !showPassword }) { Text(if (showPassword) "Hide" else "Show") } },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Button({ commitName(); onSave(options()) }, Modifier.fillMaxWidth().height(58.dp), enabled = ready) {
                Icon(DocIcons.Download, null); Spacer(Modifier.width(10.dp)); Text("Save to phone", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton({ commitName(); onShare(options()) }, Modifier.fillMaxWidth().height(54.dp), enabled = ready) {
                Icon(DocIcons.Share, null); Spacer(Modifier.width(10.dp)); Text("Share", maxLines = 1)
            }
        }
    }
}

/** A tick on the chosen chip, so the selection never relies on a faint tint alone. */
private fun checkIf(on: Boolean): (@Composable () -> Unit)? =
    if (on) ({ Icon(DocIcons.Check, null, Modifier.size(18.dp)) }) else null

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OptionRow(title: String, help: String? = null, chips: @Composable () -> Unit) {
    Column {
        Text(title, style = MaterialTheme.typography.titleSmall)
        help?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { chips() }
    }
}

@Composable
private fun SwitchRow(title: String, help: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    // The whole row toggles, not just the small switch: a bigger, clearer target.
    Row(
        Modifier.fillMaxWidth().toggleable(checked, role = Role.Switch, onValueChange = onChange).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(help, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked, onCheckedChange = null)
    }
}
