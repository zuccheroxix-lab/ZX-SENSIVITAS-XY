package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.*
import com.example.ui.components.StatusBadge
import com.example.ui.components.TacticalCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun OptimizerScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val optimizerState by viewModel.optimizerState.collectAsState()
    val memoryData by viewModel.memoryData.collectAsState()
    val batteryData by viewModel.batteryData.collectAsState()
    val thermalData by viewModel.thermalData.collectAsState()
    val displayData by viewModel.displayData.collectAsState()
    val shizukuInfo by viewModel.shizukuInfo.collectAsState()
    val executionMode by viewModel.executionMode.collectAsState()
    val smartStutter by viewModel.smartStutterAnalysis.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "GAME OPTIMIZER",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Smart Stutter Detection & Native Resource Management",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            StatusBadge(
                text = optimizerState.currentMode.name,
                color = when (optimizerState.currentMode) {
                    PerformanceMode.PERFORMANCE -> ZxNeonCyan
                    PerformanceMode.BALANCED -> ZxNeonGreen
                    PerformanceMode.BATTERY_SAVER -> ZxWarning
                }
            )
        }

        // --- SMART STUTTER DETECTION PANEL ---
        TacticalCard(
            borderColor = when (smartStutter.status) {
                SmartStutterStatus.STABLE -> ZxNeonGreen
                SmartStutterStatus.WARNING, SmartStutterStatus.HIGH_LOAD -> ZxWarning
                else -> ZxNeonRed
            }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            imageVector = when (smartStutter.status) {
                                SmartStutterStatus.STABLE -> Icons.Default.CheckCircle
                                SmartStutterStatus.WARNING -> Icons.Default.Warning
                                else -> Icons.Default.ErrorOutline
                            },
                            contentDescription = null,
                            tint = when (smartStutter.status) {
                                SmartStutterStatus.STABLE -> ZxNeonGreen
                                SmartStutterStatus.WARNING -> ZxWarning
                                else -> ZxNeonRed
                            }
                        )
                        Text(
                            text = "SMART STUTTER DIAGNOSTICS",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = ZxTextPrimary
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
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = smartStutter.status.desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = ZxTextSecondary
                )

                // 4 Real Metrics
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("RAM Free", style = MaterialTheme.typography.labelSmall, color = ZxTextSecondary)
                        Text(
                            text = "${smartStutter.memoryFreePercent.toInt()}%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (smartStutter.memoryFreePercent > 20f) ZxNeonGreen else ZxWarning
                        )
                    }

                    Column {
                        Text("Battery Temp", style = MaterialTheme.typography.labelSmall, color = ZxTextSecondary)
                        Text(
                            text = "${smartStutter.batteryTempCelsius}°C",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (smartStutter.batteryTempCelsius < 40f) ZxNeonCyan else ZxWarning
                        )
                    }

                    Column {
                        Text("Thermal Status", style = MaterialTheme.typography.labelSmall, color = ZxTextSecondary)
                        Text(
                            text = thermalData.statusString.take(12),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ZxTextPrimary
                        )
                    }

                    Column {
                        Text("Refresh Rate", style = MaterialTheme.typography.labelSmall, color = ZxTextSecondary)
                        Text(
                            text = "${displayData.refreshRateHz.toInt()} Hz",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ZxNeonGreen
                        )
                    }
                }

                if (smartStutter.recommendations.isNotEmpty()) {
                    Divider(color = ZxBorder, thickness = 0.5.dp)
                    smartStutter.recommendations.forEach { rec ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.ArrowRight, contentDescription = null, tint = ZxNeonCyan, modifier = Modifier.size(16.dp))
                            Text(rec, style = MaterialTheme.typography.bodySmall, color = ZxTextSecondary)
                        }
                    }
                }
            }
        }

        // --- OPTIMIZATION ACTION PIPELINE (ANALYZE / SIMULATE / APPLY / VERIFY) ---
        TacticalCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "OPTIMIZATION PIPELINE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Current Mode: ${executionMode.displayName} (${executionMode.badgeText})",
                    style = MaterialTheme.typography.bodySmall,
                    color = ZxNeonCyan
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.refreshHardware() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = ZxDarkCard)
                    ) {
                        Text("ANALYZE", style = MaterialTheme.typography.labelSmall)
                    }

                    Button(
                        onClick = {
                            viewModel.setExecutionMode(ExecutionMode.SIMULATION)
                            viewModel.setPerformanceMode(optimizerState.currentMode)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = ZxDarkCard)
                    ) {
                        Text("SIMULATE", style = MaterialTheme.typography.labelSmall)
                    }

                    Button(
                        onClick = {
                            viewModel.setExecutionMode(ExecutionMode.APPLY)
                            viewModel.setPerformanceMode(optimizerState.currentMode)
                        },
                        modifier = Modifier.weight(1.2f),
                        colors = ButtonDefaults.buttonColors(containerColor = ZxNeonGreen)
                    ) {
                        Text("APPLY", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = ZxDarkBackground)
                    }

                    Button(
                        onClick = { viewModel.refreshHardware() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = ZxDarkCard)
                    ) {
                        Text("VERIFY", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        // --- HARDWARE PERFORMANCE PROFILES ---
        Text(
            text = "HARDWARE PROFILES",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PerformanceMode.values().forEach { mode ->
                val isSelected = optimizerState.currentMode == mode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surface
                        )
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { viewModel.setPerformanceMode(mode) }
                        .padding(12.dp)
                        .testTag("mode_${mode.name.lowercase()}")
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = when (mode) {
                                PerformanceMode.BALANCED -> Icons.Default.Balance
                                PerformanceMode.PERFORMANCE -> Icons.Default.Bolt
                                PerformanceMode.BATTERY_SAVER -> Icons.Default.BatteryChargingFull
                            },
                            contentDescription = mode.title,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = mode.title,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Action Status Alert
        TacticalCard(borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = optimizerState.actionStatus,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // --- SAFE MEMORY MANAGEMENT ---
        TacticalCard {
            Text(
                text = "APPLICATION CACHE TRIMMING",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Safely triggers standard Android garbage collection and trims obsolete bitmap resources without killing essential system processes.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Available System RAM",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${memoryData.availableBytes / (1024 * 1024)} MB / ${memoryData.totalBytes / (1024 * 1024)} MB",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Button(
                    onClick = { viewModel.runMemoryOptimization() },
                    modifier = Modifier.testTag("trim_memory_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(imageVector = Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Trim Cache", fontWeight = FontWeight.Bold)
                }
            }

            if (optimizerState.lastTrimmedTime > 0) {
                val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Last trimmed at ${timeFormat.format(Date(optimizerState.lastTrimmedTime))} • Freed ${optimizerState.memoryFreedMb} MB",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }

        // --- SHIZUKU PRIVILEGED GAMING OPTIMIZATION ---
        TacticalCard(borderColor = if (shizukuInfo.status == ShizukuStatus.CONNECTED) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ANDROID GAME MODE API",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Native Android 12+ Game Mode Manager",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusBadge(
                    text = if (shizukuInfo.status == ShizukuStatus.CONNECTED) "SHIZUKU READY" else "SHIZUKU REQUIRED",
                    color = if (shizukuInfo.status == ShizukuStatus.CONNECTED) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "When Shizuku is connected, ZX Optimizer can invoke 'cmd game mode performance <package>' to request vendor GPU/CPU scheduling priorities natively without root.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
