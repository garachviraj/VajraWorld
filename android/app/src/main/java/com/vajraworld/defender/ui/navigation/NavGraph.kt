package com.vajraworld.defender.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.vajraworld.defender.data.repository.VajraRepository
import com.vajraworld.defender.ui.screens.clipboard.ClipboardGuardianScreen
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
import com.vajraworld.defender.ui.screens.permissions.PermissionTimelineScreen
import com.vajraworld.defender.ui.screens.permissions.PermissionTimelineViewModel
import com.vajraworld.defender.ui.screens.radar.SecurityRadarScreen
import com.vajraworld.defender.ui.screens.radar.SecurityRadarViewModel
import com.vajraworld.defender.ui.screens.scanner.FileScanScreen
import com.vajraworld.defender.ui.screens.scanner.FileScanViewModel
import com.vajraworld.defender.ui.screens.scanner.LinkScanScreen
import com.vajraworld.defender.ui.screens.scanner.LinkScanViewModel
import com.vajraworld.defender.ui.screens.settings.SettingsScreen
import com.vajraworld.defender.ui.screens.settings.SettingsViewModel
import com.vajraworld.defender.ui.screens.simulation.SimulationScreen
import com.vajraworld.defender.ui.screens.simulation.SimulationViewModel
import com.vajraworld.defender.ui.screens.trajectory.TrajectoryScreen
import com.vajraworld.defender.ui.screens.trajectory.TrajectoryViewModel
import com.vajraworld.defender.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VajraNavGraph(repository: VajraRepository) {
    val navController = rememberNavController()
    val context = LocalContext.current
    var showSurfacesSheet by remember { mutableStateOf(false) }

    val bottomBarItems = listOf(
        Screen.Overview,
        Screen.Radar,
        Screen.Trajectory,
        Screen.Network,
        Screen.Simulation,
        Screen.Incidents
    )

    val allScreens = listOf(
        Screen.Overview,
        Screen.Radar,
        Screen.Trajectory,
        Screen.Network,
        Screen.Simulation,
        Screen.Incidents,
        Screen.Explainability,
        Screen.Health,
        Screen.LinkScan,
        Screen.FileScan,
        Screen.Clipboard,
        Screen.Permissions,
        Screen.Settings
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
    val permissionViewModel = remember { PermissionTimelineViewModel(repository) }
    val settingsViewModel = remember { SettingsViewModel(repository, context) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar(
                modifier = Modifier.border(width = 1.dp, color = BorderColor),
                containerColor = Bg0,
                contentColor = TextPrimary,
                tonalElevation = 8.dp
            ) {
                bottomBarItems.forEach { screen ->
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
                    onNavigateToFileScan = { navController.navigate(Screen.FileScan.route) },
                    onNavigateToClipboard = { navController.navigate(Screen.Clipboard.route) },
                    onNavigateToExplainability = { navController.navigate(Screen.Explainability.route) },
                    onNavigateToHealth = { navController.navigate(Screen.Health.route) },
                    onNavigateToTrajectory = { navController.navigate(Screen.Trajectory.route) },
                    onNavigateToNetwork = { navController.navigate(Screen.Network.route) },
                    onNavigateToIncidents = { navController.navigate(Screen.Incidents.route) },
                    onNavigateToPermissions = { navController.navigate(Screen.Permissions.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onOpenSurfacesHub = { showSurfacesSheet = true }
                )
            }
            composable(Screen.Radar.route) {
                SecurityRadarScreen(
                    viewModel = radarViewModel,
                    onNavigateToSimulation = { navController.navigate(Screen.Simulation.route) },
                    onBack = { navController.popBackStack() },
                    onHubClick = { showSurfacesSheet = true }
                )
            }
            composable(Screen.Trajectory.route) {
                TrajectoryScreen(
                    viewModel = trajectoryViewModel,
                    onBack = { navController.popBackStack() },
                    onHubClick = { showSurfacesSheet = true }
                )
            }
            composable(Screen.Network.route) {
                NetworkGraphScreen(
                    viewModel = networkViewModel,
                    onNavigateToSimulation = { navController.navigate(Screen.Simulation.route) },
                    onBack = { navController.popBackStack() },
                    onHubClick = { showSurfacesSheet = true }
                )
            }
            composable(Screen.Simulation.route) {
                SimulationScreen(
                    viewModel = simulationViewModel,
                    onBack = { navController.popBackStack() },
                    onHubClick = { showSurfacesSheet = true }
                )
            }
            composable(Screen.Incidents.route) {
                val incState by incidentsViewModel.uiState.collectAsState()
                if (incState.selectedIncident != null) {
                    IncidentDetailScreen(
                        incident = incState.selectedIncident!!,
                        onBack = { incidentsViewModel.clearSelection() },
                        onTestDefence = { navController.navigate(Screen.Simulation.route) },
                        onAcknowledge = { id -> incidentsViewModel.acknowledgeIncident(id) },
                        onResolve = { id -> incidentsViewModel.resolveIncident(id) },
                        onContain = { id -> incidentsViewModel.containIncident(id) }
                    )
                } else {
                    IncidentsScreen(
                        viewModel = incidentsViewModel,
                        onSelectIncident = { inc -> incidentsViewModel.selectIncident(inc) },
                        onBack = { navController.popBackStack() },
                        onHubClick = { showSurfacesSheet = true }
                    )
                }
            }
            composable(Screen.Explainability.route) {
                ExplainabilityScreen(
                    viewModel = explainabilityViewModel,
                    onBack = { navController.popBackStack() },
                    onHubClick = { showSurfacesSheet = true }
                )
            }
            composable(Screen.Health.route) {
                HealthScreen(
                    viewModel = healthViewModel,
                    onBack = { navController.popBackStack() },
                    onHubClick = { showSurfacesSheet = true }
                )
            }
            composable(Screen.LinkScan.route) {
                LinkScanScreen(
                    viewModel = linkViewModel,
                    onBack = { navController.popBackStack() },
                    onHubClick = { showSurfacesSheet = true }
                )
            }
            composable(Screen.FileScan.route) {
                FileScanScreen(
                    viewModel = fileViewModel,
                    onBack = { navController.popBackStack() },
                    onHubClick = { showSurfacesSheet = true }
                )
            }
            composable(Screen.Clipboard.route) {
                ClipboardGuardianScreen(
                    repository = repository,
                    onBack = { navController.popBackStack() },
                    onHubClick = { showSurfacesSheet = true }
                )
            }
            composable(Screen.Permissions.route) {
                PermissionTimelineScreen(
                    viewModel = permissionViewModel,
                    onBack = { navController.popBackStack() },
                    onHubClick = { showSurfacesSheet = true }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onBack = { navController.popBackStack() },
                    onHubClick = { showSurfacesSheet = true }
                )
            }
        }

        // Cockpit Surfaces Hub Modal Bottom Sheet
        if (showSurfacesSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSurfacesSheet = false },
                containerColor = Bg0,
                contentColor = TextPrimary
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "VAJRAWORLD GUARDIAN SURFACES HUB",
                                style = TechnicalValue.copy(fontSize = 13.sp, color = Info, fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "13 Autonomous Cyber Defence & Intelligence Surfaces",
                                style = MetadataText
                            )
                        }
                        Box(
                            modifier = Modifier
                                .background(Surface2, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(text = "SOC COCKPIT", style = TechnicalValue.copy(fontSize = 9.sp, color = Healthy))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp)
                    ) {
                        items(allScreens) { scr ->
                            val isCurrent = currentRoute == scr.route
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isCurrent) Surface2 else Surface0)
                                    .border(1.dp, if (isCurrent) Info else BorderColor, RoundedCornerShape(8.dp))
                                    .clickable {
                                        showSurfacesSheet = false
                                        if (currentRoute != scr.route) {
                                            navController.navigate(scr.route) {
                                                popUpTo(navController.graph.startDestinationId)
                                                launchSingleTop = true
                                            }
                                        }
                                    }
                                    .padding(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = scr.icon,
                                        contentDescription = scr.title,
                                        tint = if (isCurrent) Info else TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Column {
                                        Text(
                                            text = scr.title,
                                            style = TechnicalValue.copy(
                                                fontSize = 11.sp,
                                                color = if (isCurrent) Info else TextPrimary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                        Text(
                                            text = if (isCurrent) "ACTIVE" else "READY",
                                            style = MetadataText.copy(
                                                fontSize = 8.sp,
                                                color = if (isCurrent) Healthy else TextMuted
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}
