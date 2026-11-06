package com.docfold.scanner.ui.screens

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.print.PrintManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.docfold.scanner.MainActivity
import com.docfold.scanner.ads.BannerAd
import com.docfold.scanner.data.Doc
import com.docfold.scanner.data.nameFromText
import com.docfold.scanner.pdf.PdfPrintAdapter
import com.docfold.scanner.pdf.ExportOptions
import com.docfold.scanner.ui.AddActions
import com.docfold.scanner.ui.AppViewModel
import com.docfold.scanner.ui.LocalAccess
import com.docfold.scanner.ui.LocalSnackbar
import com.docfold.scanner.ui.Screen
import com.docfold.scanner.ui.components.PageStamp
import com.docfold.scanner.ui.components.PostmarkStamp
import com.docfold.scanner.ui.components.reorderable
import com.docfold.scanner.ui.components.rememberGridReorder
import com.docfold.scanner.ui.theme.DocIcons
import com.docfold.scanner.ui.theme.LocalInks
import com.docfold.scanner.ui.theme.Mono
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentScreen(vm: AppViewModel, docId: String, actions: AddActions, activity: MainActivity, onBack: () -> Unit) {
    val docs by vm.store.docs.collectAsState()
    val doc = docs?.firstOrNull { it.id == docId }
    if (doc == null || docId in vm.hiddenDocs) {
        LaunchedEffect(docs) { if (docs != null && vm.screen == Screen.Document(docId)) vm.back() }
        return
    }
    val context = LocalContext.current
    val access = LocalAccess.current
    val ink = LocalInks.current[doc.color]
    val order = remember(doc.pages) { doc.pages.map { it.id }.toMutableStateList() }
    val byId = remember(doc.pages) { doc.pages.associateBy { it.id } }
    val selected = remember { mutableStateListOf<String>() }
    var selecting by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var showAdd by remember { mutableStateOf(false) }
    var showExport by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var filing by remember { mutableStateOf(false) }
    var confirmDeletePages by remember { mutableStateOf(false) }
    var pendingSave by remember { mutableStateOf<ExportOptions?>(null) }
    val grid = rememberLazyGridState()
    val reorder = rememberGridReorder(
        grid,
        canMove = { it is String && it in byId },
        onMove = { from, to -> if (from in order.indices && to in order.indices) order.add(to, order.removeAt(from)) },
        onEnd = { if (order != doc.pages.map { it.id }) vm.reorder(doc.id, order.toList()) },
    )

    fun saved(uri: Uri, bytes: Long) = vm.notify("Saved to Downloads › Docfold · ${formatSize(bytes)}", "Open") {
        val view = Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/pdf").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try { context.startActivity(view) } catch (e: ActivityNotFoundException) { vm.notify("No PDF viewer found. The file is in Downloads › Docfold.") }
    }
    val createDoc = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        val opts = pendingSave
        if (uri != null && opts != null) vm.savePdfTo(doc, opts, uri) { bytes -> vm.notify("PDF saved · ${formatSize(bytes)}") }
        pendingSave = null
    }

    fun share(uris: List<Uri>, mime: String) {
        val send = if (uris.size == 1) Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM, uris[0])
        else Intent(Intent.ACTION_SEND_MULTIPLE).putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
        send.type = mime
        send.clipData = ClipData.newRawUri(null, uris[0]).apply { uris.drop(1).forEach { addItem(ClipData.Item(it)) } }
        send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(send, "Send ${doc.name}"))
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = { SnackbarHost(LocalSnackbar.current) },
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        if (selecting) IconButton({ selecting = false; selected.clear() }) { Icon(DocIcons.Close, "Stop selecting") }
                        else IconButton(onBack) { Icon(DocIcons.Back, "Back") }
                    },
                    title = {
                        if (selecting) Text(if (selected.isEmpty()) "Tap pages" else "${selected.size} selected", style = MaterialTheme.typography.headlineSmall)
                    },
                    actions = {
                        if (selecting) {
                            TextButton({ if (selected.size == order.size) selected.clear() else { selected.clear(); selected.addAll(order) } }, colors = quietTextButton()) {
                                Text(if (selected.size == order.size) "None" else "All")
                            }
                        } else {
                            if (doc.pages.size > 1) TextButton({ selecting = true }, colors = quietTextButton()) {
                                Icon(DocIcons.Select, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Select")
                            }
                            Box {
                                TextButton({ menu = true }, colors = quietTextButton()) {
                                    Icon(DocIcons.More, null, Modifier.size(18.dp)); Spacer(Modifier.width(2.dp)); Text("More")
                                }
                                DropdownMenu(menu, { menu = false }) {
                                    DropdownMenuItem({ Text("Rename") }, { menu = false; renaming = true }, leadingIcon = { Icon(DocIcons.Edit, null) })
                                    DropdownMenuItem({ Text("Move to folder") }, { menu = false; filing = true }, leadingIcon = { Icon(DocIcons.Folder, null) })
                                    DropdownMenuItem({ Text("Add photos") }, { menu = false; actions.photos(doc.id) }, leadingIcon = { Icon(DocIcons.Photo, null) })
                                    DropdownMenuItem({ Text("Add pages from a PDF") }, { menu = false; actions.pdf(doc.id) }, leadingIcon = { Icon(DocIcons.Pdf, null) })
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                        DropdownMenuItem({ Text("Save pages to gallery") }, {
                                            menu = false
                                            access.saving({ vm.saveImagesToGallery(doc) }, { vm.notify("Not saved. You can allow saving in Settings.") })
                                        }, leadingIcon = { Icon(DocIcons.Images, null) })
                                    }
                                    DropdownMenuItem({ Text("Share pages as images") }, { menu = false; vm.shareImages(doc) { share(it, "image/jpeg") } }, leadingIcon = { Icon(DocIcons.Share, null) })
                                    DropdownMenuItem({ Text("Print") }, {
                                        menu = false
                                        vm.printPdf(doc) { file -> context.getSystemService(PrintManager::class.java)?.print(doc.name, PdfPrintAdapter(file, doc.name), null) }
                                    }, leadingIcon = { Icon(DocIcons.Print, null) })
                                    HorizontalDivider()
                                    DropdownMenuItem({ Text("Delete document") }, { menu = false; vm.deleteDoc(doc.id) }, leadingIcon = { Icon(DocIcons.Delete, null) })
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                )
            },
            bottomBar = {
                Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.navigationBarsPadding().topHairline()) {
                    Column {
                        if (selecting) SelectionActions(
                            enabled = selected.isNotEmpty(),
                            onRotate = { vm.rotate(doc.id, selected.toSet(), 90) },
                            onDuplicate = { vm.duplicate(doc.id, selected.toSet()); selected.clear() },
                            onExtract = { vm.extract(doc.id, selected.toSet()); selected.clear(); selecting = false },
                            onDelete = { confirmDeletePages = true },
                        ) else Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 18.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            val tight = PaddingValues(horizontal = 12.dp)
                            OutlinedButton({ showAdd = true }, Modifier.weight(1f).height(56.dp), contentPadding = tight) {
                                Icon(DocIcons.Add, null, Modifier.size(20.dp)); Spacer(Modifier.width(6.dp)); Text("Add pages", maxLines = 1)
                            }
                            Button({ showExport = true }, Modifier.weight(1.1f).height(56.dp), enabled = doc.pages.isNotEmpty(), contentPadding = tight) {
                                Icon(DocIcons.Download, null, Modifier.size(22.dp)); Spacer(Modifier.width(8.dp)); Text("Save PDF", style = MaterialTheme.typography.titleMedium, maxLines = 1)
                            }
                        }
                        if (!selecting) BannerAd()
                    }
                }
            },
        ) { pad ->
            Column(Modifier.fillMaxSize().padding(pad)) {
                // The name gets its own row so the labelled actions above never squeeze it. Tap to rename.
                Text(
                    doc.name, style = MaterialTheme.typography.headlineLarge, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().clickable(onClickLabel = "Rename") { renaming = true }.padding(start = 20.dp, end = 20.dp, bottom = 6.dp),
                )
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).background(ink))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "${plural(doc.pages.size, "page").uppercase(Locale.getDefault())} · ${formatSize(doc.sizeBytes)} · ${postalDate(doc.updated)}",
                        style = Mono.small, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    if (selecting) "Tap pages, then choose an action below." else "Tap a page to crop, filter or sign. Hold and drag to change the order.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 10.dp),
                )
                if (doc.pages.isEmpty()) {
                    Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No pages left.", style = MaterialTheme.typography.displaySmall)
                        Spacer(Modifier.height(8.dp))
                        Text("Add pages, or delete this document from the menu.", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else BoxWithConstraints(Modifier.fillMaxSize()) {
                    val cols = if (maxWidth < 600.dp) GridCells.Fixed(3) else GridCells.Adaptive(150.dp)
                    LazyVerticalGrid(
                        columns = cols,
                        state = grid,
                        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp).reorderable(reorder, enabled = !selecting),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        itemsIndexed(order, key = { _, id -> id }) { i, id ->
                            val page = byId[id] ?: return@itemsIndexed
                            val dragging = reorder.draggingKey == id
                            val isSel = id in selected
                            PageStamp(
                                vm.store.thumbFile(doc.id, id), page, i + 1, ink, selected = isSel,
                                modifier = Modifier
                                    .then(if (dragging) Modifier.zIndex(1f).graphicsLayer {
                                        translationX = reorder.offset.x; translationY = reorder.offset.y
                                        scaleX = 1.08f; scaleY = 1.08f; rotationZ = -3f
                                    } else Modifier.animateItem())
                                    .clickable(role = Role.Button, onClickLabel = if (selecting) "Select" else "Edit page") {
                                        if (selecting) { if (isSel) selected.remove(id) else selected.add(id) }
                                        else vm.open(Screen.PageView(doc.id, id))
                                    },
                            )
                        }
                        item(key = "add") { AddTile { showAdd = true } }
                    }
                }
            }
        }
        if (vm.postmarkFor == doc.id) {
            PostmarkStamp(
                top = "DOCFOLD · SEALED",
                middle = SimpleDateFormat("d MMM", Locale.getDefault()).format(Date()).uppercase(Locale.getDefault()),
                bottom = plural(doc.pages.size, "PAGE"),
                onDone = { vm.postmarkFor = null },
            )
        }
    }

    if (showAdd) AddPagesSheet({ showAdd = false }, { actions.scan(doc.id) }, { actions.photos(doc.id) }, { actions.pdf(doc.id) }, { actions.camera(doc.id) })
    if (showExport) ExportSheet(
        vm, doc, onDismiss = { showExport = false },
        onSave = { opts ->
            showExport = false
            access.saving(
                allowed = { vm.savePdfToDownloads(doc, opts, ::saved) },
                declined = { pendingSave = opts; createDoc.launch(doc.name + ".pdf") },
            )
        },
        onShare = { opts -> showExport = false; vm.sharePdf(doc, opts) { share(listOf(it), "application/pdf") } },
    )
    if (renaming) RenameDialog(vm, doc, onDismiss = { renaming = false }) { vm.rename(doc.id, it); renaming = false }
    if (filing) FolderDialog(vm.folders(), doc.folder, { filing = false }) { target -> vm.moveToFolder(listOf(doc.id), target); filing = false }
    if (confirmDeletePages) AlertDialog(
        onDismissRequest = { confirmDeletePages = false },
        title = { Text("Delete ${plural(selected.size, "page")}?") },
        text = { Text("This can't be undone.") },
        confirmButton = { TextButton({ vm.deletePages(doc.id, selected.toSet()); selected.clear(); confirmDeletePages = false; selecting = false }) { Text("Delete") } },
        dismissButton = { TextButton({ confirmDeletePages = false }) { Text("Cancel") } },
    )
}

