package com.vajraworld.defender.ui.screens.scanner

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vajraworld.defender.data.local.ScanResultEntity
import com.vajraworld.defender.data.repository.VajraRepository
import com.vajraworld.defender.domain.engine.FileInspector
import com.vajraworld.defender.domain.engine.StorageScanProgress
import com.vajraworld.defender.domain.engine.StorageScannerEngine
import com.vajraworld.defender.domain.model.GuardianFileAnalysis
import com.vajraworld.defender.service.VajraNotificationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream

class FileScanViewModel(private val repository: VajraRepository? = null) : ViewModel() {
    private val _selectedFilename = MutableStateFlow<String?>(null)
    val selectedFilename: StateFlow<String?> = _selectedFilename.asStateFlow()

    private val _fileResult = MutableStateFlow<GuardianFileAnalysis?>(null)
    val fileResult: StateFlow<GuardianFileAnalysis?> = _fileResult.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _storageScanProgress = MutableStateFlow<StorageScanProgress?>(null)
    val storageScanProgress: StateFlow<StorageScanProgress?> = _storageScanProgress.asStateFlow()

    private val _isStorageScanning = MutableStateFlow(false)
    val isStorageScanning: StateFlow<Boolean> = _isStorageScanning.asStateFlow()

    val fileScanHistory: StateFlow<List<ScanResultEntity>> = (repository?.fileScanHistoryFlow ?: kotlinx.coroutines.flow.emptyFlow())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun startStorageDeepScan(context: Context) {
        if (_isStorageScanning.value) return
        viewModelScope.launch {
            _isStorageScanning.value = true
            StorageScannerEngine.scanDeviceStorage(context).collect { progress ->
                _storageScanProgress.value = progress
                if (progress.isComplete) {
                    _isStorageScanning.value = false
                    try {
                        VajraNotificationManager.sendMasterScanReportNotification(context, progress)
                    } catch (_: Exception) {}
                }
            }
        }
    }

    fun deleteScannedFile(record: com.vajraworld.defender.domain.engine.ScannedFileRecord): Boolean {
        return try {
            val f = File(record.path)
            val deleted = if (f.exists()) f.delete() else true
            if (deleted) {
                val current = _storageScanProgress.value ?: return true
                val updatedResults = current.results.filter { it.path != record.path }
                _storageScanProgress.value = current.copy(
                    results = updatedResults,
                    suspiciousCount = updatedResults.count { it.isSuspicious }
                )
            }
            deleted
        } catch (_: Exception) {
            false
        }
    }

    fun quarantineScannedFile(record: com.vajraworld.defender.domain.engine.ScannedFileRecord): Boolean {
        return try {
            val f = File(record.path)
            if (f.exists()) {
                val quarantined = File(f.parentFile, "${f.name}.vajra_quarantine")
                val renamed = f.renameTo(quarantined)
                if (renamed) {
                    val current = _storageScanProgress.value ?: return true
                    val updatedResults = current.results.map {
                        if (it.path == record.path) it.copy(path = quarantined.absolutePath, filename = quarantined.name, isSuspicious = false)
                        else it
                    }
                    _storageScanProgress.value = current.copy(results = updatedResults)
                }
                renamed
            } else false
        } catch (_: Exception) {
            false
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository?.clearFileScanHistory()
        }
    }

