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
    val isDebuggable: Boolean,
    val dexReport: DexInspectionReport? = null,
    val hasSteganography: Boolean = false,
    val stegoReport: String? = null
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

        // 0. Offline Local Allowlist Short-Circuit (O(1))
        if (AllowlistManager.isAllowlisted(sha256)) {
            val reason = AllowlistManager.getAllowlistReason(sha256) ?: "Verified safe reference file"
            return LocalFileAnalysisResult(
                filename = filename,
                sha256 = sha256,
                isApk = isApk,
                riskScore = 0,
                confidence = 1.0f,
                whyPoints = listOf("Verified authentic item against local known-clean security allowlist: $reason"),
                permissions = emptyList(),
                archiveSafe = true,
                isDebuggable = false,
                dexReport = null,
                hasSteganography = false,
                stegoReport = null
            )
        }

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
        val dexBuffers = mutableListOf<ByteArray>()

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
                            if (dexBuffers.size < 3) {
                                val dBytes = readEntryBytes(zis, maxBytes = 10 * 1024 * 1024)
                                if (dBytes.size >= 112) {
                                    dexBuffers.add(dBytes)
                                }
                            }
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

        // Deep Dalvik Bytecode & Opcode Dissection
        var dexReport: DexInspectionReport? = null
        if (dexBuffers.isNotEmpty()) {
            val reports = dexBuffers.mapIndexed { idx, bytes ->
                DexBytecodeScanner.parseDexBytes(bytes, "classes${if (idx == 0) "" else "${idx + 1}"}.dex")
            }
            val allLoops = reports.flatMap { it.detectedLoops }.distinctBy { "${it.className}.${it.methodName}:${it.loopType}" }
            val allSignatures = reports.flatMap { it.malwareSignatures }.distinctBy { "${it.category}:${it.matchedPattern}" }
            val allSnippets = reports.flatMap { it.codeSnippets }.distinctBy { "${it.className}.${it.methodName}" }
            val totalClasses = reports.sumOf { it.classesCount }
            val totalMethods = reports.sumOf { it.methodsCount }
            val totalStrings = reports.sumOf { it.stringsCount }
            val maxBytecodeRisk = reports.maxOfOrNull { it.riskScore } ?: 0

            dexReport = DexInspectionReport(
                classesCount = totalClasses,
                methodsCount = totalMethods,
                stringsCount = totalStrings,
                detectedLoops = allLoops,
                malwareSignatures = allSignatures,
                codeSnippets = allSnippets,
                riskScore = maxBytecodeRisk,
                summary = "Dissected $totalClasses classes ($totalMethods methods). ${allLoops.size} malicious loop(s), ${allSignatures.size} malware signature(s) flagged."
            )

            // Inject bytecode findings into whyPoints
            allLoops.forEach { loop ->
                whyPoints.add(" Dalvik Bytecode Loop [${loop.loopType}]: ${loop.className}.${loop.methodName}() - ${loop.explanation}")
            }
            allSignatures.forEach { sig ->
                whyPoints.add(" Bytecode Malware Signature [${sig.category}]: ${sig.matchedPattern} - ${sig.description}")
            }
        }

        // Assess risk based on permissions and APK structure
        var riskScore = dexReport?.riskScore ?: 0

        if (!archiveSafe) {
            riskScore = maxOf(riskScore, 85)
        }

        if (isApk) {
            if (!foundManifest) {
                riskScore += 15
                whyPoints.add("Missing or corrupt AndroidManifest.xml inside package")
            }
            if (!foundDex) {
                riskScore += 15
                whyPoints.add("No compiled classes.dex executable found in package")
            }
            if (isDebuggable) {
                whyPoints.add("Package compiled with android:debuggable='true' (development build)")
            }

            // Dangerous permission combinations - multi-evidence correlation:
            // 1. Toxic Banking Trojan: Accessibility + Overlay
            val hasAccessibility = permissions.any { it.contains("BIND_ACCESSIBILITY_SERVICE", ignoreCase = true) }
            val hasOverlay = permissions.any { it.contains("SYSTEM_ALERT_WINDOW", ignoreCase = true) }
            if (hasAccessibility && hasOverlay) {
                val hasTrojanSig = dexReport?.malwareSignatures?.any { it.category == "BANKING_TROJAN" } == true
                riskScore = if (hasTrojanSig) maxOf(riskScore, 90) else maxOf(riskScore, 75)
                whyPoints.add("Toxic Banking Trojan pattern: Accessibility Service combined with Screen Overlay for deceptive overlay interception")
            } else if (hasAccessibility) {
                riskScore += 15
                whyPoints.add("Accessibility Service capability declared")
            }

            // 2. Potential OTP Interception: SMS + Internet
            val hasSms = permissions.any { it.contains("READ_SMS", ignoreCase = true) || it.contains("RECEIVE_SMS", ignoreCase = true) }
            val hasInternet = permissions.any { it.contains("INTERNET", ignoreCase = true) }
            if (hasSms && hasInternet) {
                val hasStealerSig = dexReport?.malwareSignatures?.any { it.category == "SMS_OTP_INTERCEPTOR" } == true
                if (hasStealerSig) {
                    riskScore = maxOf(riskScore, 90)
                    whyPoints.add(" Verified SMS/OTP Exfiltration Trojan: SMS reader with remote webhook endpoint")
                } else {
                    riskScore += 5
                    whyPoints.add("SMS 2FA verification capability declared with Internet access")
                }
            }

            // 3. Toxic Privilege Escalation: Device Admin + Request Install
            val hasAdmin = permissions.any { it.contains("BIND_DEVICE_ADMIN", ignoreCase = true) }
            val hasInstall = permissions.any { it.contains("REQUEST_INSTALL_PACKAGES", ignoreCase = true) }
            if (hasAdmin && hasInstall) {
                val hasDropperSig = dexReport?.malwareSignatures?.any { it.category == "DYNAMIC_CLASSLOADER_DROPPER" } == true
                if (hasDropperSig) {
                    riskScore = maxOf(riskScore, 85)
                    whyPoints.add(" Confirmed Dropper / Stager: Device Admin rights combined with dynamic package installation")
                } else {
                    riskScore += 15
                    whyPoints.add("Privilege Escalation capability: Device Admin rights combined with package installation permission")
                }
            }

            // 4. Media & Location permissions (Standard app behavior)
            val hasRecording = permissions.any { it.contains("RECORD_AUDIO", ignoreCase = true) || it.contains("CAMERA", ignoreCase = true) }
            val hasPersonalData = permissions.any {
                it.contains("READ_CONTACTS", ignoreCase = true) ||
                it.contains("READ_CALL_LOG", ignoreCase = true) ||
                it.contains("ACCESS_FINE_LOCATION", ignoreCase = true)
            }
            if (hasRecording && hasPersonalData && hasInternet) {
                whyPoints.add("Standard interactive media permissions declared (Audio/Camera/Location with Internet)")
            }

            // If no malicious bytecode, no toxic combinations, and no archive violations, cap permission contribution at 30
            val hasBytecodeMalware = dexReport?.malwareSignatures?.isNotEmpty() == true || dexReport?.detectedLoops?.isNotEmpty() == true
            val hasToxicCombo = (hasAccessibility && hasOverlay) || (hasAdmin && hasInstall) || (hasSms && hasInternet)
            if (!hasBytecodeMalware && !hasToxicCombo && archiveSafe) {
                riskScore = minOf(riskScore, 30)
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

        var hasSteganography = false
        var stegoReport: String? = null

        // Deep Steganography & Polyglot Payload Detection for all files (images, audio, video, docs)
        val stegoResult = detectSteganographyAndPolyglot(filename, inputStreamProvider, fileSize)
        if (stegoResult.isThreat) {
            hasSteganography = true
            riskScore = maxOf(riskScore, stegoResult.riskScore)
            whyPoints.addAll(0, stegoResult.whyPoints)
            stegoReport = stegoResult.summary
        } else if (!isApk && !isZip) {
            if (stegoResult.whyPoints.isNotEmpty()) {
                whyPoints.clear()
                whyPoints.addAll(stegoResult.whyPoints)
            }
            riskScore = maxOf(riskScore, stegoResult.riskScore)
        }

        val clampedScore = riskScore.coerceIn(0, 100)
        // Dynamic confidence calibration based on evidence completeness
        val confidence = when {
            hasSteganography -> 0.95f
            dexReport != null && (dexReport.malwareSignatures.isNotEmpty() || dexReport.detectedLoops.isNotEmpty()) -> 0.92f
            isApk && foundManifest && foundDex -> 0.88f
            isApk -> 0.75f
            else -> 0.80f
        }

        return LocalFileAnalysisResult(
            filename = filename,
            sha256 = sha256,
            isApk = isApk,
            riskScore = clampedScore,
            confidence = confidence,
            whyPoints = whyPoints,
            permissions = permissions.toList().sorted(),
            archiveSafe = archiveSafe,
            isDebuggable = isDebuggable,
            dexReport = dexReport,
            hasSteganography = hasSteganography,
            stegoReport = stegoReport
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

    data class StegoCheckResult(
        val isThreat: Boolean,
        val riskScore: Int,
        val whyPoints: List<String>,
        val summary: String? = null
    )

    private fun readSampleBytes(inputStreamProvider: () -> InputStream, maxBytes: Int): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        inputStreamProvider().use { input ->
            var bytesRead: Int
            var total = 0
            while (input.read(buffer).also { bytesRead = it } != -1) {
                out.write(buffer, 0, bytesRead)
                total += bytesRead
                if (total >= maxBytes) break
            }
        }
        return out.toByteArray()
    }

    fun calculateEntropy(bytes: ByteArray, offset: Int, length: Int): Double {
        if (length <= 0) return 0.0
        val counts = IntArray(256)
        val end = minOf(offset + length, bytes.size)
        val count = end - offset
        if (count <= 0) return 0.0
        for (i in offset until end) {
            counts[bytes[i].toInt() and 0xFF]++
        }
        var entropy = 0.0
        val lenDouble = count.toDouble()
        for (c in counts) {
            if (c > 0) {
                val p = c / lenDouble
                entropy -= p * (Math.log(p) / Math.log(2.0))
            }
        }
        return entropy
    }

    fun detectSteganographyAndPolyglot(
        filename: String,
        inputStreamProvider: () -> InputStream,
        fileSize: Long
    ): StegoCheckResult {
        val why = mutableListOf<String>()
        val maxInspectBytes = minOf(10 * 1024 * 1024L, if (fileSize > 0) fileSize else 10 * 1024 * 1024L).toInt()
        val bytes = try {
            readSampleBytes(inputStreamProvider, maxInspectBytes)
        } catch (_: Exception) {
            return StegoCheckResult(false, 0, emptyList())
        }

        if (bytes.size < 16) {
            return StegoCheckResult(false, 0, emptyList())
        }

        val ext = filename.substringAfterLast('.', "").lowercase(Locale.ROOT)
        var isThreat = false
        var risk = 0

        // 1. JPEG Steganography & Appended Data Check
        // Proper forward segment walking to avoid EXIF thumbnail false positives
        val isJpeg = bytes.size >= 4 &&
                (bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte())
        if (isJpeg) {
            var offset = 2
            var inScan = false
            var realEoi = -1

            while (offset < bytes.size - 1) {
                if (!inScan) {
                    if (bytes[offset] != 0xFF.toByte()) {
                        offset++
                        continue
                    }
                    // Skip consecutive 0xFF bytes
                    while (offset < bytes.size - 1 && bytes[offset + 1] == 0xFF.toByte()) {
                        offset++
                    }
                    if (offset >= bytes.size - 1) break
                    val marker = bytes[offset + 1].toInt() and 0xFF
                    offset += 2 // Skip marker bytes

                    if (marker == 0xD8) { // SOI
                        continue
                    } else if (marker == 0xD9) { // EOI
                        realEoi = offset - 2
                        break
                    } else if (marker == 0xDA) { // SOS (Start of Scan) - entropy-coded data begins
                        if (offset + 2 <= bytes.size) {
                            val sosLen = ((bytes[offset].toInt() and 0xFF) shl 8) or (bytes[offset + 1].toInt() and 0xFF)
                            offset += sosLen
                            inScan = true
                        } else {
                            break
                        }
                    } else if (marker in 0xD0..0xD7 || marker == 0x01 || marker == 0x00) {
                        // Standalone markers without length
                        continue
                    } else {
                        // Variable-length segment: read 2-byte big-endian length
                        if (offset + 2 <= bytes.size) {
                            val segLen = ((bytes[offset].toInt() and 0xFF) shl 8) or (bytes[offset + 1].toInt() and 0xFF)
                            if (segLen < 2) break
                            offset += segLen
                        } else {
                            break
                        }
                    }
                } else {
                    // Inside entropy scan: search for unescaped marker
                    if (bytes[offset] == 0xFF.toByte()) {
                        val nextByte = bytes[offset + 1].toInt() and 0xFF
                        if (nextByte == 0x00 || nextByte in 0xD0..0xD7) {
                            // Escaped 0xFF or restart marker - continue scan
                            offset += 2
                        } else if (nextByte == 0xD9) { // Real EOI reached
                            realEoi = offset
                            break
                        } else if (nextByte in 0xC0..0xFE) {
                            // Progressive scan marker: exit scan mode to read next marker
                            inScan = false
                            offset += 2
                        } else {
                            offset++
                        }
                    } else {
                        offset++
                    }
                }
            }

            // Fallback backward search if forward parse truncated before EOI
            if (realEoi == -1) {
                for (i in (bytes.size - 2) downTo maxOf(2, bytes.size - 4096)) {
                    if (bytes[i] == 0xFF.toByte() && bytes[i + 1] == 0xD9.toByte()) {
                        realEoi = i
                        break
                    }
                }
            }

            if (realEoi != -1 && realEoi < bytes.size - 32) {
                val trailerOffset = realEoi + 2
                val trailerSize = bytes.size - trailerOffset
                if (trailerSize > 32) {
                    val hasPk = containsBytePattern(bytes, trailerOffset, trailerSize, byteArrayOf(0x50, 0x4B, 0x03, 0x04))
                    val hasDex = containsBytePattern(bytes, trailerOffset, trailerSize, byteArrayOf(0x64, 0x65, 0x78, 0x0A))
                    val hasElf = containsBytePattern(bytes, trailerOffset, trailerSize, byteArrayOf(0x7F, 0x45, 0x4C, 0x46))
                    val hasMz = containsBytePattern(bytes, trailerOffset, minOf(trailerSize, 512), byteArrayOf(0x4D, 0x5A))

                    if (hasPk) {
                        isThreat = true
                        risk = maxOf(risk, 95)
                        why.add(" Steganography Polyglot: Hidden ZIP/APK archive ($trailerSize bytes) appended after JPEG End-of-Image (EOI) marker")
                    } else if (hasDex) {
                        isThreat = true
                        risk = maxOf(risk, 95)
                        why.add(" Steganography Dropper: Dalvik DEX executable bytecode embedded in JPEG trailer")
                    } else if (hasElf || hasMz) {
                        isThreat = true
                        risk = maxOf(risk, 92)
                        why.add(" Steganography Binary: Native executable code appended after JPEG image")
                    }
                }
            }
        }

        // 2. PNG Steganography & Appended Data Check
        val isPng = bytes.size >= 8 &&
                bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() && bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte()
        if (isPng) {
            val iendPattern = byteArrayOf(0x49, 0x45, 0x4E, 0x44) // "IEND"
            val iendIdx = indexOfBytePattern(bytes, 0, bytes.size, iendPattern)
            if (iendIdx != -1) {
                val trailerOffset = iendIdx + 8 // 4 bytes IEND + 4 bytes CRC
                if (trailerOffset < bytes.size - 24) {
                    val trailerSize = bytes.size - trailerOffset
                    if (trailerSize > 32) {
                        val hasPk = containsBytePattern(bytes, trailerOffset, trailerSize, byteArrayOf(0x50, 0x4B, 0x03, 0x04))
                        val hasDex = containsBytePattern(bytes, trailerOffset, trailerSize, byteArrayOf(0x64, 0x65, 0x78, 0x0A))
                        val hasElf = containsBytePattern(bytes, trailerOffset, trailerSize, byteArrayOf(0x7F, 0x45, 0x4C, 0x46))
                        val hasMz = containsBytePattern(bytes, trailerOffset, minOf(trailerSize, 512), byteArrayOf(0x4D, 0x5A))

                        if (hasPk || hasDex) {
                            isThreat = true
                            risk = maxOf(risk, 95)
                            why.add(" PNG Steganography: Executable payload/archive hidden after PNG IEND chunk ($trailerSize bytes)")
                        } else if (hasElf || hasMz) {
                            isThreat = true
                            risk = maxOf(risk, 92)
                            why.add(" PNG Steganography: Native executable binary hidden after PNG IEND chunk ($trailerSize bytes)")
                        }
                    }
                }
            }
        }

        // 3. GIF Steganography Check
        val isGif = bytes.size >= 6 &&
                bytes[0] == 'G'.code.toByte() && bytes[1] == 'I'.code.toByte() && bytes[2] == 'F'.code.toByte()
        if (isGif) {
            var lastTrailer = -1
            for (i in (bytes.size - 1) downTo 6) {
                if (bytes[i] == 0x3B.toByte()) {
                    lastTrailer = i
                    break
                }
            }
            if (lastTrailer != -1 && lastTrailer < bytes.size - 32) {
                val trailerOffset = lastTrailer + 1
                val trailerSize = bytes.size - trailerOffset
                val hasPk = containsBytePattern(bytes, trailerOffset, trailerSize, byteArrayOf(0x50, 0x4B, 0x03, 0x04))
                val hasDex = containsBytePattern(bytes, trailerOffset, trailerSize, byteArrayOf(0x64, 0x65, 0x78, 0x0A))
                if (hasPk || hasDex) {
                    isThreat = true
                    risk = maxOf(risk, 92)
                    why.add(" GIF Steganography: Hidden archive payload appended after GIF 0x3B trailer")
                }
            }
        }

        // 4. Concealed Polyglot APK Check (Any non-APK file masking as APK)
        if (ext !in listOf("apk", "xapk", "apks", "zip", "jar")) {
            val pkIdx = indexOfBytePattern(bytes, 0, minOf(bytes.size, 1024), byteArrayOf(0x50, 0x4B, 0x03, 0x04))
            if (pkIdx != -1) {
                val hasManifest = containsStringPattern(bytes, "AndroidManifest.xml")
                val hasDex = containsStringPattern(bytes, "classes.dex")
                if (hasManifest || hasDex) {
                    isThreat = true
                    risk = maxOf(risk, 98)
                    why.add(" Critical Polyglot Malware: Executable APK package masquerading as nominal .$ext file")
                }
            }
            // Disguised Dalvik DEX at start of non-DEX file
            if (ext != "dex" && bytes.size >= 8 &&
                bytes[0] == 'd'.code.toByte() && bytes[1] == 'e'.code.toByte() && bytes[2] == 'x'.code.toByte() && bytes[3] == 0x0A.toByte()
            ) {
                isThreat = true
                risk = maxOf(risk, 98)
                why.add(" Critical Executable Spoofing: Dalvik DEX bytecode disguised as nominal .$ext file")
            }
            // Disguised Linux ELF at start of non-ELF file
            if (ext != "so" && ext != "elf" && bytes.size >= 4 &&
                bytes[0] == 0x7F.toByte() && bytes[1] == 'E'.code.toByte() && bytes[2] == 'L'.code.toByte() && bytes[3] == 'F'.code.toByte()
            ) {
                isThreat = true
                risk = maxOf(risk, 98)
                why.add(" Critical Executable Spoofing: Native Linux ELF executable disguised as nominal .$ext file")
            }
        }

        if (!isThreat) {
            if (ext in listOf("jpg", "jpeg", "png", "gif", "webp", "bmp")) {
                why.add("Nominal image structure verified; zero appended payloads, polyglots, or steganographic anomalies")
                risk = 5
            } else {
                why.add("File structure verified safe; no hidden executable polyglot patterns detected")
                risk = 5
            }
        }

        val summary = if (isThreat) why.firstOrNull() else "Steganography audit clean"
        return StegoCheckResult(isThreat, risk, why, summary)
    }

    private fun containsBytePattern(bytes: ByteArray, offset: Int, length: Int, pattern: ByteArray): Boolean {
        return indexOfBytePattern(bytes, offset, length, pattern) != -1
    }

    private fun indexOfBytePattern(bytes: ByteArray, offset: Int, length: Int, pattern: ByteArray): Int {
        if (pattern.isEmpty() || length < pattern.size) return -1
        val end = minOf(offset + length, bytes.size) - pattern.size
        for (i in offset..end) {
            var match = true
            for (j in pattern.indices) {
                if (bytes[i + j] != pattern[j]) {
                    match = false
                    break
                }
            }
            if (match) return i
        }
        return -1
    }

    private fun containsStringPattern(bytes: ByteArray, target: String): Boolean {
        val targetBytes = target.toByteArray(Charsets.UTF_8)
        return indexOfBytePattern(bytes, 0, bytes.size, targetBytes) != -1
    }
}
