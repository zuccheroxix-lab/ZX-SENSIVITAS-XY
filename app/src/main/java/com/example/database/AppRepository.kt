package com.example.database

import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AppRepository(
    private val gameProfileDao: GameProfileDao,
    private val crosshairPresetDao: CrosshairPresetDao,
    private val context: Context
) {
    val allGameProfiles: Flow<List<GameProfileEntity>> = gameProfileDao.getAllProfiles()
        .map { list ->
            // Update installed status dynamically
            list.map { entity ->
                entity
            }
        }

    fun isPackageInstalled(packageName: String): Boolean {
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(packageName, 0)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getProfileById(id: Long): GameProfileEntity? = gameProfileDao.getProfileById(id)

    suspend fun insertProfile(profile: GameProfileEntity): Long = gameProfileDao.insertProfile(profile)

    suspend fun updateProfile(profile: GameProfileEntity) = gameProfileDao.updateProfile(profile)

    suspend fun deleteProfile(profile: GameProfileEntity) = gameProfileDao.deleteProfile(profile)

    suspend fun setActiveProfile(id: Long) {
        gameProfileDao.clearActiveSelection()
        gameProfileDao.setActiveProfile(id)
    }

    val allCrosshairPresets: Flow<List<CrosshairPresetEntity>> = crosshairPresetDao.getAllPresets()

    suspend fun insertPreset(preset: CrosshairPresetEntity): Long = crosshairPresetDao.insertPreset(preset)

    suspend fun deletePreset(preset: CrosshairPresetEntity) = crosshairPresetDao.deletePreset(preset)
}
