package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.database.GameProfileEntity
import com.example.ui.components.StatusBadge
import com.example.ui.components.TacticalCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel

@Composable
fun GameProfileScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val gameProfiles by viewModel.gameProfiles.collectAsState()
    val selectedProfile by viewModel.selectedProfile.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var newGameName by remember { mutableStateOf("") }
    var newGamePackage by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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
                    text = "SENSITIVITY GAME PROFILES",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Per-game X/Y curve, drag response, and pointer tuning",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.testTag("add_custom_game_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Game Profile",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Compliance & Architecture Disclaimer Card
        TacticalCard(borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "100% Legal & Safe Input Engine",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "Aplikasi ini menyimpan profil kalibrasi input X/Y, menerapkan System Pointer Speed Android, Display DPI, dan Crosshair Overlay. Tidak menyuntikkan kode atau membaca memori game, sehingga 100% aman dari ban anti-cheat.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // List of Profiles
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(gameProfiles) { profile ->
                val isSelected = selectedProfile?.id == profile.id
                val isInstalled = viewModel.isGameInstalled(profile.packageName)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable { viewModel.selectGameProfile(profile) }
                        .testTag("game_profile_${profile.packageName}"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = profile.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = profile.packageName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            StatusBadge(
                                text = if (isInstalled) "INSTALLED" else "NOT FOUND",
                                color = if (isInstalled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Stats Grid 1: X, Y, Ratio, Curve
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            GameStatBadge("Horizontal X", "${String.format("%.2f", profile.xSensitivity)}x")
                            GameStatBadge("Vertical Y", "${String.format("%.2f", profile.ySensitivity)}x")
                            GameStatBadge("X/Y Ratio", "1 : ${String.format("%.2f", profile.xyRatio)}")
                            GameStatBadge("Curve", profile.responseCurve.replace("_", " "))
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Stats Grid 2: Smoothness, Accel, Deadzone, Pointer Speed
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            GameStatBadge("Smoothness", "${String.format("%.2f", profile.dragSmoothness)}x")
                            GameStatBadge("Accel", "+${String.format("%.2f", profile.acceleration)}x")
                            GameStatBadge("Deadzone", "${String.format("%.1f", profile.deadzonePx)} px")
                            GameStatBadge("Pointer Spd", if (profile.pointerSpeed > 0) "+${profile.pointerSpeed}" else "${profile.pointerSpeed}")
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.applyGameProfileToSystem(profile) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Apply Profile")
                            }

                            if (isInstalled) {
                                OutlinedButton(
                                    onClick = { viewModel.launchGame(profile.packageName, context) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Launch")
                                }
                            }

                            if (profile.isCustom) {
                                IconButton(
                                    onClick = { viewModel.deleteGameProfile(profile) },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Custom Game Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Custom Game Profile") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newGameName,
                        onValueChange = { newGameName = it },
                        label = { Text("Game Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newGamePackage,
                        onValueChange = { newGamePackage = it },
                        label = { Text("Package Name (e.g. com.example.game)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newGameName.isNotBlank() && newGamePackage.isNotBlank()) {
                            viewModel.saveGameProfile(
                                GameProfileEntity(
                                    displayName = newGameName.trim(),
                                    packageName = newGamePackage.trim(),
                                    xSensitivity = 1.00f,
                                    ySensitivity = 1.15f,
                                    dragResponse = 1.00f,
                                    dragSmoothness = 0.65f,
                                    acceleration = 0.30f,
                                    deadzonePx = 2.0f,
                                    responseCurve = "DYNAMIC_S",
                                    isCustom = true
                                )
                            )
                            newGameName = ""
                            newGamePackage = ""
                            showAddDialog = false
                        }
                    }
                ) {
                    Text("Add Profile")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun GameStatBadge(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}
