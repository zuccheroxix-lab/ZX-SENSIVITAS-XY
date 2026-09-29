package com.example.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GlobalConfigDao {
    @Query("SELECT * FROM global_config WHERE id = 1 LIMIT 1")
    fun getGlobalConfigFlow(): Flow<GlobalConfigEntity?>

    @Query("SELECT * FROM global_config WHERE id = 1 LIMIT 1")
    suspend fun getGlobalConfig(): GlobalConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(config: GlobalConfigEntity)
}
