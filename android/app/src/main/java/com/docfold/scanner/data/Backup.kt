package com.docfold.scanner.data

import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Every document in one portable file, so a lost or replaced phone doesn't lose them. It is a plain zip:
 *
 *   docfold-backup.json    says what this file is
 *   docs/<docId>/...       each document folder exactly as stored
 *
 * Pages are already JPEGs, so nothing is compressed: backing up stays quick on a cheap phone.
 */
object Backup {
    class NotABackup : Exception("That file isn't a Docfold backup.")
    class NoSpace : Exception("There isn't enough free space on this phone to restore that backup.")

    private const val MARKER = "docfold-backup.json"

    fun write(root: File, out: OutputStream, onProgress: (Int, Int) -> Unit = { _, _ -> }) {
        val docs = root.listFiles().orEmpty().filter { File(it, "doc.json").isFile }.sortedBy { it.name }
        ZipOutputStream(out.buffered()).use { zip ->
            zip.setLevel(Deflater.NO_COMPRESSION)
            zip.putNextEntry(ZipEntry(MARKER))
            zip.write(JSONObject().put("version", 1).put("documents", docs.size).toString().toByteArray())
            zip.closeEntry()
            docs.forEachIndexed { i, doc ->
                doc.walkTopDown().filter { it.isFile }.forEach { f ->
                    zip.putNextEntry(ZipEntry("docs/${doc.name}/${f.relativeTo(doc).invariantSeparatorsPath}"))
                    f.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
                onProgress(i + 1, docs.size)
            }
        }
    }

    /**
     * Unpacks a backup into [staging] and returns the complete document folders in it. The file comes from outside
     * the app, so entry names are not trusted: anything that would land outside [staging] rejects the whole file.
     */
    fun read(input: InputStream, staging: File, maxBytes: Long): List<File> {
        staging.deleteRecursively()
        staging.mkdirs()
        val base = staging.canonicalPath + File.separator
        var marked = false
        var total = 0L
        try {
            ZipInputStream(input.buffered()).use { zip ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (entry.name == MARKER) { marked = true; continue }
                    if (entry.isDirectory || !entry.name.startsWith("docs/")) continue
                    val file = File(staging, entry.name.removePrefix("docs/"))
                    if (!file.canonicalPath.startsWith(base)) throw NotABackup()
                    file.parentFile?.mkdirs()
                    file.outputStream().use { out ->
                        while (true) {
                            val n = zip.read(buffer)
                            if (n < 0) break
                            total += n
                            if (total > maxBytes) throw NoSpace()
                            out.write(buffer, 0, n)
                        }
                    }
                }
            }
        } catch (e: ZipException) {
            throw NotABackup()
        }
        if (!marked) throw NotABackup()
        return staging.listFiles().orEmpty().filter(::isComplete)
    }

    /** The folder holds a readable document under its own id, with every page's image present. */
    private fun isComplete(folder: File): Boolean = try {
        val doc = Doc.fromJson(JSONObject(File(folder, "doc.json").readText()))
        doc.id == folder.name && doc.pages.all { File(folder, "pages/${it.id}.jpg").isFile }
    } catch (e: JSONException) {
        false
    } catch (e: IOException) {
        false
    }
}
