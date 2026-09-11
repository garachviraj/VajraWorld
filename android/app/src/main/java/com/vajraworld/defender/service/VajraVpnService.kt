package com.vajraworld.defender.service

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer

/**
 * Android VpnService packet capture skeleton for VajraWorld Standalone Mode (Mode A).
 * Reads raw IP frames from TUN interface, parses IP/TCP/UDP headers, and forwards
 * packet metadata to the local feature engine.
 */
class VajraVpnService : VpnService(), Runnable {
    private var vpnInterface: ParcelFileDescriptor? = null
    private var vpnThread: Thread? = null
    private var isRunning = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isRunning) {
            isRunning = true
            vpnThread = Thread(this, "VajraVpnThread").apply { start() }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        isRunning = false
        vpnThread?.interrupt()
        vpnInterface?.close()
        super.onDestroy()
    }

    override fun run() {
        try {
            // Configure virtual TUN interface
            vpnInterface = Builder()
                .setSession("VajraWorld Defender Cockpit")
                .addAddress("10.0.0.2", 24)
                .addRoute("0.0.0.0", 0)
                .setMtu(1500)
                .setBlocking(true)
                .establish()

            val inputStream = FileInputStream(vpnInterface?.fileDescriptor)
            val packet = ByteBuffer.allocate(32767)

            Log.i("VajraVpnService", "TUN interface established. Ingesting packet metadata...")

            while (isRunning) {
                val length = inputStream.read(packet.array())
                if (length > 0) {
                    // Extract version and protocol byte from IPv4 header
                    val proto = packet.get(9).toInt() and 0xFF
                    // Forward metadata (Src/Dst, Proto, Length) to local telemetry queue
                    packet.clear()
                }
            }
        } catch (e: Exception) {
            Log.e("VajraVpnService", "VPN loop error: ${e.message}")
        }
    }
}
