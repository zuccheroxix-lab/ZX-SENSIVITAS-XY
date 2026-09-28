package com.example.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GameProfileDao {
    @Query("SELECT * FROM game_profiles ORDER BY isDefaultSelected DESC, displayName ASC")
    fun getAllProfiles(): Flow<List<GameProfileEntity>>

    @Query("SELECT * FROM game_profiles WHERE id = :id LIMIT 1")
    suspend fun getProfileById(id: Long): GameProfileEntity?

    @Query("SELECT * FROM game_profiles WHERE packageName = :pkg LIMIT 1")
    suspend fun getProfileByPackage(pkg: String): GameProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: GameProfileEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(profiles: List<GameProfileEntity>)

    @Update
    suspend fun updateProfile(profile: GameProfileEntity)

    @Delete
    suspend fun deleteProfile(profile: GameProfileEntity)

    @Query("UPDATE game_profiles SET isDefaultSelected = 0")
    suspend fun clearActiveSelection()

    @Query("UPDATE game_profiles SET isDefaultSelected = 1 WHERE id = :id")
    suspend fun setActiveProfile(id: Long)
}
