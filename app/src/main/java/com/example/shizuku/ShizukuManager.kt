package com.example.shizuku

import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import android.util.Log
import com.example.model.ApplyResult
import com.example.model.ShizukuInfo
import com.example.model.ShizukuStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader

data class ShellResult(
    val exitCode: Int,
    val output: String,
    val error: String
)

class ShizukuManager(private val context: Context) {

    companion object {
        private const val TAG = "ShizukuManager"

        val isShizukuLibraryAvailable: Boolean by lazy {
            try {
                Class.forName("rikka.shizuku.Shizuku")
                true
            } catch (e: Throwable) {
                false
            }
        }
    }

    private val _shizukuInfo = MutableStateFlow(
        ShizukuInfo(
            status = ShizukuStatus.SERVICE_NOT_RUNNING,
            isInstalled = false,
            isRunning = false,
            isPermissionGranted = false,
            version = 0,
            uid = -1,
            message = "Shizuku service check idle."
        )
    )
    val shizukuInfo: StateFlow<ShizukuInfo> = _shizukuInfo.asStateFlow()

    private var listenersRegistered = false

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        try {
            checkStatus()
        } catch (e: Throwable) {
            Log.w(TAG, "Error in onBinderReceived: ${e.message}")
        }
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        try {
            checkStatus()
        } catch (e: Throwable) {
            Log.w(TAG, "Error in onBinderDead: ${e.message}")
        }
    }

    private val requestPermissionResultListener = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
        try {
            val granted = grantResult == PackageManager.PERMISSION_GRANTED
            _shizukuInfo.value = _shizukuInfo.value.copy(
                isPermissionGranted = granted,
                status = if (granted) ShizukuStatus.CONNECTED else ShizukuStatus.PERMISSION_REQUIRED,
                message = if (granted) "Shizuku permission granted and ready" else "Shizuku permission was denied"
            )
        } catch (e: Throwable) {
            Log.w(TAG, "Error in onRequestPermissionResult: ${e.message}")
        }
    }

    init {
        // Safe lazy listener registration: NEVER crash if Shizuku provider is absent
        try {
            if (isShizukuLibraryAvailable) {
                Shizuku.addBinderReceivedListener(binderReceivedListener)
                Shizuku.addBinderDeadListener(binderDeadListener)
                Shizuku.addRequestPermissionResultListener(requestPermissionResultListener)
                listenersRegistered = true
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Safe Shizuku listener init warning: ${e.message}")
        }
    }

    fun cleanup() {
        if (listenersRegistered) {
            try {
                Shizuku.removeBinderReceivedListener(binderReceivedListener)
                Shizuku.removeBinderDeadListener(binderDeadListener)
                Shizuku.removeRequestPermissionResultListener(requestPermissionResultListener)
                listenersRegistered = false
            } catch (e: Throwable) {
                Log.w(TAG, "Safe Shizuku cleanup warning: ${e.message}")
            }
        }
    }

    fun checkStatus(): ShizukuInfo {
        if (!isShizukuLibraryAvailable) {
            val info = ShizukuInfo(
                status = ShizukuStatus.UNSUPPORTED,
                isInstalled = false,
                isRunning = false,
                isPermissionGranted = false,
                version = 0,
                uid = -1,
                message = "Shizuku library not available."
            )
            _shizukuInfo.value = info
            return info
        }

        var isInstalled = false
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo("moe.shizuku.privileged.api", PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo("moe.shizuku.privileged.api", 0)
            }
            isInstalled = true
        } catch (e: Throwable) {
            isInstalled = false
        }

        try {
            val pingOk = try {
                Shizuku.pingBinder()
            } catch (e: Throwable) {
                false
            }

            if (!pingOk) {
                val info = ShizukuInfo(
                    status = if (isInstalled) ShizukuStatus.SERVICE_NOT_RUNNING else ShizukuStatus.UNSUPPORTED,
                    isInstalled = isInstalled,
                    isRunning = false,
                    isPermissionGranted = false,
                    version = 0,
                    uid = -1,
                    message = if (isInstalled) "Shizuku app installed, service not running." else "Shizuku is not installed on this device."
                )
                _shizukuInfo.value = info
                return info
            }

            val version = try {
                Shizuku.getVersion()
            } catch (e: Throwable) {
                0
            }

            val uid = try {
                Shizuku.getUid()
            } catch (e: Throwable) {
                -1
            }

            if (version < 11) {
                val info = ShizukuInfo(
                    status = ShizukuStatus.UNSUPPORTED,
                    isInstalled = true,
                    isRunning = true,
                    isPermissionGranted = false,
                    version = version,
                    uid = uid,
                    message = "Legacy Shizuku v$version detected. Please update Shizuku."
                )
                _shizukuInfo.value = info
                return info
            }

            val hasPermission = try {
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            } catch (e: Throwable) {
                false
            }

            val status = if (hasPermission) ShizukuStatus.CONNECTED else ShizukuStatus.PERMISSION_REQUIRED
            val message = if (hasPermission) "Shizuku Connected (v$version, UID $uid)" else "Shizuku running, authorization required"

            val info = ShizukuInfo(
                status = status,
                isInstalled = true,
                isRunning = true,
                isPermissionGranted = hasPermission,
                version = version,
                uid = uid,
                message = message
            )
            _shizukuInfo.value = info
            return info
        } catch (e: Throwable) {
            val info = ShizukuInfo(
                status = ShizukuStatus.SERVICE_NOT_RUNNING,
                isInstalled = isInstalled,
                isRunning = false,
                isPermissionGranted = false,
                version = 0,
                uid = -1,
                message = "Shizuku service check: ${e.message ?: "Not connected"}"
            )
            _shizukuInfo.value = info
            return info
        }
    }

    fun requestPermission(requestCode: Int = 1001) {
        try {
            if (!isShizukuLibraryAvailable) return
            if (Shizuku.pingBinder()) {
                if (Shizuku.getVersion() < 11) {
                    _shizukuInfo.value = _shizukuInfo.value.copy(
                        message = "Unsupported legacy Shizuku version. Please update Shizuku."
                    )
                } else {
                    Shizuku.requestPermission(requestCode)
                }
            } else {
                _shizukuInfo.value = _shizukuInfo.value.copy(
                    message = "Cannot request permission: Shizuku service is not running."
                )
            }
        } catch (e: Throwable) {
            _shizukuInfo.value = _shizukuInfo.value.copy(
                message = "Failed to request Shizuku permission: ${e.message}"
            )
        }
    }

    suspend fun runShellCommand(command: String): ShellResult = withContext(Dispatchers.IO) {
        val info = checkStatus()
        if (info.status != ShizukuStatus.CONNECTED) {
            return@withContext ShellResult(
                exitCode = -1,
                output = "",
                error = "Shizuku is not connected. Current status: ${info.status}"
            )
        }

        try {
            val method = Shizuku::class.java.getMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            val process = method.invoke(null, arrayOf("sh", "-c", command), null, null) as Process

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val errReader = BufferedReader(InputStreamReader(process.errorStream))

            val output = StringBuilder()
            val error = StringBuilder()

            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }
            while (errReader.readLine().also { line = it } != null) {
                error.append(line).append("\n")
            }

            val exitCode = process.waitFor()

            ShellResult(
                exitCode = exitCode,
                output = output.toString().trim(),
                error = error.toString().trim()
            )
        } catch (e: Throwable) {
            ShellResult(
                exitCode = -1,
                output = "",
                error = e.localizedMessage ?: "Execution exception"
            )
        }
    }

    fun getSystemPointerSpeedDirect(): Int {
        return try {
            Settings.System.getInt(context.contentResolver, "pointer_speed", 0)
        } catch (e: Throwable) {
            0
        }
    }

    suspend fun applyPointerSpeed(speed: Int): ApplyResult = withContext(Dispatchers.IO) {
        val clampedSpeed = speed.coerceIn(-7, 7)

        val info = checkStatus()
        if (info.status == ShizukuStatus.CONNECTED) {
            val cmdResult = runShellCommand("settings put system pointer_speed $clampedSpeed")
            if (cmdResult.exitCode == 0) {
                val verifyResult = runShellCommand("settings get system pointer_speed")
                val verifiedVal = verifyResult.output.toIntOrNull()
                if (verifiedVal == clampedSpeed) {
                    return@withContext ApplyResult.Success(
                        message = "Pointer speed calibrated to $clampedSpeed and verified via Shizuku.",
                        verifiedPointerSpeed = verifiedVal
                    )
                } else {
                    return@withContext ApplyResult.Error(
                        reason = "Command executed, but verification read '$verifiedVal' instead of '$clampedSpeed'."
                    )
                }
            } else {
                return@withContext ApplyResult.Error(
                    reason = "Shizuku shell failed (code ${cmdResult.exitCode}): ${cmdResult.error}"
                )
            }
        }

        if (Settings.System.canWrite(context)) {
            try {
                val ok = Settings.System.putInt(
                    context.contentResolver,
                    "pointer_speed",
                    clampedSpeed
                )
                val current = Settings.System.getInt(
                    context.contentResolver,
                    "pointer_speed",
                    -999
                )
                if (ok && current == clampedSpeed) {
                    return@withContext ApplyResult.Success(
                        message = "Pointer speed set to $clampedSpeed via WRITE_SETTINGS permission.",
                        verifiedPointerSpeed = current
                    )
                } else {
                    return@withContext ApplyResult.Error(
                        reason = "WRITE_SETTINGS succeeded but system value did not update."
                    )
                }
            } catch (e: Throwable) {
                return@withContext ApplyResult.Error(
                    reason = "Security Exception: ${e.message}",
                    suggestedAction = "Grant Shizuku or 'Modify System Settings' permission in Settings."
                )
            }
        }

        ApplyResult.Error(
            reason = "Permission required: Neither Shizuku nor WRITE_SETTINGS is active.",
            suggestedAction = "Connect Shizuku or enable 'Modify System Settings' for ZX Optimizer."
        )
    }

    suspend fun applyDisplayDensity(dpi: Int): ApplyResult = withContext(Dispatchers.IO) {
        val clampedDpi = dpi.coerceIn(160, 800)
        val info = checkStatus()
        if (info.status != ShizukuStatus.CONNECTED) {
            return@withContext ApplyResult.Error(
                reason = "Changing display density requires active Shizuku connection.",
                suggestedAction = "Open Shizuku app and start the service."
            )
        }

        val result = runShellCommand("wm density $clampedDpi")
        if (result.exitCode == 0) {
            val verify = runShellCommand("wm density")
            if (verify.output.contains(clampedDpi.toString())) {
                ApplyResult.Success(
                    message = "Display density set to $clampedDpi DPI and verified.",
                    verifiedDpi = clampedDpi
                )
            } else {
                ApplyResult.Error(
                    reason = "Density command sent, but verification showed: ${verify.output}"
                )
            }
        } else {
            ApplyResult.Error(
                reason = "wm density failed: ${result.error}"
            )
        }
    }

    suspend fun resetDisplayDensity(): ApplyResult = withContext(Dispatchers.IO) {
        val info = checkStatus()
        if (info.status != ShizukuStatus.CONNECTED) {
            return@withContext ApplyResult.Error(reason = "Shizuku not connected.")
        }
        val result = runShellCommand("wm density reset")
        if (result.exitCode == 0) {
            ApplyResult.Success(message = "Display density reset to default.")
        } else {
            ApplyResult.Error(reason = "Failed to reset density: ${result.error}")
        }
    }

    suspend fun applyPeakRefreshRate(hz: Float): ApplyResult = withContext(Dispatchers.IO) {
        val info = checkStatus()
        if (info.status != ShizukuStatus.CONNECTED) {
            return@withContext ApplyResult.Error(reason = "Shizuku required to calibrate refresh rate.")
        }
        val clampedHz = hz.coerceIn(60f, 240f)
        val cmd = "settings put system peak_refresh_rate $clampedHz && settings put system min_refresh_rate $clampedHz"
        val result = runShellCommand(cmd)
        if (result.exitCode == 0) {
            ApplyResult.Success(message = "Peak refresh rate locked to ${clampedHz.toInt()}Hz.")
        } else {
            ApplyResult.Error(reason = "Refresh rate command failed: ${result.error}")
        }
    }

    suspend fun applyGameMode(packageName: String, mode: String): ApplyResult = withContext(Dispatchers.IO) {
        val info = checkStatus()
        if (info.status != ShizukuStatus.CONNECTED) {
            return@withContext ApplyResult.Error(reason = "Shizuku required for Android Game Mode API.")
        }
        val result = runShellCommand("cmd game mode $mode $packageName")
        if (result.exitCode == 0) {
            ApplyResult.Success(message = "Android Game Mode '$mode' set for $packageName.")
        } else {
            ApplyResult.Error(reason = "cmd game failed: ${result.error}")
        }
    }
}
