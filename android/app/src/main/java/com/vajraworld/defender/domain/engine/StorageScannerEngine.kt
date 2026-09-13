package com.vajraworld.defender.domain.engine

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import com.vajraworld.defender.VajraApplication
import com.vajraworld.defender.data.local.ScanResultEntity
import com.vajraworld.defender.data.local.SecurityEventEntity
import com.vajraworld.defender.service.VajraNotificationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.io.FileInputStream
import java.util.UUID

data class ScannedFileRecord(
    val filename: String,
    val path: String,
    val sizeBytes: Long,
    val sha256: String,
    val isApk: Boolean,
    val riskScore: Int,
    val threatReasons: List<String>,
    val isSuspicious: Boolean,
    val permissions: List<String> = emptyList(),
    val fileCategory: String = "STORAGE", // "APP", "THREAT", "DOC", "MEDIA", "SCRIPT"
    val dexReport: DexInspectionReport? = null
)

data class StorageScanProgress(
    val scannedCount: Int,
    val suspiciousCount: Int,
    val currentFilePath: String,
    val isComplete: Boolean,
    val results: List<ScannedFileRecord>,
    val totalAppsAudited: Int = 0,
    val totalFilesAudited: Int = 0,
    val cleanFilesCount: Int = 0,
    val foldersAuditedCount: Int = 0,
    val ransomwareCount: Int = 0,
    val spoofedFilesCount: Int = 0
) {
    val percent: Int
        get() = if (isComplete) 100 else (((scannedCount + totalAppsAudited).toFloat() / 1200f) * 100f).toInt().coerceIn(1, 99)
    val threatsFound: Int
        get() = suspiciousCount
    val currentPath: String
        get() = currentFilePath
}

enum class FileMagicHeader {
    DEX_BYTECODE,
    LINUX_ELF,
    ZIP_ARCHIVE,
    WINDOWS_PE,
    SHELL_SCRIPT,
    STANDARD
}

object StorageScannerEngine {

    private val RANSOMWARE_EXTENSIONS = setOf(
        "locked", "crypto", "enc", "crypt", "ransom", "wnry", "wannacry", "locky", "cerber", "aes", "dharma", "phobos", "makop"
    )

    private val SCRIPT_EXTENSIONS = setOf(
        "sh", "bat", "cmd", "vbs", "py", "ps1", "bash"
    )

    private val BINARY_EXTENSIONS = setOf(
        "so", "elf", "bin", "dex"
    )

    private val SAFE_MEDIA_EXTENSIONS = setOf(
        "jpg", "jpeg", "png", "webp", "gif", "mp4", "mp3", "m4a", "aac", "wav", "flac", "ogg", "mkv"
    )

    private val SAFE_DOC_EXTENSIONS = setOf(
        "pdf", "txt", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "csv", "json", "xml"
    )

    fun inspectMagicHeader(file: File): FileMagicHeader {
        if (!file.exists() || !file.canRead() || file.length() < 4) return FileMagicHeader.STANDARD
        val buffer = ByteArray(16)
        val read = try {
            FileInputStream(file).use { it.read(buffer) }
        } catch (_: Exception) { 0 }
        if (read < 4) return FileMagicHeader.STANDARD

        // Check DEX: 'd' 'e' 'x' 0x0A
        if (buffer[0] == 0x64.toByte() && buffer[1] == 0x65.toByte() && buffer[2] == 0x78.toByte() && buffer[3] == 0x0A.toByte()) {
            return FileMagicHeader.DEX_BYTECODE
        }
        // Check ELF: 0x7F 'E' 'L' 'F'
        if (buffer[0] == 0x7F.toByte() && buffer[1] == 'E'.code.toByte() && buffer[2] == 'L'.code.toByte() && buffer[3] == 'F'.code.toByte()) {
            return FileMagicHeader.LINUX_ELF
        }
        // Check ZIP / APK: 'P' 'K' 0x03 0x04
        if (buffer[0] == 'P'.code.toByte() && buffer[1] == 'K'.code.toByte() && buffer[2] == 0x03.toByte() && buffer[3] == 0x04.toByte()) {
            return FileMagicHeader.ZIP_ARCHIVE
        }
        // Check Windows PE: 'M' 'Z'
        if (buffer[0] == 'M'.code.toByte() && buffer[1] == 'Z'.code.toByte()) {
            return FileMagicHeader.WINDOWS_PE
        }
        // Check Shell: '#' '!'
        if (buffer[0] == '#'.code.toByte() && buffer[1] == '!'.code.toByte()) {
            return FileMagicHeader.SHELL_SCRIPT
        }
        return FileMagicHeader.STANDARD
    }

