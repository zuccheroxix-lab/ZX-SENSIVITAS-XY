package com.example.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "action_history")
data class ActionHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val actionName: String,
    val targetName: String,
    val mode: String,
    val status: String,
    val detail: String,
    val verification: String,
    val timestamp: Long = System.currentTimeMillis()
)
