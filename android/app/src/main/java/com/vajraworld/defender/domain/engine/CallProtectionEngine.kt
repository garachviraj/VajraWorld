package com.vajraworld.defender.domain.engine

import android.content.Context
import android.telephony.TelephonyManager

data class CallSecurityStatus(
    val isCallActive: Boolean,
    val callStateLabel: String,
    val riskWarning: String?
)

object CallProtectionEngine {

    fun checkCallSecurity(context: Context): CallSecurityStatus {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            ?: return CallSecurityStatus(false, "NO_TELEPHONY", null)

        val state = try {
            @Suppress("DEPRECATION")
            tm.callState
        } catch (_: SecurityException) {
            TelephonyManager.CALL_STATE_IDLE
        }

        return when (state) {
            TelephonyManager.CALL_STATE_OFFHOOK -> CallSecurityStatus(
                isCallActive = true,
                callStateLabel = "ACTIVE_CALL",
                riskWarning = "ACTIVE VOICE CALL: Never share one-time passwords (OTPs) or verify banking transactions over phone."
            )
            TelephonyManager.CALL_STATE_RINGING -> CallSecurityStatus(
                isCallActive = true,
                callStateLabel = "INCOMING_CALL",
                riskWarning = "INCOMING CALL DETECTED: Caller ID unverified. Beware of urgent account suspension claims."
            )
            else -> CallSecurityStatus(
                isCallActive = false,
                callStateLabel = "IDLE",
                riskWarning = null
            )
        }
    }
}
