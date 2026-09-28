package com.example.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CrosshairPresetDao {
    @Query("SELECT * FROM crosshair_presets ORDER BY isSystemPreset DESC, id DESC")
    fun getAllPresets(): Flow<List<CrosshairPresetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: CrosshairPresetEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(presets: List<CrosshairPresetEntity>)

    @Delete
    suspend fun deletePreset(preset: CrosshairPresetEntity)
}
