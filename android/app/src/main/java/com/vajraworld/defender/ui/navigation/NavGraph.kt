package com.vajraworld.defender.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
import com.vajraworld.defender.ui.screens.simulation.SimulationScreen
import com.vajraworld.defender.ui.screens.simulation.SimulationViewModel
import com.vajraworld.defender.ui.theme.*

@Composable
fun VajraNavGraph(repository: VajraRepository) {
    val navController = rememberNavController()
    val items = listOf(
        Screen.Overview,
        Screen.Trajectory,
        Screen.Network,
        Screen.Simulation,
        Screen.Incidents,
        Screen.Health
    )

    val overviewViewModel = remember { OverviewViewModel(repository) }
    val trajectoryViewModel = remember { com.vajraworld.defender.ui.screens.trajectory.TrajectoryViewModel(repository) }
    val networkViewModel = remember { NetworkGraphViewModel() }
    val simulationViewModel = remember { SimulationViewModel(repository) }
    val incidentsViewModel = remember { IncidentsViewModel(repository) }
    val explainabilityViewModel = remember { ExplainabilityViewModel() }
    val healthViewModel = remember { HealthViewModel() }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = CardDark,
                contentColor = TextPrimary
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
                            selectedIconColor = AccentCyan,
                            selectedTextColor = AccentCyan,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                            indicatorColor = BorderDark
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
                    onNavigateToSimulation = { navController.navigate(Screen.Simulation.route) }
                )
            }
            composable(Screen.Trajectory.route) {
                com.vajraworld.defender.ui.screens.trajectory.TrajectoryScreen(viewModel = trajectoryViewModel)
            }
            composable(Screen.Network.route) {
                NetworkGraphScreen(viewModel = networkViewModel)
            }
            composable(Screen.Simulation.route) {
                SimulationScreen(viewModel = simulationViewModel)
            }
            composable(Screen.Incidents.route) {
                val incState by incidentsViewModel.uiState.collectAsState()
                if (incState.selectedIncident != null) {
                    IncidentDetailScreen(
                        incident = incState.selectedIncident!!,
                        onBack = { /* Return or toggle view */ },
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
            composable(Screen.Health.route) {
                HealthScreen(viewModel = healthViewModel)
            }
        }
    }
}
