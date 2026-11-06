package com.docfold.scanner.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class BackupTest {
    @get:Rule val temp = TemporaryFolder()

    private fun document(root: File, id: String, pages: Int) {
        val dir = File(root, id).apply { mkdirs() }
        File(dir, "pages").mkdirs()
        val list = (1..pages).map { Page("p$it") }
        list.forEach { File(dir, "pages/${it.id}.jpg").writeBytes(byteArrayOf(1, 2, 3)) }
        File(dir, "doc.json").writeText(Doc(id, "Doc $id", 1, 2, 0, list).toJson().toString())
    }

    private fun zip(vararg entries: Pair<String, ByteArray>) = ByteArrayOutputStream().also { out ->
        ZipOutputStream(out).use { z -> entries.forEach { (name, data) -> z.putNextEntry(ZipEntry(name)); z.write(data); z.closeEntry() } }
    }.toByteArray()

    @Test fun roundTripKeepsEveryDocumentAndPage() {
        val root = temp.newFolder("docs")
        document(root, "aaa", 3)
        document(root, "bbb", 1)
        val backup = ByteArrayOutputStream().also { Backup.write(root, it) }.toByteArray()

        val found = Backup.read(ByteArrayInputStream(backup), temp.newFolder("staging"), Long.MAX_VALUE)

        assertEquals(listOf("aaa", "bbb"), found.map { it.name }.sorted())
        assertEquals(3, File(found.first { it.name == "aaa" }, "pages").listFiles()!!.size)
    }

    @Test fun anEntryEscapingTheFolderRejectsTheWholeFile() {
        val staging = temp.newFolder("staging")
        val bad = zip("docfold-backup.json" to "{}".toByteArray(), "docs/../../evil.txt" to byteArrayOf(1))

        assertThrows(Backup.NotABackup::class.java) { Backup.read(ByteArrayInputStream(bad), staging, Long.MAX_VALUE) }
        assertFalse(File(staging.parentFile!!.parentFile, "evil.txt").exists())
    }

    @Test fun aZipThatIsNotABackupIsRefused() {
        val other = zip("photos/cat.jpg" to byteArrayOf(1))
        assertThrows(Backup.NotABackup::class.java) { Backup.read(ByteArrayInputStream(other), temp.newFolder("staging"), Long.MAX_VALUE) }
    }

    @Test fun aFileThatIsNotAZipIsRefused() {
        assertThrows(Backup.NotABackup::class.java) { Backup.read(ByteArrayInputStream("hello".toByteArray()), temp.newFolder("staging"), Long.MAX_VALUE) }
    }

    @Test fun stopsBeforeFillingThePhone() {
        val root = temp.newFolder("docs")
        document(root, "aaa", 2)
        val backup = ByteArrayOutputStream().also { Backup.write(root, it) }.toByteArray()
        assertThrows(Backup.NoSpace::class.java) { Backup.read(ByteArrayInputStream(backup), temp.newFolder("staging"), 5) }
    }

    @Test fun aDocumentMissingAPageIsLeftOut() {
        val root = temp.newFolder("docs")
        document(root, "good", 1)
        document(root, "torn", 2)
        assertTrue(File(root, "torn/pages/p2.jpg").delete())
        val backup = ByteArrayOutputStream().also { Backup.write(root, it) }.toByteArray()

        val found = Backup.read(ByteArrayInputStream(backup), temp.newFolder("staging"), Long.MAX_VALUE)

        assertEquals(listOf("good"), found.map { it.name })
    }
}
