package com.example.ui.screens

import android.os.Build
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.ShizukuStatus
import com.example.ui.components.StatusBadge
import com.example.ui.components.TacticalCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel

@Composable
fun DeviceMonitorScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val batteryData by viewModel.batteryData.collectAsState()
    val thermalData by viewModel.thermalData.collectAsState()
    val memoryData by viewModel.memoryData.collectAsState()
    val storageData by viewModel.storageData.collectAsState()
    val displayData by viewModel.displayData.collectAsState()
    val cpuData by viewModel.cpuData.collectAsState()
    val shizukuInfo by viewModel.shizukuInfo.collectAsState()

    val canDrawOverlays = remember { Settings.canDrawOverlays(context) }
    val canWriteSettings = remember { Settings.System.canWrite(context) }

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
                    text = "DEVICE TELEMETRY",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "100% Genuine Android hardware sensors & system metrics",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = { viewModel.refreshHardware() },
                modifier = Modifier.testTag("refresh_device_telemetry_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh Telemetry",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Battery & Charging Telemetry
        TacticalCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "BATTERY & POWER SENSORS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${batteryData.levelPercent}% • ${batteryData.chargeSource}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                StatusBadge(
                    text = if (batteryData.isCharging) "CHARGING" else "BATTERY",
                    color = if (batteryData.isCharging) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TelemetryMetricItem("Temperature", "${batteryData.temperatureCelsius}°C")
                TelemetryMetricItem("Voltage", "${batteryData.voltageMillivolts} mV")
                TelemetryMetricItem("Health", batteryData.health)
                TelemetryMetricItem("Tech", batteryData.technology)
            }
        }

        // Thermal Headroom Telemetry
        TacticalCard {
            Text(
                text = "THERMAL STATE & HEADROOM",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = thermalData.statusString,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = when (thermalData.severityLevel) {
                        0 -> MaterialTheme.colorScheme.secondary
                        1 -> MaterialTheme.colorScheme.primary
                        2 -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.error
                    }
                )
                Text(
                    text = if (thermalData.thermalHeadroom != null)
                        "Headroom: ${"%.1f".format(thermalData.thermalHeadroom)}x"
                    else "API 29+ Hardware Event",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // RAM & Storage
        TacticalCard {
            Text(
                text = "MEMORY & STORAGE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))

            // RAM Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("RAM Usage", style = MaterialTheme.typography.bodyMedium)
                Text("${memoryData.usedPercentage}% (${memoryData.availableMb} MB free of ${memoryData.totalMb} MB)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { (memoryData.usedPercentage / 100f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Storage Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Internal Storage", style = MaterialTheme.typography.bodyMedium)
                Text("${storageData.usedPercentage}% (${"%.1f".format(storageData.availableGb)} GB free)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { (storageData.usedPercentage / 100f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }

        // Display Specs & Refresh Rate
        TacticalCard {
            Text(
                text = "DISPLAY SPECIFICATIONS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TelemetryMetricItem("Resolution", "${displayData.physicalWidthPx} x ${displayData.physicalHeightPx}")
                TelemetryMetricItem("Refresh Rate", "${displayData.refreshRateHz.toInt()} Hz")
                TelemetryMetricItem("Density DPI", "${displayData.densityDpi}")
                TelemetryMetricItem("In-Game FPS", "UNAVAILABLE")
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "FPS Note: Android restricts 3rd-party frame rate metering without systrace/root. No randomized fake FPS numbers are generated. Display is locked to hardware ${displayData.refreshRateHz.toInt()} Hz.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // CPU & SoC Specs
        TacticalCard {
            Text(
                text = "CPU & SYSTEM ARCHITECTURE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TelemetryMetricItem("CPU Cores", "${cpuData.coresCount} Cores")
                TelemetryMetricItem("Architecture", cpuData.architecture)
                TelemetryMetricItem("Android Version", "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Device Model: ${Build.MANUFACTURER.uppercase()} ${Build.MODEL} (${cpuData.socModel})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // System Privileges & Permissions Checklist
        TacticalCard {
            Text(
                text = "PRIVILEGE & PERMISSION STATUS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))

            PermissionRow(
                title = "Shizuku ADB/Binder Access",
                isGranted = shizukuInfo.status == ShizukuStatus.CONNECTED,
                statusText = shizukuInfo.status.name
            )

            PermissionRow(
                title = "Overlay (Draw over other apps)",
                isGranted = canDrawOverlays,
                statusText = if (canDrawOverlays) "GRANTED" else "DENIED"
            )

            PermissionRow(
                title = "Modify System Settings",
                isGranted = canWriteSettings,
                statusText = if (canWriteSettings) "GRANTED" else "DEFAULT"
            )
        }
    }
}

@Composable
private fun TelemetryMetricItem(label: String, value: String) {
    Column {
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

@Composable
private fun PermissionRow(title: String, isGranted: Boolean, statusText: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyMedium)
        StatusBadge(
            text = statusText,
            color = if (isGranted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.tertiary
        )
    }
}
