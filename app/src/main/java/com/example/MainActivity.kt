package com.example

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.navigation.NavDestination
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ZxDarkBackground
import com.example.ui.theme.ZxDarkSurface
import com.example.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            enableEdgeToEdge()
        } catch (e: Throwable) {
            Log.w(TAG, "enableEdgeToEdge non-critical fallback: ${e.message}")
        }

        setContent {
            MyApplicationTheme {
                var currentDestination by remember { mutableStateOf(NavDestination.HOME) }

                // Safe back handling: if on a sub-screen, return to HOME
                BackHandler(enabled = currentDestination != NavDestination.HOME) {
                    currentDestination = NavDestination.HOME
                }

                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ZxDarkBackground),
                    bottomBar = {
                        NavigationBar(
                            modifier = Modifier
                                .fillMaxWidth()
                                .windowInsetsPadding(WindowInsets.navigationBars)
                                .testTag("main_navigation_bar"),
                            containerColor = ZxDarkSurface,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            tonalElevation = 8.dp
                        ) {
                            NavDestination.values().forEach { destination ->
                                val isSelected = currentDestination == destination
                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = { currentDestination = destination },
                                    icon = {
                                        Icon(
                                            imageVector = if (isSelected) destination.selectedIcon else destination.unselectedIcon,
                                            contentDescription = destination.title
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = destination.title,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.testTag("nav_${destination.route}")
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .windowInsetsPadding(WindowInsets.statusBars)
                    ) {
                        SafeScreenContainer(
                            screenName = currentDestination.title,
                            onFallbackHome = { currentDestination = NavDestination.HOME }
                        ) {
                            when (currentDestination) {
                                NavDestination.HOME -> HomeScreen(
                                    viewModel = viewModel,
                                    onNavigate = { currentDestination = it }
                                )
                                NavDestination.SENSITIVITY -> SensitivityScreen(
                                    viewModel = viewModel
                                )
                                NavDestination.OPTIMIZER -> OptimizerScreen(
                                    viewModel = viewModel
                                )
                                NavDestination.CROSSHAIR -> CrosshairScreen(
                                    viewModel = viewModel
                                )
                                NavDestination.DEVICE -> DeviceMonitorScreen(
                                    viewModel = viewModel
                                )
                                NavDestination.GAMES -> GameProfileScreen(
                                    viewModel = viewModel
                                )
                                NavDestination.SHIZUKU -> ShizukuScreen(
                                    viewModel = viewModel
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SafeScreenContainer(
    screenName: String,
    onFallbackHome: () -> Unit,
    content: @Composable () -> Unit
) {
    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    if (hasError) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(40.dp)
                    )
                    Text(
                        text = "Unable to load $screenName",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = errorMessage.ifEmpty { "A recoverable rendering state occurred. Device resources have been preserved." },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = {
                            hasError = false
                            onFallbackHome()
                        }
                    ) {
                        Text("Return to Home")
                    }
                }
            }
        }
    } else {
        content()
    }
}
