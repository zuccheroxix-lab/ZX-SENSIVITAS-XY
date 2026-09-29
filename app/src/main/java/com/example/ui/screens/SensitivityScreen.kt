package com.example.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.database.GameProfileEntity
import com.example.model.*
import com.example.ui.components.StatusBadge
import com.example.ui.components.TacticalCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import kotlin.math.pow

@SuppressLint("DefaultLocale")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensitivityScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val sensitivityConfig by viewModel.sensitivityConfig.collectAsState()
    val globalSensitivityConfig by viewModel.globalSensitivityConfig.collectAsState()
    val executionMode by viewModel.executionMode.collectAsState()
    val sensitivityMode by viewModel.sensitivityMode.collectAsState()
    val shizukuInfo by viewModel.shizukuInfo.collectAsState()
    val touchMetrics by viewModel.touchLiveMetrics.collectAsState()
    val applyResult by viewModel.applyResult.collectAsState()
    val engineStatus by viewModel.engineStatus.collectAsState()
    val selectedPreset by viewModel.selectedPreset.collectAsState()
    val dragTestMode by viewModel.dragTestMode.collectAsState()
    val calibrationStats by viewModel.calibrationStats.collectAsState()
    val rawTrail by viewModel.rawTrail.collectAsState()
    val engineTrail by viewModel.engineTrail.collectAsState()
    val gameProfiles by viewModel.gameProfiles.collectAsState()
    val selectedProfile by viewModel.selectedProfile.collectAsState()
    val sensScore by viewModel.sensCalibrationScore.collectAsState()

    val scrollState = rememberScrollState()
    var isCurveDropdownExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- SCREEN HEADER ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "SENSITIVITY X/Y ENGINE",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Independent Horizontal X & Vertical Y drag calibration",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            StatusBadge(
                text = "${sensScore}% SCORE",
                color = ZxNeonCyan
            )
        }

        // --- MODE SELECTORS (GLOBAL / PER-GAME) & (SIMULATION / APPLY) ---
        TacticalCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // SENSITIVITY MODE
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SENSITIVITY ARCHITECTURE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = ZxNeonGreen
                    )
                    Text(
                        text = if (sensitivityMode == SensitivityMode.GLOBAL) "Global Master Active" else "Custom Per-Game Active",
                        style = MaterialTheme.typography.labelSmall,
                        color = ZxTextSecondary
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.setSensitivityMode(SensitivityMode.GLOBAL) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (sensitivityMode == SensitivityMode.GLOBAL) ZxNeonCyan else ZxDarkCard
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "GLOBAL",
                            fontWeight = FontWeight.Bold,
                            color = if (sensitivityMode == SensitivityMode.GLOBAL) ZxDarkBackground else ZxTextSecondary
                        )
                    }

                    Button(
                        onClick = { viewModel.setSensitivityMode(SensitivityMode.PER_GAME) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (sensitivityMode == SensitivityMode.PER_GAME) ZxNeonCyan else ZxDarkCard
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "PER-GAME",
                            fontWeight = FontWeight.Bold,
                            color = if (sensitivityMode == SensitivityMode.PER_GAME) ZxDarkBackground else ZxTextSecondary
                        )
                    }
                }

                // OPERATION MODE BADGE
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Current Mode: ${executionMode.displayName} (${executionMode.badgeText})",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (executionMode == ExecutionMode.APPLY) ZxNeonCyan else ZxNeonGreen
                    )
                    Text(
                        text = "Tap to switch in Settings",
                        style = MaterialTheme.typography.labelSmall,
                        color = ZxTextSecondary
                    )
                }
            }
        }

        // --- PER-GAME SELECTOR (Only shown in PER-GAME mode) ---
        if (sensitivityMode == SensitivityMode.PER_GAME) {
            TacticalCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TARGET GAME PROFILE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    selectedProfile?.let { prof ->
                        val isInst = viewModel.isGameInstalled(prof.packageName)
                        Surface(
                            color = if (isInst) ZxNeonGreen.copy(alpha = 0.2f) else ZxWarning.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = if (isInst) "INSTALLED" else "NOT INSTALLED",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isInst) ZxNeonGreen else ZxWarning,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(gameProfiles) { profile ->
                        val isSelected = selectedProfile?.id == profile.id
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                )
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { viewModel.selectGameProfile(profile) }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = profile.displayName,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                selectedProfile?.let { prof ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Package: ${prof.packageName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = ZxTextSecondary
                    )
                }
            }
        } else {
            // GLOBAL MODE BANNER
            TacticalCard(borderColor = ZxNeonCyan.copy(alpha = 0.5f)) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "GLOBAL PROFILE ACTIVE",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = ZxNeonCyan
                    )
                    Text(
                        text = "This single configuration acts as the default master for all drag testing and unconfigured games.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ZxTextSecondary
                    )
                }
            }
        }

        // --- HONEST LIVE STATUS BANNER ---
        TacticalCard(
            borderColor = when (engineStatus.code) {
                EngineStatusCode.APPLIED -> MaterialTheme.colorScheme.secondary
                EngineStatusCode.READY -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                EngineStatusCode.UNSUPPORTED -> Color(0xFFFFB800).copy(alpha = 0.7f)
                EngineStatusCode.ERROR -> MaterialTheme.colorScheme.error
            },
            backgroundColor = when (engineStatus.code) {
                EngineStatusCode.APPLIED -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
                EngineStatusCode.READY -> MaterialTheme.colorScheme.surface
                EngineStatusCode.UNSUPPORTED -> Color(0xFFFFB800).copy(alpha = 0.1f)
                EngineStatusCode.ERROR -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
            }
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = when (engineStatus.code) {
                        EngineStatusCode.APPLIED -> Icons.Default.CheckCircle
                        EngineStatusCode.READY -> Icons.Default.Tune
                        EngineStatusCode.UNSUPPORTED -> Icons.Default.Info
                        EngineStatusCode.ERROR -> Icons.Default.ErrorOutline
                    },
                    contentDescription = null,
                    tint = when (engineStatus.code) {
                        EngineStatusCode.APPLIED -> MaterialTheme.colorScheme.secondary
                        EngineStatusCode.READY -> MaterialTheme.colorScheme.primary
                        EngineStatusCode.UNSUPPORTED -> Color(0xFFFFB800)
                        EngineStatusCode.ERROR -> MaterialTheme.colorScheme.error
                    },
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "STATUS: ${engineStatus.title}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = when (engineStatus.code) {
                            EngineStatusCode.APPLIED -> MaterialTheme.colorScheme.secondary
                            EngineStatusCode.READY -> MaterialTheme.colorScheme.onSurface
                            EngineStatusCode.UNSUPPORTED -> Color(0xFFFFB800)
                            EngineStatusCode.ERROR -> MaterialTheme.colorScheme.error
                        }
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = engineStatus.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    engineStatus.technicalDetails?.let { details ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = details,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // --- PRESETS BAR ---
        TacticalCard {
            Text(
                text = "EXCLUSIVE PRESET",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(SensitivityPreset.values()) { preset ->
                    val isSelected = selectedPreset == preset
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable { viewModel.applyPreset(preset) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("preset_${preset.name.lowercase()}")
                    ) {
                        Text(
                            text = preset.title,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // --- CORE X/Y PARAMETERS PANEL ---
        TacticalCard {
            Text(
                text = "CORE RESPONSE MULTIPLIERS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            // X Sensitivity Slider
            EngineSliderRow(
                axisLabel = "Horizontal X Sensitivity",
                subLabel = "Lateral camera panning & tracking response",
                value = sensitivityConfig.xSensitivity,
                formattedValue = "${String.format("%.2f", sensitivityConfig.xSensitivity)}x",
                onValueChange = { viewModel.updateXSensitivity(it) },
                valueRange = 0.20f..3.00f,
                accentColor = MaterialTheme.colorScheme.primary,
                testTag = "x_sensitivity_slider"
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Y Sensitivity Slider
            EngineSliderRow(
                axisLabel = "Vertical Y Sensitivity",
                subLabel = "Headshot elevation & swipe drag response",
                value = sensitivityConfig.ySensitivity,
                formattedValue = "${String.format("%.2f", sensitivityConfig.ySensitivity)}x",
                onValueChange = { viewModel.updateYSensitivity(it) },
                valueRange = 0.20f..3.00f,
                accentColor = MaterialTheme.colorScheme.secondary,
                testTag = "y_sensitivity_slider"
            )

            Spacer(modifier = Modifier.height(10.dp))

            // X/Y Ratio Visualizer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "X to Y Elevation Ratio:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "1 : ${String.format("%.2f", sensitivityConfig.xyRatio)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (sensitivityConfig.xyRatio in 1.10f..1.35f) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                )
            }
        }

        // --- SECONDARY ADVANCED PARAMETERS PANEL ---
        TacticalCard {
            Text(
                text = "SWIPE DYNAMICS & DEADZONE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Drag Response
            EngineSliderRow(
                axisLabel = "Global Drag Response",
                subLabel = "Overall velocity gain multiplier for swipe gestures",
                value = sensitivityConfig.dragResponse,
                formattedValue = "${String.format("%.2f", sensitivityConfig.dragResponse)}x",
                onValueChange = { viewModel.updateDragResponse(it) },
                valueRange = 0.50f..2.00f,
                accentColor = MaterialTheme.colorScheme.primary,
                testTag = "drag_response_slider"
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Smoothness
            EngineSliderRow(
                axisLabel = "Smoothness (EMA Filter)",
                subLabel = "Suppresses micro-jitter and finger tremor",
                value = sensitivityConfig.smoothness,
                formattedValue = "${(sensitivityConfig.smoothness * 100).toInt()}%",
                onValueChange = { viewModel.updateSmoothness(it) },
                valueRange = 0.00f..1.00f,
                accentColor = MaterialTheme.colorScheme.secondary,
                testTag = "smoothness_slider"
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Acceleration
            EngineSliderRow(
                axisLabel = "Dynamic Acceleration",
                subLabel = "Exponential boost on fast flick swipes",
                value = sensitivityConfig.acceleration,
                formattedValue = "+${String.format("%.2f", sensitivityConfig.acceleration)}x",
                onValueChange = { viewModel.updateAcceleration(it) },
                valueRange = 0.00f..2.00f,
                accentColor = MaterialTheme.colorScheme.tertiary,
                testTag = "acceleration_slider"
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Deadzone
            EngineSliderRow(
                axisLabel = "Deadzone Threshold",
                subLabel = "Suppresses unintended initial tap drift",
                value = sensitivityConfig.deadzonePx,
                formattedValue = "${String.format("%.1f", sensitivityConfig.deadzonePx)} px",
                onValueChange = { viewModel.updateDeadzone(it) },
                valueRange = 0.0f..12.0f,
                accentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                testTag = "deadzone_slider"
            )
        }

        // --- RESPONSE CURVE SELECTION & LIVE VISUALIZER ---
        TacticalCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "RESPONSE TRANSFER CURVE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = sensitivityConfig.responseCurve.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Box {
                    Button(
                        onClick = { isCurveDropdownExpanded = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("select_curve_button")
                    ) {
                        Text("Change Curve", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelSmall)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                    }

                    DropdownMenu(
                        expanded = isCurveDropdownExpanded,
                        onDismissRequest = { isCurveDropdownExpanded = false }
                    ) {
                        ResponseCurveType.values().forEach { curve ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(curve.displayName, fontWeight = FontWeight.Bold)
                                        Text(curve.shortDesc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                },
                                onClick = {
                                    viewModel.updateResponseCurve(curve)
                                    isCurveDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Real Canvas Response Curve Graph
            ResponseCurveVisualizer(
                curveType = sensitivityConfig.responseCurve,
                acceleration = sensitivityConfig.acceleration,
                deadzone = sensitivityConfig.deadzonePx
            )
        }

        // --- ANDROID SYSTEM POINTER SPEED ---
        TacticalCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ANDROID SYSTEM POINTER SPEED",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Native Android input parameter (Settings.System.POINTER_SPEED)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = if (sensitivityConfig.systemPointerSpeed > 0) "+${sensitivityConfig.systemPointerSpeed}" else "${sensitivityConfig.systemPointerSpeed}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Slider(
                value = sensitivityConfig.systemPointerSpeed.toFloat(),
                onValueChange = { viewModel.updatePointerSpeed(it.toInt()) },
                valueRange = -7f..7f,
                steps = 13,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("system_pointer_speed_slider"),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Rentang: -7 (Sangat Lambat) hingga +7 (Sangat Cepat). Standar sistem Android = 0.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // --- DRAG CALIBRATION TEST PAD (REAL DATA) ---
        TacticalCard(borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "DRAG CALIBRATION PAD",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Test Horizontal, Vertical & Diagonal gestures in real-time",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { viewModel.clearCalibrationTrail() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Clear Pad", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Mode Selector: Horizontal, Vertical, Diagonal, Free Drag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                DragTestMode.values().forEach { mode ->
                    val isModeSelected = dragTestMode == mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isModeSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                            .clickable { viewModel.setDragTestMode(mode) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (mode) {
                                DragTestMode.HORIZONTAL -> "HORIZ"
                                DragTestMode.VERTICAL -> "VERT"
                                DragTestMode.DIAGONAL -> "DIAG"
                                DragTestMode.FREE_DRAG -> "FREE"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isModeSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = dragTestMode.instruction,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Interactive Dual-Trajectory Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF070B11))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                    .pointerInput(dragTestMode, sensitivityConfig) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                viewModel.onCalibrationDragStart(offset, System.currentTimeMillis())
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                viewModel.onCalibrationDragMove(change.position, System.currentTimeMillis())
                            },
                            onDragEnd = {
                                viewModel.onCalibrationDragEnd()
                            },
                            onDragCancel = {
                                viewModel.onCalibrationDragEnd()
                            }
                        )
                    }
                    .testTag("drag_calibration_canvas"),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Grid Background
                    val gridStep = 40f
                    for (x in 0..(w / gridStep).toInt()) {
                        drawLine(Color(0xFF131B26), Offset(x * gridStep, 0f), Offset(x * gridStep, h), strokeWidth = 1f)
                    }
                    for (y in 0..(h / gridStep).toInt()) {
                        drawLine(Color(0xFF131B26), Offset(0f, y * gridStep), Offset(w, y * gridStep), strokeWidth = 1f)
                    }

                    // Target Guideline according to test mode
                    when (dragTestMode) {
                        DragTestMode.HORIZONTAL -> {
                            drawLine(
                                color = Color(0xFF263242),
                                start = Offset(0f, h / 2),
                                end = Offset(w, h / 2),
                                strokeWidth = 2f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                            )
                        }
                        DragTestMode.VERTICAL -> {
                            drawLine(
                                color = Color(0xFF263242),
                                start = Offset(w / 2, 0f),
                                end = Offset(w / 2, h),
                                strokeWidth = 2f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                            )
                        }
                        DragTestMode.DIAGONAL -> {
                            drawLine(
                                color = Color(0xFF263242),
                                start = Offset(0f, h),
                                end = Offset(w, 0f),
                                strokeWidth = 2f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                            )
                        }
                        DragTestMode.FREE_DRAG -> {
                            drawCircle(Color(0xFF1D2633), radius = 40f, center = Offset(w / 2, h / 2), style = Stroke(width = 1.5f))
                        }
                    }

                    // Render RAW Trail (Orange)
                    if (rawTrail.size > 1) {
                        val rawPath = Path().apply {
                            moveTo(rawTrail.first().x, rawTrail.first().y)
                            for (i in 1 until rawTrail.size) {
                                lineTo(rawTrail[i].x, rawTrail[i].y)
                            }
                        }
                        drawPath(rawPath, color = Color(0xFFFF9100).copy(alpha = 0.7f), style = Stroke(width = 2.5f))
                    }

                    // Render PROCESSED Engine Trail (Cyan)
                    if (engineTrail.size > 1) {
                        val enginePath = Path().apply {
                            moveTo(engineTrail.first().x, engineTrail.first().y)
                            for (i in 1 until engineTrail.size) {
                                lineTo(engineTrail[i].x, engineTrail[i].y)
                            }
                        }
                        drawPath(enginePath, color = Color(0xFF00F0FF), style = Stroke(width = 3.5f))
                    }
                }

                if (rawTrail.isEmpty()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.TouchApp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "SWIPE OR DRAG HERE TO CALIBRATE",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "Orange: Raw Input  |  Cyan: Processed X/Y Engine",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // REAL-TIME TELEMETRY METRICS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Sampling Rate", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = if (touchMetrics.samplingRateHz > 0) "${touchMetrics.samplingRateHz.toInt()} Hz" else "IDLE",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Column {
                    Text("Swipe Speed", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "${String.format("%.2f", calibrationStats.averageVelocityPxPerMs)} px/ms",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Column {
                    Text("Measured X/Y", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "1 : ${String.format("%.2f", calibrationStats.measuredXtoYRatio)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column {
                    Text("Linearity Error", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "${String.format("%.1f", calibrationStats.linearityRmsErrorPx)} px",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (calibrationStats.linearityRmsErrorPx < 3f) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = calibrationStats.statusSummary,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }

        // --- PRIMARY ACTION BUTTONS (GLOBAL vs PER-GAME) ---
        if (sensitivityMode == SensitivityMode.GLOBAL) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { viewModel.saveGlobalProfile() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ZxNeonCyan)
                ) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = null, tint = ZxDarkBackground)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("SAVE GLOBAL PROFILE", fontWeight = FontWeight.Bold, color = ZxDarkBackground)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.applySensitivityProfile() },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ZxNeonGreen)
                    ) {
                        Text(if (executionMode == ExecutionMode.SIMULATION) "SIMULATE" else "APPLY TO SYSTEM", fontWeight = FontWeight.Bold, color = ZxDarkBackground)
                    }

                    OutlinedButton(
                        onClick = { viewModel.resetSensitivityToDefaults() },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Text("RESET")
                    }
                }
            }
        } else {
            // PER-GAME MODE ACTIONS
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            selectedProfile?.let {
                                val updated = it.copy(
                                    xSensitivity = sensitivityConfig.xSensitivity,
                                    ySensitivity = sensitivityConfig.ySensitivity,
                                    dragResponse = sensitivityConfig.dragResponse,
                                    dragSmoothness = sensitivityConfig.smoothness,
                                    acceleration = sensitivityConfig.acceleration,
                                    deadzonePx = sensitivityConfig.deadzonePx,
                                    responseCurve = sensitivityConfig.responseCurve.name,
                                    pointerSpeed = sensitivityConfig.systemPointerSpeed
                                )
                                viewModel.saveGameProfile(updated)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ZxNeonCyan)
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = null, tint = ZxDarkBackground)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("SAVE PROFILE", fontWeight = FontWeight.Bold, color = ZxDarkBackground)
                    }

                    Button(
                        onClick = { viewModel.useGlobalForSelectedGame() },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ZxDarkCard)
                    ) {
                        Text("USE GLOBAL", fontWeight = FontWeight.Bold)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.applySensitivityProfile() },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ZxNeonGreen)
                    ) {
                        Text(if (executionMode == ExecutionMode.SIMULATION) "SIMULATE" else "APPLY", fontWeight = FontWeight.Bold, color = ZxDarkBackground)
                    }

                    OutlinedButton(
                        onClick = { viewModel.resetSelectedGameProfile() },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Text("RESET")
                    }
                }
            }
        }
    }
}

