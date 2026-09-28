package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ZxOptimizerApplication
import com.example.database.CrosshairPresetEntity
import com.example.database.GameProfileEntity
import com.example.hardware.*
import com.example.model.*
import com.example.service.CrosshairOverlayService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "MainViewModel"
    }

    private val app = application as ZxOptimizerApplication
    private val repository = app.repository
    private val shizukuManager = app.shizukuManager
    private val hardwareMonitor = app.hardwareMonitor

    // Lazy initialization of touch and XY response engines (never created at cold startup)
    private var _touchMetricsEngine: TouchMetricsEngine? = null
    val touchMetricsEngine: TouchMetricsEngine
        get() {
            if (_touchMetricsEngine == null) {
                _touchMetricsEngine = TouchMetricsEngine()
            }
            return _touchMetricsEngine!!
        }

    private var _xyResponseEngine: XYResponseEngine? = null
    val xyResponseEngine: XYResponseEngine
        get() {
            if (_xyResponseEngine == null) {
                _xyResponseEngine = XYResponseEngine()
            }
            return _xyResponseEngine!!
        }

    // Hardware Telemetry
    val batteryData: StateFlow<BatteryData> = hardwareMonitor.batteryData
    val thermalData: StateFlow<ThermalData> = hardwareMonitor.thermalData
    val memoryData: StateFlow<MemoryData> = hardwareMonitor.memoryData
    val storageData: StateFlow<StorageData> = hardwareMonitor.storageData
    val displayData: StateFlow<DisplayData> = hardwareMonitor.displayData
    val cpuData: StateFlow<CpuData> = hardwareMonitor.cpuData

    // Shizuku State
    val shizukuInfo: StateFlow<ShizukuInfo> = shizukuManager.shizukuInfo

    // Sensitivity State
    private val _sensitivityConfig = MutableStateFlow(SensitivityConfig())
    val sensitivityConfig: StateFlow<SensitivityConfig> = _sensitivityConfig.asStateFlow()

    private val _selectedPreset = MutableStateFlow<SensitivityPreset?>(SensitivityPreset.BALANCED)
    val selectedPreset: StateFlow<SensitivityPreset?> = _selectedPreset.asStateFlow()

    // Engine Status
    private val _engineStatus = MutableStateFlow(
        EngineStatusInfo(
            code = EngineStatusCode.READY,
            title = "READY",
            message = "Local X/Y Response Engine ready for calibration."
        )
    )
    val engineStatus: StateFlow<EngineStatusInfo> = _engineStatus.asStateFlow()

    private val _applyResult = MutableStateFlow<ApplyResult>(ApplyResult.Idle)
    val applyResult: StateFlow<ApplyResult> = _applyResult.asStateFlow()

    // Touch Calibration & Test Pad State
    private val _dragTestMode = MutableStateFlow(DragTestMode.FREE_DRAG)
    val dragTestMode: StateFlow<DragTestMode> = _dragTestMode.asStateFlow()

    private val _calibrationStats = MutableStateFlow(DragCalibrationResult())
    val calibrationStats: StateFlow<DragCalibrationResult> = _calibrationStats.asStateFlow()

    private val _touchLiveMetrics = MutableStateFlow(TouchLiveMetrics())
    val touchLiveMetrics: StateFlow<TouchLiveMetrics> = _touchLiveMetrics.asStateFlow()

    private val _engineTrail = MutableStateFlow<List<Offset>>(emptyList())
    val engineTrail: StateFlow<List<Offset>> = _engineTrail.asStateFlow()

    private val _rawTrail = MutableStateFlow<List<Offset>>(emptyList())
    val rawTrail: StateFlow<List<Offset>> = _rawTrail.asStateFlow()

    // Optimizer State
    private val _optimizerState = MutableStateFlow(OptimizerState())
    val optimizerState: StateFlow<OptimizerState> = _optimizerState.asStateFlow()

    // Crosshair State
    val crosshairConfig: StateFlow<CrosshairConfig> = CrosshairOverlayService.currentConfig

    // Game Profiles & Presets
    val gameProfiles: StateFlow<List<GameProfileEntity>> = repository.allGameProfiles
        .catch { e ->
            Log.w(TAG, "Error collecting game profiles: ${e.message}")
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val crosshairPresets: StateFlow<List<CrosshairPresetEntity>> = repository.allCrosshairPresets
        .catch { e ->
            Log.w(TAG, "Error collecting crosshair presets: ${e.message}")
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedProfile = MutableStateFlow<GameProfileEntity?>(null)
    val selectedProfile: StateFlow<GameProfileEntity?> = _selectedProfile.asStateFlow()

    init {
        // NON-BLOCKING ASYNC STARTUP: All reading is performed safely on background thread
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentSpeed = shizukuManager.getSystemPointerSpeedDirect()
                _sensitivityConfig.value = _sensitivityConfig.value.copy(systemPointerSpeed = currentSpeed)
            } catch (e: Throwable) {
                Log.w(TAG, "Could not read pointer speed at startup: ${e.message}")
            }

            try {
                gameProfiles.collect { profiles ->
                    if (_selectedProfile.value == null && profiles.isNotEmpty()) {
                        val active = profiles.find { it.isDefaultSelected } ?: profiles.first()
                        _selectedProfile.value = active
                        loadProfileToSensitivity(active)
                    }
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Could not load default profile at startup: ${e.message}")
            }
        }
    }

    // Refresh telemetry
    fun refreshHardware() {
        try {
            hardwareMonitor.refreshAll()
            shizukuManager.checkStatus()
        } catch (e: Throwable) {
            Log.w(TAG, "refreshHardware error: ${e.message}")
        }
    }

    // --- SENSITIVITY ENGINE PARAMETER ACTIONS ---

    fun updateXSensitivity(x: Float) {
        _selectedPreset.value = null
        _sensitivityConfig.value = _sensitivityConfig.value.copy(xSensitivity = x)
        updateStatusToReady()
    }

    fun updateYSensitivity(y: Float) {
        _selectedPreset.value = null
        _sensitivityConfig.value = _sensitivityConfig.value.copy(ySensitivity = y)
        updateStatusToReady()
    }

    fun updateDragResponse(response: Float) {
        _selectedPreset.value = null
        _sensitivityConfig.value = _sensitivityConfig.value.copy(dragResponse = response)
        updateStatusToReady()
    }

    fun updateSmoothness(value: Float) {
        _selectedPreset.value = null
        _sensitivityConfig.value = _sensitivityConfig.value.copy(smoothness = value)
        updateStatusToReady()
    }

    fun updateAcceleration(value: Float) {
        _selectedPreset.value = null
        _sensitivityConfig.value = _sensitivityConfig.value.copy(acceleration = value)
        updateStatusToReady()
    }

    fun updateDeadzone(px: Float) {
        _selectedPreset.value = null
        _sensitivityConfig.value = _sensitivityConfig.value.copy(deadzonePx = px)
        updateStatusToReady()
    }

    fun updateResponseCurve(curve: ResponseCurveType) {
        _selectedPreset.value = null
        _sensitivityConfig.value = _sensitivityConfig.value.copy(responseCurve = curve)
        updateStatusToReady()
    }

    fun updatePointerSpeed(speed: Int) {
        _sensitivityConfig.value = _sensitivityConfig.value.copy(systemPointerSpeed = speed)
    }

    fun updateTargetDpi(dpi: Int?) {
        _sensitivityConfig.value = _sensitivityConfig.value.copy(targetDpi = dpi)
    }

    private fun updateStatusToReady() {
        if (_engineStatus.value.code != EngineStatusCode.READY) {
            _engineStatus.value = EngineStatusInfo(
                code = EngineStatusCode.READY,
                title = "READY",
                message = "Parameters changed. Ready to test or apply profile."
            )
        }
    }

    // --- PRESET SELECTION ---

    fun applyPreset(preset: SensitivityPreset) {
        try {
            _selectedPreset.value = preset
            _sensitivityConfig.value = _sensitivityConfig.value.copy(
                xSensitivity = preset.xSensitivity,
                ySensitivity = preset.ySensitivity,
                dragResponse = preset.dragResponse,
                smoothness = preset.smoothness,
                acceleration = preset.acceleration,
                deadzonePx = preset.deadzonePx,
                responseCurve = preset.responseCurve
            )
            _engineStatus.value = EngineStatusInfo(
                code = EngineStatusCode.READY,
                title = "PRESET: ${preset.title}",
                message = "Preset loaded: X ${preset.xSensitivity}x, Y ${preset.ySensitivity}x, Curve ${preset.responseCurve.displayName}."
            )
        } catch (e: Throwable) {
            Log.e(TAG, "Error applying preset: ${e.message}")
        }
    }

    // --- DRAG CALIBRATION TEST PAD ACTIONS ---

    fun setDragTestMode(mode: DragTestMode) {
        _dragTestMode.value = mode
        clearCalibrationTrail()
    }

    fun clearCalibrationTrail() {
        try {
            xyResponseEngine.reset()
            _rawTrail.value = emptyList()
            _engineTrail.value = emptyList()
            _calibrationStats.value = DragCalibrationResult(testMode = _dragTestMode.value)
        } catch (e: Throwable) {
            Log.w(TAG, "Error clearing calibration trail: ${e.message}")
        }
    }

    fun onCalibrationDragStart(offset: Offset, timestamp: Long) {
        try {
            val ep = xyResponseEngine.onTouchDown(offset, timestamp)
            _rawTrail.value = listOf(ep.raw)
            _engineTrail.value = listOf(ep.processed)
            _touchLiveMetrics.value = touchMetricsEngine.onTouchDown(offset.x, offset.y, timestamp)
        } catch (e: Throwable) {
            Log.w(TAG, "Error in onCalibrationDragStart: ${e.message}")
        }
    }

    fun onCalibrationDragMove(offset: Offset, timestamp: Long) {
        try {
            val ep = xyResponseEngine.onTouchMove(offset, timestamp, _sensitivityConfig.value)
            _rawTrail.value = xyResponseEngine.rawTrail.toList()
            _engineTrail.value = xyResponseEngine.processedTrail.toList()

            val touchM = touchMetricsEngine.onTouchMove(offset.x, offset.y, timestamp)
            _touchLiveMetrics.value = touchM

            val stats = xyResponseEngine.calculateCalibrationStats(_dragTestMode.value)
            _calibrationStats.value = stats

            if (touchM.samplingRateHz > 0f) {
                _sensitivityConfig.value = _sensitivityConfig.value.copy(
                    touchSamplingRateHz = touchM.samplingRateHz,
                    jitterVariance = touchM.jitterRmsPx
                )
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Error in onCalibrationDragMove: ${e.message}")
        }
    }

    fun onCalibrationDragEnd() {
        try {
            val touchM = touchMetricsEngine.onTouchUp()
            _touchLiveMetrics.value = touchM
            val stats = xyResponseEngine.calculateCalibrationStats(_dragTestMode.value)
            _calibrationStats.value = stats
        } catch (e: Throwable) {
            Log.w(TAG, "Error in onCalibrationDragEnd: ${e.message}")
        }
    }

    // --- APPLY PROFILE & VERIFY (HONEST TRANSPARENCY) ---

    fun applySensitivityProfile() {
        viewModelScope.launch {
            try {
                val speed = _sensitivityConfig.value.systemPointerSpeed
                val dpi = _sensitivityConfig.value.targetDpi

                val shizukuState = shizukuManager.shizukuInfo.value
                val isShizukuConnected = shizukuState.status == ShizukuStatus.CONNECTED

                val speedResult = shizukuManager.applyPointerSpeed(speed)

                var verifiedDpiVal: Int? = null
                if (isShizukuConnected && dpi != null && dpi in 160..800) {
                    val dpiRes = shizukuManager.applyDisplayDensity(dpi)
                    if (dpiRes is ApplyResult.Success) {
                        verifiedDpiVal = dpiRes.verifiedDpi
                    }
                }

                val curProfile = _selectedProfile.value
                if (curProfile != null) {
                    val updatedProfile = curProfile.copy(
                        xSensitivity = _sensitivityConfig.value.xSensitivity,
                        ySensitivity = _sensitivityConfig.value.ySensitivity,
                        dragResponse = _sensitivityConfig.value.dragResponse,
                        dragSmoothness = _sensitivityConfig.value.smoothness,
                        acceleration = _sensitivityConfig.value.acceleration,
                        deadzonePx = _sensitivityConfig.value.deadzonePx,
                        responseCurve = _sensitivityConfig.value.responseCurve.name,
                        pointerSpeed = speed,
                        targetDpi = dpi ?: curProfile.targetDpi
                    )
                    repository.updateProfile(updatedProfile)
                    _selectedProfile.value = updatedProfile
                }

                if (speedResult is ApplyResult.Success) {
                    _engineStatus.value = EngineStatusInfo(
                        code = EngineStatusCode.APPLIED,
                        title = "APPLIED TO SYSTEM",
                        message = "System Pointer Speed verified at ${speedResult.verifiedPointerSpeed ?: speed}. Profile saved to local database.",
                        technicalDetails = "GAME SENSITIVITY API: NOT AVAILABLE (Android OS sandbox restricts external apps from modifying game memory directly. Use these calibrated values in your in-game sensitivity sliders)."
                    )
                    _applyResult.value = ApplyResult.Success(
                        message = "System Pointer Speed verified: ${speedResult.verifiedPointerSpeed ?: speed}. Profile saved locally.",
                        verifiedPointerSpeed = speedResult.verifiedPointerSpeed ?: speed,
                        verifiedDpi = verifiedDpiVal,
                        isGameApiAvailable = false
                    )
                } else if (speedResult is ApplyResult.Error) {
                    _engineStatus.value = EngineStatusInfo(
                        code = EngineStatusCode.ERROR,
                        title = "SYSTEM APPLY FAILED",
                        message = speedResult.reason,
                        technicalDetails = speedResult.suggestedAction
                    )
                    _applyResult.value = speedResult
                } else {
                    _engineStatus.value = EngineStatusInfo(
                        code = EngineStatusCode.UNSUPPORTED,
                        title = "GAME SENSITIVITY API: NOT AVAILABLE",
                        message = "Android sandbox restricts direct game memory injection. Profile saved to local engine.",
                        technicalDetails = "Direct in-game sensitivity modification is not supported by Android OS."
                    )
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Error applying sensitivity profile: ${e.message}")
                _engineStatus.value = EngineStatusInfo(
                    code = EngineStatusCode.ERROR,
                    title = "ERROR",
                    message = "Error applying settings: ${e.message}"
                )
                _applyResult.value = ApplyResult.Error("Error: ${e.message}")
            }
        }
    }

    fun resetSensitivityToDefaults() {
        viewModelScope.launch {
            try {
                val defaultPreset = SensitivityPreset.BALANCED
                applyPreset(defaultPreset)
                _sensitivityConfig.value = _sensitivityConfig.value.copy(
                    systemPointerSpeed = 0,
                    targetDpi = null
                )
                shizukuManager.applyPointerSpeed(0)
                if (shizukuManager.shizukuInfo.value.status == ShizukuStatus.CONNECTED) {
                    shizukuManager.resetDisplayDensity()
                }
                _engineStatus.value = EngineStatusInfo(
                    code = EngineStatusCode.READY,
                    title = "READY",
                    message = "Reset to standard Balanced defaults (Pointer Speed 0, 1.00x / 1.15x)."
                )
                _applyResult.value = ApplyResult.Idle
            } catch (e: Throwable) {
                Log.e(TAG, "Error resetting sensitivity: ${e.message}")
            }
        }
    }

    // --- SHIZUKU ACTIONS ---

    fun requestShizukuPermission() {
        try {
            shizukuManager.requestPermission()
        } catch (e: Throwable) {
            Log.e(TAG, "Error requesting Shizuku permission: ${e.message}")
        }
    }

    fun refreshShizukuStatus() {
        try {
            shizukuManager.checkStatus()
        } catch (e: Throwable) {
            Log.e(TAG, "Error refreshing Shizuku status: ${e.message}")
        }
    }

    fun openShizukuApp(context: Context) {
        try {
            val intent = context.packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
            if (intent != null) {
                context.startActivity(intent)
            } else {
                val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app"))
                context.startActivity(marketIntent)
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Could not launch Shizuku: ${e.message}")
        }
    }

    // --- CROSSHAIR ACTIONS ---

    fun toggleCrosshair(enable: Boolean, context: Context) {
        try {
            if (enable) {
                if (!Settings.canDrawOverlays(context)) {
                    _applyResult.value = ApplyResult.Error(
                        reason = "Overlay Permission Required: Izinkan 'Tampilkan di atas aplikasi lain' di Settings.",
                        suggestedAction = "Buka Settings Overlay"
                    )
                    return
                }
                val newCfg = crosshairConfig.value.copy(isEnabled = true)
                CrosshairOverlayService.updateLiveConfig(newCfg)
                val intent = Intent(context, CrosshairOverlayService::class.java).apply {
                    action = CrosshairOverlayService.ACTION_START
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } else {
                val intent = Intent(context, CrosshairOverlayService::class.java).apply {
                    action = CrosshairOverlayService.ACTION_STOP
                }
                context.startService(intent)
                CrosshairOverlayService.updateLiveConfig(crosshairConfig.value.copy(isEnabled = false))
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Error toggling crosshair: ${e.message}")
            _applyResult.value = ApplyResult.Error("Failed to toggle crosshair: ${e.message}")
        }
    }

    fun updateCrosshairConfig(config: CrosshairConfig, context: Context) {
        try {
            CrosshairOverlayService.updateLiveConfig(config)
            if (config.isEnabled && Settings.canDrawOverlays(context)) {
                val intent = Intent(context, CrosshairOverlayService::class.java).apply {
                    action = CrosshairOverlayService.ACTION_UPDATE
                }
                context.startService(intent)
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Error updating crosshair config: ${e.message}")
        }
    }

    fun resetCrosshairPosition(context: Context) {
        val updated = crosshairConfig.value.copy(offsetX = 0, offsetY = 0)
        updateCrosshairConfig(updated, context)
    }

    fun saveCrosshairPreset(name: String) {
        viewModelScope.launch {
            try {
                val cfg = crosshairConfig.value
                val preset = CrosshairPresetEntity(
                    presetName = name,
                    style = cfg.style.name,
                    colorHex = cfg.colorHex,
                    sizeDp = cfg.sizeDp,
                    thicknessDp = cfg.thicknessDp,
                    gapDp = cfg.gapDp,
                    opacity = cfg.opacity,
                    offsetX = cfg.offsetX,
                    offsetY = cfg.offsetY,
                    customImageUri = cfg.customImageUri,
                    isSystemPreset = false
                )
                repository.insertPreset(preset)
            } catch (e: Throwable) {
                Log.e(TAG, "Error saving crosshair preset: ${e.message}")
            }
        }
    }

    fun applyCrosshairPreset(preset: CrosshairPresetEntity, context: Context) {
        try {
            val styleEnum = try {
                CrosshairStyle.valueOf(preset.style)
            } catch (e: Exception) {
                CrosshairStyle.GAP_CROSS
            }
            val newConfig = crosshairConfig.value.copy(
                style = styleEnum,
                colorHex = preset.colorHex,
                sizeDp = preset.sizeDp,
                thicknessDp = preset.thicknessDp,
                gapDp = preset.gapDp,
                opacity = preset.opacity,
                offsetX = preset.offsetX,
                offsetY = preset.offsetY,
                customImageUri = preset.customImageUri
            )
            updateCrosshairConfig(newConfig, context)
        } catch (e: Throwable) {
            Log.e(TAG, "Error applying crosshair preset: ${e.message}")
        }
    }

    fun deleteCrosshairPreset(preset: CrosshairPresetEntity) {
        viewModelScope.launch {
            try {
                repository.deletePreset(preset)
            } catch (e: Throwable) {
                Log.e(TAG, "Error deleting crosshair preset: ${e.message}")
            }
        }
    }

    // --- OPTIMIZER ACTIONS ---

    fun setPerformanceMode(mode: PerformanceMode) {
        viewModelScope.launch {
            try {
                _optimizerState.value = _optimizerState.value.copy(
                    currentMode = mode,
                    actionStatus = "Applying ${mode.title}..."
                )

                when (mode) {
                    PerformanceMode.BALANCED -> {
                        shizukuManager.applyPointerSpeed(0)
                        if (shizukuManager.shizukuInfo.value.status == ShizukuStatus.CONNECTED) {
                            shizukuManager.resetDisplayDensity()
                        }
                        _optimizerState.value = _optimizerState.value.copy(
                            actionStatus = "Balanced mode active (Default Pointer Speed 0)"
                        )
                    }

                    PerformanceMode.PERFORMANCE -> {
                        shizukuManager.applyPointerSpeed(4)
                        val display = displayData.value
                        val maxHz = display.supportedRefreshRates.maxOrNull() ?: 60f
                        if (maxHz > 60f && shizukuManager.shizukuInfo.value.status == ShizukuStatus.CONNECTED) {
                            shizukuManager.applyPeakRefreshRate(maxHz)
                        }
                        val freed = hardwareMonitor.trimApplicationMemory()
                        _optimizerState.value = _optimizerState.value.copy(
                            actionStatus = "Extreme Performance active: Pointer +4, Max Refresh ${maxHz.toInt()}Hz, Cache Trimmed."
                        )
                    }

                    PerformanceMode.BATTERY_SAVER -> {
                        shizukuManager.applyPointerSpeed(-1)
                        if (shizukuManager.shizukuInfo.value.status == ShizukuStatus.CONNECTED) {
                            shizukuManager.applyPeakRefreshRate(60f)
                        }
                        val freed = hardwareMonitor.trimApplicationMemory()
                        _optimizerState.value = _optimizerState.value.copy(
                            actionStatus = "Battery Saver active: Refresh rate capped to 60Hz, memory cleaned ($freed MB)."
                        )
                    }
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Error setting performance mode: ${e.message}")
            }
        }
    }

    fun runMemoryOptimization() {
        try {
            val freedMb = hardwareMonitor.trimApplicationMemory()
            _optimizerState.value = _optimizerState.value.copy(
                lastTrimmedTime = System.currentTimeMillis(),
                memoryFreedMb = freedMb,
                actionStatus = "Memory trimmed: freed $freedMb MB of application caches."
            )
        } catch (e: Throwable) {
            Log.e(TAG, "Error running memory optimization: ${e.message}")
        }
    }

    // --- GAME PROFILE MANAGEMENT ---

    fun selectGameProfile(profile: GameProfileEntity) {
        _selectedProfile.value = profile
        loadProfileToSensitivity(profile)
    }

    private fun loadProfileToSensitivity(profile: GameProfileEntity) {
        try {
            val curve = try {
                ResponseCurveType.valueOf(profile.responseCurve)
            } catch (e: Exception) {
                ResponseCurveType.DYNAMIC_S
            }

            _sensitivityConfig.value = _sensitivityConfig.value.copy(
                xSensitivity = profile.xSensitivity,
                ySensitivity = profile.ySensitivity,
                dragResponse = profile.dragResponse,
                smoothness = profile.dragSmoothness,
                acceleration = profile.acceleration,
                deadzonePx = profile.deadzonePx,
                responseCurve = curve,
                systemPointerSpeed = profile.pointerSpeed,
                targetDpi = profile.targetDpi
            )
            _engineStatus.value = EngineStatusInfo(
                code = EngineStatusCode.READY,
                title = "LOADED: ${profile.displayName.uppercase()}",
                message = "Profile loaded. X: ${profile.xSensitivity}x, Y: ${profile.ySensitivity}x, Ratio: 1:${"%.2f".format(profile.xyRatio)}"
            )
        } catch (e: Throwable) {
            Log.e(TAG, "Error loading profile to sensitivity: ${e.message}")
        }
    }

    fun applyGameProfileToSystem(profile: GameProfileEntity) {
        viewModelScope.launch {
            try {
                loadProfileToSensitivity(profile)
                val speedResult = shizukuManager.applyPointerSpeed(profile.pointerSpeed)
                val dpiResult = if (profile.targetDpi in 160..800 && shizukuManager.shizukuInfo.value.status == ShizukuStatus.CONNECTED) {
                    shizukuManager.applyDisplayDensity(profile.targetDpi)
                } else null

                val gameModeResult = if (shizukuManager.shizukuInfo.value.status == ShizukuStatus.CONNECTED) {
                    shizukuManager.applyGameMode(profile.packageName, profile.gameMode.lowercase())
                } else null

                repository.setActiveProfile(profile.id)

                val notes = mutableListOf<String>()
                notes.add("Pointer Speed: ${profile.pointerSpeed}")
                if (dpiResult is ApplyResult.Success) notes.add("DPI: ${profile.targetDpi}")
                if (gameModeResult is ApplyResult.Success) notes.add("Game Mode: ${profile.gameMode}")

                _engineStatus.value = EngineStatusInfo(
                    code = EngineStatusCode.APPLIED,
                    title = "PROFILE APPLIED",
                    message = "Profile '${profile.displayName}' applied (${notes.joinToString(", ")}).",
                    technicalDetails = "GAME SENSITIVITY API: NOT AVAILABLE (Profile saved in engine for manual calibration)."
                )

                _applyResult.value = ApplyResult.Success(
                    message = "Profile '${profile.displayName}' applied to system (${notes.joinToString(", ")}).",
                    verifiedPointerSpeed = profile.pointerSpeed,
                    verifiedDpi = profile.targetDpi,
                    isGameApiAvailable = false
                )
            } catch (e: Throwable) {
                Log.e(TAG, "Error applying game profile: ${e.message}")
            }
        }
    }

    fun saveGameProfile(profile: GameProfileEntity) {
        viewModelScope.launch {
            try {
                if (profile.id == 0L) {
                    repository.insertProfile(profile)
                } else {
                    repository.updateProfile(profile)
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Error saving game profile: ${e.message}")
            }
        }
    }

    fun deleteGameProfile(profile: GameProfileEntity) {
        viewModelScope.launch {
            try {
                repository.deleteProfile(profile)
            } catch (e: Throwable) {
                Log.e(TAG, "Error deleting game profile: ${e.message}")
            }
        }
    }

    fun isGameInstalled(packageName: String): Boolean {
        return try {
            repository.isPackageInstalled(packageName)
        } catch (e: Throwable) {
            false
        }
    }

    fun launchGame(packageName: String, context: Context): Boolean {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (e: Throwable) {
            false
        }
    }
}
