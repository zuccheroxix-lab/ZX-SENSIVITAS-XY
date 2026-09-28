package com.example

import androidx.compose.ui.geometry.Offset
import com.example.hardware.DragCalibrationResult
import com.example.hardware.TouchMetricsEngine
import com.example.hardware.XYResponseEngine
import com.example.model.*
import org.junit.Assert.*
import org.junit.Test

class ZxOptimizerUnitTest {

    @Test
    fun testSensitivityRatioCalculation() {
        val config = SensitivityConfig(
            xSensitivity = 1.00f,
            ySensitivity = 1.25f
        )
        assertEquals(1.25f, config.xyRatio, 0.01f)
        assertEquals(1.00f, config.xSensitivity, 0.01f)
        assertEquals(1.25f, config.ySensitivity, 0.01f)
    }

    @Test
    fun testXYResponseEngineDeadzoneAndIndependentAxis() {
        val engine = XYResponseEngine()
        val config = SensitivityConfig(
            xSensitivity = 1.00f,
            ySensitivity = 1.50f,
            dragResponse = 1.00f,
            smoothness = 0.00f, // Instantaneous for testing raw output
            acceleration = 0.00f,
            deadzonePx = 5.0f,
            responseCurve = ResponseCurveType.LINEAR
        )

        // Initial down
        engine.onTouchDown(Offset(100f, 100f), 1000L)

        // Move 1: dx = 3, dy = 4 (magnitude < 5px deadzone)
        val deadzoneMove = engine.onTouchMove(Offset(103f, 104f), 1016L, config)
        // With deadzone 5px, effective movement should be suppressed
        assertEquals(100f, deadzoneMove.processed.x, 0.5f)
        assertEquals(100f, deadzoneMove.processed.y, 0.5f)

        // Move 2: Large movement dx = 20, dy = 30
        val largeMove = engine.onTouchMove(Offset(120f, 130f), 1032L, config)
        // Y should have 1.5x scaling compared to X
        assertTrue(largeMove.processed.y > largeMove.processed.x)
    }

    @Test
    fun testPresetsValues() {
        val precision = SensitivityPreset.PRECISION
        val fastDrag = SensitivityPreset.FAST_DRAG

        assertTrue(precision.smoothness > fastDrag.smoothness)
        assertTrue(fastDrag.acceleration > precision.acceleration)
        assertTrue(fastDrag.xSensitivity > precision.xSensitivity)
        assertEquals(ResponseCurveType.PRECISE, precision.responseCurve)
        assertEquals(ResponseCurveType.AGGRESSIVE, fastDrag.responseCurve)
    }

    @Test
    fun testMemoryDataCalculation() {
        val memory = MemoryData(
            availableBytes = 2L * 1024 * 1024 * 1024, // 2 GB
            totalBytes = 8L * 1024 * 1024 * 1024      // 8 GB
        )
        assertEquals(6L * 1024 * 1024 * 1024, memory.usedBytes)
        assertEquals(75, memory.usedPercentage)
        assertEquals(2048L, memory.availableMb)
        assertEquals(8192L, memory.totalMb)
    }

    @Test
    fun testStorageDataCalculation() {
        val storage = StorageData(
            availableBytes = 64L * 1024 * 1024 * 1024,
            totalBytes = 128L * 1024 * 1024 * 1024
        )
        assertEquals(50, storage.usedPercentage)
        assertEquals(64f, storage.availableGb, 0.1f)
        assertEquals(128f, storage.totalGb, 0.1f)
    }

    @Test
    fun testTouchMetricsEngine() {
        val engine = TouchMetricsEngine()
        val initial = engine.onTouchDown(100f, 100f, 1000L)
        assertTrue(initial.statusText.contains("Touch registered"))

        // Simulate 120Hz touch events (8.3ms apart)
        var result = initial
        for (i in 1..20) {
            val t = 1000L + (i * 8)
            result = engine.onTouchMove(100f + i * 5, 100f + i * 5, t)
        }
        assertTrue(result.samplingRateHz > 60f)
        assertTrue(result.dragDistancePx > 0f)
    }

    @Test
    fun testPerformanceModes() {
        val balanced = PerformanceMode.BALANCED
        val extreme = PerformanceMode.PERFORMANCE
        val saver = PerformanceMode.BATTERY_SAVER

        assertEquals("Balanced", balanced.title)
        assertEquals("Extreme Performance", extreme.title)
        assertEquals("Battery Endurance", saver.title)
    }
}
