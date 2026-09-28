package com.example.model

enum class PerformanceMode(val title: String, val description: String) {
    BALANCED(
        title = "Balanced",
        description = "System default pointer response and standard refresh rate for casual gaming & battery longevity."
    ),
    PERFORMANCE(
        title = "Extreme Performance",
        description = "Max pointer response (+5 speed), sets display to peak supported refresh rate, and boosts touch responsiveness."
    ),
    BATTERY_SAVER(
        title = "Battery Endurance",
        description = "Optimizes battery consumption by scaling refresh rate to 60Hz and trimming app memory caches."
    )
}

data class OptimizerState(
    val currentMode: PerformanceMode = PerformanceMode.BALANCED,
    val lastTrimmedTime: Long = 0L,
    val memoryFreedMb: Long = 0L,
    val actionStatus: String = "Ready"
)
