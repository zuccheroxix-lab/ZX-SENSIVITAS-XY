package com.example.ui.screens

import android.content.Context
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.ApplyResult
import com.example.model.ShizukuStatus
import com.example.ui.components.StatusBadge
import com.example.ui.components.TacticalCard
import com.example.ui.navigation.NavDestination
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigate: (NavDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val shizukuInfo by viewModel.shizukuInfo.collectAsState()
    val batteryData by viewModel.batteryData.collectAsState()
    val thermalData by viewModel.thermalData.collectAsState()
    val displayData by viewModel.displayData.collectAsState()
    val crosshairConfig by viewModel.crosshairConfig.collectAsState()
    val selectedProfile by viewModel.selectedProfile.collectAsState()
    val applyResult by viewModel.applyResult.collectAsState()

    val canDrawOverlays = remember { Settings.canDrawOverlays(context) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "ZX OPTIMIZER",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Hardware & Touch Sensitivity Engine",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = { viewModel.refreshHardware() },
                modifier = Modifier.testTag("refresh_hardware_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh System Telemetry",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Apply Result Banner if any
        when (val res = applyResult) {
            is ApplyResult.Success -> {
                TacticalCard(
                    borderColor = MaterialTheme.colorScheme.secondary,
                    backgroundColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = res.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            is ApplyResult.Error -> {
                TacticalCard(
                    borderColor = MaterialTheme.colorScheme.error,
                    backgroundColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = res.reason,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                            res.suggestedAction?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            else -> {}
        }

        // Shizuku Status Card
        TacticalCard(
            borderColor = when (shizukuInfo.status) {
                ShizukuStatus.CONNECTED -> MaterialTheme.colorScheme.secondary
                ShizukuStatus.PERMISSION_REQUIRED -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.outline
            }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SHIZUKU PRIVILEGE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "System Calibrator",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                StatusBadge(
                    text = shizukuInfo.status.name,
                    color = when (shizukuInfo.status) {
                        ShizukuStatus.CONNECTED -> MaterialTheme.colorScheme.secondary
                        ShizukuStatus.PERMISSION_REQUIRED -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.error
                    },
                    testTag = "home_shizuku_badge"
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = shizukuInfo.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onNavigate(NavDestination.SHIZUKU) },
                    modifier = Modifier.testTag("manage_shizuku_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Text(
                        text = "Manage Shizuku",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                if (shizukuInfo.status == ShizukuStatus.PERMISSION_REQUIRED) {
                    Button(
                        onClick = { viewModel.requestShizukuPermission() },
                        modifier = Modifier.testTag("home_request_permission_button")
                    ) {
                        Text("Grant Access")
                    }
                }
            }
        }

        // Active Game Profile Card
        selectedProfile?.let { profile ->
            val isInstalled = viewModel.isGameInstalled(profile.packageName)
            TacticalCard(borderColor = MaterialTheme.colorScheme.primary) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "ACTIVE PROFILE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = profile.displayName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    StatusBadge(
                        text = if (isInstalled) "INSTALLED" else "NOT INSTALLED",
                        color = if (isInstalled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Stats breakdown
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ProfileStatItem(label = "X SENS", value = "%.2fx".format(profile.xSensitivity))
                    ProfileStatItem(label = "Y SENS", value = "%.2fx".format(profile.ySensitivity))
                    ProfileStatItem(label = "RATIO", value = "1 : %.2f".format(profile.xyRatio))
                    ProfileStatItem(label = "PTR SPEED", value = if (profile.pointerSpeed > 0) "+${profile.pointerSpeed}" else "${profile.pointerSpeed}")
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { viewModel.applyGameProfileToSystem(profile) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("apply_active_profile_button")
                    ) {
                        Icon(imageVector = Icons.Default.Done, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Apply to System")
                    }
                    if (isInstalled) {
                        OutlinedButton(
                            onClick = { viewModel.launchGame(profile.packageName, context) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("launch_game_button")
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Launch Game")
                        }
                    }
                }
            }
        }

        // Quick Actions & Telemetry
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Hardware Status
            Card(
                modifier = Modifier
                    .weight(1f)
                    .testTag("home_battery_card"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Icon(
                            imageVector = Icons.Default.BatteryChargingFull,
                            contentDescription = null,
                            tint = if (batteryData.isCharging) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${batteryData.levelPercent}%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${batteryData.temperatureCelsius}°C",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = thermalData.statusString,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Crosshair Status
            Card(
                modifier = Modifier
                    .weight(1f)
                    .testTag("home_crosshair_card"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Icon(
                            imageVector = Icons.Default.Adjust,
                            contentDescription = null,
                            tint = if (crosshairConfig.isEnabled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline
                        )
                        StatusBadge(
                            text = if (crosshairConfig.isEnabled) "ACTIVE" else "OFF",
                            color = if (crosshairConfig.isEnabled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = crosshairConfig.style.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = {
                            viewModel.toggleCrosshair(!crosshairConfig.isEnabled, context)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(34.dp)
                            .testTag("home_toggle_crosshair_button"),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = if (crosshairConfig.isEnabled) "Turn Off" else "Activate",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }

        // Quick Navigation Grid
        Text(
            text = "QUICK NAVIGATION",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = { onNavigate(NavDestination.SENSITIVITY) },
                modifier = Modifier.weight(1f)
            ) {
                Icon(imageVector = Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Sensitivity")
            }
            OutlinedButton(
                onClick = { onNavigate(NavDestination.OPTIMIZER) },
                modifier = Modifier.weight(1f)
            ) {
                Icon(imageVector = Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Optimizer")
            }
        }
    }
}

@Composable
private fun ProfileStatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
