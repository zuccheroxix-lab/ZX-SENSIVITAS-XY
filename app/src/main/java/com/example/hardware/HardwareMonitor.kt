package com.example.hardware

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.os.StatFs
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HardwareMonitor(private val context: Context) {

    companion object {
        private const val TAG = "HardwareMonitor"
    }

    private val _batteryData = MutableStateFlow(BatteryData())
    val batteryData: StateFlow<BatteryData> = _batteryData.asStateFlow()

    private val _thermalData = MutableStateFlow(ThermalData())
    val thermalData: StateFlow<ThermalData> = _thermalData.asStateFlow()

    private val _memoryData = MutableStateFlow(MemoryData())
    val memoryData: StateFlow<MemoryData> = _memoryData.asStateFlow()

    private val _storageData = MutableStateFlow(StorageData())
    val storageData: StateFlow<StorageData> = _storageData.asStateFlow()

    private val _displayData = MutableStateFlow(DisplayData())
    val displayData: StateFlow<DisplayData> = _displayData.asStateFlow()

    private val _cpuData = MutableStateFlow(CpuData())
    val cpuData: StateFlow<CpuData> = _cpuData.asStateFlow()

    private var isReceiverRegistered = false

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            try {
                if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                    updateBatteryFromIntent(intent)
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Error in battery broadcast receiver: ${e.message}")
            }
        }
    }

    init {
        // Safe lazy startup: register receiver with crash guard & compatibility flag
        try {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val initialIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(batteryReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(batteryReceiver, filter)
            }
            isReceiverRegistered = true
            initialIntent?.let { updateBatteryFromIntent(it) }
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to register battery receiver safely: ${e.message}")
        }

        // Asynchronously populate hardware telemetry in background so UI startup is NEVER blocked
        CoroutineScope(Dispatchers.IO).launch {
            try {
                refreshAll()
            } catch (e: Throwable) {
                Log.w(TAG, "Async hardware refresh error: ${e.message}")
            }
        }
    }

    fun cleanup() {
        if (isReceiverRegistered) {
            try {
                context.unregisterReceiver(batteryReceiver)
                isReceiverRegistered = false
            } catch (e: Throwable) {
                Log.w(TAG, "Error unregistering battery receiver: ${e.message}")
            }
        }
    }

    private fun updateBatteryFromIntent(intent: Intent) {
        try {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val levelPct = if (level >= 0 && scale > 0) (level * 100) / scale else 0

            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL

            val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
            val chargeSource = when (plugged) {
                BatteryManager.BATTERY_PLUGGED_AC -> "AC Wall Charger"
                BatteryManager.BATTERY_PLUGGED_USB -> "USB Port"
                BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless Qi"
                else -> if (isCharging) "Charging" else "Discharging"
            }

            val tempTenths = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
            val tempCelsius = tempTenths / 10f

            val voltageMv = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)

            val healthCode = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)
            val health = when (healthCode) {
                BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
                BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat!"
                BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
                BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
                BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Failure"
                BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
                else -> "Normal"
            }

            val tech = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "Li-ion"

            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            val currentNow = try {
                batteryManager?.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
            } catch (e: Throwable) {
                null
            }

            _batteryData.value = BatteryData(
                levelPercent = levelPct,
                isCharging = isCharging,
                chargeSource = chargeSource,
                temperatureCelsius = tempCelsius,
                voltageMillivolts = voltageMv,
                health = health,
                technology = tech,
                currentNowMicroAmps = if (currentNow != null && currentNow != Long.MIN_VALUE) currentNow else null
            )
        } catch (e: Throwable) {
            Log.w(TAG, "Error updating battery data: ${e.message}")
        }
    }

    fun refreshAll() {
        refreshMemory()
        refreshStorage()
        refreshDisplay()
        refreshThermal()
        refreshCpu()
    }

    fun refreshMemory() {
        try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return
            val memInfo = ActivityManager.MemoryInfo()
            am.getMemoryInfo(memInfo)
            _memoryData.value = MemoryData(
                availableBytes = memInfo.availMem,
                totalBytes = memInfo.totalMem,
                isLowMemory = memInfo.lowMemory,
                thresholdBytes = memInfo.threshold
            )
        } catch (e: Throwable) {
            Log.w(TAG, "refreshMemory error: ${e.message}")
        }
    }

    fun refreshStorage() {
        try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val available = stat.availableBytes
            val total = stat.totalBytes
            _storageData.value = StorageData(
                availableBytes = available,
                totalBytes = total
            )
        } catch (e: Throwable) {
            Log.w(TAG, "refreshStorage error: ${e.message}")
        }
    }

    fun refreshDisplay() {
        try {
            // SAFE DISPLAY METRICS: Use resources.displayMetrics which is guaranteed safe on Application context
            val dm = context.resources.displayMetrics
            val w = dm.widthPixels
            val h = dm.heightPixels
            val dpi = dm.densityDpi
            val scale = dm.density

            // Safe fallback refresh rate
            val rate = 60f
            val supportedRates = listOf(60f)

            _displayData.value = DisplayData(
                physicalWidthPx = w,
                physicalHeightPx = h,
                refreshRateHz = rate,
                supportedRefreshRates = supportedRates,
                densityDpi = dpi,
                densityScale = scale
            )
        } catch (e: Throwable) {
            Log.w(TAG, "refreshDisplay error: ${e.message}")
        }
    }

    fun refreshThermal() {
        try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return
            var statusStr = "Normal"
            var severity = 0
            var headroom: Float? = null

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    severity = pm.currentThermalStatus
                    statusStr = when (severity) {
                        PowerManager.THERMAL_STATUS_NONE -> "Cool (No Throttling)"
                        PowerManager.THERMAL_STATUS_LIGHT -> "Light Throttling"
                        PowerManager.THERMAL_STATUS_MODERATE -> "Moderate Warm"
                        PowerManager.THERMAL_STATUS_SEVERE -> "Severe Throttling"
                        PowerManager.THERMAL_STATUS_CRITICAL -> "Critical Heat"
                        PowerManager.THERMAL_STATUS_EMERGENCY -> "Emergency Shutdown Warning"
                        PowerManager.THERMAL_STATUS_SHUTDOWN -> "Thermal Shutdown"
                        else -> "Normal"
                    }
                } catch (e: Throwable) {
                    statusStr = "Normal"
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try {
                    headroom = pm.getThermalHeadroom(30)
                } catch (e: Throwable) {
                    headroom = null
                }
            }

            _thermalData.value = ThermalData(
                statusString = statusStr,
                severityLevel = severity,
                thermalHeadroom = headroom
            )
        } catch (e: Throwable) {
            Log.w(TAG, "refreshThermal error: ${e.message}")
        }
    }

    fun refreshCpu() {
        try {
            val cores = Runtime.getRuntime().availableProcessors()
            val arch = System.getProperty("os.arch") ?: "arm64"
            val hw = Build.HARDWARE ?: "Standard"
            val soc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Build.SOC_MODEL ?: Build.BOARD ?: "Qualcomm/MTK"
            } else {
                Build.BOARD ?: "Standard"
            }

            _cpuData.value = CpuData(
                coresCount = cores,
                architecture = arch,
                socModel = soc,
                hardware = hw
            )
        } catch (e: Throwable) {
            Log.w(TAG, "refreshCpu error: ${e.message}")
        }
    }

    fun trimApplicationMemory(): Long {
        return try {
            val beforeAvail = getFreeMemoryBytes()
            System.gc()
            val afterAvail = getFreeMemoryBytes()
            refreshMemory()
            val freed = (afterAvail - beforeAvail).coerceAtLeast(0L)
            freed / (1024 * 1024)
        } catch (e: Throwable) {
            0L
        }
    }

    private fun getFreeMemoryBytes(): Long {
        return try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return 0L
            val memInfo = ActivityManager.MemoryInfo()
            am.getMemoryInfo(memInfo)
            memInfo.availMem
        } catch (e: Throwable) {
            0L
        }
    }
}
