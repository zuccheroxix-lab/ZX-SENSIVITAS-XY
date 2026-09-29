package com.example.model

enum class ResponseCurveType(val displayName: String, val shortDesc: String) {
    LINEAR("Linear (1:1)", "Direct 1:1 input to output mapping without acceleration curvature"),
    PRECISE("Precise Aim", "Sub-linear initial control for micro-aiming, progressive ramp for swipes"),
    SMOOTH_EXP("Exponential", "Curved acceleration curve for fast target acquisition"),
    DYNAMIC_S("Dynamic S-Curve", "Suppressed jitter at micro-swipes, steep mid-range drag response"),
    AGGRESSIVE("Aggressive / Snappy", "Instantaneous initial response for rapid 180° camera turns")
}

enum class DragTestMode(val displayName: String, val instruction: String) {
    HORIZONTAL("HORIZONTAL (X)", "Swipe horizontally left and right to test X-axis tracking and stability"),
    VERTICAL("VERTICAL (Y)", "Swipe vertically up and down to calibrate headshot drag swipe feel"),
    DIAGONAL("DIAGONAL (X/Y)", "Swipe diagonally at 45° to verify cross-axis ratio consistency"),
    FREE_DRAG("FREE DRAG", "Freehand swipes to test overall engine responsiveness")
}

enum class ExecutionMode(val displayName: String, val badgeText: String, val description: String) {
    SIMULATION(
        displayName = "SIMULATION",
        badgeText = "NO SYSTEM CHANGES",
        description = "Simulation Mode: Validates requirements, calculates targets, and simulates expected outcomes without altering Android system or display settings."
    ),
    APPLY(
        displayName = "APPLY",
        badgeText = "REAL ACTION",
        description = "Apply Mode: Executes real actions (pointer speed, density, crosshair overlay, game profiles) with strict security verification."
    )
}

enum class SensitivityMode(val displayName: String, val description: String) {
    GLOBAL(
        displayName = "GLOBAL",
        description = "One default master profile applied across all games and drag testing."
    ),
    PER_GAME(
        displayName = "PER-GAME",
        description = "Dedicated independent sensitivity, crosshair, and vibration profile per game."
    )
}

enum class ActionStatus(val title: String) {
    SUCCESS("SUCCESS"),
    PARTIAL("PARTIAL"),
    FAILED("FAILED"),
    NOT_SUPPORTED("NOT SUPPORTED"),
    BLOCKED("BLOCKED")
}

enum class VerificationStatus(val title: String) {
    VERIFIED("VERIFIED"),
    PARTIALLY_VERIFIED("PARTIALLY VERIFIED"),
    FAILED_TO_VERIFY("FAILED TO VERIFY"),
    VERIFICATION_UNAVAILABLE("VERIFICATION UNAVAILABLE")
}

data class ActionResultData(
    val actionName: String,
    val targetName: String,
    val mode: ExecutionMode,
    val status: ActionStatus,
    val detail: String,
    val timestamp: Long = System.currentTimeMillis(),
    val verification: VerificationStatus = VerificationStatus.VERIFICATION_UNAVAILABLE
)

enum class SmartStutterStatus(val displayName: String, val desc: String) {
    STABLE("STABLE", "Hardware headroom and memory pressure optimal for smooth gaming"),
    WARNING("WARNING", "Moderate memory load or thermal elevation detected"),
    HIGH_LOAD("HIGH LOAD", "High resource consumption may cause occasional frame drops"),
    THERMAL_WARNING("THERMAL WARNING", "Device thermal throttling active; cooling recommended"),
    MEMORY_PRESSURE("MEMORY PRESSURE", "Available RAM critically low (< 15%)"),
    LIMITED_DATA("LIMITED DATA", "System telemetry restricted by OEM environment")
}

data class SmartStutterAnalysis(
    val status: SmartStutterStatus = SmartStutterStatus.STABLE,
    val memoryFreePercent: Float = 0f,
    val thermalLevel: Int = 0,
    val batteryTempCelsius: Float = 0f,
    val currentRefreshRateHz: Float = 60f,
    val recommendations: List<String> = emptyList()
)

enum class HapticProfileType(val displayName: String) {
    TICK("Gentle Tick"),
    CLICK("Standard Click"),
    HEAVY_CLICK("Heavy Feedback"),
    DOUBLE_CLICK("Double Click"),
    PULSE("Tactical Pulse")
}

data class VibrationConfig(
    val isEnabled: Boolean = true,
    val intensity: Int = 180, // Range 1 to 255
    val durationMs: Int = 50,  // Range 10 to 300ms
    val profile: HapticProfileType = HapticProfileType.CLICK
)

enum class FeatureState(val label: String) {
    AVAILABLE("AVAILABLE"),
    REQUIRES_PERMISSION("REQUIRES PERMISSION"),
    REQUIRES_SHIZUKU("REQUIRES SHIZUKU"),
    NOT_SUPPORTED("NOT SUPPORTED"),
    READY("READY"),
    ACTIVE("ACTIVE"),
    ERROR("ERROR")
}

