package com.vajraworld.defender.domain.engine

import android.app.ActivityManager
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import java.io.File
import java.net.Inet4Address
import java.net.NetworkInterface

data class DeviceHardwareProfile(
    val manufacturer: String,
    val model: String,
    val deviceName: String,
    val androidVersion: String,
    val apiLevel: Int,
    val securityPatch: String,
    val totalRamMb: Long,
    val availableRamMb: Long,
    val ramUsagePct: Int,
    val totalStorageGb: Float,
    val freeStorageGb: Float,
    val batteryLevel: Int,
    val batteryTemperatureC: Float
)

data class SystemIntegrityReport(
    val isRooted: Boolean,
    val rootSignals: List<String>,
    val isDeveloperOptionsEnabled: Boolean,
    val isAdbEnabled: Boolean,
    val isDeviceSecure: Boolean,
    val overallIntegrityScore: Int // 0-100 (100 = perfectly secure)
)

data class NetworkSecurityState(
    val activeTransport: String,
    val isVpnActive: Boolean,
    val wifiSsid: String?,
    val linkSpeedMbps: Int,
    val ipAddress: String?
)

data class RealDeviceTelemetry(
    val hardware: DeviceHardwareProfile,
    val integrity: SystemIntegrityReport,
    val network: NetworkSecurityState,
    val overallRiskScore: Int, // 0-100 (0 = safe, 100 = critical danger)
    val postureLabel: String,
    val riskFactors: List<String>
)

object DeviceSecurityEngine {

    private val SU_BINARY_PATHS = listOf(
        "/system/app/Superuser.apk",
        "/sbin/su",
        "/system/bin/su",
        "/system/xbin/su",
        "/data/local/xbin/su",
        "/data/local/bin/su",
        "/system/sd/xbin/su",
        "/system/bin/failsafe/su",
        "/data/local/su",
        "/su/bin/su"
    )

    fun inspectHardware(context: Context): DeviceHardwareProfile {
        val manufacturer = Build.MANUFACTURER?.replaceFirstChar { it.uppercase() } ?: "Android"
        val model = Build.MODEL ?: "Device"
        val deviceName = if (model.startsWith(manufacturer, ignoreCase = true)) model else "$manufacturer $model"
        val androidVersion = Build.VERSION.RELEASE ?: "Unknown"
        val apiLevel = Build.VERSION.SDK_INT
        val securityPatch = Build.VERSION.SECURITY_PATCH ?: "Unknown"

        // Memory info
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager?.getMemoryInfo(memInfo)
        val totalRamMb = memInfo.totalMem / (1024 * 1024)
        val availRamMb = memInfo.availMem / (1024 * 1024)
        val ramUsagePct = if (totalRamMb > 0) (((totalRamMb - availRamMb).toDouble() / totalRamMb) * 100).toInt() else 0

        // Storage info
        val stat = try {
            StatFs(Environment.getDataDirectory().path)
        } catch (_: Exception) {
            null
        }
        val totalStorageGb = stat?.let { (it.blockCountLong * it.blockSizeLong).toFloat() / (1024f * 1024f * 1024f) } ?: 0f
        val freeStorageGb = stat?.let { (it.availableBlocksLong * it.blockSizeLong).toFloat() / (1024f * 1024f * 1024f) } ?: 0f

        // Battery info
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryLevel = if (level >= 0 && scale > 0) (level * 100) / scale else 85
        val tempRaw = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        val batteryTempC = if (tempRaw > 0) tempRaw / 10f else 28.5f

        return DeviceHardwareProfile(
            manufacturer = manufacturer,
            model = model,
            deviceName = deviceName,
            androidVersion = androidVersion,
            apiLevel = apiLevel,
            securityPatch = securityPatch,
            totalRamMb = totalRamMb,
            availableRamMb = availRamMb,
            ramUsagePct = ramUsagePct.coerceIn(0, 100),
            totalStorageGb = String.format(java.util.Locale.US, "%.1f", totalStorageGb).toFloatOrNull() ?: totalStorageGb,
            freeStorageGb = String.format(java.util.Locale.US, "%.1f", freeStorageGb).toFloatOrNull() ?: freeStorageGb,
            batteryLevel = batteryLevel.coerceIn(0, 100),
            batteryTemperatureC = batteryTempC
        )
    }

