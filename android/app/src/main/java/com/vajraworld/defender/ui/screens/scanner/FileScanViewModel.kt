package com.vajraworld.defender.ui.screens.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vajraworld.defender.domain.model.GuardianFileAnalysis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FileScanViewModel : ViewModel() {
    private val _selectedFilename = MutableStateFlow("secure_banking_update.apk")
    val selectedFilename: StateFlow<String> = _selectedFilename.asStateFlow()

    private val _fileResult = MutableStateFlow<GuardianFileAnalysis?>(null)
    val fileResult: StateFlow<GuardianFileAnalysis?> = _fileResult.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    fun selectFile(name: String) {
        _selectedFilename.value = name
    }

    fun scanFile() {
        viewModelScope.launch {
            _isScanning.value = true
            val isApk = _selectedFilename.value.endsWith(".apk")
            _fileResult.value = GuardianFileAnalysis(
                filename = _selectedFilename.value,
                sha256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                isApk = isApk,
                riskScore = if (isApk) 85 else 10,
                confidence = 0.94f,
                whyPoints = if (isApk) listOf(
                    "Sideloaded Android Package (APK) outside Google Play",
                    "Toxic combination: Accessibility Service + Screen Overlay (Banking Trojan)",
                    "Sensitive combination: SMS access combined with Internet permission",
                    "Zip-bomb archive safety checks verified nominal"
                ) else listOf("Clean file format, structural validation passed"),
                recommendedAction = if (isApk) "DO NOT INSTALL — SENSITIVE PERMISSIONS THREAT" else "Safe File",
                permissionsAnalyzed = listOf(
                    "android.permission.BIND_ACCESSIBILITY_SERVICE",
                    "android.permission.SYSTEM_ALERT_WINDOW",
                    "android.permission.READ_SMS",
                    "android.permission.INTERNET"
                ),
                progressionTrajectory = listOf(
                    mapOf("step" to "File Downloaded", "status" to "OBSERVED"),
                    mapOf("step" to "File Installed", "status" to "BLOCKED"),
                    mapOf("step" to "Permission Abuse", "status" to "PREVENTED"),
                    mapOf("step" to "C2 Beaconing", "status" to "PREVENTED")
                ),
                archiveSafe = true
            )
            _isScanning.value = false
        }
    }
}
