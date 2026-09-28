package com.example

import android.app.Application
import android.util.Log
import com.example.database.AppDatabase
import com.example.database.AppRepository
import com.example.hardware.HardwareMonitor
import com.example.shizuku.ShizukuManager

class ZxOptimizerApplication : Application() {

    companion object {
        private const val TAG = "ZxOptimizerApp"
        lateinit var instance: ZxOptimizerApplication
            private set
    }

    val database: AppDatabase by lazy {
        AppDatabase.getInstance(this)
    }

    val repository: AppRepository by lazy {
        AppRepository(
            gameProfileDao = database.gameProfileDao(),
            crosshairPresetDao = database.crosshairPresetDao(),
            context = this
        )
    }

    val shizukuManager: ShizukuManager by lazy {
        ShizukuManager(this)
    }

    val hardwareMonitor: HardwareMonitor by lazy {
        HardwareMonitor(this)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Global crash guard to log uncaught exceptions
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(TAG, "FATAL EXCEPTION in thread ${thread.name}: ${throwable.message}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }

        Log.d(TAG, "ZxOptimizerApplication initialized with clean lazy startup architecture.")
    }

    override fun onTerminate() {
        try {
            shizukuManager.cleanup()
            hardwareMonitor.cleanup()
        } catch (e: Throwable) {
            Log.w(TAG, "Error during app termination: ${e.message}")
        }
        super.onTerminate()
    }
}
