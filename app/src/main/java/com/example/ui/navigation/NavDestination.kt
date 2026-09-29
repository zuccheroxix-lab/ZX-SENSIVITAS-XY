package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

enum class NavDestination(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME(
        route = "home",
        title = "GamesLabs",
        selectedIcon = Icons.Filled.Dashboard,
        unselectedIcon = Icons.Outlined.Dashboard
    ),
    SENSITIVITY(
        route = "sensitivity",
        title = "Sensitivity",
        selectedIcon = Icons.Filled.Tune,
        unselectedIcon = Icons.Outlined.Tune
    ),
    OPTIMIZER(
        route = "optimizer",
        title = "Optimizer",
        selectedIcon = Icons.Filled.Speed,
        unselectedIcon = Icons.Outlined.Speed
    ),
    CROSSHAIR(
        route = "crosshair",
        title = "Crosshair",
        selectedIcon = Icons.Filled.Adjust,
        unselectedIcon = Icons.Outlined.Adjust
    ),
    DEVICE(
        route = "device",
        title = "Monitor",
        selectedIcon = Icons.Filled.DeveloperBoard,
        unselectedIcon = Icons.Outlined.DeveloperBoard
    ),
    GAMES(
        route = "games",
        title = "Games",
        selectedIcon = Icons.Filled.SportsEsports,
        unselectedIcon = Icons.Outlined.SportsEsports
    ),
    SHIZUKU(
        route = "shizuku",
        title = "Shizuku",
        selectedIcon = Icons.Filled.Terminal,
        unselectedIcon = Icons.Outlined.Terminal
    ),
    SETTINGS(
        route = "settings",
        title = "Settings",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )
}
