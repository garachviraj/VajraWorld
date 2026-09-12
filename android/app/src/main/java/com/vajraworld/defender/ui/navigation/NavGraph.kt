package com.vajraworld.defender.ui.navigation

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.vajraworld.defender.data.repository.VajraRepository
import com.vajraworld.defender.ui.screens.explainability.ExplainabilityScreen
import com.vajraworld.defender.ui.screens.explainability.ExplainabilityViewModel
import com.vajraworld.defender.ui.screens.health.HealthScreen
import com.vajraworld.defender.ui.screens.health.HealthViewModel
import com.vajraworld.defender.ui.screens.incidents.IncidentDetailScreen
import com.vajraworld.defender.ui.screens.incidents.IncidentsScreen
import com.vajraworld.defender.ui.screens.incidents.IncidentsViewModel
import com.vajraworld.defender.ui.screens.network.NetworkGraphScreen
import com.vajraworld.defender.ui.screens.network.NetworkGraphViewModel
import com.vajraworld.defender.ui.screens.overview.OverviewScreen
import com.vajraworld.defender.ui.screens.overview.OverviewViewModel
import com.vajraworld.defender.ui.screens.radar.SecurityRadarScreen
import com.vajraworld.defender.ui.screens.radar.SecurityRadarViewModel
import com.vajraworld.defender.ui.screens.scanner.FileScanScreen
import com.vajraworld.defender.ui.screens.scanner.FileScanViewModel
import com.vajraworld.defender.ui.screens.scanner.LinkScanScreen
import com.vajraworld.defender.ui.screens.scanner.LinkScanViewModel
import com.vajraworld.defender.ui.screens.simulation.SimulationScreen
import com.vajraworld.defender.ui.screens.simulation.SimulationViewModel
import com.vajraworld.defender.ui.screens.trajectory.TrajectoryScreen
import com.vajraworld.defender.ui.screens.trajectory.TrajectoryViewModel
import com.vajraworld.defender.ui.theme.*

@Composable
fun VajraNavGraph(repository: VajraRepository) {
    val navController = rememberNavController()
    val items = listOf(
        Screen.Overview,
        Screen.Radar,
        Screen.Trajectory,
        Screen.Network,
        Screen.Simulation,
        Screen.Incidents
    )

    val overviewViewModel = remember { OverviewViewModel(repository) }
    val trajectoryViewModel = remember { TrajectoryViewModel(repository) }
    val networkViewModel = remember { NetworkGraphViewModel(repository) }
    val simulationViewModel = remember { SimulationViewModel(repository) }
    val incidentsViewModel = remember { IncidentsViewModel(repository) }
    val explainabilityViewModel = remember { ExplainabilityViewModel(repository) }
    val healthViewModel = remember { HealthViewModel(repository) }
    val radarViewModel = remember { SecurityRadarViewModel(repository) }
    val linkViewModel = remember { LinkScanViewModel(repository) }
    val fileViewModel = remember { FileScanViewModel(repository) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                modifier = Modifier.border(width = 1.dp, color = BorderColor),
                containerColor = Bg0,
                contentColor = TextPrimary,
                tonalElevation = 8.dp
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                items.forEach { screen ->
                    val isSelected = currentRoute == screen.route
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title, fontSize = 10.sp) },
                        selected = isSelected,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Info,
                            selectedTextColor = Info,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                            indicatorColor = Surface2
                        ),
                        onClick = {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.startDestinationId)
                                    launchSingleTop = true
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Overview.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Overview.route) {
                OverviewScreen(
                    viewModel = overviewViewModel,
                    onNavigateToSimulation = { navController.navigate(Screen.Simulation.route) },
                    onNavigateToRadar = { navController.navigate(Screen.Radar.route) },
                    onNavigateToLinkScan = { navController.navigate(Screen.LinkScan.route) },
                    onNavigateToFileScan = { navController.navigate(Screen.FileScan.route) }
                )
            }
            composable(Screen.Radar.route) {
                SecurityRadarScreen(
                    viewModel = radarViewModel,
                    onNavigateToSimulation = { navController.navigate(Screen.Simulation.route) }
                )
            }
            composable(Screen.Trajectory.route) {
                TrajectoryScreen(viewModel = trajectoryViewModel)
            }
            composable(Screen.Network.route) {
                NetworkGraphScreen(
                    viewModel = networkViewModel,
                    onNavigateToSimulation = { navController.navigate(Screen.Simulation.route) }
                )
            }
            composable(Screen.Simulation.route) {
                SimulationScreen(viewModel = simulationViewModel)
            }
            composable(Screen.Incidents.route) {
                val incState by incidentsViewModel.uiState.collectAsState()
                if (incState.selectedIncident != null) {
                    IncidentDetailScreen(
                        incident = incState.selectedIncident!!,
                        onBack = { incidentsViewModel.clearSelection() },
                        onTestDefence = { navController.navigate(Screen.Simulation.route) },
                        onAcknowledge = { id -> incidentsViewModel.acknowledgeIncident(id) }
                    )
                } else {
                    IncidentsScreen(
                        viewModel = incidentsViewModel,
                        onSelectIncident = { inc -> incidentsViewModel.selectIncident(inc) }
                    )
                }
            }
            composable(Screen.Explainability.route) {
                ExplainabilityScreen(viewModel = explainabilityViewModel)
            }
            composable(Screen.Health.route) {
                HealthScreen(viewModel = healthViewModel)
            }
            composable(Screen.LinkScan.route) {
                LinkScanScreen(viewModel = linkViewModel)
            }
            composable(Screen.FileScan.route) {
                FileScanScreen(viewModel = fileViewModel)
            }
            composable(Screen.Clipboard.route) {
                com.vajraworld.defender.ui.screens.clipboard.ClipboardGuardianScreen(repository = repository)
            }
        }
    }
}
