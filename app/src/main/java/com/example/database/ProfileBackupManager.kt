package com.example.database

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

class ProfileBackupManager(private val repository: AppRepository) {

    companion object {
        private const val TAG = "ProfileBackupManager"
    }

    suspend fun exportBackupJson(
        profiles: List<GameProfileEntity>,
        globalConfig: GlobalConfigEntity?,
        presets: List<CrosshairPresetEntity>
    ): String {
        val root = JSONObject()
        root.put("version", 3)
        root.put("timestamp", System.currentTimeMillis())
        root.put("appName", "GAMESLABS")

        // Global Config
        val globalObj = JSONObject()
        if (globalConfig != null) {
            globalObj.put("sensitivityMode", globalConfig.sensitivityMode)
            globalObj.put("executionMode", globalConfig.executionMode)
            globalObj.put("globalX", globalConfig.globalX)
            globalObj.put("globalY", globalConfig.globalY)
            globalObj.put("globalSmoothness", globalConfig.globalSmoothness)
            globalObj.put("globalResponse", globalConfig.globalResponse)
            globalObj.put("globalAcceleration", globalConfig.globalAcceleration)
            globalObj.put("globalDeadzone", globalConfig.globalDeadzone)
            globalObj.put("globalCurve", globalConfig.globalCurve)
            globalObj.put("vibrationEnabled", globalConfig.vibrationEnabled)
            globalObj.put("vibrationIntensity", globalConfig.vibrationIntensity)
            globalObj.put("vibrationProfile", globalConfig.vibrationProfile)
        }
        root.put("globalConfig", globalObj)

        // Game Profiles
        val profilesArr = JSONArray()
        profiles.forEach { p ->
            val pObj = JSONObject()
            pObj.put("packageName", p.packageName)
            pObj.put("displayName", p.displayName)
            pObj.put("xSensitivity", p.xSensitivity)
            pObj.put("ySensitivity", p.ySensitivity)
            pObj.put("dragResponse", p.dragResponse)
            pObj.put("dragSmoothness", p.dragSmoothness)
            pObj.put("acceleration", p.acceleration)
            pObj.put("deadzonePx", p.deadzonePx)
            pObj.put("responseCurve", p.responseCurve)
            pObj.put("pointerSpeed", p.pointerSpeed)
            pObj.put("targetDpi", p.targetDpi)
            pObj.put("gameMode", p.gameMode)
            pObj.put("vibrationIntensity", p.vibrationIntensity)
            pObj.put("vibrationProfile", p.vibrationProfile)
            pObj.put("isCustom", p.isCustom)
            profilesArr.put(pObj)
        }
        root.put("gameProfiles", profilesArr)

        // Crosshairs
        val crosshairsArr = JSONArray()
        presets.forEach { c ->
            val cObj = JSONObject()
            cObj.put("presetName", c.presetName)
            cObj.put("style", c.style)
            cObj.put("colorHex", c.colorHex)
            cObj.put("sizeDp", c.sizeDp)
            cObj.put("thicknessDp", c.thicknessDp)
            cObj.put("gapDp", c.gapDp)
            cObj.put("opacity", c.opacity)
            cObj.put("isSystemPreset", c.isSystemPreset)
            crosshairsArr.put(cObj)
        }
        root.put("crosshairPresets", crosshairsArr)

        return root.toString(2)
    }

    suspend fun importBackupJson(jsonString: String): Result<Int> {
        return try {
            val root = JSONObject(jsonString)

            var importedCount = 0

            // Import Global Config
            if (root.has("globalConfig")) {
                val gObj = root.getJSONObject("globalConfig")
                val gConfig = GlobalConfigEntity(
                    id = 1,
                    sensitivityMode = gObj.optString("sensitivityMode", "GLOBAL"),
                    executionMode = gObj.optString("executionMode", "SIMULATION"),
                    globalX = gObj.optDouble("globalX", 1.90).toFloat(),
                    globalY = gObj.optDouble("globalY", 2.35).toFloat(),
                    globalSmoothness = gObj.optDouble("globalSmoothness", 0.65).toFloat(),
                    globalResponse = gObj.optDouble("globalResponse", 1.10).toFloat(),
                    globalAcceleration = gObj.optDouble("globalAcceleration", 0.30).toFloat(),
                    globalDeadzone = gObj.optDouble("globalDeadzone", 2.0).toFloat(),
                    globalCurve = gObj.optString("globalCurve", "DYNAMIC_S"),
                    vibrationEnabled = gObj.optBoolean("vibrationEnabled", true),
                    vibrationIntensity = gObj.optInt("vibrationIntensity", 180),
                    vibrationProfile = gObj.optString("vibrationProfile", "CLICK")
                )
                repository.saveGlobalConfig(gConfig)
                importedCount++
            }

            // Import Profiles
            if (root.has("gameProfiles")) {
                val pArr = root.getJSONArray("gameProfiles")
                for (i in 0 until pArr.length()) {
                    val pObj = pArr.getJSONObject(i)
                    val profile = GameProfileEntity(
                        packageName = pObj.optString("packageName", "custom.game.$i"),
                        displayName = pObj.optString("displayName", "Imported Game"),
                        xSensitivity = pObj.optDouble("xSensitivity", 1.0).toFloat(),
                        ySensitivity = pObj.optDouble("ySensitivity", 1.15).toFloat(),
                        dragResponse = pObj.optDouble("dragResponse", 1.0).toFloat(),
                        dragSmoothness = pObj.optDouble("dragSmoothness", 0.65).toFloat(),
                        acceleration = pObj.optDouble("acceleration", 0.30).toFloat(),
                        deadzonePx = pObj.optDouble("deadzonePx", 2.0).toFloat(),
                        responseCurve = pObj.optString("responseCurve", "DYNAMIC_S"),
                        pointerSpeed = pObj.optInt("pointerSpeed", 2),
                        targetDpi = pObj.optInt("targetDpi", 420),
                        gameMode = pObj.optString("gameMode", "BALANCED"),
                        vibrationIntensity = pObj.optInt("vibrationIntensity", 180),
                        vibrationProfile = pObj.optString("vibrationProfile", "CLICK"),
                        isCustom = pObj.optBoolean("isCustom", true)
                    )
                    repository.insertProfile(profile)
                    importedCount++
                }
            }

            // Import Crosshairs
            if (root.has("crosshairPresets")) {
                val cArr = root.getJSONArray("crosshairPresets")
                for (i in 0 until cArr.length()) {
                    val cObj = cArr.getJSONObject(i)
                    val preset = CrosshairPresetEntity(
                        presetName = cObj.optString("presetName", "Custom Reticle"),
                        style = cObj.optString("style", "GAP_CROSS"),
                        colorHex = cObj.optLong("colorHex", 0xFF00F0FF),
                        sizeDp = cObj.optDouble("sizeDp", 24.0).toFloat(),
                        thicknessDp = cObj.optDouble("thicknessDp", 2.5).toFloat(),
                        gapDp = cObj.optDouble("gapDp", 6.0).toFloat(),
                        opacity = cObj.optDouble("opacity", 0.95).toFloat(),
                        isSystemPreset = false
                    )
                    repository.insertPreset(preset)
                    importedCount++
                }
            }

            Result.success(importedCount)
        } catch (e: Throwable) {
            Log.e(TAG, "Import error: ${e.message}", e)
            Result.failure(e)
        }
    }
}
