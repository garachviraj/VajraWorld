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
    val fileCategory: String = "STORAGE" // "APP", "THREAT", "DOC", "MEDIA", "SCRIPT"
)

data class StorageScanProgress(
    val scannedCount: Int,
    val suspiciousCount: Int,
    val currentFilePath: String,
    val isComplete: Boolean,
    val results: List<ScannedFileRecord>,
    val totalAppsAudited: Int = 0,
    val totalFilesAudited: Int = 0
)

object StorageScannerEngine {

    private val RANSOMWARE_EXTENSIONS = setOf(
        "locked", "crypto", "enc", "crypt", "ransom", "wnry", "wannacry", "locky", "cerber"
    )

    private val SCRIPT_EXTENSIONS = setOf(
        "sh", "bat", "cmd", "vbs", "py", "ps1", "bash"
    )

    private val BINARY_EXTENSIONS = setOf(
        "so", "elf", "bin", "dex"
    )

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

        // 2. Full Storage Directories Traversal (All user files: Downloads, Documents, DCIM, Pictures, /sdcard/)
        val searchDirs = listOfNotNull(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
            Environment.getExternalStorageDirectory(),
            context.getExternalFilesDir(null),
            context.filesDir
        )

        for (dir in searchDirs) {
            try {
                if (dir.exists() && dir.canRead()) {
                    collectAllStorageFiles(dir, candidateItems, maxDepth = 4)
                }
            } catch (_: Exception) {}
        }

        val results = mutableListOf<ScannedFileRecord>()
        var suspiciousCount = 0
        var storageFilesCount = 0

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

                val sha256 = try {
                    FileInputStream(file).use { FileInspector.calculateStreamingSha256(it) }
                } catch (_: Exception) {
                    "sha256_${file.name.hashCode()}"
                }

                if (item.isInstalledApp) {
                    category = "APP"
                    perms.addAll(item.preLoadedPermissions)

                    // Toxic permission analysis
                    val hasAccessibility = perms.any { it.contains("BIND_ACCESSIBILITY_SERVICE") }
                    val hasOverlay = perms.any { it.contains("SYSTEM_ALERT_WINDOW") }
                    val hasSms = perms.any { it.contains("SMS") }
                    val hasInternet = perms.any { it.contains("INTERNET") }
                    val hasAdmin = perms.any { it.contains("BIND_DEVICE_ADMIN") }

                    if (hasAccessibility && hasOverlay && !item.isSystemApp) {
                        risk = 85
                        isSuspicious = true
                        reasons.add("🚨 Toxic Privilege Combination: Accessibility Service + Screen Overlay")
                    } else if (hasSms && hasInternet && !item.isSystemApp && !item.packageName.contains("messaging") && !item.packageName.contains("telephony")) {
                        risk = 65
                        isSuspicious = true
                        reasons.add("⚠️ Potential SMS Interception: SMS Access with Internet Socket")
                    } else if (hasAdmin && !item.isSystemApp) {
                        risk = 70
                        isSuspicious = true
                        reasons.add("⚠️ Device Administrator Privilege bound to application")
                    } else if (item.isSystemApp) {
                        risk = 5
                        reasons.add("Verified Android System Image Package")
                    } else {
                        risk = 15
                        reasons.add("Legitimate application package, standard permission model")
                    }
                } else {
                    storageFilesCount++
                    val isApk = ext in listOf("apk", "xapk", "apkm")

                    if (ext in RANSOMWARE_EXTENSIONS) {
                        risk = 95
                        isSuspicious = true
                        category = "THREAT"
                        reasons.add("🚨 CRITICAL RANSOMWARE EXTENSION DETECTED (.$ext)")
                        reasons.add("File signature indicates potential mass encryption artifact")
                    } else if (ext in SCRIPT_EXTENSIONS) {
                        risk = 75
                        isSuspicious = true
                        category = "SCRIPT"
                        reasons.add("⚠️ Executable Shell Script located in user storage (.$ext)")
                        reasons.add("Direct execution capability without Android sandboxing")
                    } else if (ext in BINARY_EXTENSIONS) {
                        risk = 60
                        isSuspicious = true
                        category = "SCRIPT"
                        reasons.add("⚠️ Unmanaged native binary / library in public storage (.$ext)")
                    } else if (isApk) {
                        category = "APP"
                        val apkResult = try {
                            FileInputStream(file).use { FileInspector.inspectStream(file.name, it) }
                        } catch (_: Exception) { null }

                        if (apkResult != null) {
                            risk = apkResult.riskScore
                            reasons.addAll(apkResult.whyPoints)
                            perms.addAll(apkResult.permissions)
                            isSuspicious = apkResult.riskScore >= 60
                        } else {
                            risk = 40
                            reasons.add("Sideloaded standalone APK package on storage")
                        }
                    } else if (ext in listOf("jpg", "jpeg", "png", "webp", "gif", "mp4", "mp3", "m4a")) {
                        risk = 5
                        category = "MEDIA"
                        reasons.add("Nominal media asset, zero executable headers")
                    } else {
                        risk = 10
                        category = "DOC"
                        reasons.add("Standard user document asset, verified format (.$ext)")
                    }
                }

                if (isSuspicious) {
                    suspiciousCount++
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
                    fileCategory = if (isSuspicious) "THREAT" else category
                )
                results.add(record)

                // Emit progress every 3 files or on suspicious file
                if (index % 3 == 0 || isSuspicious || index == distinctTargets.size - 1) {
                    emit(
                        StorageScanProgress(
                            scannedCount = index + 1,
                            suspiciousCount = suspiciousCount,
                            currentFilePath = displayName,
                            isComplete = index == distinctTargets.size - 1,
                            results = results.toList(),
                            totalAppsAudited = appsCount,
                            totalFilesAudited = storageFilesCount
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
                currentFilePath = "Storage Deep Audit Complete • ${distinctTargets.size} assets verified",
                isComplete = true,
                results = results,
                totalAppsAudited = appsCount,
                totalFilesAudited = storageFilesCount
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
        if (currentDepth > maxDepth || outputList.size >= 300) return
        val files = dir.listFiles() ?: return

        for (file in files) {
            if (file.isDirectory) {
                if (!file.name.startsWith(".")) {
                    collectAllStorageFiles(file, outputList, maxDepth, currentDepth + 1)
                }
            } else if (file.isFile && file.length() > 0) {
                outputList.add(CandidateTarget(file = file))
                if (outputList.size >= 300) return
            }
        }
    }
}
