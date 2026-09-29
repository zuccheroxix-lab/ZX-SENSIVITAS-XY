package com.example.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ActionHistoryDao {
    @Query("SELECT * FROM action_history ORDER BY timestamp DESC LIMIT 50")
    fun getAllHistory(): Flow<List<ActionHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAction(action: ActionHistoryEntity): Long

    @Query("DELETE FROM action_history")
    suspend fun clearHistory()
}