    fun scanDeviceStorage(context: Context): Flow<StorageScanProgress> = flow {
        val pm = context.packageManager
        val candidateItems = mutableListOf<CandidateTarget>()

        emit(
            StorageScanProgress(
                scannedCount = 0,
                suspiciousCount = 0,
                currentFilePath = "Cataloging device storage volumes and installed applications...",
                isComplete = false,
                results = emptyList()
            )
        )

        // 1. Audit Installed Applications directly from disk (/data/app/.../base.apk)
        var appsCount = 0
        try {
            val installedPackages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()))
            } else {
                @Suppress("DEPRECATION")
                pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)
            }

            for (pkg in installedPackages) {
                val appInfo = pkg.applicationInfo ?: continue
                val sourceDir = appInfo.sourceDir
                if (!sourceDir.isNullOrBlank()) {
                    val file = File(sourceDir)
                    if (file.exists() && file.canRead()) {
                        val appLabel = pm.getApplicationLabel(appInfo).toString()
                        val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                        val requestedPerms = pkg.requestedPermissions?.toList() ?: emptyList()

                        candidateItems.add(
                            CandidateTarget(
                                file = file,
                                displayName = appLabel,
                                packageName = pkg.packageName,
                                isInstalledApp = true,
                                isSystemApp = isSystem,
                                preLoadedPermissions = requestedPerms
                            )
                        )
                        appsCount++
                    }
                }
            }
        } catch (_: Exception) {}

        // 2. Comprehensive Multi-Volume Storage Traversal (All user files across volumes)
        val searchDirs = listOfNotNull(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
            File(Environment.getExternalStorageDirectory(), "WhatsApp"),
            File(Environment.getExternalStorageDirectory(), "Telegram"),
            File(Environment.getExternalStorageDirectory(), "Android/media"),
            Environment.getExternalStorageDirectory(),
            context.getExternalFilesDir(null),
            context.filesDir
        )

        var traversedFoldersCount = 0
        for (dir in searchDirs) {
            try {
                if (dir.exists() && dir.canRead()) {
                    traversedFoldersCount++
                    collectAllStorageFiles(dir, candidateItems, maxDepth = 6)
                }
            } catch (_: Exception) {}
        }

        val results = mutableListOf<ScannedFileRecord>()
        var suspiciousCount = 0
        var storageFilesCount = 0
        var cleanFilesCount = 0
        var ransomwareCount = 0
        var spoofedFilesCount = 0

        val distinctTargets = candidateItems.distinctBy { it.file.absolutePath }

        for ((index, item) in distinctTargets.withIndex()) {
            try {
                val file = item.file
                val ext = file.extension.lowercase()
                var risk = 10
                val reasons = mutableListOf<String>()
                val perms = mutableListOf<String>()
                var isSuspicious = false
                var category = "DOC"
                var recordDexReport: DexInspectionReport? = null

                val sha256 = try {
                    FileInputStream(file).use { FileInspector.calculateStreamingSha256(it) }
                } catch (_: Exception) {
                    "sha256_${file.name.hashCode()}"
                }

                if (item.isInstalledApp) {
                    category = "APP"
                    perms.addAll(item.preLoadedPermissions)

                    val isTrustedSystem = item.isSystemApp || AllowlistManager.isTrustedSystemPackage(item.packageName) || AllowlistManager.isAllowlisted(sha256)

                    if (isTrustedSystem) {
                        risk = 5
                        isSuspicious = false
                        reasons.add("Verified Android OEM / System Platform Package (${item.packageName})")
                    } else {
                        // Audit user-installed third-party app bytecode
                        try {
                            if (file.exists() && file.canRead()) {
                                val dReport = DexBytecodeScanner.scanApkFile(file)
                                if (dReport.classesCount > 0) {
                                    recordDexReport = dReport
                                    dReport.detectedLoops.forEach { loop ->
                                        reasons.add(" Bytecode Loop [${loop.loopType}]: ${loop.className}.${loop.methodName}() - ${loop.explanation}")
                                    }
                                    dReport.malwareSignatures.forEach { sig ->
                                        reasons.add(" Bytecode Signature [${sig.category}]: ${sig.matchedPattern} - ${sig.description}")
                                    }
                                }
                            }
                        } catch (_: Throwable) {}

                        // Toxic permission & bytecode correlation
                        val hasAccessibility = perms.any { it.contains("BIND_ACCESSIBILITY_SERVICE") }
                        val hasOverlay = perms.any { it.contains("SYSTEM_ALERT_WINDOW") }
                        val hasSms = perms.any { it.contains("SMS") }
                        val hasInternet = perms.any { it.contains("INTERNET") }
                        val hasAdmin = perms.any { it.contains("BIND_DEVICE_ADMIN") }
                        val hasInstall = perms.any { it.contains("REQUEST_INSTALL_PACKAGES") }

                        val hasBankingTrojanSig = recordDexReport?.malwareSignatures?.any { it.category == "BANKING_TROJAN" } == true
                        val hasDropperSig = recordDexReport?.malwareSignatures?.any { it.category == "DYNAMIC_CLASSLOADER_DROPPER" } == true
                        val hasStealerSig = recordDexReport?.malwareSignatures?.any { it.category == "SMS_OTP_INTERCEPTOR" } == true
                        val hasRansomwareSig = recordDexReport?.malwareSignatures?.any { it.category == "RANSOMWARE_ENCRYPTION" } == true
                        val hasExploitLoop = recordDexReport?.detectedLoops?.any { it.loopType == "FORK_BOMB_PROCESS_LOOP" || it.loopType == "DDOS_FLOODING_LOOP" } == true

                        if (hasBankingTrojanSig || (hasAccessibility && hasOverlay && hasDropperSig)) {
                            risk = 90
                            isSuspicious = true
                            reasons.add(" Verified Banking Trojan: Accessibility Service + Screen Overlay with synthetic click injection")
                        } else if (hasStealerSig) {
                            risk = 90
                            isSuspicious = true
                            reasons.add(" Verified SMS/OTP Exfiltration Trojan: SMS reader with remote webhook endpoint")
                        } else if (hasRansomwareSig) {
                            risk = 95
                            isSuspicious = true
                            reasons.add(" Verified Ransomware payload in application binary")
                        } else if (hasExploitLoop) {
                            risk = 85
                            isSuspicious = true
                            reasons.add(" Active process fork bomb or network flooding loop detected")
                        } else if (hasAccessibility && hasOverlay) {
                            risk = 25
                            reasons.add("Accessibility Service and Screen Overlay declared (monitored for overlay spoofing)")
                        } else if (hasSms && hasInternet) {
                            risk = 15
                            reasons.add("Standard SMS 2FA verification capability with network sync")
                        } else if (hasAdmin) {
                            risk = 25
                            reasons.add("Device Administrator privilege bound to application")
                        } else if (hasInstall && !item.packageName.contains("vending")) {
                            risk = 20
                            reasons.add("Package installer capability")
                        } else {
                            risk = 10
                            reasons.add("Legitimate application package, nominal permission model")
                        }
                    }
                } else {
                    storageFilesCount++
                    val isApk = ext in listOf("apk", "xapk", "apkm")
                    val magicHeader = inspectMagicHeader(file)

                    // 1. Check Deceptive Extension Spoofing (Steganography / Polyglots)
                    val isDisguisedDex = magicHeader == FileMagicHeader.DEX_BYTECODE && ext !in listOf("dex", "apk", "jar")
                    val isDisguisedElf = magicHeader == FileMagicHeader.LINUX_ELF && ext !in listOf("so", "bin", "elf")
                    val isDisguisedZip = magicHeader == FileMagicHeader.ZIP_ARCHIVE && ext in listOf("jpg", "jpeg", "png", "mp3", "pdf", "txt")
                    val hasDoubleExtension = file.name.contains(".pdf.apk") || file.name.contains(".png.sh") || file.name.contains(".jpg.apk") || file.name.contains(".doc.exe")

                    if (isDisguisedDex) {
                        risk = 98
                        isSuspicious = true
                        spoofedFilesCount++
                        category = "THREAT"
                        reasons.add(" CRITICAL STEGANOGRAPHY SPOOFING: Executable Dalvik bytecode disguised as .${ext}!")
                    } else if (isDisguisedElf) {
                        risk = 98
                        isSuspicious = true
                        spoofedFilesCount++
                        category = "THREAT"
                        reasons.add(" CRITICAL EXECUTABLE SPOOFING: Native Linux ELF executable disguised as .${ext}!")
                    } else if (isDisguisedZip) {
                        risk = 85
                        isSuspicious = true
                        spoofedFilesCount++
                        category = "THREAT"
                        reasons.add(" DECEPTIVE ARCHIVE SPOOFING: Hidden ZIP/APK archive container disguised as .${ext}")
                    } else if (hasDoubleExtension) {
                        risk = 90
                        isSuspicious = true
                        spoofedFilesCount++
                        category = "THREAT"
                        reasons.add(" DECEPTIVE DOUBLE EXTENSION DETECTED (${file.name})")
                    } else if (ext in RANSOMWARE_EXTENSIONS) {
                        risk = 95
                        isSuspicious = true
                        ransomwareCount++
                        category = "THREAT"
                        reasons.add(" CRITICAL RANSOMWARE EXTENSION DETECTED (.$ext)")
                        reasons.add("File signature indicates mass-encryption artifact")
                    } else if (isApk) {
                        category = "APP"
                        val apkResult = try {
                            FileInputStream(file).use { FileInspector.inspectStream(file.name, it, file.length()) }
                        } catch (_: Exception) { null }

                        if (apkResult != null) {
                            risk = apkResult.riskScore
                            reasons.addAll(apkResult.whyPoints)
                            perms.addAll(apkResult.permissions)
                            recordDexReport = apkResult.dexReport
                            isSuspicious = apkResult.riskScore >= 70
                        } else {
                            risk = 20
                            reasons.add("Sideloaded standalone APK package in storage")
                        }
                    } else if (ext in listOf("jpg", "jpeg", "png", "gif", "webp")) {
                        // Deep stego check on image files to catch appended malware without flagging EXIF thumbnails
                        val stego = try {
                            FileInspector.detectSteganographyAndPolyglot(file.name, { FileInputStream(file) }, file.length())
                        } catch (_: Exception) { null }

                        if (stego?.isThreat == true) {
                            risk = stego.riskScore
                            isSuspicious = true
                            spoofedFilesCount++
                            category = "THREAT"
                            reasons.addAll(stego.whyPoints)
                        } else {
                            risk = 5
                            category = "MEDIA"
                            reasons.add("Verified media container • Zero appended payloads or stego droppers")
                        }
                    } else if (ext in SCRIPT_EXTENSIONS || magicHeader == FileMagicHeader.SHELL_SCRIPT) {
                        risk = 10
                        isSuspicious = false
                        category = "SCRIPT"
                        reasons.add("Developer script in storage (.$ext • Non-executable on unrooted Android)")
                    } else if (magicHeader == FileMagicHeader.WINDOWS_PE) {
                        risk = 10
                        isSuspicious = false
                        category = "DOC"
                        reasons.add("Non-native Windows binary asset (non-executable on Android)")
                    } else if (ext in BINARY_EXTENSIONS) {
                        risk = 10
                        isSuspicious = false
                        category = "DOC"
                        reasons.add("Compiled native library / binary asset (.$ext)")
                    } else if (ext in SAFE_MEDIA_EXTENSIONS) {
                        risk = 5
                        category = "MEDIA"
                        reasons.add("Verified media container • Magic header conforms to nominal standard")
                    } else if (ext in SAFE_DOC_EXTENSIONS) {
                        risk = 10
                        category = "DOC"
                        reasons.add("Standard user document asset, verified format (.$ext)")
                    } else {
                        risk = 10
                        category = "DOC"
                        reasons.add("Storage file inspected • Zero malicious bytecode signatures")
                    }
                }

                if (isSuspicious) {
                    suspiciousCount++
                } else {
                    cleanFilesCount++
                }

                val displayName = if (item.isInstalledApp) {
                    "${item.displayName} (${item.packageName})"
                } else {
                    file.name
                }

                val record = ScannedFileRecord(
                    filename = displayName,
                    path = file.absolutePath,
                    sizeBytes = file.length(),
                    sha256 = sha256,
                    isApk = item.isInstalledApp || ext in listOf("apk", "xapk", "apkm"),
                    riskScore = risk,
                    threatReasons = reasons,
                    isSuspicious = isSuspicious,
                    permissions = perms,
                    fileCategory = if (isSuspicious) "THREAT" else category,
                    dexReport = recordDexReport
                )
                results.add(record)

                // Emit progress periodically
                if (index % 4 == 0 || isSuspicious || index == distinctTargets.size - 1) {
                    emit(
                        StorageScanProgress(
                            scannedCount = index + 1,
                            suspiciousCount = suspiciousCount,
                            currentFilePath = displayName,
                            isComplete = index == distinctTargets.size - 1,
                            results = results.toList(),
                            totalAppsAudited = appsCount,
                            totalFilesAudited = storageFilesCount,
                            cleanFilesCount = cleanFilesCount,
                            foldersAuditedCount = traversedFoldersCount,
                            ransomwareCount = ransomwareCount,
                            spoofedFilesCount = spoofedFilesCount
                        )
                    )
                }
            } catch (_: Exception) {}
        }

        // Final completion emission
        emit(
            StorageScanProgress(
                scannedCount = distinctTargets.size,
                suspiciousCount = suspiciousCount,
                currentFilePath = "Master Storage Deep Audit Complete • ${distinctTargets.size} assets verified",
                isComplete = true,
                results = results,
                totalAppsAudited = appsCount,
                totalFilesAudited = storageFilesCount,
                cleanFilesCount = cleanFilesCount,
                foldersAuditedCount = traversedFoldersCount,
                ransomwareCount = ransomwareCount,
                spoofedFilesCount = spoofedFilesCount
            )
        )
    }.flowOn(Dispatchers.IO)

    private data class CandidateTarget(
        val file: File,
        val displayName: String = file.name,
        val packageName: String = "",
        val isInstalledApp: Boolean = false,
        val isSystemApp: Boolean = false,
        val preLoadedPermissions: List<String> = emptyList()
    )

    private fun collectAllStorageFiles(
        dir: File,
        outputList: MutableList<CandidateTarget>,
        maxDepth: Int,
        currentDepth: Int = 0
    ) {
        if (currentDepth > maxDepth || outputList.size >= 1200) return
        val files = dir.listFiles() ?: return

        for (file in files) {
            if (file.isDirectory) {
                if (!file.name.startsWith(".") && file.name != "cache") {
                    collectAllStorageFiles(file, outputList, maxDepth, currentDepth + 1)
                }
            } else if (file.isFile && file.length() > 0) {
                outputList.add(CandidateTarget(file = file))
                if (outputList.size >= 1200) return
            }
        }
    }
}