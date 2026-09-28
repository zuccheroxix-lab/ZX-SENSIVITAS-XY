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
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sign

@SuppressLint("DefaultLocale")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensitivityScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val sensitivityConfig by viewModel.sensitivityConfig.collectAsState()
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
                    text = "Independent Horizontal X & Vertical Y drag response",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            StatusBadge(
                text = engineStatus.title,
                color = when (engineStatus.code) {
                    EngineStatusCode.APPLIED -> MaterialTheme.colorScheme.secondary
                    EngineStatusCode.READY -> MaterialTheme.colorScheme.primary
                    EngineStatusCode.UNSUPPORTED -> Color(0xFFFFB800)
                    EngineStatusCode.ERROR -> MaterialTheme.colorScheme.error
                }
            )
        }

        // --- GAME PROFILE SELECTOR TABS ---
        TacticalCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ACTIVE GAME PROFILE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = selectedProfile?.displayName ?: "Generic Profile",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold
                )
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
                                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
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
                text = "ENGINE PRESETS",
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
                    OutlinedButton(
                        onClick = { viewModel.applyPreset(preset) },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent,
                            contentColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = preset.title,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
            selectedPreset?.let { preset ->
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = preset.description,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // --- INDEPENDENT X & Y SENSITIVITY CALIBRATION ---
        TacticalCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "INDEPENDENT AXIS TUNING",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "X/Y RATIO: 1 : ${String.format("%.2f", sensitivityConfig.xyRatio)}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            Spacer(modifier = Modifier.height(14.dp))

            // X SENSITIVITY SLIDER
            EngineSliderRow(
                axisLabel = "HORIZONTAL X",
                subLabel = "Controls left/right camera panning & flick speed",
                value = sensitivityConfig.xSensitivity,
                formattedValue = "${String.format("%.2f", sensitivityConfig.xSensitivity)}x",
                onValueChange = { viewModel.updateXSensitivity(it) },
                valueRange = 0.20f..3.00f,
                accentColor = Color(0xFF00F0FF),
                testTag = "x_sensitivity_slider"
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Y SENSITIVITY SLIDER
            EngineSliderRow(
                axisLabel = "VERTICAL Y",
                subLabel = "Controls vertical swipe response & headshot drag elevation",
                value = sensitivityConfig.ySensitivity,
                formattedValue = "${String.format("%.2f", sensitivityConfig.ySensitivity)}x",
                onValueChange = { viewModel.updateYSensitivity(it) },
                valueRange = 0.20f..3.00f,
                accentColor = Color(0xFF00E676),
                testTag = "y_sensitivity_slider"
            )
        }

        // --- DYNAMICS & RESPONSE ENGINE PARAMETERS ---
        TacticalCard {
            Text(
                text = "DRAG RESPONSE & SMOOTHING ENGINE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(14.dp))

            // DRAG RESPONSE
            EngineSliderRow(
                axisLabel = "DRAG RESPONSE",
                subLabel = "Global response gain for swipe gestures",
                value = sensitivityConfig.dragResponse,
                formattedValue = "${String.format("%.2f", sensitivityConfig.dragResponse)}x",
                onValueChange = { viewModel.updateDragResponse(it) },
                valueRange = 0.50f..2.00f,
                accentColor = MaterialTheme.colorScheme.primary,
                testTag = "drag_response_slider"
            )

            Spacer(modifier = Modifier.height(16.dp))

            // SMOOTHNESS
            EngineSliderRow(
                axisLabel = "SMOOTHNESS",
                subLabel = "Low-pass filter to dampen jitter without adding input lag",
                value = sensitivityConfig.smoothness,
                formattedValue = "${String.format("%.2f", sensitivityConfig.smoothness)}x",
                onValueChange = { viewModel.updateSmoothness(it) },
                valueRange = 0.00f..1.00f,
                accentColor = Color(0xFFB388FF),
                testTag = "smoothness_slider"
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ACCELERATION
            EngineSliderRow(
                axisLabel = "ACCELERATION",
                subLabel = "Dynamic response boost during fast reflex flicks",
                value = sensitivityConfig.acceleration,
                formattedValue = "${String.format("%.2f", sensitivityConfig.acceleration)}x",
                onValueChange = { viewModel.updateAcceleration(it) },
                valueRange = 0.00f..2.00f,
                accentColor = Color(0xFFFF9100),
                testTag = "acceleration_slider"
            )

            Spacer(modifier = Modifier.height(16.dp))

            // DEADZONE
            EngineSliderRow(
                axisLabel = "DEADZONE",
                subLabel = "Suppresses tiny unintentional finger micro-tremors",
                value = sensitivityConfig.deadzonePx,
                formattedValue = "${String.format("%.1f", sensitivityConfig.deadzonePx)} px",
                onValueChange = { viewModel.updateDeadzone(it) },
                valueRange = 0.0f..12.0f,
                accentColor = Color(0xFFFF5252),
                testTag = "deadzone_slider"
            )

            Spacer(modifier = Modifier.height(16.dp))

            // RESPONSE CURVE SELECTOR
            Text(
                text = "RESPONSE CURVE",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Transfer function transforming raw touch delta into engine coordinate delta",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            ExposedDropdownMenuBox(
                expanded = isCurveDropdownExpanded,
                onExpandedChange = { isCurveDropdownExpanded = !isCurveDropdownExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = sensitivityConfig.responseCurve.displayName,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCurveDropdownExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    shape = RoundedCornerShape(10.dp)
                )

                ExposedDropdownMenu(
                    expanded = isCurveDropdownExpanded,
                    onDismissRequest = { isCurveDropdownExpanded = false }
                ) {
                    ResponseCurveType.values().forEach { curve ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(curve.displayName, fontWeight = FontWeight.SemiBold)
                                    Text(curve.shortDesc, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

            Spacer(modifier = Modifier.height(14.dp))

            // RESPONSE CURVE VISUALIZER GRAPH
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

                    // 1. Grid background
                    val step = 28.dp.toPx()
                    var gx = 0f
                    while (gx < w) {
                        drawLine(Color(0xFF131A24), Offset(gx, 0f), Offset(gx, h), strokeWidth = 1f)
                        gx += step
                    }
                    var gy = 0f
                    while (gy < h) {
                        drawLine(Color(0xFF131A24), Offset(0f, gy), Offset(w, gy), strokeWidth = 1f)
                        gy += step
                    }

                    // 2. Guide lines based on selected test mode
                    val guideEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                    when (dragTestMode) {
                        DragTestMode.HORIZONTAL -> {
                            drawLine(
                                color = Color(0xFF00F0FF).copy(alpha = 0.4f),
                                start = Offset(20f, h / 2f),
                                end = Offset(w - 20f, h / 2f),
                                strokeWidth = 2f,
                                pathEffect = guideEffect
                            )
                        }
                        DragTestMode.VERTICAL -> {
                            drawLine(
                                color = Color(0xFF00E676).copy(alpha = 0.4f),
                                start = Offset(w / 2f, 20f),
                                end = Offset(w / 2f, h - 20f),
                                strokeWidth = 2f,
                                pathEffect = guideEffect
                            )
                        }
                        DragTestMode.DIAGONAL -> {
                            drawLine(
                                color = Color(0xFFFFB800).copy(alpha = 0.4f),
                                start = Offset(20f, h - 20f),
                                end = Offset(w - 20f, 20f),
                                strokeWidth = 2f,
                                pathEffect = guideEffect
                            )
                        }
                        DragTestMode.FREE_DRAG -> {}
                    }

                    // 3. Draw RAW input path (Orange trail)
                    if (rawTrail.size > 1) {
                        for (i in 1 until rawTrail.size) {
                            drawLine(
                                color = Color(0xFFFF9100).copy(alpha = 0.5f),
                                start = rawTrail[i - 1],
                                end = rawTrail[i],
                                strokeWidth = 3f
                            )
                        }
                    }

                    // 4. Draw PROCESSED engine path (Cyan trail)
                    if (engineTrail.size > 1) {
                        for (i in 1 until engineTrail.size) {
                            drawLine(
                                color = Color(0xFF00F0FF),
                                start = engineTrail[i - 1],
                                end = engineTrail[i],
                                strokeWidth = 4f
                            )
                        }
                        // Target indicator on latest processed point
                        drawCircle(
                            color = Color(0xFF00E676),
                            radius = 6f,
                            center = engineTrail.last()
                        )
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

        // --- PRIMARY ACTION BUTTONS ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { viewModel.applySensitivityProfile() },
                modifier = Modifier
                    .weight(1.3f)
                    .height(48.dp)
                    .testTag("apply_profile_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Apply Profile", fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = { viewModel.clearCalibrationTrail() },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("test_drag_button")
            ) {
                Icon(imageVector = Icons.Default.Tune, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Test Drag")
            }

            IconButton(
                onClick = { viewModel.resetSensitivityToDefaults() },
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                    .testTag("reset_sensitivity_button")
            ) {
                Icon(imageVector = Icons.Default.Restore, contentDescription = "Reset", tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
                val inputRatio = i.toFloat() / steps // 0 to 1
                val rawPx = inputRatio * 60f

                // Deadzone
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
                text = "${curveType.displayName} | Accel: +${"%.2f".format(acceleration)}x",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF00F0FF),
                fontSize = 10.sp
            )
        }
    }
}
