package com.example.hardware

import androidx.compose.ui.geometry.Offset
import com.example.model.DragTestMode
import com.example.model.ResponseCurveType
import com.example.model.SensitivityConfig
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sqrt

data class EnginePoint(
    val raw: Offset,
    val processed: Offset,
    val velocityPxPerMs: Float,
    val timestamp: Long
)

data class DragCalibrationResult(
    val samplesRecorded: Int = 0,
    val averageSamplingHz: Float = 0f,
    val averageVelocityPxPerMs: Float = 0f,
    val peakVelocityPxPerMs: Float = 0f,
    val rawTotalDistancePx: Float = 0f,
    val processedTotalDistancePx: Float = 0f,
    val measuredXtoYRatio: Float = 1f,
    val linearityRmsErrorPx: Float = 0f,
    val dragConsistencyPercent: Float = 0f,
    val testMode: DragTestMode = DragTestMode.FREE_DRAG,
    val statusSummary: String = "Perform touch drag to calibrate"
)

class XYResponseEngine {
    private var lastRawPoint: Offset? = null
    private var lastProcessedPoint: Offset? = null
    private var lastTimestamp: Long = 0L

    // Low-pass EMA filter state
    private var smoothedOutputX = 0f
    private var smoothedOutputY = 0f

    // Buffers for visual representation & stats
    val rawTrail = mutableListOf<Offset>()
    val processedTrail = mutableListOf<Offset>()
    val velocityHistory = mutableListOf<Float>()

    fun reset() {
        lastRawPoint = null
        lastProcessedPoint = null
        lastTimestamp = 0L
        smoothedOutputX = 0f
        smoothedOutputY = 0f
        rawTrail.clear()
        processedTrail.clear()
        velocityHistory.clear()
    }

    /**
     * Mathematical pipeline for each touch move event:
     *
     * 1. Raw Delta: dx = x(t) - x(t-1), dy = y(t) - y(t-1)
     * 2. Deadzone Filter: Reduces micro-finger jitter below noise threshold
     * 3. Velocity & Acceleration: Computes instantaneous speed (px/ms) and velocity-based scale
     * 4. Response Curve: Non-linear response transfer function (Linear, Precise, Dynamic S, Aggressive, Exp)
     * 5. Independent Axis Multipliers:
     *      outDx = curvedDx * config.xSensitivity * config.dragResponse * accelMultiplier
     *      outDy = curvedDy * config.ySensitivity * config.dragResponse * accelMultiplier
     * 6. Smoothing: Low-pass EMA filter to eliminate hardware digitizer jaggedness
     */
    fun onTouchDown(point: Offset, timestamp: Long): EnginePoint {
        reset()
        lastRawPoint = point
        lastProcessedPoint = point
        lastTimestamp = timestamp
        smoothedOutputX = point.x
        smoothedOutputY = point.y
        rawTrail.add(point)
        processedTrail.add(point)
        return EnginePoint(point, point, 0f, timestamp)
    }

    fun onTouchMove(
        rawPos: Offset,
        timestamp: Long,
        config: SensitivityConfig
    ): EnginePoint {
        val prevRaw = lastRawPoint
        val prevTime = lastTimestamp

        if (prevRaw == null || prevTime == 0L) {
            return onTouchDown(rawPos, timestamp)
        }

        val dt = (timestamp - prevTime).coerceAtLeast(1L).toFloat() // ms
        val rawDx = rawPos.x - prevRaw.x
        val rawDy = rawPos.y - prevRaw.y

        // 1. DEADZONE FILTER
        // Zeroes out minuscule trembling below configured pixel threshold
        val deadzone = config.deadzonePx
        val effectiveDx = if (abs(rawDx) <= deadzone) 0f else (rawDx - sign(rawDx) * deadzone)
        val effectiveDy = if (abs(rawDy) <= deadzone) 0f else (rawDy - sign(rawDy) * deadzone)

        // 2. VELOCITY & SPEED CALCULATION
        val moveDistance = sqrt(effectiveDx * effectiveDx + effectiveDy * effectiveDy)
        val velocityPxPerMs = moveDistance / dt // px/ms
        velocityHistory.add(velocityPxPerMs)
        if (velocityHistory.size > 80) velocityHistory.removeAt(0)

        // Acceleration boost: when swipe speed accelerates, response multiplies dynamically
        // Normalized against baseline 1.5 px/ms typical drag speed
        val normalizedSpeed = (velocityPxPerMs / 1.5f).coerceIn(0f, 3.5f)
        val accelMultiplier = 1.0f + (config.acceleration * normalizedSpeed)

        // 3. RESPONSE CURVE CALCULATION
        val curvedDx = computeCurveTransfer(effectiveDx, config.responseCurve)
        val curvedDy = computeCurveTransfer(effectiveDy, config.responseCurve)

        // 4. INDEPENDENT AXIS MULTIPLIERS (HORIZONTAL X vs VERTICAL Y)
        val scaledDx = curvedDx * config.xSensitivity * config.dragResponse * accelMultiplier
        val scaledDy = curvedDy * config.ySensitivity * config.dragResponse * accelMultiplier

        // 5. SMOOTHING (Low-Pass Filter)
        // Smoothness 0.00 -> alpha = 1.0 (pure instantaneous raw output)
        // Smoothness 1.00 -> alpha = 0.15 (heavy lag-free low-pass filter)
        val alpha = (1.0f - (config.smoothness * 0.85f)).coerceIn(0.12f, 1.0f)

        smoothedOutputX += scaledDx * alpha
        smoothedOutputY += scaledDy * alpha

        val processedPos = Offset(smoothedOutputX, smoothedOutputY)

        lastRawPoint = rawPos
        lastProcessedPoint = processedPos
        lastTimestamp = timestamp

        rawTrail.add(rawPos)
        processedTrail.add(processedPos)
        if (rawTrail.size > 80) {
            rawTrail.removeAt(0)
            processedTrail.removeAt(0)
        }

        return EnginePoint(rawPos, processedPos, velocityPxPerMs, timestamp)
    }

