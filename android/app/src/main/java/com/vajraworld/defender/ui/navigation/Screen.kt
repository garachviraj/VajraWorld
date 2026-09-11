package com.vajraworld.defender.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Overview : Screen("overview", "Overview", Icons.Default.Dashboard)
    object Trajectory : Screen("trajectory", "Trajectory", Icons.Default.Timeline)
    object Network : Screen("network", "Network", Icons.Default.Hub)
    object Simulation : Screen("simulation", "Simulate", Icons.Default.Security)
    object Incidents : Screen("incidents", "Incidents", Icons.Default.Warning)
    object Health : Screen("health", "Health", Icons.Default.Settings)
}
