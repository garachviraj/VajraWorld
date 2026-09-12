package com.vajraworld.defender.service

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import com.vajraworld.defender.VajraApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SecurityActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return

        val action = intent.action ?: return
        val targetId = intent.getStringExtra(VajraNotificationManager.EXTRA_TARGET_ID) ?: ""
        val targetType = intent.getStringExtra(VajraNotificationManager.EXTRA_TARGET_TYPE) ?: "PACKAGE"
        val notificationId = intent.getIntExtra(VajraNotificationManager.EXTRA_NOTIFICATION_ID, -1)

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        if (notificationId != -1) {
            nm?.cancel(notificationId)
        }

        val prefs = context.getSharedPreferences("vajra_security_rules", Context.MODE_PRIVATE)

        when (action) {
            VajraNotificationManager.ACTION_BLOCK -> {
                if (targetType == "PACKAGE") {
                    val quarantined = prefs.getStringSet("quarantined_packages", emptySet())?.toMutableSet() ?: mutableSetOf()
                    quarantined.add(targetId)
                    prefs.edit().putStringSet("quarantined_packages", quarantined).apply()

                    // Open App Info settings so the user can immediately Force Stop or Uninstall
                    try {
                        val appSettingsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", targetId, null)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(appSettingsIntent)
                    } catch (_: Exception) {}

                    Toast.makeText(context, "🛡️ VajraWorld: App '$targetId' quarantined. Force stop or revoke permissions.", Toast.LENGTH_LONG).show()
                } else {
                    val blockedUrls = prefs.getStringSet("blocked_domains", emptySet())?.toMutableSet() ?: mutableSetOf()
                    blockedUrls.add(targetId)
                    prefs.edit().putStringSet("blocked_domains", blockedUrls).apply()
                    Toast.makeText(context, "🛡️ VajraWorld: Domain '$targetId' added to shield blocklist.", Toast.LENGTH_SHORT).show()
                }

                // Update Room incident status
                val app = context.applicationContext as? VajraApplication
                app?.let {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val dao = it.database.dao()
                            val incidents = dao.getAllIncidentsSync()
                            val match = incidents.find { inc -> inc.affectedAssetsJson.contains(targetId) || inc.title.contains(targetId) }
                            match?.let { inc ->
                                dao.updateIncidentStatus(inc.incidentId, "CONTAINED")
                            }
                        } catch (_: Exception) {}
                    }
                }
            }

            VajraNotificationManager.ACTION_UNBLOCK -> {
                if (targetType == "PACKAGE") {
                    val quarantined = prefs.getStringSet("quarantined_packages", emptySet())?.toMutableSet() ?: mutableSetOf()
                    quarantined.remove(targetId)
                    val whitelisted = prefs.getStringSet("whitelisted_packages", emptySet())?.toMutableSet() ?: mutableSetOf()
                    whitelisted.add(targetId)
                    prefs.edit()
                        .putStringSet("quarantined_packages", quarantined)
                        .putStringSet("whitelisted_packages", whitelisted)
                        .apply()
                    Toast.makeText(context, "✅ VajraWorld: Package '$targetId' unblocked and whitelisted.", Toast.LENGTH_SHORT).show()
                } else {
                    val blockedUrls = prefs.getStringSet("blocked_domains", emptySet())?.toMutableSet() ?: mutableSetOf()
                    blockedUrls.remove(targetId)
                    val whitelistedUrls = prefs.getStringSet("whitelisted_domains", emptySet())?.toMutableSet() ?: mutableSetOf()
                    whitelistedUrls.add(targetId)
                    prefs.edit()
                        .putStringSet("blocked_domains", blockedUrls)
                        .putStringSet("whitelisted_domains", whitelistedUrls)
                        .apply()
                    Toast.makeText(context, "✅ VajraWorld: Domain '$targetId' unblocked and whitelisted.", Toast.LENGTH_SHORT).show()
                }

                val app = context.applicationContext as? VajraApplication
                app?.let {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val dao = it.database.dao()
                            val incidents = dao.getAllIncidentsSync()
                            val match = incidents.find { inc -> inc.affectedAssetsJson.contains(targetId) || inc.title.contains(targetId) }
                            match?.let { inc ->
                                dao.updateIncidentStatus(inc.incidentId, "RESOLVED")
                            }
                        } catch (_: Exception) {}
                    }
                }
            }
        }
    }
}
