package com.example.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ModuleSettingsDao {
    @Query("SELECT * FROM module_settings")
    fun getAllSettings(): Flow<List<ModuleSettingsEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetting(setting: ModuleSettingsEntity)

    @Query("SELECT isEnabled FROM module_settings WHERE moduleId = :moduleId")
    suspend fun isModuleEnabled(moduleId: String): Boolean?
}
