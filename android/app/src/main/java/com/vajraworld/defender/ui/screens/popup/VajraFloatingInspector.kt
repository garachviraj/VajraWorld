package com.vajraworld.defender.ui.screens.popup

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.*
import com.vajraworld.defender.data.local.SecurityEventEntity
import com.vajraworld.defender.data.local.VajraDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

object VajraFloatingInspector {

    private var currentFloatingView: View? = null

    fun showInspectionOverlay(
        context: Context,
        threatType: String,
        targetName: String,
        targetId: String,
        riskScore: Int,
        details: List<String>,
        magicHeader: String = "AUDIT_VERIFIED"
    ) {
        val appContext = context.applicationContext

        Log.i("VajraFloatingInspector", "showInspectionOverlay called: type=$threatType, name=$targetName, risk=$riskScore, canDrawOverlays=${Settings.canDrawOverlays(appContext)}")

        if (!Settings.canDrawOverlays(appContext)) {
            launchActivity(appContext, threatType, targetName, targetId, riskScore, details, magicHeader)
            return
        }

        Handler(Looper.getMainLooper()).post {
            try {
                val wm = appContext.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return@post
                dismissOverlay()
                Log.i("VajraFloatingInspector", "Building WindowManager floating overlay view...")

                val isHighRisk = riskScore >= 40
                val primaryColor = if (isHighRisk) Color.parseColor("#FF5252") else Color.parseColor("#00E676")
                val darkBgColor = Color.parseColor("#0F141C")
                val cardBgColor = Color.parseColor("#18202C")
                val textColor = Color.WHITE
                val textMutedColor = Color.parseColor("#A0AEC0")

                val rootLayout = FrameLayout(appContext).apply {
                    setBackgroundColor(Color.parseColor("#99000000"))
                    setPadding(dpToPx(appContext, 20), dpToPx(appContext, 40), dpToPx(appContext, 20), dpToPx(appContext, 40))
                }

                val cardLayout = LinearLayout(appContext).apply {
                    orientation = LinearLayout.VERTICAL
                    background = GradientDrawable().apply {
                        setColor(darkBgColor)
                        cornerRadius = dpToPx(appContext, 16).toFloat()
                        setStroke(dpToPx(appContext, 2), primaryColor)
                    }
                    setPadding(dpToPx(appContext, 20), dpToPx(appContext, 20), dpToPx(appContext, 20), dpToPx(appContext, 20))
                }

                val cardParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = Gravity.CENTER
                }
                rootLayout.addView(cardLayout, cardParams)

                val headerRow = LinearLayout(appContext).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                }

                val titleText = TextView(appContext).apply {
                    text = if (threatType == "NEW_APP_INSTALL") {
                        if (isHighRisk) " SUSPICIOUS APP INSTALLED" else " APP VERIFIED SAFE"
                    } else {
                        if (isHighRisk) " THREAT INTERCEPTED" else " DOWNLOAD VERIFIED SAFE"
                    }
                    setTextColor(primaryColor)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                headerRow.addView(titleText)

                val badgeText = TextView(appContext).apply {
                    text = if (isHighRisk) "RISK $riskScore/100" else "VERIFIED CLEAN"
                    setTextColor(primaryColor)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    background = GradientDrawable().apply {
                        setColor(if (isHighRisk) Color.parseColor("#33FF5252") else Color.parseColor("#3300E676"))
                        cornerRadius = dpToPx(appContext, 6).toFloat()
                        setStroke(dpToPx(appContext, 1), primaryColor)
                    }
                    setPadding(dpToPx(appContext, 8), dpToPx(appContext, 4), dpToPx(appContext, 8), dpToPx(appContext, 4))
                }
                headerRow.addView(badgeText)

                val closeBtn = TextView(appContext).apply {
                    text = "  X  "
                    setTextColor(Color.parseColor("#A0AEC0"))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    setPadding(dpToPx(appContext, 8), dpToPx(appContext, 2), 0, dpToPx(appContext, 2))
                    setOnClickListener { dismissOverlay() }
                }
                headerRow.addView(closeBtn)
                cardLayout.addView(headerRow)

                val nameView = TextView(appContext).apply {
                    text = targetName
                    setTextColor(textColor)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    setPadding(0, dpToPx(appContext, 12), 0, dpToPx(appContext, 4))
                }
                cardLayout.addView(nameView)

                val idView = TextView(appContext).apply {
                    text = targetId
                    setTextColor(textMutedColor)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                    maxLines = 2
                    setPadding(0, 0, 0, dpToPx(appContext, 12))
                }
                cardLayout.addView(idView)

                val detailsBox = LinearLayout(appContext).apply {
                    orientation = LinearLayout.VERTICAL
                    background = GradientDrawable().apply {
                        setColor(cardBgColor)
                        cornerRadius = dpToPx(appContext, 8).toFloat()
                    }
                    setPadding(dpToPx(appContext, 12), dpToPx(appContext, 10), dpToPx(appContext, 12), dpToPx(appContext, 10))
                }

                val detailsTitle = TextView(appContext).apply {
                    text = "VAJRAWORLD FORENSIC ATTRIBUTION:"
                    setTextColor(primaryColor)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    setPadding(0, 0, 0, dpToPx(appContext, 6))
                }
                detailsBox.addView(detailsTitle)

                details.take(4).forEach { point ->
                    val pointView = TextView(appContext).apply {
                        text = "• $point"
                        setTextColor(textColor)
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                        setPadding(0, dpToPx(appContext, 2), 0, dpToPx(appContext, 2))
                    }
                    detailsBox.addView(pointView)
                }
                cardLayout.addView(detailsBox)

                val buttonRow = LinearLayout(appContext).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(0, dpToPx(appContext, 16), 0, 0)
                }

                val allowBtn = Button(appContext).apply {
                    text = "ALLOW"
                    setTextColor(Color.WHITE)
                    background = GradientDrawable().apply {
                        setColor(Color.parseColor("#2E7D32"))
                        cornerRadius = dpToPx(appContext, 8).toFloat()
                    }
                    layoutParams = LinearLayout.LayoutParams(0, dpToPx(appContext, 48), 1f).apply {
                        marginEnd = dpToPx(appContext, 8)
                    }
                    setOnClickListener {
                        handleAllow(appContext, threatType, targetName, targetId, riskScore)
                        dismissOverlay()
                    }
                }
                buttonRow.addView(allowBtn)

                val blockBtn = Button(appContext).apply {
                    text = if (threatType == "NEW_APP_INSTALL") "UNINSTALL" else "BLOCK & DELETE"
                    setTextColor(Color.WHITE)
                    background = GradientDrawable().apply {
                        setColor(Color.parseColor("#C62828"))
                        cornerRadius = dpToPx(appContext, 8).toFloat()
                    }
                    layoutParams = LinearLayout.LayoutParams(0, dpToPx(appContext, 48), 1f).apply {
                        marginStart = dpToPx(appContext, 8)
                    }
                    setOnClickListener {
                        handleBlockAndDelete(appContext, threatType, targetName, targetId, riskScore)
                        dismissOverlay()
                    }
                }
                buttonRow.addView(blockBtn)
                cardLayout.addView(buttonRow)

                rootLayout.setOnClickListener { /* keep overlay visible until user clicks Allow, Block, or Close */ }
                cardLayout.setOnClickListener { /* prevent touch passthrough */ }

                val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                }

