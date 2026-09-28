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
    entities = [GameProfileEntity::class, CrosshairPresetEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gameProfileDao(): GameProfileDao
    abstract fun crosshairPresetDao(): CrosshairPresetDao

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

        private suspend fun seedInitialData(database: AppDatabase) {
            val defaultGames = listOf(
                GameProfileEntity(
                    packageName = "com.dts.freefireth",
                    displayName = "Free Fire (Standard)",
                    xSensitivity = 1.10f,
                    ySensitivity = 1.35f,
                    dragResponse = 1.10f,
                    dragSmoothness = 0.60f,
                    acceleration = 0.40f,
                    deadzonePx = 2.0f,
                    responseCurve = "DYNAMIC_S",
                    aimStability = 85f,
                    touchResponse = 90f,
                    pointerSpeed = 3,
                    targetDpi = 440,
                    gameMode = "PERFORMANCE",
                    isCustom = false,
                    isDefaultSelected = true
                ),
                GameProfileEntity(
                    packageName = "com.dts.freefiremax",
                    displayName = "Free Fire MAX",
                    xSensitivity = 1.15f,
                    ySensitivity = 1.40f,
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
                    isCustom = false,
                    isDefaultSelected = false
                ),
                GameProfileEntity(
                    packageName = "com.tencent.ig",
                    displayName = "PUBG Mobile",
                    xSensitivity = 0.90f,
                    ySensitivity = 0.95f,
                    dragResponse = 0.95f,
                    dragSmoothness = 0.75f,
                    acceleration = 0.15f,
                    deadzonePx = 3.0f,
                    responseCurve = "PRECISE",
                    aimStability = 88f,
                    touchResponse = 80f,
                    pointerSpeed = 2,
                    targetDpi = 420,
                    gameMode = "PERFORMANCE",
                    isCustom = false,
                    isDefaultSelected = false
                ),
                GameProfileEntity(
                    packageName = "com.activision.callofduty.shooter",
                    displayName = "Call of Duty: Mobile",
                    xSensitivity = 1.00f,
                    ySensitivity = 1.05f,
                    dragResponse = 1.00f,
                    dragSmoothness = 0.70f,
                    acceleration = 0.25f,
                    deadzonePx = 2.0f,
                    responseCurve = "SMOOTH_EXP",
                    aimStability = 82f,
                    touchResponse = 85f,
                    pointerSpeed = 2,
                    targetDpi = 420,
                    gameMode = "PERFORMANCE",
                    isCustom = false,
                    isDefaultSelected = false
                ),
                GameProfileEntity(
                    packageName = "com.mobile.legends",
                    displayName = "Mobile Legends: Bang Bang",
                    xSensitivity = 1.00f,
                    ySensitivity = 1.00f,
                    dragResponse = 1.00f,
                    dragSmoothness = 0.65f,
                    acceleration = 0.20f,
                    deadzonePx = 2.0f,
                    responseCurve = "LINEAR",
                    aimStability = 75f,
                    touchResponse = 85f,
                    pointerSpeed = 1,
                    targetDpi = 400,
                    gameMode = "BALANCED",
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
