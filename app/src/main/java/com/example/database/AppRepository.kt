package com.example.database

import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.flow.Flow

class AppRepository(
    private val gameProfileDao: GameProfileDao,
    private val crosshairPresetDao: CrosshairPresetDao,
    private val actionHistoryDao: ActionHistoryDao,
    private val globalConfigDao: GlobalConfigDao,
    private val context: Context
) {
    val allGameProfiles: Flow<List<GameProfileEntity>> = gameProfileDao.getAllProfiles()

    fun isPackageInstalled(packageName: String): Boolean {
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(packageName, 0)
            }
            true
        } catch (e: Throwable) {
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

    // Crosshair Presets
    val allCrosshairPresets: Flow<List<CrosshairPresetEntity>> = crosshairPresetDao.getAllPresets()

    suspend fun insertPreset(preset: CrosshairPresetEntity): Long = crosshairPresetDao.insertPreset(preset)

    suspend fun deletePreset(preset: CrosshairPresetEntity) = crosshairPresetDao.deletePreset(preset)

    // Action History
    val allActionHistory: Flow<List<ActionHistoryEntity>> = actionHistoryDao.getAllHistory()

    suspend fun insertAction(action: ActionHistoryEntity): Long = actionHistoryDao.insertAction(action)

    suspend fun clearActionHistory() = actionHistoryDao.clearHistory()

    // Global Config
    val globalConfigFlow: Flow<GlobalConfigEntity?> = globalConfigDao.getGlobalConfigFlow()

    suspend fun getGlobalConfig(): GlobalConfigEntity? = globalConfigDao.getGlobalConfig()

    suspend fun saveGlobalConfig(config: GlobalConfigEntity) = globalConfigDao.insertOrUpdate(config)
}
