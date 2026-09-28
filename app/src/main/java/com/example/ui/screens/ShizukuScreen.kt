package com.example.ui.screens

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.ShizukuStatus
import com.example.ui.components.StatusBadge
import com.example.ui.components.TacticalCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel

@Composable
fun ShizukuScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val shizukuInfo by viewModel.shizukuInfo.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "SHIZUKU MANAGER",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Non-root ADB elevated permission engine",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            StatusBadge(
                text = shizukuInfo.status.name,
                color = when (shizukuInfo.status) {
                    ShizukuStatus.CONNECTED -> MaterialTheme.colorScheme.secondary
                    ShizukuStatus.PERMISSION_REQUIRED -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.error
                }
            )
        }

        // Live Status Card
        TacticalCard(
            borderColor = when (shizukuInfo.status) {
                ShizukuStatus.CONNECTED -> MaterialTheme.colorScheme.secondary
                ShizukuStatus.PERMISSION_REQUIRED -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.outline
            }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SERVICE STATUS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = when (shizukuInfo.status) {
                            ShizukuStatus.CONNECTED -> "Privileged Mode Active"
                            ShizukuStatus.PERMISSION_REQUIRED -> "Waiting for User Grant"
                            ShizukuStatus.SERVICE_NOT_RUNNING -> "Service Stopped"
                            ShizukuStatus.UNSUPPORTED -> "Not Installed"
                            ShizukuStatus.ERROR -> "Diagnosis Error"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Icon(
                    imageVector = when (shizukuInfo.status) {
                        ShizukuStatus.CONNECTED -> Icons.Default.CheckCircle
                        ShizukuStatus.PERMISSION_REQUIRED -> Icons.Default.Lock
                        else -> Icons.Default.Cancel
                    },
                    contentDescription = null,
                    tint = when (shizukuInfo.status) {
                        ShizukuStatus.CONNECTED -> MaterialTheme.colorScheme.secondary
                        ShizukuStatus.PERMISSION_REQUIRED -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.error
                    }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = shizukuInfo.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Diagnostic Specs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("API Version", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = if (shizukuInfo.version > 0) "v${shizukuInfo.version}" else "N/A", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("Privilege UID", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = if (shizukuInfo.uid >= 0) "${shizukuInfo.uid} (ADB)" else "N/A", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("App Installed", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = if (shizukuInfo.isInstalled) "Yes" else "No", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (shizukuInfo.status == ShizukuStatus.PERMISSION_REQUIRED) {
                    Button(
                        onClick = { viewModel.requestShizukuPermission() },
                        modifier = Modifier.weight(1f).testTag("request_shizuku_permission_button")
                    ) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Grant Permission")
                    }
                }

                Button(
                    onClick = { viewModel.openShizukuApp(context) },
                    modifier = Modifier.weight(1f).testTag("open_shizuku_app_button")
                ) {
                    Icon(Icons.Default.Launch, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Open Shizuku")
                }

                OutlinedButton(
                    onClick = { viewModel.refreshShizukuStatus() },
                    modifier = Modifier.weight(1f).testTag("reconnect_shizuku_button")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reconnect")
                }
            }
        }

        // Setup Guide for Android 11+ and Android 8-10
        TacticalCard {
            Text(
                text = "CARA MENGAKTIFKAN SHIZUKU (TANPA ROOT)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Android 11+ Wireless Debugging Guide
                Text(
                    text = "Metode 1: Wireless Debugging (Rekomendasi Android 11+)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                InstructionStep(1, "Aktifkan Developer Options di Android Settings -> Tentang Ponsel -> Ketuk 'Nomor Bentukan' 7 kali.")
                InstructionStep(2, "Buka Opsi Pengembang (Developer Options) -> Aktifkan 'Wireless Debugging'.")
                InstructionStep(3, "Buka aplikasi Shizuku -> Pilih 'Pairing' (Pasangkan dengan Wireless Debugging).")
                InstructionStep(4, "Masukkan 6-digit kode pairing dari notifikasi Wireless Debugging.")
                InstructionStep(5, "Tekan tombol 'Start' di Shizuku -> Kembali ke aplikasi ini dan klik 'Grant Permission'.")
            } else {
                // Android 8-10 ADB Cable Guide
                Text(
                    text = "Metode 2: ADB Via Komputer (Android 8 - 10)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                InstructionStep(1, "Aktifkan USB Debugging di Developer Options.")
                InstructionStep(2, "Hubungkan HP ke PC melalui kabel USB.")
                InstructionStep(3, "Jalankan perintah berikut di Command Prompt / Terminal PC:")
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF070A0E))
                        .padding(8.dp)
                ) {
                    Text(
                        text = "adb shell sh /sdcard/Android/data/moe.shizuku.privileged.api/start.sh",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun InstructionStep(stepNumber: Int, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$stepNumber",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
