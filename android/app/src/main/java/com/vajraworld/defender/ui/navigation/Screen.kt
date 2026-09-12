package com.vajraworld.defender.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Overview : Screen("overview", "Overview", Icons.Default.Dashboard)
    object Radar : Screen("radar", "Radar", Icons.Default.Radar)
    object Trajectory : Screen("trajectory", "Trajectory", Icons.Default.Timeline)
    object Network : Screen("network", "Network", Icons.Default.Hub)
    object Simulation : Screen("simulation", "Simulate", Icons.Default.Security)
    object Incidents : Screen("incidents", "Incidents", Icons.Default.Warning)
    object Explainability : Screen("explainability", "Explain", Icons.Default.Info)
    object Health : Screen("health", "Health", Icons.Default.Settings)
    object LinkScan : Screen("link_scan", "Link Scanner", Icons.Default.Link)
    object FileScan : Screen("file_scan", "File Scanner", Icons.Default.Folder)
    object Clipboard : Screen("clipboard", "Clipboard", Icons.Default.ContentPaste)
    object Permissions : Screen("permissions", "Permissions", Icons.Default.AppRegistration)
    object Settings : Screen("settings", "Settings", Icons.Default.Tune)
}
