package com.example.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "crosshair_presets")
data class CrosshairPresetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val presetName: String,
    val style: String = "GAP_CROSS",
    val colorHex: Long = 0xFF00F0FF,
    val sizeDp: Float = 26f,
    val thicknessDp: Float = 2.5f,
    val gapDp: Float = 6f,
    val opacity: Float = 0.95f,
    val offsetX: Int = 0,
    val offsetY: Int = 0,
    val customImageUri: String? = null,
    val isSystemPreset: Boolean = false
)
