package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.model.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val executionMode by viewModel.executionMode.collectAsState()
    val sensitivityMode by viewModel.sensitivityMode.collectAsState()
    val vibrationConfig by viewModel.vibrationConfig.collectAsState()
    val preflightResult by viewModel.preflightResult.collectAsState()
    val capabilityMatrix by viewModel.capabilityMatrix.collectAsState()
    val actionHistory by viewModel.actionHistory.collectAsState()
    val gameProfiles by viewModel.gameProfiles.collectAsState()
    val globalConfig by viewModel.globalSensitivityConfig.collectAsState()
    val crosshairPresets by viewModel.crosshairPresets.collectAsState()

    var showExportDialog by remember { mutableStateOf(false) }
    var exportJsonText by remember { mutableStateOf("") }
    var showImportDialog by remember { mutableStateOf(false) }
    var importJsonInput by remember { mutableStateOf("") }
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ZxDarkBackground),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // HEADER
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ZxDarkSurface),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(ZxBorder))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = ZxNeonCyan,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "GAMESLABS SETTINGS & SECURITY",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ZxTextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Global runtime policies, haptic calibration, pre-flight system diagnostics, and configuration backup.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ZxTextSecondary
                    )
                }
            }
        }

        // EXECUTION MODE SWITCH
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ZxDarkSurface),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(ZxBorder))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "EXECUTION MODE",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = ZxNeonGreen
                    )
                    Text(
                        text = "Choose whether actions run in pure Simulation Mode or execute actual system calibration.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ZxTextSecondary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ExecutionMode.values().forEach { mode ->
                            val isSelected = executionMode == mode
                            Button(
                                onClick = { viewModel.setExecutionMode(mode) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) {
                                        if (mode == ExecutionMode.APPLY) ZxNeonCyan else ZxNeonGreen
                                    } else ZxDarkCard
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = mode.displayName,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) ZxDarkBackground else ZxTextSecondary
                                    )
                                    Text(
                                        text = mode.badgeText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) ZxDarkBackground else ZxTextSecondary
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = executionMode.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = ZxNeonCyan
                    )
                }
            }
        }

        // SENSITIVITY MODE SWITCH
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ZxDarkSurface),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(ZxBorder))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "SENSITIVITY ARCHITECTURE",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = ZxNeonCyan
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SensitivityMode.values().forEach { sMode ->
                            val isSelected = sensitivityMode == sMode
                            OutlinedButton(
                                onClick = { viewModel.setSensitivityMode(sMode) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSelected) ZxDarkCard else androidx.compose.ui.graphics.Color.Transparent
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) ZxNeonCyan else ZxBorder
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = sMode.displayName,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) ZxNeonCyan else ZxTextSecondary
                                )
                            }
                        }
                    }

                    Text(
                        text = sensitivityMode.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = ZxTextSecondary
                    )
                }
            }
        }

        // VIBRATION & HAPTIC CONTROL
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ZxDarkSurface),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(ZxBorder))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Vibration, contentDescription = null, tint = ZxNeonGreen)
                            Text(
                                text = "HAPTIC VIBRATION CONTROL",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = ZxNeonGreen
                            )
                        }
                        Switch(
                            checked = vibrationConfig.isEnabled,
                            onCheckedChange = { viewModel.updateVibrationConfig(vibrationConfig.copy(isEnabled = it)) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ZxNeonGreen,
                                checkedTrackColor = ZxNeonGreen.copy(alpha = 0.3f)
                            )
                        )
                    }

                    if (vibrationConfig.isEnabled) {
                        Text(
                            text = "Intensity: ${vibrationConfig.intensity} / 255",
                            style = MaterialTheme.typography.bodySmall,
                            color = ZxTextPrimary
                        )
                        Slider(
                            value = vibrationConfig.intensity.toFloat(),
                            onValueChange = { viewModel.updateVibrationConfig(vibrationConfig.copy(intensity = it.toInt())) },
                            valueRange = 10f..255f,
                            colors = SliderDefaults.colors(
                                thumbColor = ZxNeonGreen,
                                activeTrackColor = ZxNeonGreen
                            )
                        )

                        Text(
                            text = "Haptic Waveform Profile:",
                            style = MaterialTheme.typography.bodySmall,
                            color = ZxTextSecondary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            HapticProfileType.values().take(3).forEach { profile ->
                                val isSel = vibrationConfig.profile == profile
                                FilterChip(
                                    selected = isSel,
                                    onClick = { viewModel.updateVibrationConfig(vibrationConfig.copy(profile = profile)) },
                                    label = { Text(profile.displayName, style = MaterialTheme.typography.labelSmall) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ZxNeonGreen.copy(alpha = 0.2f),
                                        selectedLabelColor = ZxNeonGreen
                                    )
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.testVibration() },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = ZxDarkCard)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Test Haptic")
                            }
                            OutlinedButton(
                                onClick = { viewModel.resetVibrationConfig() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Reset")
                            }
                        }
                    }
                }
            }
        }

        // PREFLIGHT SYSTEM DIAGNOSTICS
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ZxDarkSurface),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(ZxBorder))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PRE-FLIGHT SYSTEM CHECK",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = ZxNeonCyan
                        )
                        Button(
                            onClick = { viewModel.runPreflightCheck() },
                            colors = ButtonDefaults.buttonColors(containerColor = ZxNeonCyan),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("RUN CHECK", style = MaterialTheme.typography.labelSmall, color = ZxDarkBackground)
                        }
                    }

                    if (preflightResult.items.isNotEmpty()) {
                        Text(
                            text = "Overall Status: ${preflightResult.status.name}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (preflightResult.status == PreflightStatus.READY) ZxNeonGreen else ZxWarning
                        )
                        preflightResult.items.forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (item.isPassed) Icons.Default.CheckCircle else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (item.isPassed) ZxNeonGreen else ZxWarning,
                                    modifier = Modifier.size(16.dp)
                                )
                                Column {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = ZxTextPrimary
                                    )
                                    Text(
                                        text = item.detail,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ZxTextSecondary
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "Press 'RUN CHECK' to verify permissions, storage, device telemetry, and Shizuku status.",
                            style = MaterialTheme.typography.bodySmall,
                            color = ZxTextSecondary
                        )
                    }
                }
            }
        }

        // CAPABILITY MATRIX
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ZxDarkSurface),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(ZxBorder))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "CAPABILITY MATRIX (HONEST DISCLOSURE)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = ZxNeonGreen
                    )
                    Text(
                        text = "Real-time verification of supported vs restricted features on this Android device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ZxTextSecondary
                    )

                    capabilityMatrix.forEach { cap ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(ZxDarkCard)
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = cap.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ZxTextPrimary
                                )
                                Text(
                                    text = cap.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ZxTextSecondary
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = when (cap.state) {
                                    FeatureState.READY, FeatureState.AVAILABLE -> ZxNeonGreen.copy(alpha = 0.2f)
                                    FeatureState.REQUIRES_PERMISSION, FeatureState.REQUIRES_SHIZUKU -> ZxWarning.copy(alpha = 0.2f)
                                    else -> ZxNeonRed.copy(alpha = 0.2f)
                                },
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = cap.state.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = when (cap.state) {
                                        FeatureState.READY, FeatureState.AVAILABLE -> ZxNeonGreen
                                        FeatureState.REQUIRES_PERMISSION, FeatureState.REQUIRES_SHIZUKU -> ZxWarning
                                        else -> ZxNeonRed
                                    },
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // PROFILE BACKUP & RESTORE
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ZxDarkSurface),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(ZxBorder))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "PROFILE BACKUP & RESTORE (LOCAL JSON)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = ZxNeonCyan
                    )
                    Text(
                        text = "Export your calibrated game profiles, global sensitivity, and crosshair reticles safely.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ZxTextSecondary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val json = viewModel.backupManager.exportBackupJson(
                                        profiles = gameProfiles,
                                        globalConfig = null,
                                        presets = crosshairPresets
                                    )
                                    exportJsonText = json
                                    showExportDialog = true
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = ZxDarkCard)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Export Backup")
                        }

                        Button(
                            onClick = { showImportDialog = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = ZxDarkCard)
                        ) {
                            Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Import Backup")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.restorePreviousSystemSettings() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Restore Previous")
                        }
                        OutlinedButton(
                            onClick = { showResetConfirmDialog = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ZxNeonRed)
                        ) {
                            Text("Reset Data")
                        }
                    }
                }
            }
        }

        // ACTION HISTORY
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ZxDarkSurface),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(ZxBorder))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ACTION HISTORY",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = ZxTextPrimary
                        )
                        if (actionHistory.isNotEmpty()) {
                            TextButton(onClick = { viewModel.clearActionHistory() }) {
                                Text("CLEAR", style = MaterialTheme.typography.labelSmall, color = ZxNeonRed)
                            }
                        }
                    }

                    if (actionHistory.isEmpty()) {
                        Text(
                            text = "No recorded actions yet. All executed calibrations and tests will appear here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = ZxTextSecondary
                        )
                    } else {
                        val dateFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
                        actionHistory.take(10).forEach { item ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ZxDarkCard)
                                    .padding(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${item.actionName} • ${item.targetName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = ZxNeonCyan
                                    )
                                    Text(
                                        text = "[${item.status}]",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (item.status == "SUCCESS") ZxNeonGreen else ZxWarning
                                    )
                                }
                                Text(
                                    text = item.detail,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ZxTextSecondary
                                )
                                Text(
                                    text = "Mode: ${item.mode} | Time: ${dateFormat.format(Date(item.timestamp))}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ZxTextSecondary.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // LIFETIME UPDATE & OFFLINE ENGINE INFO
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ZxDarkSurface),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(ZxBorder))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "LIFETIME UPDATE & ARCHITECTURE",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = ZxNeonGreen
                    )
                    Text(
                        text = "Installed Engine: GAMESLABS v1.2.0 (Offline-First Build)",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = ZxTextPrimary
                    )
                    Text(
                        text = "Status: OFFLINE-READY • Up to date with latest calibration curves and security sandbox isolation.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ZxTextSecondary
                    )
                }
            }
        }
    }

    // EXPORT DIALOG
    if (showExportDialog) {
        Dialog(onDismissRequest = { showExportDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = ZxDarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Exported Backup JSON", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = ZxNeonCyan)
                    OutlinedTextField(
                        value = exportJsonText,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        textStyle = MaterialTheme.typography.bodySmall
                    )
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                            clipboard?.setPrimaryClip(android.content.ClipData.newPlainText("GAMESLABS Backup", exportJsonText))
                            Toast.makeText(context, "Copied backup JSON to clipboard!", Toast.LENGTH_SHORT).show()
                            showExportDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Copy to Clipboard")
                    }
                }
            }
        }
    }

    // IMPORT DIALOG
    if (showImportDialog) {
        Dialog(onDismissRequest = { showImportDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = ZxDarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Import Backup JSON", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = ZxNeonGreen)
                    OutlinedTextField(
                        value = importJsonInput,
                        onValueChange = { importJsonInput = it },
                        placeholder = { Text("Paste JSON string here...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        textStyle = MaterialTheme.typography.bodySmall
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showImportDialog = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val res = viewModel.backupManager.importBackupJson(importJsonInput)
                                    if (res.isSuccess) {
                                        Toast.makeText(context, "Imported ${res.getOrNull()} items successfully!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Invalid JSON format: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                    }
                                    showImportDialog = false
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = ZxNeonGreen)
                        ) {
                            Text("Import", color = ZxDarkBackground)
                        }
                    }
                }
            }
        }
    }

    // RESET CONFIRM DIALOG
    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text("Reset App Data?") },
            text = { Text("This will restore default sensitivity curves, balanced presets, and clear action logs.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetSensitivityToDefaults()
                        viewModel.clearActionHistory()
                        viewModel.resetVibrationConfig()
                        Toast.makeText(context, "Factory defaults restored.", Toast.LENGTH_SHORT).show()
                        showResetConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ZxNeonRed)
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showResetConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