                val params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    windowType,
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.CENTER
                }

                wm.addView(rootLayout, params)
                currentFloatingView = rootLayout
                Log.i("VajraFloatingInspector", "Successfully added floating overlay view to WindowManager!")

            } catch (e: Exception) {
                Log.e("VajraFloatingInspector", "Failed to add WindowManager overlay, launching Activity fallback: ${e.message}", e)
                launchActivity(appContext, threatType, targetName, targetId, riskScore, details, magicHeader)
            }
        }
    }

    fun dismissOverlay() {
        try {
            val view = currentFloatingView
            if (view != null && view.isAttachedToWindow) {
                val wm = view.context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
                wm?.removeViewImmediate(view)
            }
        } catch (_: Exception) {}
        currentFloatingView = null
    }

    private fun handleAllow(context: Context, threatType: String, name: String, targetId: String, riskScore: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = VajraDatabase.getInstance(context)
                val event = SecurityEventEntity(
                    id = UUID.randomUUID().toString(),
                    timestamp = System.currentTimeMillis(),
                    eventType = "USER_ALLOWED_THREAT",
                    source = targetId,
                    risk = riskScore.toFloat(),
                    confidence = 0.95f,
                    explanation = "User manually whitelisted: $name ($targetId)",
                    rawContentHash = "allowed_${System.currentTimeMillis()}",
                    isSynthetic = false
                )
                db.dao().insertSecurityEvent(event)
            } catch (_: Exception) {}
        }
        Toast.makeText(context, "Allowed '$name' - verified safe by user.", Toast.LENGTH_LONG).show()
    }

    private fun handleBlockAndDelete(context: Context, threatType: String, name: String, targetId: String, riskScore: Int) {
        if (threatType == "DOWNLOAD_FILE") {
            try {
                val file = File(targetId)
                val deleted = if (file.exists()) file.delete() else false
                if (deleted) {
                    Toast.makeText(context, " Threat Blocked & Deleted: $name", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "File quarantined or already removed.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Deletion failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            // App uninstallation prompt
            try {
                val uninstallIntent = Intent(Intent.ACTION_DELETE).apply {
                    data = Uri.parse("package:$targetId")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(uninstallIntent)
                Toast.makeText(context, "Prompting uninstallation for '$name'...", Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {}
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = VajraDatabase.getInstance(context)
                val event = SecurityEventEntity(
                    id = UUID.randomUUID().toString(),
                    timestamp = System.currentTimeMillis(),
                    eventType = "USER_BLOCKED_AND_DELETED",
                    source = targetId,
                    risk = riskScore.toFloat(),
                    confidence = 0.99f,
                    explanation = "User blocked & removed: $name ($targetId)",
                    rawContentHash = "blocked_${System.currentTimeMillis()}",
                    isSynthetic = false
                )
                db.dao().insertSecurityEvent(event)
            } catch (_: Exception) {}
        }
    }

    private fun launchActivity(
        context: Context,
        threatType: String,
        name: String,
        targetId: String,
        riskScore: Int,
        details: List<String>,
        header: String
    ) {
        try {
            val intent = Intent(context, ThreatInspectionPopupActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(ThreatInspectionPopupActivity.EXTRA_THREAT_TYPE, threatType)
                putExtra(ThreatInspectionPopupActivity.EXTRA_TARGET_NAME, name)
                putExtra(ThreatInspectionPopupActivity.EXTRA_TARGET_ID, targetId)
                putExtra(ThreatInspectionPopupActivity.EXTRA_RISK_SCORE, riskScore)
                putStringArrayListExtra(ThreatInspectionPopupActivity.EXTRA_DETAILS, ArrayList(details))
                putExtra(ThreatInspectionPopupActivity.EXTRA_MAGIC_HEADER, header)
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    private fun dpToPx(context: Context, dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }
}