    fun scanFilePath(file: File) {
        viewModelScope.launch {
            _isScanning.value = true
            _selectedFilename.value = file.name
            withContext(Dispatchers.IO) {
                try {
                    val localResult = FileInputStream(file).use { stream ->
                        FileInspector.inspectStream(file.name, stream, file.length())
                    }
                    val action = if (localResult.riskScore >= 70) {
                        "DO NOT INSTALL / QUARANTINE IMMEDIATELY"
                    } else if (localResult.riskScore >= 40) {
                        "Review Requested Permissions Carefully"
                    } else {
                        "Verified Safe Package"
                    }

                    _fileResult.value = GuardianFileAnalysis(
                        filename = localResult.filename,
                        sha256 = localResult.sha256,
                        isApk = localResult.isApk,
                        riskScore = localResult.riskScore,
                        confidence = localResult.confidence,
                        whyPoints = localResult.whyPoints,
                        recommendedAction = action,
                        permissionsAnalyzed = localResult.permissions,
                        progressionTrajectory = listOf(
                            mapOf("step" to "Local Storage File Read", "status" to "OBSERVED"),
                            mapOf("step" to "Streaming SHA-256 Calculated", "status" to "VERIFIED"),
                            mapOf("step" to "Archive & Manifest Analysis", "status" to if (localResult.riskScore >= 60) "SUSPICIOUS" else "NOMINAL")
                        ),
                        archiveSafe = localResult.archiveSafe
                    )
                } catch (e: Exception) {
                    _fileResult.value = GuardianFileAnalysis(
                        filename = file.name,
                        sha256 = "N/A (Read error)",
                        isApk = file.name.endsWith(".apk"),
                        riskScore = 50,
                        confidence = 0.5f,
                        whyPoints = listOf("Error inspecting file: ${e.localizedMessage}"),
                        recommendedAction = "Retry Scan",
                        permissionsAnalyzed = emptyList(),
                        progressionTrajectory = emptyList(),
                        archiveSafe = false
                    )
                } finally {
                    _isScanning.value = false
                }
            }
        }
    }

    /**
     * Inspect a genuine file selected by the user via Storage Access Framework (SAF).
     * Calculates streaming SHA-256 over InputStream and parses APK manifest directly.
     */
    fun scanUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            _isScanning.value = true
            withContext(Dispatchers.IO) {
                try {
                    val resolver = context.contentResolver
                    var displayName = "picked_file"
                    var fileSize = 0L

                    resolver.query(uri, null, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            if (nameIndex >= 0) displayName = cursor.getString(nameIndex)
                            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                            if (sizeIndex >= 0) fileSize = cursor.getLong(sizeIndex)
                        }
                    }

                    _selectedFilename.value = displayName

                    resolver.openInputStream(uri)?.use { stream ->
                        val localResult = if (repository != null) {
                            repository.analyzeStreamLocally(displayName, stream, fileSize)
                        } else {
                            FileInspector.inspectStream(displayName, stream, fileSize)
                        }

                        val action = if (localResult.riskScore >= 70) {
                            "DO NOT INSTALL / QUARANTINE IMMEDIATELY"
                        } else if (localResult.riskScore >= 40) {
                            "Review Requested Permissions Carefully"
                        } else {
                            "Verified Safe File"
                        }

                        _fileResult.value = GuardianFileAnalysis(
                            filename = localResult.filename,
                            sha256 = localResult.sha256,
                            isApk = localResult.isApk,
                            riskScore = localResult.riskScore,
                            confidence = localResult.confidence,
                            whyPoints = localResult.whyPoints,
                            recommendedAction = action,
                            permissionsAnalyzed = localResult.permissions,
                            progressionTrajectory = listOf(
                                mapOf("step" to "File Picked (SAF)", "status" to "OBSERVED"),
                                mapOf("step" to "Archive Inspection", "status" to if (localResult.archiveSafe) "SAFE" else "REJECTED"),
                                mapOf("step" to "Permission Analysis", "status" to if (localResult.riskScore >= 60) "SUSPICIOUS" else "NOMINAL")
                            ),
                            archiveSafe = localResult.archiveSafe
                        )
                    }
                } catch (e: Exception) {
                    _fileResult.value = GuardianFileAnalysis(
                        filename = _selectedFilename.value ?: "Unknown",
                        sha256 = "N/A (Read error)",
                        isApk = _selectedFilename.value?.endsWith(".apk") == true,
                        riskScore = 50,
                        confidence = 0.5f,
                        whyPoints = listOf("Error inspecting selected file: ${e.localizedMessage}"),
                        recommendedAction = "Retry Scan",
                        permissionsAnalyzed = emptyList(),
                        progressionTrajectory = emptyList(),
                        archiveSafe = false
                    )
                } finally {
                    _isScanning.value = false
                }
            }
        }
    }
}
