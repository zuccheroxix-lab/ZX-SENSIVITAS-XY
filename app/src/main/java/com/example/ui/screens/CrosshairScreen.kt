package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.database.CrosshairPresetEntity
import com.example.model.CrosshairConfig
import com.example.model.CrosshairStyle
import com.example.ui.components.StatusBadge
import com.example.ui.components.TacticalCard
import com.example.ui.components.ValueSlider
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel

@Composable
fun CrosshairScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val config by viewModel.crosshairConfig.collectAsState()
    val presets by viewModel.crosshairPresets.collectAsState()

    var hasOverlayPermission by remember {
        mutableStateOf(Settings.canDrawOverlays(context))
    }

    var showSaveDialog by remember { mutableStateOf(false) }
    var presetNameInput by remember { mutableStateOf("") }

    // Modern Android Photo Picker (zero-permission, Play Store policy compliant)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val updated = config.copy(
                style = CrosshairStyle.CUSTOM_IMAGE,
                customImageUri = uri.toString()
            )
            viewModel.updateCrosshairConfig(updated, context)
        }
    }

    // Refresh permission when screen is visible
    LaunchedEffect(Unit) {
        hasOverlayPermission = Settings.canDrawOverlays(context)
    }

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
                    text = "PRECISION CROSSHAIR",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Custom tactical reticle overlay for gaming",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            StatusBadge(
                text = if (config.isEnabled) "ACTIVE" else "OFF",
                color = if (config.isEnabled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline
            )
        }

        // Overlay Permission Check
        if (!hasOverlayPermission) {
            TacticalCard(
                borderColor = MaterialTheme.colorScheme.error,
                backgroundColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Overlay Permission Required",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "Agar crosshair dapat muncul di atas game, izinkan 'Draw over other apps' di Android Settings.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("open_overlay_settings_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Open Overlay Settings")
                }
            }
        }

        // Master Toggle & Reticle Preview
        TacticalCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "FLOATING OVERLAY SERVICE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (config.isEnabled) "Overlay is rendering on screen" else "Service is paused",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = config.isEnabled,
                    onCheckedChange = { enabled ->
                        hasOverlayPermission = Settings.canDrawOverlays(context)
                        viewModel.toggleCrosshair(enabled, context)
                    },
                    modifier = Modifier.testTag("crosshair_master_switch")
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Real-time Preview Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF07090C))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Tactical target concentric rings preview
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f

                    // Subtle background target grid
                    drawCircle(Color(0xFF151B24), radius = 60.dp.toPx(), style = Stroke(1f))
                    drawCircle(Color(0xFF151B24), radius = 35.dp.toPx(), style = Stroke(1f))
                    drawLine(Color(0xFF151B24), Offset(cx - 70.dp.toPx(), cy), Offset(cx + 70.dp.toPx(), cy), 1f)
                    drawLine(Color(0xFF151B24), Offset(cx, cy - 70.dp.toPx()), Offset(cx, cy + 70.dp.toPx()), 1f)

                    // Draw actual reticle
                    val reticleColor = Color((config.colorHex and 0xFFFFFFFFL).toInt()).copy(alpha = config.opacity)
                    val sizePx = config.sizeDp * density
                    val half = sizePx / 2f
                    val gap = config.gapDp * density
                    val thick = config.thicknessDp * density

                    when (config.style) {
                        CrosshairStyle.CLASSIC_CROSS -> {
                            drawLine(reticleColor, Offset(cx - half, cy), Offset(cx + half, cy), thick)
                            drawLine(reticleColor, Offset(cx, cy - half), Offset(cx, cy + half), thick)
                            drawCircle(reticleColor, radius = thick / 2f + 1f, center = Offset(cx, cy))
                        }
                        CrosshairStyle.DOT -> {
                            drawCircle(reticleColor, radius = (config.sizeDp * density) / 3f, center = Offset(cx, cy))
                        }
                        CrosshairStyle.GAP_CROSS -> {
                            drawLine(reticleColor, Offset(cx - half, cy), Offset(cx - gap, cy), thick)
                            drawLine(reticleColor, Offset(cx + gap, cy), Offset(cx + half, cy), thick)
                            drawLine(reticleColor, Offset(cx, cy - half), Offset(cx, cy - gap), thick)
                            drawLine(reticleColor, Offset(cx, cy + gap), Offset(cx, cy + half), thick)
                            drawCircle(reticleColor, radius = thick / 2f, center = Offset(cx, cy))
                        }
                        CrosshairStyle.T_SHAPE -> {
                            drawLine(reticleColor, Offset(cx - half, cy), Offset(cx - gap, cy), thick)
                            drawLine(reticleColor, Offset(cx + gap, cy), Offset(cx + half, cy), thick)
                            drawLine(reticleColor, Offset(cx, cy + gap), Offset(cx, cy + half), thick)
                            drawCircle(reticleColor, radius = thick / 2f, center = Offset(cx, cy))
                        }
                        CrosshairStyle.CIRCLE_DOT -> {
                            drawCircle(reticleColor, radius = half, center = Offset(cx, cy), style = Stroke(thick))
                            drawCircle(reticleColor, radius = thick * 1.2f, center = Offset(cx, cy))
                        }
                        CrosshairStyle.BOX_CROSS -> {
                            val r = half
                            val cl = r * 0.45f
                            drawLine(reticleColor, Offset(cx - r, cy - r), Offset(cx - r + cl, cy - r), thick)
                            drawLine(reticleColor, Offset(cx - r, cy - r), Offset(cx - r, cy - r + cl), thick)
                            drawLine(reticleColor, Offset(cx + r, cy - r), Offset(cx + r - cl, cy - r), thick)
                            drawLine(reticleColor, Offset(cx + r, cy - r), Offset(cx + r, cy - r + cl), thick)
                            drawLine(reticleColor, Offset(cx - r, cy + r), Offset(cx - r + cl, cy + r), thick)
                            drawLine(reticleColor, Offset(cx - r, cy + r), Offset(cx - r, cy + r - cl), thick)
                            drawLine(reticleColor, Offset(cx + r, cy + r), Offset(cx + r - cl, cy + r), thick)
                            drawLine(reticleColor, Offset(cx + r, cy + r), Offset(cx + r, cy + r - cl), thick)
                            drawCircle(reticleColor, radius = thick / 2f, center = Offset(cx, cy))
                        }
                        CrosshairStyle.CUSTOM_IMAGE -> {
                            drawCircle(reticleColor, radius = half, center = Offset(cx, cy), style = Stroke(2f))
                            drawLine(reticleColor, Offset(cx - half, cy), Offset(cx + half, cy), 2f)
                            drawLine(reticleColor, Offset(cx, cy - half), Offset(cx, cy + half), 2f)
                        }
                    }
                }
            }
        }

        // Style Selector
        TacticalCard {
            Text(
                text = "RETICLE STYLE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(CrosshairStyle.values()) { style ->
                    val isSelected = config.style == style
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            if (style == CrosshairStyle.CUSTOM_IMAGE) {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            } else {
                                val updated = config.copy(style = style)
                                viewModel.updateCrosshairConfig(updated, context)
                            }
                        },
                        label = { Text(style.displayName) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.testTag("style_${style.name.lowercase()}")
                    )
                }
            }
        }

        // Color Picker
        TacticalCard {
            Text(
                text = "RETICLE COLOR",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))

            val colors = listOf(
                0xFF00F0FF to "Cyan",
                0xFF00E676 to "Emerald",
                0xFFFF3366 to "Crimson",
                0xFFFFB800 to "Amber",
                0xFFFFFFFF to "White",
                0xFFBB86FC to "Violet"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                colors.forEach { (colorVal, _) ->
                    val isSelected = config.colorHex == colorVal
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(colorVal.toInt()))
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.onBackground else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable {
                                val updated = config.copy(colorHex = colorVal)
                                viewModel.updateCrosshairConfig(updated, context)
                            }
                            .testTag("color_${colorVal.toString(16)}")
                    )
                }
            }
        }

        // Customization Sliders
        TacticalCard {
            Text(
                text = "DIMENSIONS & OPACITY",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))

            ValueSlider(
                label = "Crosshair Size",
                value = config.sizeDp,
                onValueChange = {
                    val updated = config.copy(sizeDp = it)
                    viewModel.updateCrosshairConfig(updated, context)
                },
                valueRange = 10f..60f,
                displayUnit = " dp",
                testTag = "crosshair_size_slider"
            )

            Spacer(modifier = Modifier.height(6.dp))

            ValueSlider(
                label = "Line Thickness",
                value = config.thicknessDp,
                onValueChange = {
                    val updated = config.copy(thicknessDp = it)
                    viewModel.updateCrosshairConfig(updated, context)
                },
                valueRange = 1f..8f,
                displayUnit = " dp",
                testTag = "crosshair_thickness_slider"
            )

            Spacer(modifier = Modifier.height(6.dp))

            ValueSlider(
                label = "Center Reticle Gap",
                value = config.gapDp,
                onValueChange = {
                    val updated = config.copy(gapDp = it)
                    viewModel.updateCrosshairConfig(updated, context)
                },
                valueRange = 0f..20f,
                displayUnit = " dp",
                testTag = "crosshair_gap_slider"
            )

            Spacer(modifier = Modifier.height(6.dp))

            ValueSlider(
                label = "Opacity Alpha",
                value = config.opacity * 100f,
                onValueChange = {
                    val updated = config.copy(opacity = it / 100f)
                    viewModel.updateCrosshairConfig(updated, context)
                },
                valueRange = 20f..100f,
                displayUnit = "%",
                testTag = "crosshair_opacity_slider"
            )
        }

        // Position Offset & Calibration
        TacticalCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "POSITION CALIBRATION",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "X: ${config.offsetX}px | Y: ${config.offsetY}px",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (config.isLocked) "Locked" else "Draggable",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (config.isLocked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = {
                            val updated = config.copy(isLocked = !config.isLocked)
                            viewModel.updateCrosshairConfig(updated, context)
                        },
                        modifier = Modifier.testTag("toggle_lock_position_button")
                    ) {
                        Icon(
                            imageVector = if (config.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = "Lock Position",
                            tint = if (config.isLocked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.resetCrosshairPosition(context) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("reset_crosshair_position_button")
                ) {
                    Icon(imageVector = Icons.Default.CenterFocusStrong, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Center (0,0)")
                }

                Button(
                    onClick = { showSaveDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("save_crosshair_preset_button")
                ) {
                    Icon(imageVector = Icons.Default.Bookmark, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save Preset")
                }
            }
        }

        // Saved Presets List
        if (presets.isNotEmpty()) {
            TacticalCard {
                Text(
                    text = "SAVED PRESETS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    presets.forEach { preset ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = preset.presetName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${preset.style} | ${preset.sizeDp.toInt()}dp",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row {
                                IconButton(
                                    onClick = { viewModel.applyCrosshairPreset(preset, context) },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Load Preset", tint = MaterialTheme.colorScheme.primary)
                                }
                                if (!preset.isSystemPreset) {
                                    IconButton(
                                        onClick = { viewModel.deleteCrosshairPreset(preset) },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Preset", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Save Preset Dialog
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Crosshair Preset") },
            text = {
                OutlinedTextField(
                    value = presetNameInput,
                    onValueChange = { presetNameInput = it },
                    label = { Text("Preset Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (presetNameInput.isNotBlank()) {
                            viewModel.saveCrosshairPreset(presetNameInput.trim())
                            presetNameInput = ""
                            showSaveDialog = false
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