data class CapabilityItem(
    val name: String,
    val isSupported: Boolean,
    val requiredPermission: String?,
    val requiresShizuku: Boolean,
    val applyAvailable: Boolean,
    val verifyAvailable: Boolean,
    val state: FeatureState,
    val description: String
)

enum class PreflightStatus {
    READY,
    PARTIAL,
    ACTION_REQUIRED,
    UNSUPPORTED
}

data class PreflightCheckItem(
    val title: String,
    val isPassed: Boolean,
    val detail: String
)

data class PreflightCheckResult(
    val status: PreflightStatus = PreflightStatus.READY,
    val items: List<PreflightCheckItem> = emptyList(),
    val timestamp: Long = 0L
)

enum class SensitivityPreset(
    val title: String,
    val xSensitivity: Float,
    val ySensitivity: Float,
    val dragResponse: Float,
    val smoothness: Float,
    val acceleration: Float,
    val deadzonePx: Float,
    val responseCurve: ResponseCurveType,
    val description: String
) {
    PRECISION(
        title = "PRECISION",
        xSensitivity = 0.85f,
        ySensitivity = 0.90f,
        dragResponse = 0.90f,
        smoothness = 0.75f,
        acceleration = 0.15f,
        deadzonePx = 3.0f,
        responseCurve = ResponseCurveType.PRECISE,
        description = "Fine-tuned for steady crosshair control & micro-aiming"
    ),
    SMOOTH(
        title = "SMOOTH",
        xSensitivity = 1.00f,
        ySensitivity = 1.05f,
        dragResponse = 1.00f,
        smoothness = 0.85f,
        acceleration = 0.20f,
        deadzonePx = 2.0f,
        responseCurve = ResponseCurveType.LINEAR,
        description = "Maximum jitter filtering with smooth linear transitions"
    ),
    BALANCED(
        title = "BALANCED",
        xSensitivity = 1.00f,
        ySensitivity = 1.15f,
        dragResponse = 1.00f,
        smoothness = 0.65f,
        acceleration = 0.30f,
        deadzonePx = 2.0f,
        responseCurve = ResponseCurveType.DYNAMIC_S,
        description = "Optimal standard for balanced camera rotation and vertical drag"
    ),
    FAST_DRAG(
        title = "FAST DRAG",
        xSensitivity = 1.35f,
        ySensitivity = 1.55f,
        dragResponse = 1.25f,
        smoothness = 0.45f,
        acceleration = 0.65f,
        deadzonePx = 1.0f,
        responseCurve = ResponseCurveType.AGGRESSIVE,
        description = "High velocity response for rapid target acquisition and jump shots"
    ),
    LOW_RESPONSE(
        title = "LOW RESPONSE",
        xSensitivity = 0.70f,
        ySensitivity = 0.75f,
        dragResponse = 0.80f,
        smoothness = 0.80f,
        acceleration = 0.10f,
        deadzonePx = 4.0f,
        responseCurve = ResponseCurveType.LINEAR,
        description = "Heavy recoil control and high resistance against accidental drift"
    ),
    HIGH_RESPONSE(
        title = "HIGH RESPONSE",
        xSensitivity = 1.50f,
        ySensitivity = 1.80f,
        dragResponse = 1.40f,
        smoothness = 0.35f,
        acceleration = 0.80f,
        deadzonePx = 1.0f,
        responseCurve = ResponseCurveType.SMOOTH_EXP,
        description = "Ultra snappy response for quick reflex swipe rotations"
    )
}

data class SensitivityConfig(
    val xSensitivity: Float = 1.00f,        // Range: 0.20x to 3.00x
    val ySensitivity: Float = 1.15f,        // Range: 0.20x to 3.00x
    val dragResponse: Float = 1.00f,        // Range: 0.50x to 2.00x
    val smoothness: Float = 0.65f,          // Range: 0.00x to 1.00x (0% to 100% filter)
    val acceleration: Float = 0.30f,        // Range: 0.00x to 2.00x
    val deadzonePx: Float = 2.0f,           // Range: 0.0f to 15.0f px
    val responseCurve: ResponseCurveType = ResponseCurveType.DYNAMIC_S,
    val systemPointerSpeed: Int = 2,        // Range: -7 to +7 (Android system)
    val targetDpi: Int? = null,
    val touchSamplingRateHz: Float = 0f,
    val jitterVariance: Float = 0f
) {
    val xyRatio: Float
        get() = if (xSensitivity > 0f) ySensitivity / xSensitivity else 1f
}

enum class EngineStatusCode {
    READY,
    APPLIED,
    UNSUPPORTED,
    ERROR
}

data class EngineStatusInfo(
    val code: EngineStatusCode = EngineStatusCode.READY,
    val title: String = "READY",
    val message: String = "X/Y Engine calibrated and ready for drag testing.",
    val technicalDetails: String? = null
)

sealed class ApplyResult {
    data class Success(
        val message: String,
        val verifiedPointerSpeed: Int? = null,
        val verifiedDpi: Int? = null,
        val isGameApiAvailable: Boolean = false
    ) : ApplyResult()

    data class Error(
        val reason: String,
        val suggestedAction: String? = null
    ) : ApplyResult()

    object Idle : ApplyResult()
}
