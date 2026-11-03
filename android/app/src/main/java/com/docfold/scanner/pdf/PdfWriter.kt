package com.docfold.scanner.pdf

import java.io.BufferedOutputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * A small streaming PDF writer built for scanned pages.
 *
 * Android's PdfDocument re-compresses photos losslessly, which makes scans huge. This writer embeds each
 * page's JPEG bytes as they are (DCTDecode), so files stay small and only one page is in memory at a time.
 * Optional AES-256 password protection follows the PDF 2.0 standard security handler (V5 / R6), which
 * every current viewer — Chrome, Acrobat, Google Drive, phone PDF apps — opens.
 */
class PdfWriter(out: OutputStream, password: String?) {
    private val o = CountingStream(BufferedOutputStream(out, 64 * 1024))
    private val offsets = HashMap<Int, Long>()
    private var nextObj = 1
    private val crypt = password?.takeIf { it.isNotEmpty() }?.let { Aes256Security(it) }

    val encrypted get() = crypt != null

    init {
        o.write("%PDF-1.7\n%".toByteArray(Charsets.ISO_8859_1))
        o.write(byteArrayOf(0xE2.toByte(), 0xE3.toByte(), 0xCF.toByte(), 0xD3.toByte(), '\n'.code.toByte()))
    }

    fun reserve(): Int = nextObj++

    /** Writes `n 0 obj <dict> endobj`. */
    fun obj(num: Int, dict: String) {
        offsets[num] = o.count
        ascii("$num 0 obj\n$dict\nendobj\n")
    }

    /** Writes a stream object; [data] is encrypted when a password is set. [dict] must not contain /Length. */
    fun stream(num: Int, dict: String, data: ByteArray) {
        val body = crypt?.encrypt(data) ?: data
        offsets[num] = o.count
        val d = dict.trim().removeSuffix(">>")
        ascii("$num 0 obj\n$d /Length ${body.size} >>\nstream\n")
        o.write(body)
        ascii("\nendstream\nendobj\n")
    }

    /** A text string operand: a literal when plain, encrypted hex when protected. */
    fun string(bytes: ByteArray): String = crypt?.let { "<" + hex(it.encrypt(bytes)) + ">" } ?: literal(bytes)

    fun finish(rootNum: Int, infoNum: Int?) {
        val id = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val encNum = crypt?.let { c ->
            val n = reserve()
            obj(n, c.encryptDict())
            n
        }
        val xref = o.count
        val size = nextObj
        val sb = StringBuilder("xref\n0 $size\n0000000000 65535 f \n")
        for (i in 1 until size) {
            val off = offsets[i] ?: 0L
            sb.append(String.format(java.util.Locale.ROOT, "%010d 00000 %s \n", off, if (offsets.containsKey(i)) "n" else "f"))
        }
        sb.append("trailer\n<< /Size $size /Root $rootNum 0 R")
        infoNum?.let { sb.append(" /Info $it 0 R") }
        encNum?.let { sb.append(" /Encrypt $it 0 R") }
        sb.append(" /ID [<${hex(id)}> <${hex(id)}>] >>\nstartxref\n$xref\n%%EOF\n")
        ascii(sb.toString())
        o.flush()
    }

    /** Catalog extension needed for AES-256 under PDF 1.7. */
    val catalogExtras get() = if (crypt != null) " /Extensions << /ADBE << /BaseVersion /1.7 /ExtensionLevel 8 >> >>" else ""

    private fun ascii(s: String) = o.write(s.toByteArray(Charsets.ISO_8859_1))

    private class CountingStream(private val out: OutputStream) : OutputStream() {
        var count = 0L; private set
        override fun write(b: Int) { out.write(b); count++ }
        override fun write(b: ByteArray, off: Int, len: Int) { out.write(b, off, len); count += len }
        override fun flush() = out.flush()
        override fun close() = out.close()
    }

    companion object {
        fun hex(b: ByteArray) = buildString(b.size * 2) { b.forEach { append(String.format(java.util.Locale.ROOT, "%02X", it.toInt() and 0xff)) } }

        fun literal(bytes: ByteArray): String = buildString {
            append('(')
            bytes.forEach { b ->
                when (val c = b.toInt() and 0xff) {
                    '('.code, ')'.code, '\\'.code -> { append('\\'); append(c.toChar()) }
                    in 32..126 -> append(c.toChar())
                    else -> append('\\').append(Integer.toOctalString(c).padStart(3, '0'))
                }
            }
            append(')')
        }
    }
}

