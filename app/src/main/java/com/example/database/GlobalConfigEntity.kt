package com.example.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "global_config")
data class GlobalConfigEntity(
    @PrimaryKey
    val id: Int = 1,
    val sensitivityMode: String = "GLOBAL",
    val executionMode: String = "SIMULATION",
    val globalX: Float = 1.90f,
    val globalY: Float = 2.35f,
    val globalSmoothness: Float = 0.65f,
    val globalResponse: Float = 1.10f,
    val globalAcceleration: Float = 0.30f,
    val globalDeadzone: Float = 2.0f,
    val globalCurve: String = "DYNAMIC_S",
    val vibrationEnabled: Boolean = true,
    val vibrationIntensity: Int = 180,
    val vibrationProfile: String = "CLICK",
    val previousPointerSpeed: Int = 0,
    val previousDpi: Int = 0
)