    fun inspectIntegrity(context: Context): SystemIntegrityReport {
        val rootSignals = mutableListOf<String>()

        // 1. Test-keys tag in build
        if (Build.TAGS?.contains("test-keys") == true) {
            rootSignals.add("Build compiled with test-keys (custom ROM signature)")
        }

        // 2. su binary presence in filesystem
        for (path in SU_BINARY_PATHS) {
            if (File(path).exists()) {
                rootSignals.add("su binary found at $path")
                break
            }
        }

        // 3. which su check
        try {
            val process = Runtime.getRuntime().exec(arrayOf("/system/xbin/which", "su"))
            val output = process.inputStream.bufferedReader().readLine()
            if (!output.isNullOrBlank()) {
                rootSignals.add("su binary reachable via system path: $output")
            }
        } catch (_: Exception) {}

        // 4. Magisk package check
        val pm = context.packageManager
        val knownRootPackages = listOf("com.topjohnwu.magisk", "eu.chainfire.supersu", "com.koushikdutta.superuser")
        for (pkg in knownRootPackages) {
            try {
                pm.getPackageInfo(pkg, 0)
                rootSignals.add("Root management package detected: $pkg")
            } catch (_: Exception) {}
        }

        val isRooted = rootSignals.isNotEmpty()

        // Developer options
        val isDevOptionsEnabled = try {
            Settings.Global.getInt(context.contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) == 1
        } catch (_: Exception) {
            false
        }

        // USB Debugging (ADB)
        val isAdbEnabled = try {
            Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED, 0) == 1
        } catch (_: Exception) {
            false
        }

        // Lock screen protection
        val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        val isDeviceSecure = keyguard?.isDeviceSecure ?: false

        var integrityScore = 100
        if (isRooted) integrityScore -= 50
        if (!isDeviceSecure) integrityScore -= 20
        if (isAdbEnabled) integrityScore -= 15
        if (isDevOptionsEnabled) integrityScore -= 10

        return SystemIntegrityReport(
            isRooted = isRooted,
            rootSignals = rootSignals,
            isDeveloperOptionsEnabled = isDevOptionsEnabled,
            isAdbEnabled = isAdbEnabled,
            isDeviceSecure = isDeviceSecure,
            overallIntegrityScore = integrityScore.coerceIn(0, 100)
        )
    }

    fun inspectNetwork(context: Context): NetworkSecurityState {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNetwork = cm?.activeNetwork
        val caps = activeNetwork?.let { cm.getNetworkCapabilities(it) }

        val transport = when {
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true -> "VPN"
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "WIFI"
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "CELLULAR"
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> "ETHERNET"
            else -> "OFFLINE"
        }

        val isVpn = caps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) ?: false

        // Wi-Fi SSID
        val wifiSsid = if (transport == "WIFI" || transport == "VPN") {
            try {
                val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                val info = wm?.connectionInfo
                val ssid = info?.ssid?.replace("\"", "")
                if (ssid == "<unknown ssid>" || ssid.isNullOrBlank()) "Connected Wi-Fi" else ssid
            } catch (_: Exception) {
                "Connected Wi-Fi"
            }
        } else null

        // Link speed
        val linkSpeed = try {
            val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            wm?.connectionInfo?.linkSpeed ?: 0
        } catch (_: Exception) {
            0
        }

        // Local IP address
        val ipAddress = getLocalIpAddress()

        return NetworkSecurityState(
            activeTransport = transport,
            isVpnActive = isVpn,
            wifiSsid = wifiSsid,
            linkSpeedMbps = linkSpeed,
            ipAddress = ipAddress
        )
    }

    private fun getLocalIpAddress(): String? {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (iface.isLoopback || !iface.isUp) continue
                val addresses = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        return addr.hostAddress
                    }
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    fun getTelemetry(context: Context, appRiskPenalty: Int = 0): RealDeviceTelemetry {
        val hardware = inspectHardware(context)
        val integrity = inspectIntegrity(context)
        val network = inspectNetwork(context)

        val riskFactors = mutableListOf<String>()
        var computedRisk = 100 - integrity.overallIntegrityScore

        if (integrity.isRooted) {
            riskFactors.add("Root access / custom binaries detected on device")
        }
        if (integrity.isAdbEnabled) {
            riskFactors.add("USB Debugging (ADB) is enabled (unrestricted shell exposure)")
        }
        if (!integrity.isDeviceSecure) {
            riskFactors.add("No screen lock / biometric protection configured")
        }
        if (network.activeTransport == "OFFLINE") {
            riskFactors.add("Device is offline")
        }

        if (appRiskPenalty > 0) {
            computedRisk = maxOf(computedRisk, (computedRisk * 0.5f + appRiskPenalty * 0.5f).toInt())
            riskFactors.add("Application permission anomalies detected ($appRiskPenalty% risk)")
        }

        val overallRiskScore = computedRisk.coerceIn(5, 95)
        val postureLabel = when {
            overallRiskScore >= 70 -> "CRITICAL RISK"
            overallRiskScore >= 45 -> "ELEVATED POSTURE"
            overallRiskScore >= 25 -> "MODERATE CAUTION"
            else -> "SECURE & NOMINAL"
        }

        return RealDeviceTelemetry(
            hardware = hardware,
            integrity = integrity,
            network = network,
            overallRiskScore = overallRiskScore,
            postureLabel = postureLabel,
            riskFactors = riskFactors
        )
    }
}
