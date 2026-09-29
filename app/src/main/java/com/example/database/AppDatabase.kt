package com.example.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        GameProfileEntity::class,
        CrosshairPresetEntity::class,
        ActionHistoryEntity::class,
        GlobalConfigEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gameProfileDao(): GameProfileDao
    abstract fun crosshairPresetDao(): CrosshairPresetDao
    abstract fun actionHistoryDao(): ActionHistoryDao
    abstract fun globalConfigDao(): GlobalConfigDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "zx_optimizer_database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        seedInitialData(database)
                    }
                }
            }
        }

        suspend fun seedInitialData(database: AppDatabase) {
            val globalConfig = GlobalConfigEntity(
                id = 1,
                sensitivityMode = "GLOBAL",
                executionMode = "SIMULATION",
                globalX = 1.90f,
                globalY = 2.35f,
                globalSmoothness = 0.65f,
                globalResponse = 1.10f,
                globalAcceleration = 0.30f,
                globalDeadzone = 2.0f,
                globalCurve = "DYNAMIC_S",
                vibrationEnabled = true,
                vibrationIntensity = 180,
                vibrationProfile = "CLICK",
                previousPointerSpeed = 0,
                previousDpi = 0
            )
            database.globalConfigDao().insertOrUpdate(globalConfig)

            val defaultGames = listOf(
                GameProfileEntity(
                    packageName = "com.dts.freefireth",
                    displayName = "Free Fire (Standard)",
                    xSensitivity = 1.90f,
                    ySensitivity = 2.35f,
                    dragResponse = 1.15f,
                    dragSmoothness = 0.60f,
                    acceleration = 0.40f,
                    deadzonePx = 2.0f,
                    responseCurve = "DYNAMIC_S",
                    aimStability = 85f,
                    touchResponse = 90f,
                    pointerSpeed = 3,
                    targetDpi = 440,
                    gameMode = "PERFORMANCE",
                    vibrationIntensity = 180,
                    vibrationProfile = "CLICK",
                    isCustom = false,
                    isDefaultSelected = true
                ),
                GameProfileEntity(
                    packageName = "com.dts.freefiremax",
                    displayName = "Free Fire MAX",
                    xSensitivity = 1.95f,
                    ySensitivity = 2.40f,
                    dragResponse = 1.15f,
                    dragSmoothness = 0.55f,
                    acceleration = 0.45f,
                    deadzonePx = 1.5f,
                    responseCurve = "DYNAMIC_S",
                    aimStability = 85f,
                    touchResponse = 92f,
                    pointerSpeed = 3,
                    targetDpi = 440,
                    gameMode = "PERFORMANCE",
                    vibrationIntensity = 180,
                    vibrationProfile = "CLICK",
                    isCustom = false,
                    isDefaultSelected = false
                ),
                GameProfileEntity(
                    packageName = "com.mobile.legends",
                    displayName = "Mobile Legends: Bang Bang",
                    xSensitivity = 1.70f,
                    ySensitivity = 2.10f,
                    dragResponse = 1.05f,
                    dragSmoothness = 0.65f,
                    acceleration = 0.20f,
                    deadzonePx = 2.0f,
                    responseCurve = "LINEAR",
                    aimStability = 78f,
                    touchResponse = 85f,
                    pointerSpeed = 1,
                    targetDpi = 400,
                    gameMode = "BALANCED",
                    vibrationIntensity = 160,
                    vibrationProfile = "TICK",
                    isCustom = false,
                    isDefaultSelected = false
                ),
                GameProfileEntity(
                    packageName = "com.tencent.ig",
                    displayName = "PUBG Mobile",
                    xSensitivity = 1.50f,
                    ySensitivity = 1.80f,
                    dragResponse = 1.00f,
                    dragSmoothness = 0.75f,
                    acceleration = 0.15f,
                    deadzonePx = 3.0f,
                    responseCurve = "PRECISE",
                    aimStability = 88f,
                    touchResponse = 80f,
                    pointerSpeed = 2,
                    targetDpi = 420,
                    gameMode = "PERFORMANCE",
                    vibrationIntensity = 200,
                    vibrationProfile = "HEAVY_CLICK",
                    isCustom = false,
                    isDefaultSelected = false
                ),
                GameProfileEntity(
                    packageName = "com.roblox.client",
                    displayName = "Roblox",
                    xSensitivity = 1.60f,
                    ySensitivity = 1.90f,
                    dragResponse = 1.10f,
                    dragSmoothness = 0.60f,
                    acceleration = 0.35f,
                    deadzonePx = 2.0f,
                    responseCurve = "DYNAMIC_S",
                    aimStability = 80f,
                    touchResponse = 85f,
                    pointerSpeed = 2,
                    targetDpi = 420,
                    gameMode = "BALANCED",
                    vibrationIntensity = 150,
                    vibrationProfile = "CLICK",
                    isCustom = false,
                    isDefaultSelected = false
                ),
                GameProfileEntity(
                    packageName = "com.activision.callofduty.shooter",
                    displayName = "Call of Duty: Mobile",
                    xSensitivity = 1.65f,
                    ySensitivity = 1.95f,
                    dragResponse = 1.05f,
                    dragSmoothness = 0.70f,
                    acceleration = 0.25f,
                    deadzonePx = 2.0f,
                    responseCurve = "SMOOTH_EXP",
                    aimStability = 82f,
                    touchResponse = 85f,
                    pointerSpeed = 2,
                    targetDpi = 420,
                    gameMode = "PERFORMANCE",
                    vibrationIntensity = 190,
                    vibrationProfile = "DOUBLE_CLICK",
                    isCustom = false,
                    isDefaultSelected = false
                )
            )
            database.gameProfileDao().insertAll(defaultGames)

            val defaultPresets = listOf(
                CrosshairPresetEntity(
                    presetName = "Cyber Cyan Gap Cross",
                    style = "GAP_CROSS",
                    colorHex = 0xFF00F0FF,
                    sizeDp = 24f,
                    thicknessDp = 2.5f,
                    gapDp = 6f,
                    opacity = 0.95f,
                    isSystemPreset = true
                ),
                CrosshairPresetEntity(
                    presetName = "Emerald Headshot Dot",
                    style = "DOT",
                    colorHex = 0xFF00E676,
                    sizeDp = 10f,
                    thicknessDp = 4f,
                    gapDp = 0f,
                    opacity = 1f,
                    isSystemPreset = true
                ),
                CrosshairPresetEntity(
                    presetName = "Tactical Red T",
                    style = "T_SHAPE",
                    colorHex = 0xFFFF3366,
                    sizeDp = 22f,
                    thicknessDp = 2.2f,
                    gapDp = 5f,
                    opacity = 0.9f,
                    isSystemPreset = true
                ),
                CrosshairPresetEntity(
                    presetName = "Neon Amber Circle",
                    style = "CIRCLE_DOT",
                    colorHex = 0xFFFFB800,
                    sizeDp = 28f,
                    thicknessDp = 2f,
                    gapDp = 4f,
                    opacity = 0.9f,
                    isSystemPreset = true
                )
            )
            database.crosshairPresetDao().insertAll(defaultPresets)
        }
    }
}
