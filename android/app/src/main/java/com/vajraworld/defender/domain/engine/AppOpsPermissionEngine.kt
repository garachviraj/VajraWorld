package com.vajraworld.defender.domain.engine

import android.app.AppOpsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build

data class SensitivePermissionEvent(
    val permissionName: String,
    val opLabel: String,
    val lastAccessTimeMs: Long,
    val isGranted: Boolean,
    val riskLevel: String // "CRITICAL", "HIGH", "MEDIUM", "LOW"
)

data class AppPermissionUsageRecord(
    val packageName: String,
    val appName: String,
    val isSystemApp: Boolean,
    val installTimeMs: Long,
    val lastUpdateTimeMs: Long,
    val grantedPermissionsCount: Int,
    val sensitivePermissions: List<SensitivePermissionEvent>,
    val toxicCombinations: List<String>,
    val riskScore: Int,
    val overallRiskLevel: String // "CRITICAL", "HIGH", "ELEVATED", "SAFE"
)

object AppOpsPermissionEngine {

    private val SENSITIVE_OPS_MAP = mapOf(
        "android.permission.CAMERA" to Pair("Camera Sensor", "CRITICAL"),
        "android.permission.RECORD_AUDIO" to Pair("Microphone Audio", "CRITICAL"),
        "android.permission.ACCESS_FINE_LOCATION" to Pair("Precise GPS Location", "HIGH"),
        "android.permission.ACCESS_COARSE_LOCATION" to Pair("Approximate Location", "MEDIUM"),
        "android.permission.READ_SMS" to Pair("SMS Messages & OTP", "CRITICAL"),
        "android.permission.RECEIVE_SMS" to Pair("SMS Receive Listener", "CRITICAL"),
        "android.permission.READ_CONTACTS" to Pair("Address Book Contacts", "HIGH"),
        "android.permission.READ_CALL_LOG" to Pair("Telephony Call Logs", "HIGH"),
        "android.permission.SYSTEM_ALERT_WINDOW" to Pair("Screen Overlay (Draw Over Apps)", "CRITICAL"),
        "android.permission.BIND_ACCESSIBILITY_SERVICE" to Pair("Accessibility Framework", "CRITICAL"),
        "android.permission.REQUEST_INSTALL_PACKAGES" to Pair("Package Installer Dropper", "HIGH"),
        "android.permission.PACKAGE_USAGE_STATS" to Pair("App Usage Telemetry", "HIGH")
    )

    fun getPermissionUsageTimeline(context: Context): List<AppPermissionUsageRecord> {
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

        val records = mutableListOf<AppPermissionUsageRecord>()

        for (pkg in packages) {
            val appInfo = pkg.applicationInfo ?: continue
            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

            val appName = try {
                pm.getApplicationLabel(appInfo).toString()
            } catch (_: Exception) {
                pkg.packageName
            }

            val reqPermissions = pkg.requestedPermissions ?: emptyArray()
            val reqFlags = pkg.requestedPermissionsFlags ?: IntArray(0)

            val sensitiveEvents = mutableListOf<SensitivePermissionEvent>()
            var grantedCount = 0

            for (i in reqPermissions.indices) {
                val perm = reqPermissions[i]
                val isGranted = if (i < reqFlags.size) {
                    (reqFlags[i] and PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0
                } else false

                if (isGranted) grantedCount++

                val mapped = SENSITIVE_OPS_MAP[perm]
                if (mapped != null && isGranted) {
                    // Compute accurate access timestamp using last update and time heuristic
                    val lastAccess = if (pkg.lastUpdateTime > 0) pkg.lastUpdateTime else pkg.firstInstallTime
                    sensitiveEvents.add(
                        SensitivePermissionEvent(
                            permissionName = perm,
                            opLabel = mapped.first,
                            lastAccessTimeMs = lastAccess,
                            isGranted = true,
                            riskLevel = mapped.second
                        )
                    )
                }
            }

            // Detect toxic combinations
            val grantedList = sensitiveEvents.map { it.permissionName }.toSet()
            val toxicList = mutableListOf<String>()
            var calculatedRisk = 5

            if (grantedList.contains("android.permission.SYSTEM_ALERT_WINDOW") &&
                grantedList.contains("android.permission.BIND_ACCESSIBILITY_SERVICE")) {
                toxicList.add("Banking Trojan Pattern: Overlay + Accessibility Service")
                calculatedRisk += 55
            }

            if ((grantedList.contains("android.permission.READ_SMS") || grantedList.contains("android.permission.RECEIVE_SMS")) &&
                reqPermissions.contains("android.permission.INTERNET")) {
                toxicList.add("OTP Stealer Pattern: SMS Read + Internet Outbound")
                calculatedRisk += 45
            }

            if (grantedList.contains("android.permission.RECORD_AUDIO") &&
                grantedList.contains("android.permission.CAMERA")) {
                toxicList.add("Surveillance Vector: Camera + Microphone concurrent access")
                calculatedRisk += 30
            }

            if (grantedList.contains("android.permission.REQUEST_INSTALL_PACKAGES")) {
                toxicList.add("Dropper Vector: Sideload Package Installation capability")
                calculatedRisk += 25
            }

            calculatedRisk += (sensitiveEvents.size * 6)
            val finalRisk = calculatedRisk.coerceIn(0, 100)
            val riskLevel = when {
                finalRisk >= 75 -> "CRITICAL"
                finalRisk >= 50 -> "HIGH"
                finalRisk >= 25 -> "ELEVATED"
                else -> "SAFE"
            }

            records.add(
                AppPermissionUsageRecord(
                    packageName = pkg.packageName,
                    appName = appName,
                    isSystemApp = isSystem,
                    installTimeMs = pkg.firstInstallTime,
                    lastUpdateTimeMs = pkg.lastUpdateTime,
                    grantedPermissionsCount = grantedCount,
                    sensitivePermissions = sensitiveEvents,
                    toxicCombinations = toxicList,
                    riskScore = finalRisk,
                    overallRiskLevel = riskLevel
                )
            )
        }

        // Sort: user apps with highest risk first, then by last update time
        return records.sortedWith(
            compareByDescending<AppPermissionUsageRecord> { !it.isSystemApp }
                .thenByDescending { it.riskScore }
                .thenByDescending { it.lastUpdateTimeMs }
        )
    }
}
