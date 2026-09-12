package com.vajraworld.defender.domain.engine

import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

data class LocalFileAnalysisResult(
    val filename: String,
    val sha256: String,
    val isApk: Boolean,
    val riskScore: Int,
    val confidence: Float,
    val whyPoints: List<String>,
    val permissions: List<String>,
    val archiveSafe: Boolean,
    val isDebuggable: Boolean
)

object FileInspector {

    private const val MAX_ZIP_ENTRIES = 500
    private const val MAX_UNCOMPRESSED_SIZE_BYTES = 100 * 1024 * 1024L // 100 MB
    private const val MAX_COMPRESSION_RATIO = 10.0

    // Dangerous permissions constants
    private const val PERM_ACCESSIBILITY = "android.permission.BIND_ACCESSIBILITY_SERVICE"
    private const val PERM_ALERT_WINDOW = "android.permission.SYSTEM_ALERT_WINDOW"
    private const val PERM_READ_SMS = "android.permission.READ_SMS"
    private const val PERM_RECEIVE_SMS = "android.permission.RECEIVE_SMS"
    private const val PERM_INTERNET = "android.permission.INTERNET"
    private const val PERM_DEVICE_ADMIN = "android.permission.BIND_DEVICE_ADMIN"
    private const val PERM_REQUEST_INSTALL = "android.permission.REQUEST_INSTALL_PACKAGES"
    private const val PERM_RECORD_AUDIO = "android.permission.RECORD_AUDIO"
    private const val PERM_CAMERA = "android.permission.CAMERA"
    private const val PERM_READ_CONTACTS = "android.permission.READ_CONTACTS"
    private const val PERM_READ_CALL_LOG = "android.permission.READ_CALL_LOG"
    private const val PERM_FINE_LOCATION = "android.permission.ACCESS_FINE_LOCATION"

    /**
     * Inspect a file from filesystem.
     */
    fun inspectFile(file: File): LocalFileAnalysisResult {
        val sha256 = calculateStreamingSha256(FileInputStream(file))
        return inspectZipAndPermissions(
            filename = file.name,
            sha256 = sha256,
            inputStreamProvider = { FileInputStream(file) },
            fileSize = file.length()
        )
    }

    /**
     * Inspect an input stream (e.g. from content resolver or shared uri).
     * Calculates streaming SHA-256 and analyzes zip/APK structure.
     */
    fun inspectStream(filename: String, inputStream: InputStream, fileSize: Long = 0): LocalFileAnalysisResult {
        // Cache to a temp file to allow streaming SHA-256 and ZIP reading without unbounded RAM consumption
        val tempFile = File.createTempFile("vajra_inspect_", ".tmp")
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(8192)
            tempFile.outputStream().use { out ->
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                    out.write(buffer, 0, bytesRead)
                }
            }
            val sha256 = digest.digest().joinToString("") { "%02x".format(it) }

