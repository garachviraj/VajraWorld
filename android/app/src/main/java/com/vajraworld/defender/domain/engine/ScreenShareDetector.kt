package com.vajraworld.defender.domain.engine

import android.content.Context
import android.hardware.display.DisplayManager
import android.view.Display

data class ScreenShareStatus(
    val isScreenSharingOrRecording: Boolean,
    val activeDisplayCount: Int,
    val details: String
)

object ScreenShareDetector {

    fun detectScreenSharing(context: Context): ScreenShareStatus {
        val dm = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
            ?: return ScreenShareStatus(false, 1, "Display service unavailable")

        val displays = dm.displays
        var isVirtualSharingActive = false
        val displayInfoList = mutableListOf<String>()

        for (d in displays) {
            val isDefault = d.displayId == Display.DEFAULT_DISPLAY
            val name = d.name ?: "Unknown"
            val flags = d.flags

            val isPresentation = (flags and Display.FLAG_PRESENTATION) != 0
            val isPrivate = (flags and Display.FLAG_PRIVATE) != 0

            // If there is a non-default display that is presentation, non-private, or virtual capture
            if (!isDefault) {
                if (isPresentation || !isPrivate ||
                    name.contains("Virtual", ignoreCase = true) ||
                    name.contains("Cast", ignoreCase = true) ||
                    name.contains("Mirror", ignoreCase = true) ||
                    name.contains("ScreenShare", ignoreCase = true) ||
                    name.contains("Recording", ignoreCase = true) ||
                    name.contains("MediaProjection", ignoreCase = true)
                ) {
                    isVirtualSharingActive = true
                    displayInfoList.add("Virtual/Cast Display: '$name' (ID: ${d.displayId})")
                }
            }
        }

        return ScreenShareStatus(
            isScreenSharingOrRecording = isVirtualSharingActive,
            activeDisplayCount = displays.size,
            details = if (isVirtualSharingActive) {
                "Active Screen Sharing / Recording Detected: ${displayInfoList.joinToString("; ")}"
            } else {
                "Display Security Nominal: Standard Hardware Screen (No Mirrored Displays)"
            }
        )
    }
}
