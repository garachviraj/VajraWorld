package com.vajraworld.defender.domain.engine

import android.util.Log
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

data class BytecodeLoopFinding(
    val className: String,
    val methodName: String,
    val loopType: String, // "TIGHT_CPU_DOS_LOOP", "FORK_BOMB_PROCESS_LOOP", "DDOS_FLOODING_LOOP", "BUSY_WAIT_BATTERY_DRAINER"
    val branchOffset: Int,
    val cycleInstructionCount: Int,
    val explanation: String,
    val severity: String = "CRITICAL"
)

data class BytecodeMalwareSignature(
    val category: String, // "BANKING_TROJAN", "SMS_OTP_INTERCEPTOR", "DYNAMIC_CLASSLOADER_DROPPER", "ROOTKIT_SHELL_EXECUTION", "RANSOMWARE_ENCRYPTION", "COVERT_SPYWARE"
    val severity: String, // "CRITICAL", "HIGH", "MEDIUM"
    val matchedPattern: String,
    val location: String,
    val description: String
)

data class CodeDissectionSnippet(
    val className: String,
    val methodName: String,
    val snippetType: String,
    val pseudocode: String,
    val riskWarning: String
)

data class DexInspectionReport(
    val classesCount: Int,
    val methodsCount: Int,
    val stringsCount: Int,
    val detectedLoops: List<BytecodeLoopFinding>,
    val malwareSignatures: List<BytecodeMalwareSignature>,
    val codeSnippets: List<CodeDissectionSnippet>,
    val riskScore: Int,
    val summary: String
)

object DexBytecodeScanner {

    private const val TAG = "DexBytecodeScanner"
    private const val MAX_STRINGS_TO_PARSE = 20000
    private const val MAX_CLASSES_TO_PARSE = 2000
    private const val MAX_DEX_FILES_IN_APK = 2

    /**
     * Scan an APK file on disk, extracting classes.dex and classes2.dex to run deep bytecode analysis.
     */
    fun scanApkFile(apkFile: File): DexInspectionReport {
        if (!apkFile.exists() || !apkFile.canRead()) {
            return emptyReport("File unreadable: ${apkFile.name}")
        }
        return try {
            FileInputStream(apkFile).use { scanApkStream(it, apkFile.name) }
        } catch (e: Throwable) {
            Log.e(TAG, "Error scanning APK file: ${e.message}", e)
            emptyReport("Error parsing APK: ${e.message}")
        }
    }