    private fun computeCurveTransfer(delta: Float, curve: ResponseCurveType): Float {
        if (delta == 0f) return 0f
        val sign = sign(delta)
        val mag = abs(delta)

        return when (curve) {
            ResponseCurveType.LINEAR -> delta
            ResponseCurveType.PRECISE -> {
                // Progressive: fine control at small distances (< 10px), standard beyond
                val norm = mag / 12f
                val transformed = norm.pow(1.30f) * 12f
                sign * transformed
            }
            ResponseCurveType.SMOOTH_EXP -> {
                // Exponential ramp
                val norm = mag / 10f
                val transformed = norm.pow(1.22f) * 10f
                sign * transformed
            }
            ResponseCurveType.DYNAMIC_S -> {
                // Smoothstep S-curve: flat near zero, linear mid-range, gentle roll-off
                val x = (mag / 25f).coerceIn(0f, 2.0f)
                val sVal = (3f * x.pow(2) - 2f * x.pow(3)) * 0.45f + (x * 0.55f)
                sign * (sVal * 25f)
            }
            ResponseCurveType.AGGRESSIVE -> {
                // Snappy start for fast snap turns
                val norm = mag / 8f
                val transformed = norm.pow(0.85f) * 8f
                sign * transformed
            }
        }
    }

    fun calculateCalibrationStats(testMode: DragTestMode): DragCalibrationResult {
        if (rawTrail.size < 3) {
            return DragCalibrationResult(
                statusSummary = "Swipe across the calibration area to gather data."
            )
        }

        val count = rawTrail.size
        var rawDist = 0f
        var processedDist = 0f
        var totalDx = 0f
        var totalDy = 0f

        for (i in 1 until count) {
            val rDx = rawTrail[i].x - rawTrail[i - 1].x
            val rDy = rawTrail[i].y - rawTrail[i - 1].y
            rawDist += sqrt(rDx * rDx + rDy * rDy)
            totalDx += abs(rDx)
            totalDy += abs(rDy)

            val pDx = processedTrail[i].x - processedTrail[i - 1].x
            val pDy = processedTrail[i].y - processedTrail[i - 1].y
            processedDist += sqrt(pDx * pDx + pDy * pDy)
        }

        val avgVelocity = if (velocityHistory.isNotEmpty()) velocityHistory.average().toFloat() else 0f
        val peakVelocity = velocityHistory.maxOrNull() ?: 0f

        val ratio = if (totalDx > 0f) totalDy / totalDx else 1f

        // Linearity calculation (Least squares regression deviation)
        var linearityRms = 0f
        if (rawTrail.size >= 4) {
            val meanX = rawTrail.map { it.x }.average().toFloat()
            val meanY = rawTrail.map { it.y }.average().toFloat()
            var num = 0f
            var den = 0f
            for (p in rawTrail) {
                num += (p.x - meanX) * (p.y - meanY)
                den += (p.x - meanX).pow(2)
            }
            if (den != 0f) {
                val slope = num / den
                val intercept = meanY - slope * meanX
                var sumErrSq = 0f
                for (p in rawTrail) {
                    val predictedY = slope * p.x + intercept
                    sumErrSq += (p.y - predictedY).pow(2)
                }
                linearityRms = sqrt(sumErrSq / count)
            }
        }

        // Consistency score: between 0% and 100%
        val consistency = (100f - (linearityRms * 8f)).coerceIn(40f, 99.5f)

        val summary = when (testMode) {
            DragTestMode.HORIZONTAL -> "Horizontal Swipe | X-Response Active | Linearity: ${"%.1f".format(linearityRms)}px"
            DragTestMode.VERTICAL -> "Vertical Headshot Drag | Y-Response Multiplier: ${"%.2f".format(ratio)}x"
            DragTestMode.DIAGONAL -> "Diagonal Swipe | Cross-Axis Tracking Ratio: 1:${"%.2f".format(ratio)}"
            DragTestMode.FREE_DRAG -> "Free Drag | Speed: ${"%.2f".format(avgVelocity)} px/ms | Consistency: ${"%.0f".format(consistency)}%"
        }

        return DragCalibrationResult(
            samplesRecorded = count,
            averageVelocityPxPerMs = avgVelocity,
            peakVelocityPxPerMs = peakVelocity,
            rawTotalDistancePx = rawDist,
            processedTotalDistancePx = processedDist,
            measuredXtoYRatio = ratio,
            linearityRmsErrorPx = linearityRms,
            dragConsistencyPercent = consistency,
            testMode = testMode,
            statusSummary = summary
        )
    }
}
