package com.docfold.scanner.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.docfold.scanner.ads.BannerAd
import com.docfold.scanner.data.Doc
import com.docfold.scanner.ui.AddActions
import com.docfold.scanner.ui.AppViewModel
import com.docfold.scanner.ui.LocalSnackbar
import com.docfold.scanner.ui.Screen
import com.docfold.scanner.ui.components.AirmailBand
import com.docfold.scanner.ui.components.EmptyEnvelope
import com.docfold.scanner.ui.components.PageImage
import com.docfold.scanner.ui.components.airmailEdge
import com.docfold.scanner.ui.components.perforatedPaper
import com.docfold.scanner.ui.components.Postmark
import com.docfold.scanner.ui.theme.ArchivoCondensed
import com.docfold.scanner.ui.theme.DocIcons
import com.docfold.scanner.ui.theme.LocalDark
import com.docfold.scanner.ui.theme.LocalInks
import com.docfold.scanner.ui.theme.Mono
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(vm: AppViewModel, actions: AddActions) {
    val all by vm.store.docs.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    var showAdd by remember { mutableStateOf(false) }
    val selected = remember { mutableStateListOf<String>() }
    var confirmDelete by remember { mutableStateOf(false) }
    var filing by remember { mutableStateOf(false) }
    var folder by rememberSaveable { mutableStateOf<String?>(null) }
    val folders = remember(all) { vm.folders() }
    // A folder lasts only while a document is in it, so its filter can outlive it.
    LaunchedEffect(folders) { if (folder !in folders) folder = null }
    val docs = remember(all, query, folder, vm.hiddenDocs.toList()) {
        all.orEmpty().filter {
            it.id !in vm.hiddenDocs && (folder == null || it.folder == folder) && (query.isBlank() || it.name.contains(query.trim(), ignoreCase = true))
        }
    }
    val list = rememberLazyListState()

    Scaffold(
        snackbarHost = { SnackbarHost(LocalSnackbar.current) },
        contentWindowInsets = WindowInsets(0),
        floatingActionButton = {
            if (selected.isEmpty() && all?.isNotEmpty() == true) {
                ExtendedFloatingActionButton(
                    onClick = { actions.scan(null) },
                    icon = { Icon(DocIcons.Scan, null, Modifier.size(26.dp)) },
                    text = { Text("Scan", style = MaterialTheme.typography.titleLarge) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    // Clear space above the banner so a hurried thumb never lands on the ad.
                    modifier = Modifier.padding(bottom = 16.dp).height(64.dp).semantics { contentDescription = "Scan" },
                )
            }
        },
        bottomBar = { Column(Modifier.navigationBarsPadding()) { BannerAd() } },
    ) { pad ->
        LazyColumn(
            state = list,
            modifier = Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(bottom = 112.dp),
        ) {
            item(key = "head") {
                Column(Modifier.statusBarsPadding()) {
                    AirmailBand()
                    if (selected.isEmpty()) Header(vm, all.orEmpty(), query, { query = it }, folders, folder, { folder = it }, onImport = { showAdd = true })
                    else SelectionBar(selected.size, onClose = { selected.clear() }, onMerge = {
                        val order = all.orEmpty().filter { it.id in selected }.sortedBy { it.created }.map { it.id }
                        selected.clear(); vm.merge(order)
                    }, onFolder = { filing = true }, onDelete = { confirmDelete = true })
                }
            }
            when {
                all == null -> Unit
                all!!.isEmpty() -> item(key = "empty") { EmptyState(onScan = { actions.scan(null) }, onImport = { showAdd = true }) }
                docs.isEmpty() -> item(key = "nores") {
                    Text("No document called “$query”.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(24.dp))
                }
                else -> {
                    stickyHeader(key = "guide") { GuideWords(docs, list) }
                    items(docs, key = { it.id }) { doc ->
                        Envelope(
                            vm, doc, selected = doc.id in selected,
                            onClick = {
                                if (selected.isNotEmpty()) { if (doc.id in selected) selected.remove(doc.id) else selected.add(doc.id) }
                                else vm.open(Screen.Document(doc.id))
                            },
                            onLongClick = { if (doc.id !in selected) selected.add(doc.id) },
                            modifier = Modifier.animateItem().padding(horizontal = 16.dp, vertical = 6.dp),
                        )
                    }
                }
            }
        }
    }

    if (showAdd) AddPagesSheet({ showAdd = false }, { actions.scan(null) }, { actions.photos(null) }, { actions.pdf(null) }, { actions.camera(null) })
    if (filing) FolderDialog(folders, "", { filing = false }) { target ->
        vm.moveToFolder(selected.toList(), target)
        selected.clear()
        filing = false
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete ${plural(selected.size, "document")}?") },
            text = { Text("They're removed from this phone. Files you already saved or shared are not affected.") },
            confirmButton = { TextButton({ confirmDelete = false; selected.toList().forEach(vm::deleteDoc); selected.clear() }) { Text("Delete") } },
            dismissButton = { TextButton({ confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun Header(
    vm: AppViewModel, docs: List<Doc>, query: String, onQuery: (String) -> Unit,
    folders: List<String>, folder: String?, onFolder: (String?) -> Unit, onImport: () -> Unit,
) {
    Column(Modifier.padding(start = 20.dp, end = 12.dp, top = 14.dp, bottom = 8.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Text("Docfold", style = MaterialTheme.typography.displayLarge, modifier = Modifier.weight(1f).padding(top = 4.dp))
            TextButton(onClick = { vm.open(Screen.Settings) }, modifier = Modifier.padding(top = 6.dp), colors = quietTextButton()) {
                Icon(DocIcons.Settings, null, Modifier.size(20.dp)); Spacer(Modifier.width(6.dp)); Text("Settings")
            }
        }
        val pages = docs.sumOf { it.pages.size }
        Text(
            if (docs.isEmpty()) "Scan to PDF · Free · Offline" else "${plural(docs.size, "document")} · ${plural(pages, "page")} · on this phone",
            style = Mono.medium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (docs.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 8.dp)) {
                OutlinedTextField(
                    value = query, onValueChange = onQuery,
                    placeholder = { Text("Search", maxLines = 1) },
                    leadingIcon = { Icon(DocIcons.Search, null) },
                    trailingIcon = if (query.isNotEmpty()) ({ IconButton({ onQuery("") }) { Icon(DocIcons.Close, "Clear search") } }) else null,
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest, focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(10.dp))
                OutlinedButton(onClick = onImport, modifier = Modifier.height(56.dp)) {
                    Icon(DocIcons.Images, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text("Import")
                }
            }
            if (folders.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(folder == null, { onFolder(null) }, { Text("All") }, leadingIcon = checkIf(folder == null))
                    folders.forEach { f -> FilterChip(folder == f, { onFolder(f) }, { Text(f) }, leadingIcon = checkIf(folder == f)) }
                    Spacer(Modifier.width(4.dp))
                }
            }
        }
    }
}

@Composable
private fun SelectionBar(count: Int, onClose: () -> Unit, onMerge: () -> Unit, onFolder: () -> Unit, onDelete: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClose) { Icon(DocIcons.Close, "Cancel selection") }
            Text("$count selected", style = MaterialTheme.typography.headlineMedium)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            TextButton(onClick = onMerge, enabled = count >= 2) { Icon(DocIcons.Merge, null); Spacer(Modifier.width(6.dp)); Text("Merge") }
            TextButton(onClick = onFolder) { Icon(DocIcons.Folder, null); Spacer(Modifier.width(6.dp)); Text("Folder") }
            TextButton(onClick = onDelete) { Icon(DocIcons.Delete, null); Spacer(Modifier.width(6.dp)); Text("Delete") }
        }
    }
}

/** Running head: the first and last date in view, like guide words at the top of a dictionary page. */
@Composable
private fun GuideWords(docs: List<Doc>, list: androidx.compose.foundation.lazy.LazyListState) {
    val span by remember(docs) {
        derivedStateOf {
            val ids = list.layoutInfo.visibleItemsInfo.mapNotNull { it.key as? String }.toSet()
            val visible = docs.filter { it.id in ids }
            if (visible.isEmpty()) null else postalDate(visible.first().updated, false) to postalDate(visible.last().updated, false)
        }
    }
    Surface(color = MaterialTheme.colorScheme.background) {
        Column {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                val (a, b) = span ?: ("" to "")
                Text(a, style = Mono.strong, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Box(Modifier.weight(1f).padding(horizontal = 10.dp).height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
                Text(if (a == b) "" else b, style = Mono.strong, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Envelope(vm: AppViewModel, doc: Doc, selected: Boolean, onClick: () -> Unit, onLongClick: () -> Unit, modifier: Modifier) {
    val ink = LocalInks.current[doc.color]
    val first = doc.pages.firstOrNull()
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shadowElevation = if (selected) 0.dp else 1.dp,
        border = if (selected) BorderStroke(2.dp, ink) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth().airmailEdge(RoundedCornerShape(8.dp)).combinedClickable(onClick = onClick, onLongClick = onLongClick, onLongClickLabel = "Select")
            .semantics { contentDescription = "${doc.name}, ${plural(doc.pages.size, "page")}" },
    ) {
        Row(Modifier.padding(start = 14.dp, end = 16.dp, top = 14.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(width = 64.dp, height = 82.dp)) {
                Box(
                    Modifier.size(width = 58.dp, height = 76.dp).align(Alignment.Center).rotate(-3f)
                        .perforatedPaper(if (LocalDark.current) MaterialTheme.colorScheme.surfaceContainerHighest else Color.White, MaterialTheme.colorScheme.outlineVariant, 2.5.dp, 7.dp)
                        .padding(5.dp).background(ink).padding(3.dp),
                ) {
                    if (first != null) PageImage(vm.store.thumbFile(doc.id, first.id), first, side = 200, contentScale = ContentScale.Crop)
                }
                if (doc.sentAt > 0) {
                    // Exported documents carry a postmark across their stamp, the way real mail does.
                    Postmark(
                        "DOCFOLD", postalDate(doc.sentAt, false).take(6), "SENT", MaterialTheme.colorScheme.secondary,
                        Modifier.align(Alignment.BottomEnd).offset(x = 20.dp, y = 4.dp).rotate(-12f).graphicsLayer { alpha = 0.7f }, size = 50.dp,
                    )
                }
                if (selected) {
                    Surface(shape = RoundedCornerShape(50), color = ink, modifier = Modifier.size(24.dp).align(Alignment.TopStart)) {
                        Icon(DocIcons.Check, null, tint = MaterialTheme.colorScheme.surfaceContainerLowest, modifier = Modifier.padding(4.dp))
                    }
                }
            }
            Spacer(Modifier.width(22.dp))
            Column(Modifier.weight(1f)) {
                Text(doc.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(6.dp))
                Text("${postalDate(doc.updated)} · ${formatSize(doc.sizeBytes)}", style = Mono.small, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(10.dp))
            Box(contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.widthIn(min = 48.dp)) {
                    Text(
                        doc.pages.size.toString().padStart(2, '0'), color = ink,
                        style = MaterialTheme.typography.displayMedium.copy(fontFamily = ArchivoCondensed, fontSize = 50.sp, lineHeight = 48.sp),
                    )
                    Text(if (doc.pages.size == 1) "PAGE" else "PAGES", style = Mono.small, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun EmptyState(onScan: () -> Unit, onImport: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        EmptyEnvelope(Modifier.fillMaxWidth().height(250.dp))
        Text("Nothing sealed yet.", style = MaterialTheme.typography.displaySmall, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Text(
            "Point your camera at any page — a form, an ID, a receipt. Your first PDF takes about ten seconds.",
            style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onScan, modifier = Modifier.fillMaxWidth().height(60.dp)) {
            Icon(DocIcons.Scan, null, Modifier.size(24.dp)); Spacer(Modifier.width(10.dp)); Text("Scan a document", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onImport, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Icon(DocIcons.Images, null, Modifier.size(20.dp)); Spacer(Modifier.width(10.dp)); Text("Import photos or a PDF")
        }
        Spacer(Modifier.height(20.dp))
        Text("No account · No watermark · No limits", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