    /**
     * Scan an APK from an InputStream.
     */
    fun scanApkStream(inputStream: InputStream, filename: String): DexInspectionReport {
        var totalClasses = 0
        var totalMethods = 0
        var totalStrings = 0
        val allLoops = mutableListOf<BytecodeLoopFinding>()
        val allSignatures = mutableListOf<BytecodeMalwareSignature>()
        val allSnippets = mutableListOf<CodeDissectionSnippet>()
        var highestRisk = 0
        var dexParsedCount = 0

        try {
            ZipInputStream(inputStream).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null && dexParsedCount < MAX_DEX_FILES_IN_APK) {
                    val name = entry.name
                    if (name.startsWith("classes") && name.endsWith(".dex")) {
                        // Max 3MB sample per DEX to prevent OutOfMemoryError on large APKs
                        val bytes = readEntryBytes(zis, maxBytes = 3 * 1024 * 1024)
                        if (bytes.size >= 112) {
                            try {
                                val report = parseDexBytes(bytes, name)
                                totalClasses += report.classesCount
                                totalMethods += report.methodsCount
                                totalStrings += report.stringsCount
                                allLoops.addAll(report.detectedLoops)
                                allSignatures.addAll(report.malwareSignatures)
                                allSnippets.addAll(report.codeSnippets)
                                if (report.riskScore > highestRisk) highestRisk = report.riskScore
                                dexParsedCount++
                            } catch (e: Throwable) {
                                Log.w(TAG, "Error parsing DEX entry $name in $filename: ${e.message}")
                            }
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Archive read error on $filename: ${e.message}")
        }

        if (dexParsedCount == 0) {
            return emptyReport("No classes.dex found in $filename")
        }

        // De-duplicate findings
        val uniqueLoops = allLoops.distinctBy { "${it.className}.${it.methodName}:${it.loopType}" }
        val uniqueSignatures = allSignatures.distinctBy { "${it.category}:${it.matchedPattern}" }
        val uniqueSnippets = allSnippets.distinctBy { "${it.className}.${it.methodName}" }

        val finalRisk = calculateOverallRisk(uniqueLoops, uniqueSignatures, highestRisk)
        val summary = buildSummary(totalClasses, totalMethods, uniqueLoops, uniqueSignatures)

        return DexInspectionReport(
            classesCount = totalClasses,
            methodsCount = totalMethods,
            stringsCount = totalStrings,
            detectedLoops = uniqueLoops,
            malwareSignatures = uniqueSignatures,
            codeSnippets = uniqueSnippets,
            riskScore = finalRisk,
            summary = summary
        )
    }

    /**
     * Direct parser for DEX bytecode buffer.
     */
    fun parseDexBytes(bytes: ByteArray, dexIdentifier: String = "classes.dex"): DexInspectionReport {
        if (bytes.size < 112) return emptyReport("Truncated DEX buffer (< 112 bytes)")

        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

        // Validate DEX magic: 'dex\n035\0' or 'dex\n037\0' or 'dex\n038\0' or 'dex\n039\0'
        val magic = ByteArray(8)
        buffer.get(magic)
        if (magic[0] != 'd'.code.toByte() || magic[1] != 'e'.code.toByte() || magic[2] != 'x'.code.toByte() || magic[3] != 0x0A.toByte()) {
            return emptyReport("Invalid DEX magic header")
        }

        buffer.position(32)
        val fileSize = buffer.int
        buffer.position(56)
        val stringIdsSize = buffer.int.coerceIn(0, MAX_STRINGS_TO_PARSE)
        val stringIdsOff = buffer.int
        val typeIdsSize = buffer.int.coerceIn(0, 15000)
        val typeIdsOff = buffer.int
        val protoIdsSize = buffer.int
        val protoIdsOff = buffer.int
        val fieldIdsSize = buffer.int
        val fieldIdsOff = buffer.int
        val methodIdsSize = buffer.int.coerceIn(0, 25000)
        val methodIdsOff = buffer.int
        val classDefsSize = buffer.int.coerceIn(0, MAX_CLASSES_TO_PARSE)
        val classDefsOff = buffer.int

        // 1. Extract String Table
        val stringTable = ArrayList<String>(stringIdsSize)
        try {
            for (i in 0 until stringIdsSize) {
                val offPos = stringIdsOff + i * 4
                if (offPos + 4 > bytes.size) break
                buffer.position(offPos)
                val dataOff = buffer.int
                if (dataOff in 0 until bytes.size) {
                    buffer.position(dataOff)
                    val utf16Len = readUleb128(buffer)
                    val str = readMutf8String(buffer)
                    stringTable.add(str)
                } else {
                    stringTable.add("<off_bound>")
                }
            }
        } catch (_: Exception) {}

        // 2. Extract Type Table (maps type_idx -> string descriptor)
        val typeTable = ArrayList<String>(typeIdsSize)
        try {
            for (i in 0 until typeIdsSize) {
                val offPos = typeIdsOff + i * 4
                if (offPos + 4 > bytes.size) break
                buffer.position(offPos)
                val descriptorIdx = buffer.int
                val typeStr = stringTable.getOrElse(descriptorIdx) { "Lunknown/Type$i;" }
                typeTable.add(typeStr)
            }
        } catch (_: Exception) {}

        // 3. Extract Method IDs (maps method_idx -> Pair(className, methodName))
        data class MethodRef(val className: String, val methodName: String)
        val methodTable = ArrayList<MethodRef>(methodIdsSize)
        try {
            for (i in 0 until methodIdsSize) {
                val offPos = methodIdsOff + i * 8
                if (offPos + 8 > bytes.size) break
                buffer.position(offPos)
                val classIdx = buffer.short.toInt() and 0xFFFF
                val protoIdx = buffer.short.toInt() and 0xFFFF
                val nameIdx = buffer.int
                val className = typeTable.getOrElse(classIdx) { "Lunknown/Class;" }
                val methodName = stringTable.getOrElse(nameIdx) { "method$i" }
                methodTable.add(MethodRef(className, methodName))
            }
        } catch (_: Exception) {}

        // 4. Heuristic Scan across String Table for Known Malware / Spyware / Trojan Indicators
        val effectiveStrings = if (stringTable.isNotEmpty()) {
            stringTable
        } else {
            extractRawAsciiStrings(bytes)
        }
        val signatures = mutableListOf<BytecodeMalwareSignature>()
        scanStringPoolForThreatSignatures(effectiveStrings, signatures)

        // 5. Parse Class Definitions & Disassemble Dalvik Bytecode for Loops & Invocations
        val detectedLoops = mutableListOf<BytecodeLoopFinding>()
        val codeSnippets = mutableListOf<CodeDissectionSnippet>()

        try {
            for (i in 0 until classDefsSize) {
                val defOff = classDefsOff + i * 32
                if (defOff + 32 > bytes.size) break
                buffer.position(defOff)
                val classIdx = buffer.int
                val accessFlags = buffer.int
                val superclassIdx = buffer.int
                val interfacesOff = buffer.int
                val sourceFileIdx = buffer.int
                val annotationsOff = buffer.int
                val classDataOff = buffer.int
                val staticValuesOff = buffer.int

                val className = typeTable.getOrElse(classIdx) { "Lclass$i;" }
                if (classDataOff <= 0 || classDataOff >= bytes.size) continue

                // Read Class Data
                buffer.position(classDataOff)
                val staticFieldsSize = readUleb128(buffer)
                val instanceFieldsSize = readUleb128(buffer)
                val directMethodsSize = readUleb128(buffer)
                val virtualMethodsSize = readUleb128(buffer)

                // Skip static & instance fields
                for (f in 0 until (staticFieldsSize + instanceFieldsSize)) {
                    readUleb128(buffer) // field_idx_diff
                    readUleb128(buffer) // access_flags
                }

                // Process Direct & Virtual Methods
                val totalMethodsInClass = directMethodsSize + virtualMethodsSize
                var currentMethodIdx = 0
                for (m in 0 until totalMethodsInClass) {
                    val methodIdxDiff = readUleb128(buffer)
                    currentMethodIdx += methodIdxDiff
                    val methodAccessFlags = readUleb128(buffer)
                    val codeOff = readUleb128(buffer)

                    if (codeOff > 0 && codeOff + 16 <= bytes.size) {
                        val methodRef = methodTable.getOrNull(currentMethodIdx)
                        val methodName = methodRef?.methodName ?: "method_$currentMethodIdx"

                        inspectMethodBytecode(
                            buffer = buffer,
                            codeOff = codeOff,
                            bytes = bytes,
                            className = className,
                            methodName = methodName,
                            methodTable = methodTable,
                            detectedLoops = detectedLoops,
                            signatures = signatures,
                            codeSnippets = codeSnippets
                        )
                    }
                }
            }
        } catch (_: Exception) {}

        val riskScore = calculateOverallRisk(detectedLoops, signatures, 0)
        val summary = buildSummary(classDefsSize, methodIdsSize, detectedLoops, signatures)

        return DexInspectionReport(
            classesCount = classDefsSize,
            methodsCount = methodIdsSize,
            stringsCount = stringTable.size,
            detectedLoops = detectedLoops.distinctBy { "${it.className}.${it.methodName}:${it.loopType}" },
            malwareSignatures = signatures.distinctBy { "${it.category}:${it.matchedPattern}" },
            codeSnippets = codeSnippets.distinctBy { "${it.className}.${it.methodName}" },
            riskScore = riskScore,
            summary = summary
        )
    }

    /**
     * Inspect Dalvik bytecode instructions inside a code_item to find loops, process forks, and malware calls.
     */
    private fun inspectMethodBytecode(
        buffer: ByteBuffer,
        codeOff: Int,
        bytes: ByteArray,
        className: String,
        methodName: String,
        methodTable: List<Any>,
        detectedLoops: MutableList<BytecodeLoopFinding>,
        signatures: MutableList<BytecodeMalwareSignature>,
        codeSnippets: MutableList<CodeDissectionSnippet>
    ) {
        // Exclude standard platform and framework classes to eliminate false positives
        if (className.startsWith("Landroid/") ||
            className.startsWith("Landroidx/") ||
            className.startsWith("Lkotlin/") ||
            className.startsWith("Lkotlinx/") ||
            className.startsWith("Ljava/") ||
            className.startsWith("Ljavax/") ||
            className.startsWith("Lcom/google/") ||
            className.startsWith("Lorg/apache/") ||
            className.startsWith("Lcom/android/") ||
            className.startsWith("Lokio/") ||
            className.startsWith("Lokhttp3/") ||
            className.startsWith("Lcom/facebook/") ||
            className.startsWith("Lio/reactivex/") ||
            className.startsWith("Lorg/jetbrains/") ||
            className.startsWith("Lcom/airbnb/") ||
            className.startsWith("Lcom/squareup/") ||
            className.startsWith("Lcom/github/") ||
            className.startsWith("Lcom/bumptech/") ||
            className.startsWith("Lorg/chromium/") ||
            className.startsWith("Lretrofit2/") ||
            className.startsWith("Ldagger/") ||
            className.startsWith("Lorg/koin/")
        ) {
            return
        }

        val savePos = buffer.position()
        try {
            buffer.position(codeOff)
            val registersSize = buffer.short.toInt() and 0xFFFF
            val insSize = buffer.short.toInt() and 0xFFFF
            val outsSize = buffer.short.toInt() and 0xFFFF
            val triesSize = buffer.short.toInt() and 0xFFFF
            val debugInfoOff = buffer.int
            val insnsSize = buffer.int.coerceIn(0, 10000)

            if (insnsSize <= 0 || codeOff + 16 + insnsSize * 2 > bytes.size) return

            val insns = ShortArray(insnsSize)
            for (k in 0 until insnsSize) {
                insns[k] = buffer.short
            }

            var pc = 0
            while (pc < insnsSize) {
                val insn = insns[pc].toInt() and 0xFFFF
                val opcode = insn and 0xFF

                var branchOffset = 0
                var isBackwardJump = false

                var isUnconditionalBackwardGoto = false

                when (opcode) {
                    0x28 -> { // goto +AA (8-bit signed)
                        val offset8 = (insn shr 8).toByte().toInt()
                        if (offset8 < 0) {
                            branchOffset = offset8
                            isBackwardJump = true
                            isUnconditionalBackwardGoto = true
                        }
                    }
                    0x29 -> { // goto/16 +AAAA (16-bit signed)
                        if (pc + 1 < insnsSize) {
                            val offset16 = insns[pc + 1].toInt()
                            if (offset16 < 0) {
                                branchOffset = offset16
                                isBackwardJump = true
                                isUnconditionalBackwardGoto = true
                            }
                        }
                    }
                    0x2A -> { // goto/32 +AAAAAAAA (32-bit signed)
                        if (pc + 2 < insnsSize) {
                            val low = insns[pc + 1].toInt() and 0xFFFF
                            val high = insns[pc + 2].toInt() and 0xFFFF
                            val offset32 = (high shl 16) or low
                            if (offset32 < 0) {
                                branchOffset = offset32
                                isBackwardJump = true
                                isUnconditionalBackwardGoto = true
                            }
                        }
                    }
                    in 0x32..0x3D -> { // conditional if-* (normal for/while bound)
                        if (pc + 1 < insnsSize) {
                            val offset16 = insns[pc + 1].toInt()
                            if (offset16 < 0) {
                                branchOffset = offset16
                                isBackwardJump = true
                                isUnconditionalBackwardGoto = false // Normal conditional loop
                            }
                        }
                    }
                }

                if (isBackwardJump) {
                    val targetPc = (pc + branchOffset).coerceAtLeast(0)
                    val cycleLength = pc - targetPc

                    var hasSleepOrYield = false
                    var hasProcessExec = false
                    var hasRawSocketFlood = false
                    var hasConditionalExit = false

                    for (cyclePc in targetPc until pc) {
                        val cInsn = insns[cyclePc].toInt() and 0xFFFF
                        val cOpcode = cInsn and 0xFF

                        // Check if loop has a conditional branch exiting the loop
                        if (cOpcode in 0x32..0x3D) {
                            hasConditionalExit = true
                        }

                        if (cOpcode in 0x6E..0x78) { // invoke-*
                            if (cyclePc + 1 < insnsSize) {
                                val methodIdx = insns[cyclePc + 1].toInt() and 0xFFFF
                                val invoked = methodTable.getOrNull(methodIdx)?.toString()?.lowercase(Locale.US) ?: ""
                                if (invoked.contains("sleep") || invoked.contains("delay") || invoked.contains("yield") || invoked.contains("wait")) {
                                    hasSleepOrYield = true
                                }
                                if (invoked.contains("runtime;->exec") || invoked.contains("processbuilder;->start")) {
                                    hasProcessExec = true
                                }
                                // Only flag raw unthrottled socket connection/packet flooding, not normal buffer stream writes
                                if (invoked.contains("socket;->connect") || invoked.contains("datagramsocket;->send")) {
                                    hasRawSocketFlood = true
                                }
                            }
                        }
                    }

                    val cleanClassName = formatClassName(className)

                    // ONLY flag as FORK BOMB if process execution is inside an unconditional loop
                    if (hasProcessExec && isUnconditionalBackwardGoto) {
                        detectedLoops.add(
                            BytecodeLoopFinding(
                                className = cleanClassName,
                                methodName = methodName,
                                loopType = "FORK_BOMB_PROCESS_LOOP",
                                branchOffset = branchOffset,
                                cycleInstructionCount = cycleLength,
                                explanation = "Infinite process execution invoked inside unconditional loop.",
                                severity = "CRITICAL"
                            )
                        )
                        codeSnippets.add(
                            CodeDissectionSnippet(
                                className = cleanClassName,
                                methodName = methodName,
                                snippetType = "FORK_BOMB_OPCODE_LOOP",
                                pseudocode = "while (true) {\n    Runtime.getRuntime().exec(\"...\");\n    // goto offset: $branchOffset\n}",
                                riskWarning = "Detected fork bomb process generation in loop."
                            )
                        )
                    } else if (hasRawSocketFlood && isUnconditionalBackwardGoto && !hasSleepOrYield && !hasConditionalExit) {
                        detectedLoops.add(
                            BytecodeLoopFinding(
                                className = cleanClassName,
                                methodName = methodName,
                                loopType = "DDOS_FLOODING_LOOP",
                                branchOffset = branchOffset,
                                cycleInstructionCount = cycleLength,
                                explanation = "Continuous unthrottled network socket writes in unconditional infinite loop.",
                                severity = "CRITICAL"
                            )
                        )
                    }
                }

                // Advance PC according to instruction format length
                val insnLen = getInstructionLength(opcode)
                pc += insnLen.coerceAtLeast(1)
            }
        } catch (_: Exception) {
        } finally {
            buffer.position(savePos)
        }
    }

    /**
     * Heuristic inspection of string table for malware, trojan, dropper, and spyware strings.
     */
    private fun scanStringPoolForThreatSignatures(
        strings: List<String>,
        signatures: MutableList<BytecodeMalwareSignature>
    ) {
        var hasAccessibilityActionClick = false
        var hasOverlayType = false
        var hasDexClassLoader = false
        var hasInMemoryDexClassLoader = false
        var hasSmsSend = false
        var hasDiscordWebhook = false
        var hasTelegramBot = false
        var hasSuRoot = false
        var hasAesCipher = false
        var hasFileRenameOrDelete = false
        var hasAudioRecord = false

        var hasRansomNote = false

        var foundAccessibility = false
        var foundPerformAction = false
        var foundActionClick = false

        for (s in strings) {
            val lower = s.lowercase(Locale.US)

            // Banking Trojan: Only flag if synthetic touch action click injection is present
            if (s.contains("AccessibilityNodeInfo") || s.contains("accessibility")) {
                foundAccessibility = true
            }
            if (s.contains("performAction")) {
                foundPerformAction = true
            }
            if (s.contains("ACTION_CLICK") || s.contains("16")) {
                foundActionClick = true
            }
            if ((s.contains("AccessibilityNodeInfo") || s.contains("accessibility")) &&
                (s.contains("ACTION_CLICK") || s.contains("16")) &&
                s.contains("performAction")) {
                hasAccessibilityActionClick = true
            }
            if (s.contains("AccessibilityNodeInfo;->performAction") && (s.contains("ACTION_CLICK") || s.contains("16"))) {
                hasAccessibilityActionClick = true
            }
            if (s.contains("TYPE_APPLICATION_OVERLAY") || s.contains("android.permission.SYSTEM_ALERT_WINDOW") || s.contains("SYSTEM_ALERT_WINDOW")) {
                hasOverlayType = true
            }

            // Dropper / Stager: Only in-memory dynamic bytecode execution
            if (s.contains("InMemoryDexClassLoader")) {
                hasInMemoryDexClassLoader = true
            }

            // SMS Stealer / Interceptor
            if (s.contains("SmsManager;->sendTextMessage") || s.contains("createFromPdu")) {
                hasSmsSend = true
            }
            if (lower.contains("discord.com/api/webhooks") || lower.contains("discordapp.com/api/webhooks")) {
                hasDiscordWebhook = true
            }
            if (lower.contains("api.telegram.org/bot")) {
                hasTelegramBot = true
            }

            // Rootkit / Command Exec (Explicit root escalation commands only, not mere root check path strings)
            if (lower.contains("chmod 777 /system") || lower.contains("mount -o remount,rw /system") || lower.contains("/system/bin/su -c") || lower.contains("su -c id")) {
                hasSuRoot = true
            }

            // Ransomware: MUST have explicit extortion note or ransomware lock extension
            if (lower.contains("your files are encrypted") ||
                lower.contains("your files have been encrypted") ||
                lower.contains("pay ransom to") ||
                lower.contains("decrypt_instructions") ||
                (lower.contains(".locked") && lower.contains(".crypto"))
            ) {
                hasRansomNote = true
            }
        }

        if (foundAccessibility && foundPerformAction && foundActionClick) {
            hasAccessibilityActionClick = true
        }

        if (hasAccessibilityActionClick && hasOverlayType) {
            signatures.add(
                BytecodeMalwareSignature(
                    category = "BANKING_TROJAN",
                    severity = "CRITICAL",
                    matchedPattern = "AccessibilityNodeInfo.performAction(ACTION_CLICK) + TYPE_APPLICATION_OVERLAY",
                    location = "classes.dex String & Method Pool",
                    description = "Synthetic touch injection with screen overlay hijack. Signature of modern banking trojans (Anatsa / Teabot / SharkBot) stealing banking credentials."
                )
            )
        }

        if (hasInMemoryDexClassLoader || hasDexClassLoader) {
            signatures.add(
                BytecodeMalwareSignature(
                    category = "DYNAMIC_CLASSLOADER_DROPPER",
                    severity = "HIGH",
                    matchedPattern = if (hasInMemoryDexClassLoader) "dalvik.system.InMemoryDexClassLoader" else "dalvik.system.DexClassLoader",
                    location = "classes.dex Class Loader Pool",
                    description = "Dynamic runtime classloader execution detected. Used by droppers to execute encrypted secondary payloads from memory or storage."
                )
            )
        }

        if (hasSmsSend && (hasDiscordWebhook || hasTelegramBot)) {
            signatures.add(
                BytecodeMalwareSignature(
                    category = "SMS_OTP_INTERCEPTOR",
                    severity = "CRITICAL",
                    matchedPattern = "SmsManager.sendTextMessage + C2 Webhook (${if (hasTelegramBot) "Telegram Bot API" else "Discord Webhook"})",
                    location = "classes.dex Telephony & HTTP Pool",
                    description = "Automatic SMS exfiltration beaconing directly to remote threat actor bot channels. Bypasses 2-Factor Authentication."
                )
            )
        }

        if (hasSuRoot) {
            signatures.add(
                BytecodeMalwareSignature(
                    category = "ROOTKIT_SHELL_EXECUTION",
                    severity = "HIGH",
                    matchedPattern = "Privileged Binary Target (/system/bin/su, chmod 777)",
                    location = "classes.dex Shell Command Pool",
                    description = "Hardcoded privilege escalation probe seeking root binary execution on device."
                )
            )
        }

        if (hasRansomNote) {
            signatures.add(
                BytecodeMalwareSignature(
                    category = "RANSOMWARE_ENCRYPTION",
                    severity = "CRITICAL",
                    matchedPattern = "Ransomware extortion artifact and encrypted file indicator",
                    location = "classes.dex String Table",
                    description = "Explicit mobile ransomware extortion notice or locked extension signature identified."
                )
            )
        }

        // Normal media recording is safe and standard
    }

    private fun getInstructionLength(opcode: Int): Int {
        return when (opcode) {
            0x00, 0x01, 0x04, 0x07, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F,
            0x10, 0x11, 0x12, 0x1D, 0x1E, 0x21, 0x27, 0x28, 0x7B, 0x7C,
            0x7D, 0x7E, 0x7F, 0x80, 0x81, 0x82, 0x83, 0x84, 0x85, 0x86,
            0x87, 0x88, 0x89, 0x8A, 0x8B, 0x8C, 0x8D, 0x8E, 0x8F, 0xB0,
            0xB1, 0xB2, 0xB3, 0xB4, 0xB5, 0xB6, 0xB7, 0xB8, 0xB9, 0xBA,
            0xBB, 0xBC, 0xBD, 0xBE, 0xBF, 0xC0, 0xC1, 0xC2, 0xC3, 0xC4,
            0xC5, 0xC6, 0xC7, 0xC8, 0xC9, 0xCA, 0xCB, 0xCC, 0xCD, 0xCE,
            0xCF, 0xD0, 0xD1, 0xD2, 0xD3, 0xD4, 0xD5, 0xD6, 0xD7 -> 1

            0x02, 0x05, 0x08, 0x13, 0x15, 0x16, 0x19, 0x1A, 0x1C, 0x1F,
            0x20, 0x22, 0x23, 0x29, 0x2D, 0x2E, 0x2F, 0x30, 0x31, 0x32,
            0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x3A, 0x3B, 0x3C,
            0x3D, 0x44, 0x45, 0x46, 0x47, 0x48, 0x49, 0x4A, 0x4B, 0x4C,
            0x4D, 0x4E, 0x4F, 0x50, 0x51, 0x52, 0x53, 0x54, 0x55, 0x56,
            0x57, 0x58, 0x59, 0x5A, 0x5B, 0x5C, 0x5D, 0x5E, 0x5F, 0x60,
            0x61, 0x62, 0x63, 0x64, 0x65, 0x66, 0x67, 0x68, 0x69, 0x6A,
            0x6B, 0x6C, 0x6D, 0x90, 0x91, 0x92, 0x93, 0x94, 0x95, 0x96,
            0x97, 0x98, 0x99, 0x9A, 0x9B, 0x9C, 0x9D, 0x9E, 0x9F, 0xA0,
            0xA1, 0xA2, 0xA3, 0xA4, 0xA5, 0xA6, 0xA7, 0xA8, 0xA9, 0xAA,
            0xAB, 0xAC, 0xAD, 0xAE, 0xAF, 0xD8, 0xD9, 0xDA, 0xDB, 0xDC,
            0xDD, 0xDE, 0xDF, 0xE0, 0xE1, 0xE2 -> 2

            0x03, 0x06, 0x09, 0x14, 0x17, 0x1B, 0x24, 0x25, 0x26, 0x2A,
            0x2B, 0x2C, 0x6E, 0x6F, 0x70, 0x71, 0x72, 0x74, 0x75, 0x76,
            0x77, 0x78, 0xFC, 0xFD, 0xFE, 0xFF -> 3

            0x18 -> 5 // const-wide/32 or const-string/jumbo
            else -> 1
        }
    }

    private fun readUleb128(buffer: ByteBuffer): Int {
        var result = 0
        var shift = 0
        while (buffer.hasRemaining()) {
            val byteVal = buffer.get().toInt() and 0xFF
            result = result or ((byteVal and 0x7F) shl shift)
            if ((byteVal and 0x80) == 0) break
            shift += 7
        }
        return result
    }

    private fun readMutf8String(buffer: ByteBuffer): String {
        val bytes = mutableListOf<Byte>()
        while (buffer.hasRemaining()) {
            val b = buffer.get()
            if (b == 0.toByte()) break
            bytes.add(b)
        }
        return try {
            String(bytes.toByteArray(), Charsets.UTF_8)
        } catch (_: Exception) {
            ""
        }
    }

    private fun extractRawAsciiStrings(bytes: ByteArray): List<String> {
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        for (b in bytes) {
            val c = b.toInt() and 0xFF
            if (c in 32..126) {
                sb.append(c.toChar())
            } else {
                if (sb.length >= 4) {
                    result.add(sb.toString())
                }
                sb.setLength(0)
            }
        }
        if (sb.length >= 4) {
            result.add(sb.toString())
        }
        return result
    }

    private fun readEntryBytes(zis: ZipInputStream, maxBytes: Int): ByteArray {
        return try {
            val out = java.io.ByteArrayOutputStream(minOf(maxBytes, 1024 * 1024))
            val buffer = ByteArray(8192)
            var totalRead = 0
            var read: Int
            while (zis.read(buffer).also { read = it } != -1) {
                out.write(buffer, 0, read)
                totalRead += read
                if (totalRead >= maxBytes) break
            }
            out.toByteArray()
        } catch (e: Throwable) {
            Log.w(TAG, "DEX entry read memory limit reached: ${e.message}")
            System.gc()
            ByteArray(0)
        }
    }

    private fun formatClassName(descriptor: String): String {
        return descriptor
            .removePrefix("L")
            .removeSuffix(";")
            .replace('/', '.')
    }

    private fun calculateOverallRisk(
        loops: List<BytecodeLoopFinding>,
        signatures: List<BytecodeMalwareSignature>,
        baselineRisk: Int
    ): Int {
        var risk = baselineRisk
        loops.forEach { loop ->
            when (loop.severity) {
                "CRITICAL" -> risk += 45
                "HIGH" -> risk += 30
                else -> risk += 15
            }
        }
        signatures.forEach { sig ->
            when (sig.severity) {
                "CRITICAL" -> risk += 50
                "HIGH" -> risk += 35
                else -> risk += 15
            }
        }
        return risk.coerceIn(0, 100)
    }

    private fun buildSummary(
        classesCount: Int,
        methodsCount: Int,
        loops: List<BytecodeLoopFinding>,
        signatures: List<BytecodeMalwareSignature>
    ): String {
        if (loops.isEmpty() && signatures.isEmpty()) {
            return "Audited $classesCount classes & $methodsCount methods. No malicious loops, droppers, or banking trojans detected in bytecode."
        }
        val threatCount = loops.size + signatures.size
        return "Dissected $classesCount classes ($methodsCount methods). Identified $threatCount bytecode security finding(s): ${loops.size} malicious loop(s), ${signatures.size} malware signature(s)."
    }

    private fun emptyReport(reason: String): DexInspectionReport {
        return DexInspectionReport(
            classesCount = 0,
            methodsCount = 0,
            stringsCount = 0,
            detectedLoops = emptyList(),
            malwareSignatures = emptyList(),
            codeSnippets = emptyList(),
            riskScore = 0,
            summary = reason
        )
    }
}
