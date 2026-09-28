package com.example.model

enum class ShizukuStatus {
    CONNECTED,
    PERMISSION_REQUIRED,
    SERVICE_NOT_RUNNING,
    UNSUPPORTED,
    ERROR
}

data class ShizukuInfo(
    val status: ShizukuStatus = ShizukuStatus.SERVICE_NOT_RUNNING,
    val isInstalled: Boolean = false,
    val isRunning: Boolean = false,
    val isPermissionGranted: Boolean = false,
    val version: Int = 0,
    val uid: Int = -1,
    val message: String = "Shizuku service check pending"
)
