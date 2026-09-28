package com.example.hardware

import android.view.MotionEvent
import androidx.compose.ui.geometry.Offset
import kotlin.math.pow
import kotlin.math.sqrt

data class TouchLiveMetrics(
    val samplingRateHz: Float = 0f,
    val averageIntervalMs: Float = 0f,
    val jitterRmsPx: Float = 0f,
    val dragDistancePx: Float = 0f,
    val samplesRecorded: Int = 0,
    val statusText: String = "Drag on canvas to test touch digitizer"
)

class TouchMetricsEngine {
    private val timestamps = mutableListOf<Long>()
    private val points = mutableListOf<Offset>()
    private var downTime = 0L

    fun onTouchDown(x: Float, y: Float, eventTime: Long): TouchLiveMetrics {
        timestamps.clear()
        points.clear()
        downTime = eventTime
        timestamps.add(eventTime)
        points.add(Offset(x, y))
        return TouchLiveMetrics(
            statusText = "Touch registered. Continue dragging..."
        )
    }

    fun onTouchMove(x: Float, y: Float, eventTime: Long): TouchLiveMetrics {
        timestamps.add(eventTime)
        points.add(Offset(x, y))

        // Keep rolling buffer of last 60 samples
        if (timestamps.size > 60) {
            timestamps.removeAt(0)
            points.removeAt(0)
        }

        if (timestamps.size < 3) {
            return TouchLiveMetrics(statusText = "Gathering digitizer samples...")
        }

        // 1. Calculate touch sampling rate (Hz) from intervals
        var totalIntervalMs = 0L
        var validIntervals = 0
        for (i in 1 until timestamps.size) {
            val delta = timestamps[i] - timestamps[i - 1]
            if (delta in 1..100) { // filter out stalled frames
                totalIntervalMs += delta
                validIntervals++
            }
        }

        val avgIntervalMs = if (validIntervals > 0) totalIntervalMs.toFloat() / validIntervals else 16.6f
        val samplingHz = if (avgIntervalMs > 0) (1000f / avgIntervalMs).coerceIn(30f, 1000f) else 0f

        // 2. Calculate Jitter / Linearity using Least-Squares best fit line
        var jitterRms = 0f
        if (points.size >= 5) {
            val n = points.size.toFloat()
            var sumX = 0f
            var sumY = 0f
            for (p in points) {
                sumX += p.x
                sumY += p.y
            }
            val meanX = sumX / n
            val meanY = sumY / n

            var num = 0f
            var den = 0f
            for (p in points) {
                num += (p.x - meanX) * (p.y - meanY)
                den += (p.x - meanX).pow(2)
            }

            if (den != 0f) {
                val slope = num / den
                val intercept = meanY - slope * meanX

                var sumSquaredError = 0f
                for (p in points) {
                    val predictedY = slope * p.x + intercept
                    sumSquaredError += (p.y - predictedY).pow(2)
                }
                jitterRms = sqrt(sumSquaredError / n)
            }
        }

        // 3. Total path distance
        var totalDist = 0f
        for (i in 1 until points.size) {
            val dx = points[i].x - points[i - 1].x
            val dy = points[i].y - points[i - 1].y
            totalDist += sqrt(dx * dx + dy * dy)
        }

        val quality = when {
            samplingHz >= 240f -> "Ultra Low Latency (Gaming Grade $samplingHz Hz)"
            samplingHz >= 120f -> "High Speed (${samplingHz.toInt()} Hz Digitizer)"
            samplingHz >= 90f -> "Standard Smooth (${samplingHz.toInt()} Hz)"
            else -> "Standard (${samplingHz.toInt()} Hz)"
        }

        return TouchLiveMetrics(
            samplingRateHz = samplingHz,
            averageIntervalMs = avgIntervalMs,
            jitterRmsPx = jitterRms,
            dragDistancePx = totalDist,
            samplesRecorded = points.size,
            statusText = "$quality | Jitter: ${"%.2f".format(jitterRms)}px"
        )
    }

    fun onTouchUp(): TouchLiveMetrics {
        return TouchLiveMetrics(
            statusText = "Stroke finished. Touch again to measure."
        )
    }
}
