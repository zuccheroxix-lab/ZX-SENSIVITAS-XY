package com.example.model

data class BatteryData(
    val levelPercent: Int = 0,
    val isCharging: Boolean = false,
    val chargeSource: String = "Battery",
    val temperatureCelsius: Float = 0f,
    val voltageMillivolts: Int = 0,
    val health: String = "Good",
    val technology: String = "Li-ion",
    val currentNowMicroAmps: Long? = null
)

data class ThermalData(
    val statusString: String = "Normal",
    val severityLevel: Int = 0,
    val thermalHeadroom: Float? = null
)

data class MemoryData(
    val availableBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val isLowMemory: Boolean = false,
    val thresholdBytes: Long = 0L
) {
    val usedBytes: Long get() = totalBytes - availableBytes
    val usedPercentage: Int get() = if (totalBytes > 0) ((usedBytes * 100) / totalBytes).toInt() else 0
    val availableMb: Long get() = availableBytes / (1024 * 1024)
    val totalMb: Long get() = totalBytes / (1024 * 1024)
}

data class StorageData(
    val availableBytes: Long = 0L,
    val totalBytes: Long = 0L
) {
    val usedBytes: Long get() = totalBytes - availableBytes
    val usedPercentage: Int get() = if (totalBytes > 0) ((usedBytes * 100) / totalBytes).toInt() else 0
    val availableGb: Float get() = availableBytes / (1024f * 1024f * 1024f)
    val totalGb: Float get() = totalBytes / (1024f * 1024f * 1024f)
}

data class DisplayData(
    val physicalWidthPx: Int = 1080,
    val physicalHeightPx: Int = 2400,
    val refreshRateHz: Float = 60f,
    val supportedRefreshRates: List<Float> = listOf(60f),
    val densityDpi: Int = 420,
    val densityScale: Float = 2.625f
)

data class CpuData(
    val coresCount: Int = Runtime.getRuntime().availableProcessors(),
    val architecture: String = System.getProperty("os.arch") ?: "arm64",
    val socModel: String = "",
    val hardware: String = ""
)
