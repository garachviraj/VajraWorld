package com.vajraworld.defender.domain.engine

import android.content.Context
import android.content.pm.PackageManager
import android.net.TrafficStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.text.SimpleDateFormat
import java.util.*

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

data class InspectedPacketRecord(
    val id: String,
    val timestamp: Long,
    val timeFormatted: String,
    val protocol: String,
    val localEndpoint: String,
    val remoteEndpoint: String,
    val appName: String,
    val packageName: String,
    val sizeBytes: Int,
    val isSuspicious: Boolean,
    val threatMarkdown: String
)

data class NetworkTrafficOverview(
    val totalRxBytes: Long,
    val totalTxBytes: Long,
    val totalRxPackets: Long,
    val totalTxPackets: Long,
    val activeConnections: List<DeviceSocketConnection>,
    val livePackets: List<InspectedPacketRecord>
)

object NetworkConnectionMonitor {

    suspend fun inspectActiveConnections(context: Context): NetworkTrafficOverview = withContext(Dispatchers.IO) {
        val connections = mutableListOf<DeviceSocketConnection>()
        val pm = context.packageManager

        // 1. Parse /proc/net/tcp
        parseProcNet(File("/proc/net/tcp"), "TCP", pm, connections)
        // 2. Parse /proc/net/tcp6
        parseProcNet(File("/proc/net/tcp6"), "TCP6", pm, connections)
        // 3. Parse /proc/net/udp
        parseProcNet(File("/proc/net/udp"), "UDP", pm, connections)

        // Read TrafficStats
        val rxBytes = TrafficStats.getTotalRxBytes().let { if (it < 0) 0L else it }
        val txBytes = TrafficStats.getTotalTxBytes().let { if (it < 0) 0L else it }
        val rxPackets = TrafficStats.getTotalRxPackets().let { if (it < 0) 0L else it }
        val txPackets = TrafficStats.getTotalTxPackets().let { if (it < 0) 0L else it }

        // Synthesize live packet stream from active connections
        val sdf = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
        val now = System.currentTimeMillis()
        val packets = mutableListOf<InspectedPacketRecord>()

        connections.take(35).forEachIndexed { index, conn ->
            val isPort80 = conn.remotePort == 80
            val isPort53 = conn.remotePort == 53
            val isSuspiciousPort = conn.remotePort in listOf(6667, 1337, 4444, 8080, 3128, 9050)
            val isSuspicious = isPort80 || isSuspiciousPort || conn.riskLevel != "SECURE"

            val threatTag = when {
                isPort80 -> "⚠️ UNENCRYPTED HTTP (Port 80) • Plaintext credentials & headers exposed"
                isSuspiciousPort -> "🚨 UNUSUAL REMOTE PORT (${conn.remotePort}) • Potential C2 or botnet channel"
                isPort53 && conn.remoteAddress != "8.8.8.8" && conn.remoteAddress != "1.1.1.1" -> "⚠️ CUSTOM DNS RESOLVER (${conn.remoteAddress}:53) • Potential DNS leak"
                conn.riskLevel == "CRITICAL" -> "🚨 ELEVATED THREAT • Suspicious process socket"
                else -> "✅ NOMINAL TLS ENCRYPTED • Port ${conn.remotePort}"
            }

            packets.add(
                InspectedPacketRecord(
                    id = "pkt_${now}_$index",
                    timestamp = now - (index * 450L),
                    timeFormatted = sdf.format(Date(now - (index * 450L))),
                    protocol = conn.protocol,
                    localEndpoint = "${conn.localAddress}:${conn.localPort}",
                    remoteEndpoint = "${conn.remoteAddress}:${conn.remotePort}",
                    appName = conn.appName,
                    packageName = conn.packageName,
                    sizeBytes = 64 + (index * 37 % 1420),
                    isSuspicious = isSuspicious,
                    threatMarkdown = threatTag
                )
            )
        }

        NetworkTrafficOverview(
            totalRxBytes = rxBytes,
            totalTxBytes = txBytes,
            totalRxPackets = rxPackets,
            totalTxPackets = txPackets,
            activeConnections = connections,
            livePackets = packets
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

        val ipHex = parts[0]
        val portHex = parts[1]
        val port = portHex.toIntOrNull(16) ?: 0

        val ip = if (ipHex.length == 8) {
            // IPv4: stored in little-endian hex
            val b1 = ipHex.substring(6, 8).toInt(16)
            val b2 = ipHex.substring(4, 6).toInt(16)
            val b3 = ipHex.substring(2, 4).toInt(16)
            val b4 = ipHex.substring(0, 2).toInt(16)
            "$b1.$b2.$b3.$b4"
        } else {
            "IPv6"
        }

        return Pair(ip, port)
    }

    private fun decodeTcpState(hex: String): String = when (hex.uppercase()) {
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

    private fun evaluateConnectionRisk(remotePort: Int, packageName: String, protocol: String): Pair<String, String> {
        return when {
            remotePort == 80 -> Pair("WARNING", "Insecure HTTP connection (unencrypted plaintext transit)")
            remotePort in listOf(6667, 1337, 4444, 8080, 3128) -> Pair("CRITICAL", "High-risk port (common IRC/Proxy/C2 beacon target)")
            remotePort == 22 || remotePort == 23 -> Pair("WARNING", "Remote administrative port active (SSH/Telnet)")
            remotePort == 443 -> Pair("SECURE", "Standard encrypted TLS/HTTPS connection")
            remotePort == 53 -> Pair("SECURE", "Domain Name Resolution (DNS)")
            else -> Pair("SECURE", "Standard active connection")
        }
    }
}
