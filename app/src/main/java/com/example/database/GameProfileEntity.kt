package com.example.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_profiles")
data class GameProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val displayName: String,
    val xSensitivity: Float = 1.00f,
    val ySensitivity: Float = 1.15f,
    val dragResponse: Float = 1.00f,
    val dragSmoothness: Float = 0.65f,
    val acceleration: Float = 0.30f,
    val deadzonePx: Float = 2.0f,
    val responseCurve: String = "DYNAMIC_S",
    val aimStability: Float = 80f,
    val touchResponse: Float = 85f,
    val pointerSpeed: Int = 2,
    val targetDpi: Int = 420,
    val gameMode: String = "PERFORMANCE",
    val isCustom: Boolean = false,
    val isDefaultSelected: Boolean = false
) {
    val xyRatio: Float
        get() = if (xSensitivity > 0f) ySensitivity / xSensitivity else 1f
}
