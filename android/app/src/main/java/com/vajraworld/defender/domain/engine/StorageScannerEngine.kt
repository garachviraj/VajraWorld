package com.vajraworld.defender.domain.engine

import android.content.Context
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
    val isSuspicious: Boolean
)

data class StorageScanProgress(
    val scannedCount: Int,
    val suspiciousCount: Int,
    val currentFilePath: String,
    val isComplete: Boolean,
    val results: List<ScannedFileRecord>
)

object StorageScannerEngine {

    private val SUSPICIOUS_EXTENSIONS = setOf(
        "apk", "xapk", "apkm", "dex", "so", "bin", "elf", "sh", "bat", "py"
    )

    fun scanDeviceStorage(context: Context): Flow<StorageScanProgress> = flow {
        val candidateFiles = mutableListOf<File>()

        // 1. Target Directories: Downloads, Documents, External Cache
        val searchDirs = listOfNotNull(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            context.getExternalFilesDir(null),
            context.filesDir
        )

        for (dir in searchDirs) {
            if (dir.exists() && dir.canRead()) {
                collectCandidateFiles(dir, candidateFiles, maxDepth = 4)
            }
        }

        val results = mutableListOf<ScannedFileRecord>()
        var suspiciousCount = 0

        emit(
            StorageScanProgress(
                scannedCount = 0,
                suspiciousCount = 0,
                currentFilePath = "Initializing deep storage auditor...",
                isComplete = false,
                results = emptyList()
            )
        )

        for ((index, file) in candidateFiles.withIndex()) {
            try {
                val filename = file.name
                val ext = file.extension.lowercase()
                val isApk = ext in listOf("apk", "xapk", "apkm")
                var risk = 10
                val reasons = mutableListOf<String>()

                val sha256 = try {
                    FileInputStream(file).use { FileInspector.calculateStreamingSha256(it) }
                } catch (_: Exception) {
                    "unavailable_unreadable"
                }

                if (isApk) {
                    val apkResult = try {
                        FileInputStream(file).use { FileInspector.inspectStream(filename, it) }
                    } catch (_: Exception) {
                        null
                    }

                    if (apkResult != null) {
                        risk = apkResult.riskScore
                        reasons.addAll(apkResult.whyPoints)
                    } else {
                        risk = 55
                        reasons.add("Corrupted or obfuscated APK archive in public storage")
                    }
                } else if (ext in listOf("sh", "bat", "py")) {
                    risk = 45
                    reasons.add("Executable script detected in public user storage: .$ext")
                } else if (ext in listOf("dex", "so", "bin", "elf")) {
                    risk = 60
                    reasons.add("Unbundled executable binary / ELF payload: .$ext")
                }

                val isSuspicious = risk >= 40
                if (isSuspicious) suspiciousCount++

                val record = ScannedFileRecord(
                    filename = filename,
                    path = file.absolutePath,
                    sizeBytes = file.length(),
                    sha256 = sha256,
                    isApk = isApk,
                    riskScore = risk,
                    threatReasons = reasons,
                    isSuspicious = isSuspicious
                )
                results.add(record)

                // Persist each file result
                try {
                    val app = context.applicationContext as? VajraApplication
                    app?.database?.dao()?.insertScanResult(
                        ScanResultEntity(
                            id = UUID.randomUUID().toString(),
                            target = filename,
                            scanType = "FILE",
                            riskScore = risk,
                            confidence = 0.90f,
                            signalsJson = com.google.gson.Gson().toJson(reasons),
                            sha256 = sha256,
                            createdAt = System.currentTimeMillis()
                        )
                    )

                    if (risk >= 65) {
                        app?.database?.dao()?.insertSecurityEvent(
                            SecurityEventEntity(
                                id = UUID.randomUUID().toString(),
                                timestamp = System.currentTimeMillis(),
                                eventType = "SUSPICIOUS_FILE_FOUND",
                                source = filename,
                                risk = risk.toFloat(),
                                confidence = 0.90f,
                                explanation = "Storage audit flagged '$filename': ${reasons.firstOrNull()}",
                                rawContentHash = sha256,
                                isSynthetic = false
                            )
                        )
                    }
                } catch (_: Exception) {}

                emit(
                    StorageScanProgress(
                        scannedCount = index + 1,
                        suspiciousCount = suspiciousCount,
                        currentFilePath = file.absolutePath,
                        isComplete = false,
                        results = results.toList()
                    )
                )
            } catch (_: Exception) {}
        }

        // Complete notification
        VajraNotificationManager.sendScanCompleteNotification(
            context = context,
            title = "Deep Storage File Audit Complete",
            message = "Audited ${results.size} files in user storage. Flagged $suspiciousCount suspicious executables/APKs."
        )

        emit(
            StorageScanProgress(
                scannedCount = results.size,
                suspiciousCount = suspiciousCount,
                currentFilePath = "Audit Complete",
                isComplete = true,
                results = results
            )
        )
    }.flowOn(Dispatchers.IO)

    private fun collectCandidateFiles(dir: File, collected: MutableList<File>, maxDepth: Int, currentDepth: Int = 0) {
        if (currentDepth > maxDepth || collected.size >= 300) return
        val files = dir.listFiles() ?: return

        for (file in files) {
            if (file.isDirectory) {
                if (!file.name.startsWith(".")) {
                    collectCandidateFiles(file, collected, maxDepth, currentDepth + 1)
                }
            } else {
                val ext = file.extension.lowercase()
                if (ext in SUSPICIOUS_EXTENSIONS) {
                    collected.add(file)
                }
            }
        }
    }
}