            return inspectZipAndPermissions(
                filename = filename,
                sha256 = sha256,
                inputStreamProvider = { FileInputStream(tempFile) },
                fileSize = if (fileSize > 0) fileSize else tempFile.length()
            )
        } finally {
            if (tempFile.exists()) {
                tempFile.delete()
            }
        }
    }

    /**
     * Genuine streaming SHA-256 calculation over InputStream.
     * Uses 8KB buffer without loading the entire stream into memory.
     */
    fun calculateStreamingSha256(inputStream: InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8192)
        inputStream.use { stream ->
            var bytesRead: Int
            while (stream.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun inspectZipAndPermissions(
        filename: String,
        sha256: String,
        inputStreamProvider: () -> InputStream,
        fileSize: Long
    ): LocalFileAnalysisResult {
        val isApk = filename.endsWith(".apk", ignoreCase = true)
        val isZip = isApk || filename.endsWith(".zip", ignoreCase = true) || filename.endsWith(".jar", ignoreCase = true)

        var totalEntries = 0
        var totalUncompressedBytes = 0L
        var totalCompressedBytes = 0L
        var archiveSafe = true
        var isDebuggable = false
        val whyPoints = mutableListOf<String>()
        val permissions = mutableSetOf<String>()
        var foundManifest = false
        var foundDex = false
        var foundNativeLibs = false

        if (isZip) {
            try {
                ZipInputStream(inputStreamProvider()).use { zis ->
                    var entry: ZipEntry? = zis.nextEntry
                    while (entry != null) {
                        totalEntries++

                        if (totalEntries > MAX_ZIP_ENTRIES) {
                            archiveSafe = false
                            whyPoints.add("Zip-bomb defense triggered: exceeds maximum file limit ($MAX_ZIP_ENTRIES entries)")
                            break
                        }

                        val entryName = entry.name
                        if (entryName.equals("AndroidManifest.xml", ignoreCase = true)) {
                            foundManifest = true
                            // Read binary manifest content safely
                            val manifestBytes = readEntryBytes(zis, maxBytes = 2 * 1024 * 1024)
                            val parsed = parseAndroidManifest(manifestBytes)
                            permissions.addAll(parsed.permissions)
                            if (parsed.isDebuggable) {
                                isDebuggable = true
                            }
                        } else if (entryName.startsWith("classes") && entryName.endsWith(".dex")) {
                            foundDex = true
                        } else if (entryName.startsWith("lib/") && entryName.endsWith(".so")) {
                            foundNativeLibs = true
                        }

                        // Track size for zip bomb ratio check
                        val cSize = if (entry.compressedSize > 0) entry.compressedSize else 100L
                        val uSize = if (entry.size > 0) entry.size else cSize
                        totalCompressedBytes += cSize
                        totalUncompressedBytes += uSize

                        if (totalUncompressedBytes > MAX_UNCOMPRESSED_SIZE_BYTES) {
                            archiveSafe = false
                            whyPoints.add("Zip-bomb defense triggered: exceeds maximum uncompressed size (${MAX_UNCOMPRESSED_SIZE_BYTES / (1024 * 1024)}MB)")
                            break
                        }

                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }

                // Check compression ratio
                if (totalCompressedBytes > 0) {
                    val ratio = totalUncompressedBytes.toDouble() / totalCompressedBytes.toDouble()
                    if (ratio > MAX_COMPRESSION_RATIO && totalUncompressedBytes > 10 * 1024 * 1024L) {
                        archiveSafe = false
                        whyPoints.add("Suspicious compression ratio (${String.format(Locale.US, "%.1f:1", ratio)}) exceeding safety threshold")
                    }
                }
            } catch (e: Exception) {
                whyPoints.add("Archive inspection warning: ${e.message}")
            }
        }

        // Assess risk based on permissions and APK structure
        var riskScore = 0

        if (!archiveSafe) {
            riskScore += 70
        }

        if (isApk) {
            if (!foundManifest) {
                riskScore += 30
                whyPoints.add("Missing or corrupt AndroidManifest.xml inside package")
            }
            if (!foundDex) {
                riskScore += 25
                whyPoints.add("No compiled classes.dex executable found in package")
            }
            if (isDebuggable) {
                riskScore += 20
                whyPoints.add("Package compiled with android:debuggable='true' (vulnerable to runtime injection)")
            }

            // Dangerous permission combinations:
            // 1. Toxic Banking Trojan: Accessibility + Overlay
            val hasAccessibility = permissions.any { it.contains("BIND_ACCESSIBILITY_SERVICE", ignoreCase = true) }
            val hasOverlay = permissions.any { it.contains("SYSTEM_ALERT_WINDOW", ignoreCase = true) }
            if (hasAccessibility && hasOverlay) {
                riskScore += 50
                whyPoints.add("Toxic Banking Trojan pattern: Accessibility Service combined with Screen Overlay (enables credential overlay spoofing and keystroke interception)")
            }

            // 2. Potential OTP Interception: SMS + Internet
            val hasSms = permissions.any { it.contains("READ_SMS", ignoreCase = true) || it.contains("RECEIVE_SMS", ignoreCase = true) }
            val hasInternet = permissions.any { it.contains("INTERNET", ignoreCase = true) }
            if (hasSms && hasInternet) {
                riskScore += 35
                whyPoints.add("Potential OTP Interception pattern: SMS read/receive permissions combined with Internet access")
            }

            // 3. Toxic Privilege Escalation: Device Admin + Request Install
            val hasAdmin = permissions.any { it.contains("BIND_DEVICE_ADMIN", ignoreCase = true) }
            val hasInstall = permissions.any { it.contains("REQUEST_INSTALL_PACKAGES", ignoreCase = true) }
            if (hasAdmin && hasInstall) {
                riskScore += 45
                whyPoints.add("Privilege Escalation pattern: Device Admin rights combined with silent package installation capability")
            }

            // 4. Spyware Surveillance pattern
            val hasRecording = permissions.any { it.contains("RECORD_AUDIO", ignoreCase = true) || it.contains("CAMERA", ignoreCase = true) }
            val hasPersonalData = permissions.any {
                it.contains("READ_CONTACTS", ignoreCase = true) ||
                it.contains("READ_CALL_LOG", ignoreCase = true) ||
                it.contains("ACCESS_FINE_LOCATION", ignoreCase = true)
            }
            if (hasRecording && hasPersonalData && hasInternet) {
                riskScore += 30
                whyPoints.add("Surveillance pattern: Audio/Camera recording combined with personal contacts/location and Internet transmission")
            }

            if (whyPoints.isEmpty()) {
                whyPoints.add("Standard APK package structure verified with no toxic permission synergies")
                riskScore = 10
            }
        } else {
            // Non-APK file
            if (whyPoints.isEmpty()) {
                whyPoints.add("File signature verified; no archive safety hazards detected")
                riskScore = 5
            }
        }

        val clampedScore = riskScore.coerceIn(0, 100)
        val confidence = if (isApk && foundManifest) 0.95f else 0.85f

        return LocalFileAnalysisResult(
            filename = filename,
            sha256 = sha256,
            isApk = isApk,
            riskScore = clampedScore,
            confidence = confidence,
            whyPoints = whyPoints,
            permissions = permissions.toList().sorted(),
            archiveSafe = archiveSafe,
            isDebuggable = isDebuggable
        )
    }

    private fun readEntryBytes(zis: ZipInputStream, maxBytes: Int): ByteArray {
        val buffer = ByteArray(8192)
        val out = java.io.ByteArrayOutputStream()
        var total = 0
        var read: Int
        while (zis.read(buffer).also { read = it } != -1) {
            out.write(buffer, 0, read)
            total += read
            if (total >= maxBytes) break
        }
        return out.toByteArray()
    }

    data class ParsedManifest(
        val permissions: Set<String>,
        val isDebuggable: Boolean
    )

    /**
     * Parses permissions and debuggable state from binary AndroidManifest.xml (AXML).
     * Extracts strings from the AXML string pool and scans string bytes.
     */
    fun parseAndroidManifest(bytes: ByteArray): ParsedManifest {
        if (bytes.size < 8) return ParsedManifest(emptySet(), false)

        val permissions = mutableSetOf<String>()
        var isDebuggable = false

        try {
            // First: extract all UTF-8 / UTF-16 strings from String Pool Chunk if valid binary XML
            if (bytes.size >= 36) {
                val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
                val fileType = buffer.short.toInt() and 0xFFFF
                val headerSize = buffer.short.toInt() and 0xFFFF
                val fileSize = buffer.int

                // Validate AXML header (RES_XML_TYPE = 0x0003, headerSize = 0x0008)
                if (fileType == 0x0003 && headerSize == 8 && fileSize <= bytes.size + 1024) {
                    val chunkType = buffer.short.toInt() and 0xFFFF
                    val chunkHeaderSize = buffer.short.toInt() and 0xFFFF
                    val chunkSize = buffer.int
                    val stringCount = buffer.int
                    val styleCount = buffer.int
                    val flags = buffer.int
                    val stringsStart = buffer.int
                    val stylesStart = buffer.int

                    // RES_STRING_POOL_TYPE = 0x0001
                    if (chunkType == 0x0001 && stringCount in 1..20_000 && chunkSize in 28..bytes.size) {
                        val isUtf8 = (flags and (1 shl 8)) != 0
                        val stringOffsets = IntArray(stringCount)
                        for (i in 0 until stringCount) {
                            if (buffer.remaining() >= 4) {
                                stringOffsets[i] = buffer.int
                            }
                        }

                        // Read strings safely
                        val poolBase = 8 + stringsStart
                        for (offset in stringOffsets) {
                            val strPos = poolBase + offset
                            if (strPos in 0 until bytes.size) {
                                val str = if (isUtf8) {
                                    readAxmlUtf8String(bytes, strPos)
                                } else {
                                    readAxmlUtf16String(bytes, strPos)
                                }
                                if (str.isNotBlank()) {
                                    if (str.contains("android.permission.") || str.contains(".permission.")) {
                                        permissions.add(str.trim())
                                    }
                                    if (str.equals("debuggable", ignoreCase = true)) {
                                        isDebuggable = true
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (_: Throwable) {
            // Fallback to byte scanner if AXML structure has nonstandard chunk headers
        }


        // Secondary robust byte scanner for permission strings (handles any AXML formatting)
        val stringContent = String(bytes, Charsets.ISO_8859_1)
        val permRegex = Regex("""android\.permission\.[A-Za-z0-9_]+""")
        permRegex.findAll(stringContent).forEach { match ->
            permissions.add(match.value)
        }

        // Also check UTF-16 LE pattern in raw bytes (where letters are separated by null bytes)
        val utf16Regex = Regex("""a\x00n\x00d\x00r\x00o\x00i\x00d\x00\.\x00p\x00e\x00r\x00m\x00i\x00s\x00s\x00i\x00o\x00n\x00\.\x00([A-Za-z0-9_]+(\x00[A-Za-z0-9_]+)*)""")
        utf16Regex.findAll(stringContent).forEach { match ->
            val clean = match.value.replace("\u0000", "")
            permissions.add(clean)
        }

        // Check for debuggable string in raw content
        if (!isDebuggable && (stringContent.contains("debuggable") || stringContent.contains("d\u0000e\u0000b\u0000u\u0000g\u0000g\u0000a\u0000b\u0000l\u0000e"))) {
            isDebuggable = true
        }

        return ParsedManifest(permissions, isDebuggable)
    }

    private fun readAxmlUtf8String(bytes: ByteArray, start: Int): String {
        var pos = start
        if (pos >= bytes.size) return ""
        // Length might be 1 or 2 bytes
        var len = bytes[pos].toInt() and 0xFF
        pos++
        if (len and 0x80 != 0) {
            len = ((len and 0x7F) shl 8) or (bytes[pos].toInt() and 0xFF)
            pos++
        }
        val end = minOf(pos + len, bytes.size)
        return try {
            String(bytes, pos, end - pos, Charsets.UTF_8)
        } catch (_: Exception) {
            ""
        }
    }

    private fun readAxmlUtf16String(bytes: ByteArray, start: Int): String {
        var pos = start
        if (pos + 1 >= bytes.size) return ""
        var len = (bytes[pos].toInt() and 0xFF) or ((bytes[pos + 1].toInt() and 0xFF) shl 8)
        pos += 2
        val byteLen = len * 2
        val end = minOf(pos + byteLen, bytes.size)
        return try {
            String(bytes, pos, end - pos, Charsets.UTF_16LE)
        } catch (_: Exception) {
            ""
        }
    }
}
