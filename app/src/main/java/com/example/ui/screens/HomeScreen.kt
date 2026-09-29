package com.example.ui.screens

import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.*
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
    val executionMode by viewModel.executionMode.collectAsState()
    val sensitivityMode by viewModel.sensitivityMode.collectAsState()
    val sensScore by viewModel.sensCalibrationScore.collectAsState()
    val touchScore by viewModel.touchCalibrationScore.collectAsState()
    val smartStutter by viewModel.smartStutterAnalysis.collectAsState()
    val vibrationConfig by viewModel.vibrationConfig.collectAsState()
    val selectedPreset by viewModel.selectedPreset.collectAsState()
    val lastActionResult by viewModel.lastActionResult.collectAsState()

    val canDrawOverlays = remember { Settings.canDrawOverlays(context) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ZxDarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // APP HEADER
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "GAMESLABS",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = ZxNeonCyan
                )
                Text(
                    text = "SENSITIVITY X/Y + OPTIMIZER",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = ZxTextPrimary
                )
            }
            IconButton(
                onClick = { viewModel.refreshHardware() },
                modifier = Modifier.testTag("refresh_hardware_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh System Telemetry",
                    tint = ZxNeonCyan
                )
            }
        }

        // GLOBAL EXECUTION MODE SWITCH (SIMULATION vs APPLY)
        Card(
            colors = CardDefaults.cardColors(containerColor = ZxDarkSurface),
            shape = RoundedCornerShape(14.dp),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(ZxBorder))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "OPERATION MODE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = ZxTextSecondary
                    )
                    Surface(
                        color = if (executionMode == ExecutionMode.APPLY) ZxNeonCyan.copy(alpha = 0.2f) else ZxNeonGreen.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = executionMode.badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (executionMode == ExecutionMode.APPLY) ZxNeonCyan else ZxNeonGreen,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.setExecutionMode(ExecutionMode.SIMULATION) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (executionMode == ExecutionMode.SIMULATION) ZxNeonGreen else ZxDarkCard
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "SIMULATION",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (executionMode == ExecutionMode.SIMULATION) ZxDarkBackground else ZxTextSecondary
                        )
                    }

                    Button(
                        onClick = { viewModel.setExecutionMode(ExecutionMode.APPLY) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (executionMode == ExecutionMode.APPLY) ZxNeonCyan else ZxDarkCard
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "APPLY",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (executionMode == ExecutionMode.APPLY) ZxDarkBackground else ZxTextSecondary
                        )
                    }
                }
            }
        }

        // ACTION RESULT BANNER (Shows last real action details and verification)
        lastActionResult?.let { action ->
            Card(
                colors = CardDefaults.cardColors(containerColor = ZxDarkSurface),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(
                        when (action.status) {
                            ActionStatus.SUCCESS -> ZxNeonGreen
                            ActionStatus.NOT_SUPPORTED, ActionStatus.BLOCKED -> ZxWarning
                            else -> ZxNeonCyan
                        }
                    )
                )
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ACTION: ${action.actionName.uppercase()}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = ZxTextPrimary
                        )
                        Text(
                            text = "[${action.status.title}]",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = when (action.status) {
                                ActionStatus.SUCCESS -> ZxNeonGreen
                                ActionStatus.NOT_SUPPORTED, ActionStatus.BLOCKED -> ZxWarning
                                else -> ZxNeonCyan
                            }
                        )
                    }
                    Text(
                        text = action.detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = ZxTextSecondary
                    )
                    Text(
                        text = "Target: ${action.targetName} • Verification: ${action.verification.title}",
                        style = MaterialTheme.typography.labelSmall,
                        color = ZxNeonCyan
                    )
                }
            }
        }

        // --- MAIN GAMESLABS DASHBOARD PANEL ---
        Card(
            colors = CardDefaults.cardColors(containerColor = ZxDarkSurface),
            shape = RoundedCornerShape(16.dp),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(ZxBorder))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "GAMESLABS TELEMETRY & CALIBRATION",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = ZxNeonGreen
                    )
                    Surface(
                        color = ZxDarkCard,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = sensitivityMode.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = ZxNeonCyan,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                // 2 PRIMARY BENCHMARK TILES: SENSITIVITY CALIBRATION (85%) & TOUCH CALIBRATION (70%)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GamesLabsMetricTile(
                        title = "SENSITIVITY CALIBRATION",
                        scoreText = "$sensScore%",
                        subtext = "Balance & Elevation Index",
                        accentColor = ZxNeonCyan,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigate(NavDestination.SENSITIVITY) }
                    )

                    GamesLabsMetricTile(
                        title = "TOUCH CALIBRATION",
                        scoreText = "$touchScore%",
                        subtext = "${displayData.refreshRateHz.toInt()}Hz Hardware Rating",
                        accentColor = ZxNeonGreen,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigate(NavDestination.SENSITIVITY) }
                    )
                }

                Divider(color = ZxBorder, thickness = 0.5.dp)

                // SMART STUTTER STATUS
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "SMART STUTTER",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = ZxTextPrimary
                        )
                        Text(
                            text = smartStutter.status.desc,
                            style = MaterialTheme.typography.bodySmall,
                            color = ZxTextSecondary
                        )
                    }
                    Surface(
                        color = when (smartStutter.status) {
                            SmartStutterStatus.STABLE -> ZxNeonGreen.copy(alpha = 0.2f)
                            SmartStutterStatus.WARNING -> ZxWarning.copy(alpha = 0.2f)
                            else -> ZxNeonRed.copy(alpha = 0.2f)
                        },
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = smartStutter.status.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = when (smartStutter.status) {
                                SmartStutterStatus.STABLE -> ZxNeonGreen
                                SmartStutterStatus.WARNING -> ZxWarning
                                else -> ZxNeonRed
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                // VIBRATION CONTROL STATUS
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "VIBRATION CONTROL",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = ZxTextPrimary
                        )
                        Text(
                            text = if (vibrationConfig.isEnabled) "Haptic profile: ${vibrationConfig.profile.displayName}" else "Vibration feedback disabled",
                            style = MaterialTheme.typography.bodySmall,
                            color = ZxTextSecondary
                        )
                    }
                    Button(
                        onClick = { viewModel.testVibration() },
                        colors = ButtonDefaults.buttonColors(containerColor = ZxDarkCard),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Vibration, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("TEST", style = MaterialTheme.typography.labelSmall)
                    }
                }

                // EXCLUSIVE PRESET & LIFETIME STATUS
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "EXCLUSIVE PRESET",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = ZxTextPrimary
                        )
                        Text(
                            text = selectedPreset?.title ?: "CUSTOM (Manual tuning)",
                            style = MaterialTheme.typography.bodySmall,
                            color = ZxNeonCyan
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "LIFETIME UPDATE",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = ZxTextPrimary
                        )
                        Text(
                            text = "v1.2.0 (OFFLINE)",
                            style = MaterialTheme.typography.bodySmall,
                            color = ZxNeonGreen
                        )
                    }
                }
            }
        }

        // QUICK JUMP GRID (6 CORE CAPABILITIES)
        Text(
            text = "COMMAND MODULES",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = ZxTextSecondary
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ModuleNavCard(
                title = "Sensitivity",
                subtitle = "X/Y Drag Engine",
                icon = Icons.Default.Tune,
                badge = "${selectedProfile?.displayName?.take(10) ?: "Global"}",
                modifier = Modifier.weight(1f),
                onClick = { onNavigate(NavDestination.SENSITIVITY) }
            )
            ModuleNavCard(
                title = "Optimizer",
                subtitle = "Stutter & Cache",
                icon = Icons.Default.Speed,
                badge = "${smartStutter.status.name}",
                modifier = Modifier.weight(1f),
                onClick = { onNavigate(NavDestination.OPTIMIZER) }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ModuleNavCard(
                title = "Crosshair",
                subtitle = "Tactical Reticle",
                icon = Icons.Default.Adjust,
                badge = if (canDrawOverlays) (if (crosshairConfig.isEnabled) "ACTIVE" else "READY") else "PERM REQ",
                badgeColor = if (canDrawOverlays) ZxNeonGreen else ZxWarning,
                modifier = Modifier.weight(1f),
                onClick = { onNavigate(NavDestination.CROSSHAIR) }
            )
            ModuleNavCard(
                title = "Monitor",
                subtitle = "Hardware State",
                icon = Icons.Default.DeveloperBoard,
                badge = "${batteryData.temperatureCelsius.toInt()}°C",
                modifier = Modifier.weight(1f),
                onClick = { onNavigate(NavDestination.DEVICE) }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ModuleNavCard(
                title = "Games",
                subtitle = "Game Profiles",
                icon = Icons.Default.SportsEsports,
                badge = "${selectedProfile?.displayName?.take(8) ?: "Profiles"}",
                modifier = Modifier.weight(1f),
                onClick = { onNavigate(NavDestination.GAMES) }
            )
            ModuleNavCard(
                title = "Shizuku",
                subtitle = "Privileged Center",
                icon = Icons.Default.Terminal,
                badge = if (shizukuInfo.status == ShizukuStatus.CONNECTED) "ONLINE" else "OFFLINE",
                badgeColor = if (shizukuInfo.status == ShizukuStatus.CONNECTED) ZxNeonGreen else ZxTextSecondary,
                modifier = Modifier.weight(1f),
                onClick = { onNavigate(NavDestination.SHIZUKU) }
            )
        }

        // SETTINGS & SYSTEM CHECK SHORTCUT
        Button(
            onClick = { onNavigate(NavDestination.SETTINGS) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = ZxDarkSurface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Settings, contentDescription = null, tint = ZxNeonCyan)
            Spacer(modifier = Modifier.width(8.dp))
            Text("OPEN SETTINGS & SYSTEM DIAGNOSTICS", color = ZxTextPrimary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun GamesLabsMetricTile(
    title: String,
    scoreText: String,
    subtext: String,
    accentColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = ZxDarkCard),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(ZxBorder))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = ZxTextSecondary
            )
            Text(
                text = scoreText,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                color = accentColor
            )
            Text(
                text = subtext,
                style = MaterialTheme.typography.labelSmall,
                color = ZxTextSecondary
            )
        }
    }
}

@Composable
private fun ModuleNavCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    badge: String,
    badgeColor: androidx.compose.ui.graphics.Color = ZxNeonCyan,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = ZxDarkSurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(ZxBorder))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = ZxNeonCyan, modifier = Modifier.size(22.dp))
                Surface(
                    color = badgeColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = ZxTextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = ZxTextSecondary
            )
        }
    }
}
