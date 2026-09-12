package com.vajraworld.defender.domain.engine

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build

data class ScannedAppReport(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val isSystemApp: Boolean,
    val isSideloaded: Boolean,
    val riskScore: Int, // 0-100
    val riskLevel: String, // "CRITICAL", "HIGH", "ELEVATED", "SAFE"
    val riskReasons: List<String>,
    val requestedPermissions: List<String>,
    val installTimeMs: Long
)

data class AppSecurityAudit(
    val totalAppsScanned: Int,
    val userAppsCount: Int,
    val systemAppsCount: Int,
    val highRiskApps: List<ScannedAppReport>,
    val mediumRiskApps: List<ScannedAppReport>,
    val safeApps: List<ScannedAppReport>,
    val overallAppRiskScore: Int // 0-100
)

object InstalledAppScanner {

    fun scanInstalledApps(context: Context): AppSecurityAudit {
        val pm = context.packageManager
        val packages: List<PackageInfo> = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()))
            } else {
                @Suppress("DEPRECATION")
                pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)
            }
        } catch (_: Exception) {
            emptyList()
        }

        val scannedApps = mutableListOf<ScannedAppReport>()
        var userAppsCount = 0
        var systemAppsCount = 0

        for (pkg in packages) {
            val appInfo = pkg.applicationInfo ?: continue
            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            if (isSystem) {
                systemAppsCount++
                continue // Prioritize user-installed apps for forensic security scan
            }
            userAppsCount++

            val appName = try {
                pm.getApplicationLabel(appInfo).toString()
            } catch (_: Exception) {
                pkg.packageName
            }

            val permissions = pkg.requestedPermissions?.toList() ?: emptyList()
            val riskReasons = mutableListOf<String>()
            var appRisk = 5 // Base baseline

            // 1. Toxic Combo: Overlay + Accessibility (Banking Trojan pattern)
            val hasOverlay = permissions.contains("android.permission.SYSTEM_ALERT_WINDOW")
            val hasAccessibility = permissions.contains("android.permission.BIND_ACCESSIBILITY_SERVICE")
            if (hasOverlay && hasAccessibility) {
                appRisk += 50
                riskReasons.add("Toxic Combo: Screen Overlay + Accessibility (Banking Trojan signature)")
            } else if (hasAccessibility) {
                appRisk += 30
                riskReasons.add("Requests Accessibility Service (can intercept keystrokes and screen)")
            } else if (hasOverlay) {
                appRisk += 15
                riskReasons.add("Requests System Alert Window (can draw overlays above other apps)")
            }

            // 2. Toxic Combo: SMS + Internet (OTP stealer pattern)
            val hasSms = permissions.any { it.contains("SMS") }
            val hasInternet = permissions.contains("android.permission.INTERNET")
            if (hasSms && hasInternet) {
                appRisk += 40
                riskReasons.add("Toxic Combo: SMS Read/Receive + Internet (Potential OTP interceptor)")
            } else if (hasSms) {
                appRisk += 20
                riskReasons.add("Requests SMS read/receive permissions")
            }

            // 3. Toxic Combo: Install Packages + Boot (Dropper pattern)
            val hasInstall = permissions.contains("android.permission.REQUEST_INSTALL_PACKAGES")
            val hasBoot = permissions.contains("android.permission.RECEIVE_BOOT_COMPLETED")
            if (hasInstall && hasBoot) {
                appRisk += 35
                riskReasons.add("Toxic Combo: Package Installer + Boot Receiver (Dropper malware signature)")
            } else if (hasInstall) {
                appRisk += 20
                riskReasons.add("Requests permission to install unknown applications")
            }

            // 4. Sensitive Surveillance Permissions
            if (permissions.contains("android.permission.RECORD_AUDIO")) {
                appRisk += 10
                riskReasons.add("Can record audio / microphone")
            }
            if (permissions.contains("android.permission.CAMERA")) {
                appRisk += 10
                riskReasons.add("Can access camera hardware")
            }
            if (permissions.contains("android.permission.ACCESS_FINE_LOCATION") || permissions.contains("android.permission.ACCESS_BACKGROUND_LOCATION")) {
                appRisk += 10
                riskReasons.add("Can track precise physical location")
            }
            if (permissions.contains("android.permission.READ_CONTACTS") || permissions.contains("android.permission.READ_CALL_LOG")) {
                appRisk += 10
                riskReasons.add("Can harvest contacts or call history")
            }

            // 5. Sideloaded detection
            var isSideloaded = false
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val info = pm.getInstallSourceInfo(pkg.packageName)
                    val installer = info.installingPackageName
                    if (installer == null || installer == "com.android.packageinstaller" || installer == "com.google.android.packageinstaller") {
                        isSideloaded = true
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val installer = pm.getInstallerPackageName(pkg.packageName)
                    if (installer == null || installer.contains("packageinstaller")) {
                        isSideloaded = true
                    }
                }
            } catch (_: Exception) {}

            if (isSideloaded && appRisk > 20) {
                appRisk += 15
                riskReasons.add("Sideloaded outside official Google Play Store")
            }

            val finalRisk = appRisk.coerceIn(0, 100)
            val riskLevel = when {
                finalRisk >= 65 -> "CRITICAL"
                finalRisk >= 40 -> "HIGH"
                finalRisk >= 20 -> "ELEVATED"
                else -> "SAFE"
            }

            scannedApps.add(
                ScannedAppReport(
                    packageName = pkg.packageName,
                    appName = appName,
                    versionName = pkg.versionName ?: "1.0",
                    isSystemApp = false,
                    isSideloaded = isSideloaded,
                    riskScore = finalRisk,
                    riskLevel = riskLevel,
                    riskReasons = riskReasons,
                    requestedPermissions = permissions,
                    installTimeMs = pkg.firstInstallTime
                )
            )
        }

        // Sort by risk descending
        scannedApps.sortByDescending { it.riskScore }

        val highRisk = scannedApps.filter { it.riskScore >= 40 }
        val mediumRisk = scannedApps.filter { it.riskScore in 20..39 }
        val safeApps = scannedApps.filter { it.riskScore < 20 }

        val overallRisk = if (highRisk.isNotEmpty()) {
            (highRisk.maxOf { it.riskScore } * 0.7f + highRisk.size * 4f).toInt().coerceIn(0, 100)
        } else if (mediumRisk.isNotEmpty()) {
            (mediumRisk.maxOf { it.riskScore } * 0.5f).toInt().coerceIn(0, 45)
        } else {
            10
        }

        return AppSecurityAudit(
            totalAppsScanned = packages.size,
            userAppsCount = userAppsCount,
            systemAppsCount = systemAppsCount,
            highRiskApps = highRisk,
            mediumRiskApps = mediumRisk,
            safeApps = safeApps,
            overallAppRiskScore = overallRisk
        )
    }
}