@Composable
private fun AddTile(onClick: () -> Unit) {
    val line = MaterialTheme.colorScheme.outline
    Column(
        Modifier.aspectRatio(0.74f).clickable(role = Role.Button, onClickLabel = "Add pages", onClick = onClick)
            .drawBehind {
                drawRect(line, style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))))
            },
        verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(DocIcons.Add, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
        Spacer(Modifier.height(6.dp))
        Text("Add pages", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun SelectionActions(enabled: Boolean, onRotate: () -> Unit, onDuplicate: () -> Unit, onExtract: () -> Unit, onDelete: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
        ToolButton(DocIcons.RotateRight, "Rotate", enabled, onClick = onRotate)
        ToolButton(DocIcons.Duplicate, "Duplicate", enabled, onClick = onDuplicate)
        ToolButton(DocIcons.MoveOut, "New doc", enabled, onClick = onExtract)
        ToolButton(DocIcons.Delete, "Delete", enabled, onClick = onDelete)
    }
}

/** An icon with its word under it: every tool says what it does. */
@Composable
fun ToolButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, enabled: Boolean = true, active: Boolean = false, onClick: () -> Unit) {
    val color = when {
        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        active -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }
    Column(
        Modifier.heightIn(min = 56.dp).width(60.dp).clickable(enabled = enabled, role = Role.Button, onClick = onClick).padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = color, maxLines = 1)
    }
}

@Composable
fun RenameDialog(vm: AppViewModel, doc: Doc, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf(doc.name) }
    var reading by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    fun nameFromFirstPage() {
        val first = doc.pages.firstOrNull() ?: return
        reading = true
        note = null
        scope.launch {
            val text = runCatching { vm.readText(doc.id, first) }.getOrNull()
            val found = text?.let(::nameFromText)
            if (found != null) name = found
            else note = if (text == null) "Text reading needs a one-time download. Connect to the internet and try again." else "No words found on page 1."
            reading = false
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename") },
        text = {
            Column {
                OutlinedTextField(name, { name = it; note = null }, singleLine = true, label = { Text("Document name") })
                note?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp)) }
                if (doc.pages.isNotEmpty()) TextButton(::nameFromFirstPage, enabled = !reading, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.secondary)) {
                    Text(if (reading) "Reading page 1…" else "Use the words on page 1")
                }
            }
        },
        confirmButton = { TextButton({ onSave(name) }, enabled = name.isNotBlank()) { Text("Save") } },
        dismissButton = { TextButton(onDismiss) { Text("Cancel") } },
    )
}
