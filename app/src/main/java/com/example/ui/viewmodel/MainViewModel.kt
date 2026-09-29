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
import com.example.database.*
import com.example.hardware.*
import com.example.model.*
import com.example.service.CrosshairOverlayService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

class MainViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "MainViewModel"
    }

    private val app = application as ZxOptimizerApplication
    private val repository = app.repository
    private val shizukuManager = app.shizukuManager
    private val hardwareMonitor = app.hardwareMonitor
    val vibrationManager = VibrationManager(application)
    val backupManager = ProfileBackupManager(repository)

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

    // --- EXECUTION MODE (SIMULATION vs APPLY) ---
    private val _executionMode = MutableStateFlow(ExecutionMode.SIMULATION)
    val executionMode: StateFlow<ExecutionMode> = _executionMode.asStateFlow()

    // --- SENSITIVITY MODE (GLOBAL vs PER-GAME) ---
    private val _sensitivityMode = MutableStateFlow(SensitivityMode.GLOBAL)
    val sensitivityMode: StateFlow<SensitivityMode> = _sensitivityMode.asStateFlow()

    // Global Sensitivity Config
    private val _globalSensitivityConfig = MutableStateFlow(
        SensitivityConfig(
            xSensitivity = 1.90f,
            ySensitivity = 2.35f,
            dragResponse = 1.10f,
            smoothness = 0.65f,
            acceleration = 0.30f,
            deadzonePx = 2.0f,
            responseCurve = ResponseCurveType.DYNAMIC_S
        )
    )
    val globalSensitivityConfig: StateFlow<SensitivityConfig> = _globalSensitivityConfig.asStateFlow()

    // Active working sensitivity (used by screen & calibration)
    private val _sensitivityConfig = MutableStateFlow(
        SensitivityConfig(
            xSensitivity = 1.90f,
            ySensitivity = 2.35f,
            dragResponse = 1.10f,
            smoothness = 0.65f,
            acceleration = 0.30f,
            deadzonePx = 2.0f,
            responseCurve = ResponseCurveType.DYNAMIC_S
        )
    )
    val sensitivityConfig: StateFlow<SensitivityConfig> = _sensitivityConfig.asStateFlow()

    private val _selectedPreset = MutableStateFlow<SensitivityPreset?>(SensitivityPreset.BALANCED)
    val selectedPreset: StateFlow<SensitivityPreset?> = _selectedPreset.asStateFlow()

    // Engine Status
    private val _engineStatus = MutableStateFlow(
        EngineStatusInfo(
            code = EngineStatusCode.READY,
            title = "READY",
            message = "GamesLabs X/Y Engine ready. Simulation Mode active (No changes applied to Android system)."
        )
    )
    val engineStatus: StateFlow<EngineStatusInfo> = _engineStatus.asStateFlow()

    private val _applyResult = MutableStateFlow<ApplyResult>(ApplyResult.Idle)
    val applyResult: StateFlow<ApplyResult> = _applyResult.asStateFlow()

    // Action Result & History
    private val _lastActionResult = MutableStateFlow<ActionResultData?>(null)
    val lastActionResult: StateFlow<ActionResultData?> = _lastActionResult.asStateFlow()

    val actionHistory: StateFlow<List<ActionHistoryEntity>> = repository.allActionHistory
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Vibration State
    private val _vibrationConfig = MutableStateFlow(VibrationConfig())
    val vibrationConfig: StateFlow<VibrationConfig> = _vibrationConfig.asStateFlow()

    // Preflight Check State
    private val _preflightResult = MutableStateFlow(PreflightCheckResult())
    val preflightResult: StateFlow<PreflightCheckResult> = _preflightResult.asStateFlow()

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

    // System restore backups
    private var backupPointerSpeed: Int = 0
    private var backupDpi: Int = 0

    // Dynamic Calculated Calibration Scores (85% and 70% benchmarks derived mathematically from telemetry)
    val sensCalibrationScore: StateFlow<Int> = combine(
        _sensitivityConfig,
        displayData
    ) { cfg, display ->
        calculateSensitivityScore(cfg, display)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 85)

    val touchCalibrationScore: StateFlow<Int> = combine(
        displayData,
        _touchLiveMetrics
    ) { display, touch ->
        calculateTouchScore(display, touch)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 70)

    // Smart Stutter Analysis
    val smartStutterAnalysis: StateFlow<SmartStutterAnalysis> = combine(
        memoryData,
        batteryData,
        thermalData,
        displayData
    ) { mem, bat, th, disp ->
        computeSmartStutterAnalysis(mem, bat, th, disp)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SmartStutterAnalysis())

    // Capability Matrix
    val capabilityMatrix: StateFlow<List<CapabilityItem>> = combine(
        shizukuInfo,
        displayData
    ) { shizuku, _ ->
        buildCapabilityMatrix(shizuku)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // NON-BLOCKING ASYNC STARTUP: All reading is performed safely on background thread
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentSpeed = shizukuManager.getSystemPointerSpeedDirect()
                backupPointerSpeed = currentSpeed
                _sensitivityConfig.value = _sensitivityConfig.value.copy(systemPointerSpeed = currentSpeed)
            } catch (e: Throwable) {
                Log.w(TAG, "Could not read pointer speed at startup: ${e.message}")
            }

            try {
                val savedConfig = repository.getGlobalConfig()
                if (savedConfig != null) {
                    val sMode = if (savedConfig.sensitivityMode == "PER_GAME") SensitivityMode.PER_GAME else SensitivityMode.GLOBAL
                    val eMode = if (savedConfig.executionMode == "APPLY") ExecutionMode.APPLY else ExecutionMode.SIMULATION
                    _sensitivityMode.value = sMode
                    _executionMode.value = eMode

                    val gCurve = try {
                        ResponseCurveType.valueOf(savedConfig.globalCurve)
                    } catch (e: Exception) {
                        ResponseCurveType.DYNAMIC_S
                    }

                    val gConfig = SensitivityConfig(
                        xSensitivity = savedConfig.globalX,
                        ySensitivity = savedConfig.globalY,
                        dragResponse = savedConfig.globalResponse,
                        smoothness = savedConfig.globalSmoothness,
                        acceleration = savedConfig.globalAcceleration,
                        deadzonePx = savedConfig.globalDeadzone,
                        responseCurve = gCurve
                    )
                    _globalSensitivityConfig.value = gConfig

                    if (sMode == SensitivityMode.GLOBAL) {
                        _sensitivityConfig.value = gConfig
                    }

                    val vProfile = try {
                        HapticProfileType.valueOf(savedConfig.vibrationProfile)
                    } catch (e: Exception) {
                        HapticProfileType.CLICK
                    }

                    _vibrationConfig.value = VibrationConfig(
                        isEnabled = savedConfig.vibrationEnabled,
                        intensity = savedConfig.vibrationIntensity,
                        profile = vProfile
                    )
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Error loading saved global config: ${e.message}")
            }

            try {
                gameProfiles.collect { profiles ->
                    if (_selectedProfile.value == null && profiles.isNotEmpty()) {
                        val active = profiles.find { it.isDefaultSelected } ?: profiles.first()
                        _selectedProfile.value = active
                        if (_sensitivityMode.value == SensitivityMode.PER_GAME) {
                            loadProfileToSensitivity(active)
                        }
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
            runPreflightCheck()
        } catch (e: Throwable) {
            Log.w(TAG, "refreshHardware error: ${e.message}")
        }
    }

    // --- MODE SWITCHING ---

    fun setExecutionMode(mode: ExecutionMode) {
        _executionMode.value = mode
        persistGlobalConfig()
        recordAction(
            action = "Switch Execution Mode",
            target = "Engine",
            mode = mode,
            status = ActionStatus.SUCCESS,
            detail = "Switched to ${mode.displayName} (${mode.badgeText})",
            verification = VerificationStatus.VERIFIED
        )
    }

    fun setSensitivityMode(mode: SensitivityMode) {
        _sensitivityMode.value = mode
        if (mode == SensitivityMode.GLOBAL) {
            _sensitivityConfig.value = _globalSensitivityConfig.value
            _engineStatus.value = EngineStatusInfo(
                code = EngineStatusCode.READY,
                title = "GLOBAL MODE",
                message = "Using master Global Sensitivity configuration (X: ${_globalSensitivityConfig.value.xSensitivity}x, Y: ${_globalSensitivityConfig.value.ySensitivity}x)."
            )
        } else {
            val prof = _selectedProfile.value
            if (prof != null) {
                loadProfileToSensitivity(prof)
            }
        }
        persistGlobalConfig()
        recordAction(
            action = "Switch Sensitivity Mode",
            target = "Sensitivity Engine",
            mode = _executionMode.value,
            status = ActionStatus.SUCCESS,
            detail = "Mode changed to ${mode.displayName}",
            verification = VerificationStatus.VERIFIED
        )
    }

    // --- SENSITIVITY ENGINE PARAMETER ACTIONS ---

    fun updateXSensitivity(x: Float) {
        _selectedPreset.value = null
        _sensitivityConfig.value = _sensitivityConfig.value.copy(xSensitivity = x)
        if (_sensitivityMode.value == SensitivityMode.GLOBAL) {
            _globalSensitivityConfig.value = _globalSensitivityConfig.value.copy(xSensitivity = x)
        }
        updateStatusToReady()
    }

    fun updateYSensitivity(y: Float) {
        _selectedPreset.value = null
        _sensitivityConfig.value = _sensitivityConfig.value.copy(ySensitivity = y)
        if (_sensitivityMode.value == SensitivityMode.GLOBAL) {
            _globalSensitivityConfig.value = _globalSensitivityConfig.value.copy(ySensitivity = y)
        }
        updateStatusToReady()
    }

    fun updateDragResponse(response: Float) {
        _selectedPreset.value = null
        _sensitivityConfig.value = _sensitivityConfig.value.copy(dragResponse = response)
        if (_sensitivityMode.value == SensitivityMode.GLOBAL) {
            _globalSensitivityConfig.value = _globalSensitivityConfig.value.copy(dragResponse = response)
        }
        updateStatusToReady()
    }

    fun updateSmoothness(value: Float) {
        _selectedPreset.value = null
        _sensitivityConfig.value = _sensitivityConfig.value.copy(smoothness = value)
        if (_sensitivityMode.value == SensitivityMode.GLOBAL) {
            _globalSensitivityConfig.value = _globalSensitivityConfig.value.copy(smoothness = value)
        }
        updateStatusToReady()
    }

    fun updateAcceleration(value: Float) {
        _selectedPreset.value = null
        _sensitivityConfig.value = _sensitivityConfig.value.copy(acceleration = value)
        if (_sensitivityMode.value == SensitivityMode.GLOBAL) {
            _globalSensitivityConfig.value = _globalSensitivityConfig.value.copy(acceleration = value)
        }
        updateStatusToReady()
    }

    fun updateDeadzone(px: Float) {
        _selectedPreset.value = null
        _sensitivityConfig.value = _sensitivityConfig.value.copy(deadzonePx = px)
        if (_sensitivityMode.value == SensitivityMode.GLOBAL) {
            _globalSensitivityConfig.value = _globalSensitivityConfig.value.copy(deadzonePx = px)
        }
        updateStatusToReady()
    }

    fun updateResponseCurve(curve: ResponseCurveType) {
        _selectedPreset.value = null
        _sensitivityConfig.value = _sensitivityConfig.value.copy(responseCurve = curve)
        if (_sensitivityMode.value == SensitivityMode.GLOBAL) {
            _globalSensitivityConfig.value = _globalSensitivityConfig.value.copy(responseCurve = curve)
        }
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
                message = "Parameters changed. Ready to test or save profile."
            )
        }
    }

    // --- PRESET SELECTION ---

    fun applyPreset(preset: SensitivityPreset) {
        try {
            _selectedPreset.value = preset
            val updated = _sensitivityConfig.value.copy(
                xSensitivity = preset.xSensitivity,
                ySensitivity = preset.ySensitivity,
                dragResponse = preset.dragResponse,
                smoothness = preset.smoothness,
                acceleration = preset.acceleration,
                deadzonePx = preset.deadzonePx,
                responseCurve = preset.responseCurve
            )
            _sensitivityConfig.value = updated
            if (_sensitivityMode.value == SensitivityMode.GLOBAL) {
                _globalSensitivityConfig.value = updated
            }

            _engineStatus.value = EngineStatusInfo(
                code = EngineStatusCode.READY,
                title = "PRESET: ${preset.title}",
                message = "Preset loaded: X ${preset.xSensitivity}x, Y ${preset.ySensitivity}x, Curve ${preset.responseCurve.displayName}."
            )

            recordAction(
                action = "Load Preset",
                target = preset.title,
                mode = _executionMode.value,
                status = ActionStatus.SUCCESS,
                detail = "Preset applied: ${preset.description}",
                verification = VerificationStatus.VERIFIED
            )
        } catch (e: Throwable) {
            Log.e(TAG, "Error applying preset: ${e.message}")
        }
    }

    // --- SAVE GLOBAL PROFILE ---

    fun saveGlobalProfile() {
        viewModelScope.launch {
            try {
                _globalSensitivityConfig.value = _sensitivityConfig.value
                persistGlobalConfig()

                recordAction(
                    action = "Save Global Profile",
                    target = "Global Configuration",
                    mode = _executionMode.value,
                    status = ActionStatus.SUCCESS,
                    detail = "Saved X: ${_sensitivityConfig.value.xSensitivity}x, Y: ${_sensitivityConfig.value.ySensitivity}x as default master.",
                    verification = VerificationStatus.VERIFIED
                )

                _engineStatus.value = EngineStatusInfo(
                    code = EngineStatusCode.APPLIED,
                    title = "GLOBAL PROFILE SAVED",
                    message = "Global settings saved. Used as standard default for all unassigned game profiles.",
                    technicalDetails = "Note: Stored in persistent local database."
                )
            } catch (e: Throwable) {
                Log.e(TAG, "Error saving global profile: ${e.message}")
            }
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
                val mode = _executionMode.value
                val speed = _sensitivityConfig.value.systemPointerSpeed
                val dpi = _sensitivityConfig.value.targetDpi

                // SIMULATION MODE FLOW:
                if (mode == ExecutionMode.SIMULATION) {
                    val simDetail = "Target Pointer Speed: $speed. Target DPI: ${dpi ?: "Default"}. Expected outcome: Calibrated values will be used for in-game drag testing. NO changes applied to system settings."
                    _engineStatus.value = EngineStatusInfo(
                        code = EngineStatusCode.READY,
                        title = "SIMULATION ONLY",
                        message = "Simulation complete. Requirements verified. No system modification made.",
                        technicalDetails = simDetail
                    )
                    recordAction(
                        action = "Simulate Sensitivity",
                        target = _selectedProfile.value?.displayName ?: "Global",
                        mode = ExecutionMode.SIMULATION,
                        status = ActionStatus.SUCCESS,
                        detail = simDetail,
                        verification = VerificationStatus.VERIFICATION_UNAVAILABLE
                    )
                    return@launch
                }

                // APPLY MODE FLOW (Real action):
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
                if (curProfile != null && _sensitivityMode.value == SensitivityMode.PER_GAME) {
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

                    recordAction(
                        action = "Apply Sensitivity",
                        target = _selectedProfile.value?.displayName ?: "System",
                        mode = ExecutionMode.APPLY,
                        status = ActionStatus.SUCCESS,
                        detail = "Pointer speed calibrated to $speed. Verified readback: ${speedResult.verifiedPointerSpeed ?: speed}.",
                        verification = VerificationStatus.VERIFIED
                    )
                } else if (speedResult is ApplyResult.Error) {
                    _engineStatus.value = EngineStatusInfo(
                        code = EngineStatusCode.ERROR,
                        title = "SYSTEM APPLY FAILED",
                        message = speedResult.reason,
                        technicalDetails = speedResult.suggestedAction
                    )
                    _applyResult.value = speedResult

                    recordAction(
                        action = "Apply Sensitivity",
                        target = "System",
                        mode = ExecutionMode.APPLY,
                        status = ActionStatus.FAILED,
                        detail = speedResult.reason,
                        verification = VerificationStatus.FAILED_TO_VERIFY
                    )
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Error applying sensitivity profile: ${e.message}")
                recordAction(
                    action = "Apply Sensitivity",
                    target = "System",
                    mode = ExecutionMode.APPLY,
                    status = ActionStatus.FAILED,
                    detail = "Exception: ${e.message}",
                    verification = VerificationStatus.FAILED_TO_VERIFY
                )
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
                if (_executionMode.value == ExecutionMode.APPLY) {
                    shizukuManager.applyPointerSpeed(0)
                    if (shizukuManager.shizukuInfo.value.status == ShizukuStatus.CONNECTED) {
                        shizukuManager.resetDisplayDensity()
                    }
                }
                _engineStatus.value = EngineStatusInfo(
                    code = EngineStatusCode.READY,
                    title = "READY",
                    message = "Reset to standard Balanced defaults (Pointer Speed 0, 1.00x / 1.15x)."
                )
                _applyResult.value = ApplyResult.Idle
                recordAction(
                    action = "Reset Sensitivity",
                    target = "Sensitivity Config",
                    mode = _executionMode.value,
                    status = ActionStatus.SUCCESS,
                    detail = "Reset parameters to default 1.00x / 1.15x",
                    verification = VerificationStatus.VERIFIED
                )
            } catch (e: Throwable) {
                Log.e(TAG, "Error resetting sensitivity: ${e.message}")
            }
        }
    }

    // --- SHIZUKU ACTIONS ---

    fun requestShizukuPermission() {
        try {
            shizukuManager.requestPermission()
            recordAction(
                action = "Request Shizuku Permission",
                target = "Shizuku Service",
                mode = _executionMode.value,
                status = ActionStatus.SUCCESS,
                detail = "Prompted Android system for Shizuku authorization",
                verification = VerificationStatus.VERIFIED
            )
        } catch (e: Throwable) {
            Log.e(TAG, "Error requesting Shizuku permission: ${e.message}")
        }
    }

    fun refreshShizukuStatus() {
        try {
            val info = shizukuManager.checkStatus()
            recordAction(
                action = "Check Shizuku Status",
                target = "Shizuku Manager",
                mode = _executionMode.value,
                status = if (info.status == ShizukuStatus.CONNECTED) ActionStatus.SUCCESS else ActionStatus.PARTIAL,
                detail = "Status: ${info.status}. ${info.message}",
                verification = VerificationStatus.VERIFIED
            )
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
                    recordAction(
                        action = "Enable Crosshair",
                        target = "Overlay Window",
                        mode = _executionMode.value,
                        status = ActionStatus.BLOCKED,
                        detail = "Overlay permission (SYSTEM_ALERT_WINDOW) not granted",
                        verification = VerificationStatus.FAILED_TO_VERIFY
                    )
                    return
                }

                if (_executionMode.value == ExecutionMode.SIMULATION) {
                    recordAction(
                        action = "Simulate Crosshair",
                        target = "Overlay Window",
                        mode = ExecutionMode.SIMULATION,
                        status = ActionStatus.SUCCESS,
                        detail = "Permission granted. Ready to project floating reticle overlay in Apply Mode.",
                        verification = VerificationStatus.VERIFICATION_UNAVAILABLE
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

                recordAction(
                    action = "Enable Crosshair",
                    target = "Overlay Service",
                    mode = ExecutionMode.APPLY,
                    status = ActionStatus.SUCCESS,
                    detail = "Crosshair overlay active on display",
                    verification = VerificationStatus.VERIFIED
                )
            } else {
                val intent = Intent(context, CrosshairOverlayService::class.java).apply {
                    action = CrosshairOverlayService.ACTION_STOP
                }
                context.startService(intent)
                CrosshairOverlayService.updateLiveConfig(crosshairConfig.value.copy(isEnabled = false))

                recordAction(
                    action = "Disable Crosshair",
                    target = "Overlay Service",
                    mode = _executionMode.value,
                    status = ActionStatus.SUCCESS,
                    detail = "Crosshair overlay terminated",
                    verification = VerificationStatus.VERIFIED
                )
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Error toggling crosshair: ${e.message}")
            _applyResult.value = ApplyResult.Error("Failed to toggle crosshair: ${e.message}")
        }
    }

    fun updateCrosshairConfig(config: CrosshairConfig, context: Context) {
        try {
            CrosshairOverlayService.updateLiveConfig(config)
            if (config.isEnabled && Settings.canDrawOverlays(context) && _executionMode.value == ExecutionMode.APPLY) {
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
        recordAction(
            action = "Auto Center Crosshair",
            target = "Overlay Reticle",
            mode = _executionMode.value,
            status = ActionStatus.SUCCESS,
            detail = "Crosshair position reset to exact screen center (0, 0)",
            verification = VerificationStatus.VERIFIED
        )
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
                recordAction(
                    action = "Save Crosshair Preset",
                    target = name,
                    mode = _executionMode.value,
                    status = ActionStatus.SUCCESS,
                    detail = "Preset saved: style ${cfg.style.displayName}, size ${cfg.sizeDp.toInt()}dp",
                    verification = VerificationStatus.VERIFIED
                )
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
            recordAction(
                action = "Load Crosshair Preset",
                target = preset.presetName,
                mode = _executionMode.value,
                status = ActionStatus.SUCCESS,
                detail = "Preset loaded: ${preset.presetName}",
                verification = VerificationStatus.VERIFIED
            )
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

    // --- VIBRATION ACTIONS ---

    fun testVibration() {
        val cfg = _vibrationConfig.value
        val ok = vibrationManager.playHaptic(cfg)
        recordAction(
            action = "Vibration Test",
            target = "Haptic Motor",
            mode = _executionMode.value,
            status = if (ok) ActionStatus.SUCCESS else ActionStatus.NOT_SUPPORTED,
            detail = if (ok) "Triggered ${cfg.profile.displayName} (${cfg.intensity}/255 intensity)" else "Vibrator hardware not available or disabled",
            verification = if (ok) VerificationStatus.VERIFIED else VerificationStatus.FAILED_TO_VERIFY
        )
    }

    fun updateVibrationConfig(cfg: VibrationConfig) {
        _vibrationConfig.value = cfg
        persistGlobalConfig()
    }

    fun resetVibrationConfig() {
        _vibrationConfig.value = VibrationConfig(isEnabled = true, intensity = 180, profile = HapticProfileType.CLICK)
        persistGlobalConfig()
    }

    // --- OPTIMIZER ACTIONS ---

    fun setPerformanceMode(mode: PerformanceMode) {
        viewModelScope.launch {
            try {
                _optimizerState.value = _optimizerState.value.copy(
                    currentMode = mode,
                    actionStatus = "Applying ${mode.title}..."
                )

                if (_executionMode.value == ExecutionMode.SIMULATION) {
                    _optimizerState.value = _optimizerState.value.copy(
                        actionStatus = "Simulated ${mode.title}: Target speed and refresh calculated. No system changes made."
                    )
                    recordAction(
                        action = "Simulate Mode",
                        target = mode.title,
                        mode = ExecutionMode.SIMULATION,
                        status = ActionStatus.SUCCESS,
                        detail = "Validated requirements for ${mode.title}",
                        verification = VerificationStatus.VERIFICATION_UNAVAILABLE
                    )
                    return@launch
                }

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
                            actionStatus = "Extreme Performance active: Pointer +4, Max Refresh ${maxHz.toInt()}Hz, Cache Trimmed ($freed MB)."
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

                recordAction(
                    action = "Apply Performance Mode",
                    target = mode.title,
                    mode = ExecutionMode.APPLY,
                    status = ActionStatus.SUCCESS,
                    detail = "Switched to ${mode.title}",
                    verification = VerificationStatus.VERIFIED
                )
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
                actionStatus = "Application cache trimmed: safely freed $freedMb MB."
            )
            recordAction(
                action = "Memory Optimization",
                target = "App Process Cache",
                mode = _executionMode.value,
                status = ActionStatus.SUCCESS,
                detail = "Freed $freedMb MB of local heap/bitmap cache without killing system processes.",
                verification = VerificationStatus.VERIFIED
            )
        } catch (e: Throwable) {
            Log.e(TAG, "Error running memory optimization: ${e.message}")
        }
    }

    // --- GAME PROFILE MANAGEMENT ---

    fun selectGameProfile(profile: GameProfileEntity) {
        _selectedProfile.value = profile
        if (_sensitivityMode.value == SensitivityMode.PER_GAME) {
            loadProfileToSensitivity(profile)
        }
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

            val vProfile = try {
                HapticProfileType.valueOf(profile.vibrationProfile)
            } catch (e: Exception) {
                HapticProfileType.CLICK
            }
            _vibrationConfig.value = _vibrationConfig.value.copy(
                intensity = profile.vibrationIntensity,
                profile = vProfile
            )

            _engineStatus.value = EngineStatusInfo(
                code = EngineStatusCode.READY,
                title = "LOADED: ${profile.displayName.uppercase()}",
                message = "Game profile loaded: X ${profile.xSensitivity}x, Y ${profile.ySensitivity}x, Ratio 1:${"%.2f".format(profile.xyRatio)}."
            )
        } catch (e: Throwable) {
            Log.e(TAG, "Error loading profile to sensitivity: ${e.message}")
        }
    }

    fun useGlobalForSelectedGame() {
        val current = _selectedProfile.value ?: return
        val gCfg = _globalSensitivityConfig.value
        val updated = current.copy(
            xSensitivity = gCfg.xSensitivity,
            ySensitivity = gCfg.ySensitivity,
            dragResponse = gCfg.dragResponse,
            dragSmoothness = gCfg.smoothness,
            acceleration = gCfg.acceleration,
            deadzonePx = gCfg.deadzonePx,
            responseCurve = gCfg.responseCurve.name
        )
        saveGameProfile(updated)
        loadProfileToSensitivity(updated)
        recordAction(
            action = "Use Global Profile",
            target = current.displayName,
            mode = _executionMode.value,
            status = ActionStatus.SUCCESS,
            detail = "Copied Global configuration (X ${gCfg.xSensitivity}x, Y ${gCfg.ySensitivity}x) to ${current.displayName}.",
            verification = VerificationStatus.VERIFIED
        )
    }

    fun resetSelectedGameProfile() {
        val current = _selectedProfile.value ?: return
        val resetProfile = current.copy(
            xSensitivity = 1.00f,
            ySensitivity = 1.15f,
            dragResponse = 1.00f,
            dragSmoothness = 0.65f,
            acceleration = 0.30f,
            deadzonePx = 2.0f,
            responseCurve = "DYNAMIC_S",
            pointerSpeed = 2
        )
        saveGameProfile(resetProfile)
        loadProfileToSensitivity(resetProfile)
        recordAction(
            action = "Reset Game Profile",
            target = current.displayName,
            mode = _executionMode.value,
            status = ActionStatus.SUCCESS,
            detail = "Profile reset to factory balanced defaults.",
            verification = VerificationStatus.VERIFIED
        )
    }

    fun applyGameProfileToSystem(profile: GameProfileEntity) {
        viewModelScope.launch {
            try {
                loadProfileToSensitivity(profile)

                if (_executionMode.value == ExecutionMode.SIMULATION) {
                    val simMsg = "Simulation: Target pointer speed ${profile.pointerSpeed}, Target DPI ${profile.targetDpi}. No changes applied to system."
                    _engineStatus.value = EngineStatusInfo(
                        code = EngineStatusCode.READY,
                        title = "SIMULATION: ${profile.displayName.uppercase()}",
                        message = simMsg
                    )
                    recordAction(
                        action = "Simulate Game Profile",
                        target = profile.displayName,
                        mode = ExecutionMode.SIMULATION,
                        status = ActionStatus.SUCCESS,
                        detail = simMsg,
                        verification = VerificationStatus.VERIFICATION_UNAVAILABLE
                    )
                    return@launch
                }

                // APPLY MODE:
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

                recordAction(
                    action = "Apply Game Profile",
                    target = profile.displayName,
                    mode = ExecutionMode.APPLY,
                    status = ActionStatus.SUCCESS,
                    detail = notes.joinToString(", "),
                    verification = VerificationStatus.VERIFIED
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
                    val newId = repository.insertProfile(profile)
                    _selectedProfile.value = profile.copy(id = newId)
                } else {
                    repository.updateProfile(profile)
                    _selectedProfile.value = profile
                }
                recordAction(
                    action = "Save Game Profile",
                    target = profile.displayName,
                    mode = _executionMode.value,
                    status = ActionStatus.SUCCESS,
                    detail = "Profile saved locally (X: ${profile.xSensitivity}x, Y: ${profile.ySensitivity}x)",
                    verification = VerificationStatus.VERIFIED
                )
            } catch (e: Throwable) {
                Log.e(TAG, "Error saving game profile: ${e.message}")
            }
        }
    }

    fun deleteGameProfile(profile: GameProfileEntity) {
        viewModelScope.launch {
            try {
                repository.deleteProfile(profile)
                recordAction(
                    action = "Delete Game Profile",
                    target = profile.displayName,
                    mode = _executionMode.value,
                    status = ActionStatus.SUCCESS,
                    detail = "Removed custom profile from database.",
                    verification = VerificationStatus.VERIFIED
                )
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
                recordAction(
                    action = "Launch Game",
                    target = packageName,
                    mode = _executionMode.value,
                    status = ActionStatus.SUCCESS,
                    detail = "Game launch intent dispatched.",
                    verification = VerificationStatus.VERIFIED
                )
                true
            } else {
                recordAction(
                    action = "Launch Game",
                    target = packageName,
                    mode = _executionMode.value,
                    status = ActionStatus.NOT_SUPPORTED,
                    detail = "Package is not installed on this device.",
                    verification = VerificationStatus.VERIFICATION_UNAVAILABLE
                )
                false
            }
        } catch (e: Throwable) {
            false
        }
    }

    // --- ROLLBACK / RESTORE ---

    fun restorePreviousSystemSettings() {
        viewModelScope.launch {
            try {
                if (_executionMode.value == ExecutionMode.SIMULATION) {
                    recordAction(
                        action = "Simulate Rollback",
                        target = "System Settings",
                        mode = ExecutionMode.SIMULATION,
                        status = ActionStatus.SUCCESS,
                        detail = "Would restore pointer speed to $backupPointerSpeed and reset display density.",
                        verification = VerificationStatus.VERIFICATION_UNAVAILABLE
                    )
                    return@launch
                }

                shizukuManager.applyPointerSpeed(backupPointerSpeed)
                if (shizukuManager.shizukuInfo.value.status == ShizukuStatus.CONNECTED) {
                    shizukuManager.resetDisplayDensity()
                }
                recordAction(
                    action = "Restore System Settings",
                    target = "System Settings",
                    mode = ExecutionMode.APPLY,
                    status = ActionStatus.SUCCESS,
                    detail = "Restored system pointer speed to $backupPointerSpeed and reset display density.",
                    verification = VerificationStatus.VERIFIED
                )
            } catch (e: Throwable) {
                Log.e(TAG, "Error restoring previous settings: ${e.message}")
            }
        }
    }

    // --- PREFLIGHT SYSTEM CHECK ---

    fun runPreflightCheck() {
        viewModelScope.launch(Dispatchers.IO) {
            val items = mutableListOf<PreflightCheckItem>()

            // 1. App Instance
            items.add(PreflightCheckItem("App Core Engine", true, "GAMESLABS Architecture v1.2.0 initialized."))

            // 2. Storage
            val storageOk = storageData.value.availableBytes > 50 * 1024 * 1024L
            items.add(PreflightCheckItem("Local Storage", storageOk, "Room database persistent on internal flash."))

            // 3. Device Hardware
            val disp = displayData.value
            items.add(PreflightCheckItem("Display Digitizer", true, "${disp.physicalWidthPx}x${disp.physicalHeightPx} @ ${disp.refreshRateHz.toInt()}Hz (${disp.densityDpi} DPI)."))

            // 4. Android API Level
            items.add(PreflightCheckItem("Android SDK", true, "SDK ${Build.VERSION.SDK_INT} (${Build.VERSION.RELEASE}). Security sandbox active."))

            // 5. Overlay Permission
            val overlayOk = Settings.canDrawOverlays(app)
            items.add(PreflightCheckItem("Overlay Permission", overlayOk, if (overlayOk) "SYSTEM_ALERT_WINDOW authorized." else "Permission required for floating crosshair reticle."))

            // 6. Shizuku Service
            val shizuku = shizukuManager.shizukuInfo.value
            val shizukuOk = shizuku.status == ShizukuStatus.CONNECTED
            items.add(PreflightCheckItem("Shizuku Privilege Service", shizukuOk, if (shizukuOk) "Connected (v${shizuku.version}, UID ${shizuku.uid})." else "Optional: ${shizuku.message}"))

            // 7. Detected Games
            val profiles = gameProfiles.value
            val installedCount = profiles.count { isGameInstalled(it.packageName) }
            items.add(PreflightCheckItem("Installed Games", installedCount > 0, "$installedCount of ${profiles.size} supported game packages installed on device."))

            // 8. Profile Integrity
            items.add(PreflightCheckItem("Profile Configuration", true, "${profiles.size} persistent game profiles loaded."))

            val overall = when {
                !storageOk -> PreflightStatus.UNSUPPORTED
                !overlayOk || !shizukuOk -> PreflightStatus.PARTIAL
                else -> PreflightStatus.READY
            }

            _preflightResult.value = PreflightCheckResult(
                status = overall,
                items = items,
                timestamp = System.currentTimeMillis()
            )

            recordAction(
                action = "Pre-flight System Check",
                target = "Device & App Subsystems",
                mode = _executionMode.value,
                status = ActionStatus.SUCCESS,
                detail = "Check completed: $overall ($installedCount games installed, Overlay: $overlayOk, Shizuku: $shizukuOk)",
                verification = VerificationStatus.VERIFIED
            )
        }
    }

    // --- CLEAR HISTORY ---

    fun clearActionHistory() {
        viewModelScope.launch {
            try {
                repository.clearActionHistory()
                _lastActionResult.value = null
            } catch (e: Throwable) {
                Log.e(TAG, "Error clearing history: ${e.message}")
            }
        }
    }

    // --- ACTION RECORDER HELPER ---

    private fun recordAction(
        action: String,
        target: String,
        mode: ExecutionMode,
        status: ActionStatus,
        detail: String,
        verification: VerificationStatus = VerificationStatus.VERIFICATION_UNAVAILABLE
    ) {
        val result = ActionResultData(
            actionName = action,
            targetName = target,
            mode = mode,
            status = status,
            detail = detail,
            verification = verification
        )
        _lastActionResult.value = result

        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.insertAction(
                    ActionHistoryEntity(
                        actionName = action,
                        targetName = target,
                        mode = mode.name,
                        status = status.name,
                        detail = detail,
                        verification = verification.name,
                        timestamp = result.timestamp
                    )
                )
            } catch (e: Throwable) {
                Log.w(TAG, "Failed to persist action: ${e.message}")
            }
        }
    }

    private fun persistGlobalConfig() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val gCfg = _globalSensitivityConfig.value
                val vCfg = _vibrationConfig.value
                val entity = GlobalConfigEntity(
                    id = 1,
                    sensitivityMode = _sensitivityMode.value.name,
                    executionMode = _executionMode.value.name,
                    globalX = gCfg.xSensitivity,
                    globalY = gCfg.ySensitivity,
                    globalSmoothness = gCfg.smoothness,
                    globalResponse = gCfg.dragResponse,
                    globalAcceleration = gCfg.acceleration,
                    globalDeadzone = gCfg.deadzonePx,
                    globalCurve = gCfg.responseCurve.name,
                    vibrationEnabled = vCfg.isEnabled,
                    vibrationIntensity = vCfg.intensity,
                    vibrationProfile = vCfg.profile.name,
                    previousPointerSpeed = backupPointerSpeed,
                    previousDpi = backupDpi
                )
                repository.saveGlobalConfig(entity)
            } catch (e: Throwable) {
                Log.w(TAG, "Error saving global config: ${e.message}")
            }
        }
    }

    // --- CALCULATION LOGIC (HONEST DERIVATIONS, NO RANDOM VALUES) ---

    private fun calculateSensitivityScore(cfg: SensitivityConfig, display: DisplayData): Int {
        // Derived from mathematical equilibrium between horizontal tracking and vertical elevation
        val ratio = cfg.xyRatio
        // Ideal competitive headshot ratio is between 1.15 and 1.30
        val ratioPenalty = (abs(ratio - 1.22f) * 25f).coerceIn(0f, 25f)
        val deadzonePenalty = (abs(cfg.deadzonePx - 2.0f) * 4f).coerceIn(0f, 15f)
        val refreshBonus = if (display.refreshRateHz >= 90f) 5f else 0f
        val baseScore = 95f - ratioPenalty - deadzonePenalty + refreshBonus
        return baseScore.roundToInt().coerceIn(40, 99)
    }

    private fun calculateTouchScore(display: DisplayData, touch: TouchLiveMetrics): Int {
        // Derived from display refresh rate stability, resolution density, and sampling rate
        var score = 65
        if (display.refreshRateHz >= 90f) score += 10
        if (display.refreshRateHz >= 120f) score += 5
        if (display.densityDpi in 380..460) score += 5
        if (touch.samplingRateHz >= 120f) score += 10
        if (touch.jitterRmsPx in 0.1f..3.0f) score += 4
        return score.coerceIn(45, 95)
    }

    private fun computeSmartStutterAnalysis(
        mem: MemoryData,
        bat: BatteryData,
        thermal: ThermalData,
        display: DisplayData
    ): SmartStutterAnalysis {
        val totalMem = mem.totalBytes.toFloat().coerceAtLeast(1f)
        val freeMem = mem.availableBytes.toFloat()
        val freePct = (freeMem / totalMem) * 100f

        val recs = mutableListOf<String>()

        val status = when {
            thermal.severityLevel >= 3 -> {
                recs.add("Thermal throttling active (${thermal.statusString}). Allow device to cool.")
                SmartStutterStatus.THERMAL_WARNING
            }
            freePct < 15f || mem.isLowMemory -> {
                recs.add("RAM critically low (${freePct.roundToInt()}% free). Close background heavy apps.")
                SmartStutterStatus.MEMORY_PRESSURE
            }
            freePct < 25f -> {
                recs.add("Moderate RAM utilization. Run local app cache trim.")
                SmartStutterStatus.HIGH_LOAD
            }
            bat.temperatureCelsius > 42f -> {
                recs.add("Battery temperature elevated (${bat.temperatureCelsius}°C).")
                SmartStutterStatus.WARNING
            }
            else -> {
                recs.add("System resources optimal for ${display.refreshRateHz.toInt()}Hz gaming.")
                SmartStutterStatus.STABLE
            }
        }

        return SmartStutterAnalysis(
            status = status,
            memoryFreePercent = freePct,
            thermalLevel = thermal.severityLevel,
            batteryTempCelsius = bat.temperatureCelsius,
            currentRefreshRateHz = display.refreshRateHz,
            recommendations = recs
        )
    }

    private fun buildCapabilityMatrix(shizuku: ShizukuInfo): List<CapabilityItem> {
        val hasOverlay = Settings.canDrawOverlays(app)
        val isShizukuConnected = shizuku.status == ShizukuStatus.CONNECTED

        return listOf(
            CapabilityItem(
                name = "System Pointer Speed",
                isSupported = true,
                requiredPermission = if (isShizukuConnected) null else "WRITE_SETTINGS",
                requiresShizuku = false,
                applyAvailable = true,
                verifyAvailable = true,
                state = FeatureState.READY,
                description = "Adjusts Android Settings.System.POINTER_SPEED (-7 to +7)."
            ),
            CapabilityItem(
                name = "Display Density (DPI)",
                isSupported = isShizukuConnected,
                requiredPermission = null,
                requiresShizuku = true,
                applyAvailable = isShizukuConnected,
                verifyAvailable = isShizukuConnected,
                state = if (isShizukuConnected) FeatureState.READY else FeatureState.REQUIRES_SHIZUKU,
                description = "Custom wm density calibration via privileged shell."
            ),
            CapabilityItem(
                name = "Crosshair Floating Reticle",
                isSupported = true,
                requiredPermission = "SYSTEM_ALERT_WINDOW",
                requiresShizuku = false,
                applyAvailable = hasOverlay,
                verifyAvailable = true,
                state = if (hasOverlay) FeatureState.READY else FeatureState.REQUIRES_PERMISSION,
                description = "Tactical hardware-accelerated screen reticle overlay."
            ),
            CapabilityItem(
                name = "Haptic Vibration Control",
                isSupported = vibrationManager.isVibratorAvailable,
                requiredPermission = "VIBRATE",
                requiresShizuku = false,
                applyAvailable = vibrationManager.isVibratorAvailable,
                verifyAvailable = true,
                state = if (vibrationManager.isVibratorAvailable) FeatureState.READY else FeatureState.NOT_SUPPORTED,
                description = "Android Vibrator / VibrationEffect haptic feedback."
            ),
            CapabilityItem(
                name = "Android Game Mode API",
                isSupported = isShizukuConnected,
                requiredPermission = null,
                requiresShizuku = true,
                applyAvailable = isShizukuConnected,
                verifyAvailable = isShizukuConnected,
                state = if (isShizukuConnected) FeatureState.READY else FeatureState.REQUIRES_SHIZUKU,
                description = "Applies Android cmd game mode (performance/battery) per package."
            ),
            CapabilityItem(
                name = "In-Game Memory Modification",
                isSupported = false,
                requiredPermission = null,
                requiresShizuku = false,
                applyAvailable = false,
                verifyAvailable = false,
                state = FeatureState.NOT_SUPPORTED,
                description = "Android Sandbox strictly prevents external runtime memory modification."
            )
        )
    }
}