@Composable
private fun EngineSliderRow(
    axisLabel: String,
    subLabel: String,
    value: Float,
    formattedValue: String,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    accentColor: Color,
    testTag: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = axisLabel,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = formattedValue,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag),
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

@Composable
private fun ResponseCurveVisualizer(
    curveType: ResponseCurveType,
    acceleration: Float,
    deadzone: Float
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF090D14))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Axes
            drawLine(Color(0xFF1E2836), Offset(0f, h), Offset(w, h), strokeWidth = 1f)
            drawLine(Color(0xFF1E2836), Offset(0f, 0f), Offset(0f, h), strokeWidth = 1f)

            // Linear 1:1 baseline (dimmed dashed)
            drawLine(
                color = Color(0xFF263242),
                start = Offset(0f, h),
                end = Offset(w, 0f),
                strokeWidth = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
            )

            // Generate Curve Points
            val path = Path()
            val steps = 60
            for (i in 0..steps) {
                val inputRatio = i.toFloat() / steps
                val rawPx = inputRatio * 60f

                val eff = if (rawPx <= deadzone) 0f else (rawPx - deadzone)
                val normSpeed = (inputRatio * 2f).coerceIn(0f, 3f)
                val accel = 1f + (acceleration * normSpeed * 0.5f)

                val outRatio = when (curveType) {
                    ResponseCurveType.LINEAR -> (eff / 60f) * accel
                    ResponseCurveType.PRECISE -> ((eff / 60f).pow(1.30f)) * accel
                    ResponseCurveType.SMOOTH_EXP -> ((eff / 60f).pow(1.22f)) * accel
                    ResponseCurveType.DYNAMIC_S -> {
                        val x = (eff / 60f).coerceIn(0f, 1f)
                        val s = (3f * x.pow(2) - 2f * x.pow(3)) * 0.5f + (x * 0.5f)
                        s * accel
                    }
                    ResponseCurveType.AGGRESSIVE -> ((eff / 60f).pow(0.85f)) * accel
                }.coerceIn(0f, 1.3f)

                val px = inputRatio * w
                val py = (1f - (outRatio / 1.3f).coerceIn(0f, 1f)) * h

                if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }

            drawPath(
                path = path,
                color = Color(0xFF00F0FF),
                style = Stroke(width = 2.5f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().align(Alignment.TopEnd),
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = "${curveType.displayName} | Accel: +${String.format("%.2f", acceleration)}x",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF00F0FF),
                fontSize = 10.sp
            )
        }
    }
}