/** PDF standard security handler, revision 6 (AES-256), per ISO 32000-2 §7.6.4. */
private class Aes256Security(password: String) {
    private val rnd = SecureRandom()
    private val fileKey = ByteArray(32).also { rnd.nextBytes(it) }
    private val u: ByteArray
    private val ue: ByteArray
    private val oKey: ByteArray
    private val oe: ByteArray
    private val perms: ByteArray

    init {
        // SASLprep is skipped: passwords are UTF-8, capped at 127 bytes as the spec requires.
        val pw = password.toByteArray(Charsets.UTF_8).let { if (it.size > 127) it.copyOf(127) else it }
        val uValSalt = salt(); val uKeySalt = salt()
        u = hash(pw, uValSalt, EMPTY) + uValSalt + uKeySalt
        ue = aesNoPad(hash(pw, uKeySalt, EMPTY), fileKey)
        // Owner password = user password: whoever can open the file may also edit or print it.
        val oValSalt = salt(); val oKeySalt = salt()
        oKey = hash(pw, oValSalt, u) + oValSalt + oKeySalt
        oe = aesNoPad(hash(pw, oKeySalt, u), fileKey)
        val p = PERMISSIONS
        val block = byteArrayOf(
            (p and 0xff).toByte(), (p shr 8 and 0xff).toByte(), (p shr 16 and 0xff).toByte(), (p shr 24 and 0xff).toByte(),
            -1, -1, -1, -1, 'T'.code.toByte(), 'a'.code.toByte(), 'd'.code.toByte(), 'b'.code.toByte(), 0, 0, 0, 0,
        )
        System.arraycopy(ByteArray(4).also { rnd.nextBytes(it) }, 0, block, 12, 4)
        perms = Cipher.getInstance("AES/ECB/NoPadding").run {
            init(Cipher.ENCRYPT_MODE, SecretKeySpec(fileKey, "AES"))
            doFinal(block)
        }
    }

    /** AES-256-CBC with a random IV prepended and PKCS#7 padding, as /AESV3 requires. */
    fun encrypt(data: ByteArray): ByteArray {
        val iv = ByteArray(16).also { rnd.nextBytes(it) }
        val c = Cipher.getInstance("AES/CBC/PKCS5Padding")
        c.init(Cipher.ENCRYPT_MODE, SecretKeySpec(fileKey, "AES"), IvParameterSpec(iv))
        return iv + c.doFinal(data)
    }

    fun encryptDict() = "<< /Filter /Standard /V 5 /R 6 /Length 256 " +
        "/CF << /StdCF << /AuthEvent /DocOpen /CFM /AESV3 /Length 32 >> >> /StmF /StdCF /StrF /StdCF " +
        "/O <${PdfWriter.hex(oKey)}> /U <${PdfWriter.hex(u)}> /OE <${PdfWriter.hex(oe)}> /UE <${PdfWriter.hex(ue)}> " +
        "/Perms <${PdfWriter.hex(perms)}> /P $PERMISSIONS /EncryptMetadata true >>"

    private fun salt() = ByteArray(8).also { rnd.nextBytes(it) }

    private fun aesNoPad(key: ByteArray, data: ByteArray): ByteArray = Cipher.getInstance("AES/CBC/NoPadding").run {
        init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(ByteArray(16)))
        doFinal(data)
    }

    /** Algorithm 2.B: the iterated SHA-2 / AES-128 hash used for every R6 key. */
    private fun hash(pw: ByteArray, salt: ByteArray, userKey: ByteArray): ByteArray {
        var k = MessageDigest.getInstance("SHA-256").digest(pw + salt + userKey)
        var round = 0
        var last = 0
        while (round < 64 || last > round - 32) {
            val seq = pw + k + userKey
            val k1 = ByteArray(seq.size * 64)
            for (j in 0 until 64) System.arraycopy(seq, 0, k1, j * seq.size, seq.size)
            val c = Cipher.getInstance("AES/CBC/NoPadding")
            c.init(Cipher.ENCRYPT_MODE, SecretKeySpec(k, 0, 16, "AES"), IvParameterSpec(k, 16, 16))
            val e = c.doFinal(k1)
            var sum = 0
            for (j in 0 until 16) sum += e[j].toInt() and 0xff
            k = MessageDigest.getInstance(
                when (sum % 3) { 0 -> "SHA-256"; 1 -> "SHA-384"; else -> "SHA-512" }
            ).digest(e)
            last = e[e.size - 1].toInt() and 0xff
            round++
        }
        return k.copyOf(32)
    }

    companion object {
        private val EMPTY = ByteArray(0)
        /** Everything allowed (print, copy, edit…) once opened. */
        const val PERMISSIONS = -4
    }
}
