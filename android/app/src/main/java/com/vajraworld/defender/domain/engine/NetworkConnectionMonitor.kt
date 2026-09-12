package com.vajraworld.defender.domain.engine

import android.content.Context
import android.content.pm.PackageManager
import android.net.TrafficStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileReader

data class DeviceSocketConnection(
    val localAddress: String,
    val localPort: Int,
    val remoteAddress: String,
    val remotePort: Int,
    val state: String,
    val uid: Int,
    val appName: String,
    val packageName: String,
    val protocol: String,
    val riskLevel: String, // "SECURE", "WARNING", "CRITICAL"
    val securityNote: String
)

data class NetworkTrafficOverview(
    val totalRxBytes: Long,
    val totalTxBytes: Long,
    val totalRxPackets: Long,
    val totalTxPackets: Long,
    val activeConnections: List<DeviceSocketConnection>
)

object NetworkConnectionMonitor {

    suspend fun inspectActiveConnections(context: Context): NetworkTrafficOverview = withContext(Dispatchers.IO) {
        val connections = mutableListOf<DeviceSocketConnection>()
        val pm = context.packageManager

        // 1. Parse /proc/net/tcp
        parseProcNet(File("/proc/net/tcp"), "TCP", pm, connections)
        // 2. Parse /proc/net/tcp6 (if available)
        parseProcNet(File("/proc/net/tcp6"), "TCP6", pm, connections)
        // 3. Parse /proc/net/udp
        parseProcNet(File("/proc/net/udp"), "UDP", pm, connections)

        // Read TrafficStats
        val rxBytes = TrafficStats.getTotalRxBytes().let { if (it < 0) 0L else it }
        val txBytes = TrafficStats.getTotalTxBytes().let { if (it < 0) 0L else it }
        val rxPackets = TrafficStats.getTotalRxPackets().let { if (it < 0) 0L else it }
        val txPackets = TrafficStats.getTotalTxPackets().let { if (it < 0) 0L else it }

        NetworkTrafficOverview(
            totalRxBytes = rxBytes,
            totalTxBytes = txBytes,
            totalRxPackets = rxPackets,
            totalTxPackets = txPackets,
            activeConnections = connections
        )
    }

    private fun parseProcNet(
        file: File,
        protocol: String,
        pm: PackageManager,
        outputList: MutableList<DeviceSocketConnection>
    ) {
        if (!file.exists() || !file.canRead()) return

        try {
            BufferedReader(FileReader(file)).use { reader ->
                var line = reader.readLine() // Skip header
                while (reader.readLine().also { line = it } != null) {
                    val tokens = line!!.trim().split("\\s+".toRegex())
                    if (tokens.size < 10) continue

                    val localHex = tokens[1]
                    val remoteHex = tokens[2]
                    val stateHex = tokens[3]
                    val uid = tokens[7].toIntOrNull() ?: 0

                    val (localIp, localPort) = parseIpPort(localHex)
                    val (remoteIp, remotePort) = parseIpPort(remoteHex)

                    // Skip loopback and 0.0.0.0 if not listening
                    if (remoteIp == "0.0.0.0" && stateHex != "0A") continue

                    val stateLabel = decodeTcpState(stateHex)
                    var appName = "Android System (UID $uid)"
                    var packageName = "android.system"

                    val packages = pm.getPackagesForUid(uid)
                    if (!packages.isNullOrEmpty()) {
                        packageName = packages[0]
                        appName = try {
                            pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
                        } catch (_: Exception) {
                            packageName
                        }
                    }

                    val (risk, note) = evaluateConnectionRisk(remotePort, packageName, protocol)

                    outputList.add(
                        DeviceSocketConnection(
                            localAddress = localIp,
                            localPort = localPort,
                            remoteAddress = remoteIp,
                            remotePort = remotePort,
                            state = stateLabel,
                            uid = uid,
                            appName = appName,
                            packageName = packageName,
                            protocol = protocol,
                            riskLevel = risk,
                            securityNote = note
                        )
                    )
                }
            }
        } catch (_: Exception) {}
    }

    private fun parseIpPort(hex: String): Pair<String, Int> {
        val parts = hex.split(":")
        if (parts.size != 2) return Pair("0.0.0.0", 0)

        val port = parts[1].toIntOrNull(16) ?: 0

        // Parse little-endian hex IPv4
        val ipHex = parts[0]
        if (ipHex.length == 8) {
            val a = ipHex.substring(6, 8).toInt(16)
            val b = ipHex.substring(4, 6).toInt(16)
            val c = ipHex.substring(2, 4).toInt(16)
            val d = ipHex.substring(0, 2).toInt(16)
            return Pair("$a.$b.$c.$d", port)
        }

        return Pair(ipHex.take(15), port)
    }

    private fun decodeTcpState(hex: String): String {
        return when (hex.uppercase()) {
            "01" -> "ESTABLISHED"
            "02" -> "SYN_SENT"
            "03" -> "SYN_RECV"
            "04" -> "FIN_WAIT1"
            "05" -> "FIN_WAIT2"
            "06" -> "TIME_WAIT"
            "07" -> "CLOSE"
            "08" -> "CLOSE_WAIT"
            "09" -> "LAST_ACK"
            "0A" -> "LISTEN"
            "0B" -> "CLOSING"
            else -> "UNKNOWN ($hex)"
        }
    }

    private fun evaluateConnectionRisk(remotePort: Int, packageName: String, protocol: String): Pair<String, String> {
        return when (remotePort) {
            443 -> Pair("SECURE", "Encrypted TLS 1.3/HTTPS session")
            80 -> Pair("WARNING", "Insecure plaintext HTTP socket - sensitive data exposed")
            53 -> Pair("SECURE", "Standard DNS query resolution")
            22, 23, 21 -> Pair("CRITICAL", "High-risk administrative or plaintext protocol (SSH/Telnet/FTP)")
            5555 -> Pair("CRITICAL", "Android ADB remote debugging port open")
            445, 139 -> Pair("CRITICAL", "SMB Windows sharing port exposed over mobile radio")
            in 8000..9000 -> Pair("WARNING", "Custom HTTP/REST API development port")
            else -> {
                if (remotePort > 10000) {
                    Pair("SECURE", "Dynamic outbound ephemeral port")
                } else {
                    Pair("SECURE", "Standard socket connection")
                }
            }
        }
    }
}
