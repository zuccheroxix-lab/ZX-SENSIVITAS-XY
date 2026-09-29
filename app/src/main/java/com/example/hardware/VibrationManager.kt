package com.example.hardware

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.example.model.HapticProfileType
import com.example.model.VibrationConfig

class VibrationManager(private val context: Context) {

    companion object {
        private const val TAG = "VibrationManager"
    }

    private val vibrator: Vibrator? by lazy {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to obtain vibrator service: ${e.message}")
            null
        }
    }

    val isVibratorAvailable: Boolean
        get() = try {
            vibrator?.hasVibrator() == true
        } catch (e: Throwable) {
            false
        }

    val hasAmplitudeControl: Boolean
        get() = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.hasAmplitudeControl() == true
            } else {
                false
            }
        } catch (e: Throwable) {
            false
        }

    fun playHaptic(config: VibrationConfig): Boolean {
        if (!config.isEnabled || vibrator == null || !isVibratorAvailable) {
            return false
        }

        return try {
            val intensity = config.intensity.coerceIn(1, 255)
            val duration = config.durationMs.coerceIn(10, 500).toLong()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = when (config.profile) {
                    HapticProfileType.TICK -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                        } else {
                            VibrationEffect.createOneShot(20, (intensity * 0.4f).toInt().coerceIn(1, 255))
                        }
                    }
                    HapticProfileType.CLICK -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                        } else {
                            VibrationEffect.createOneShot(duration, intensity)
                        }
                    }
                    HapticProfileType.HEAVY_CLICK -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
                        } else {
                            VibrationEffect.createOneShot((duration * 1.5f).toLong(), intensity)
                        }
                    }
                    HapticProfileType.DOUBLE_CLICK -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK)
                        } else {
                            val timings = longArrayOf(0, 30, 60, 30)
                            val amplitudes = intArrayOf(0, intensity, 0, intensity)
                            VibrationEffect.createWaveform(timings, amplitudes, -1)
                        }
                    }
                    HapticProfileType.PULSE -> {
                        val timings = longArrayOf(0, duration / 2, 40, duration)
                        val amplitudes = intArrayOf(0, (intensity * 0.6f).toInt(), 0, intensity)
                        VibrationEffect.createWaveform(timings, amplitudes, -1)
                    }
                }
                vibrator?.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(duration)
            }
            true
        } catch (e: Throwable) {
            Log.w(TAG, "Error playing haptic: ${e.message}")
            false
        }
    }

    fun stop() {
        try {
            vibrator?.cancel()
        } catch (e: Throwable) {
            Log.w(TAG, "Error stopping vibrator: ${e.message}")
        }
    }
}
