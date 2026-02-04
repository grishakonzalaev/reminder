package com.example.reminder.data.dao

import androidx.room.*
import com.example.reminder.data.model.ImportedCalendarInstance

@Dao
interface ImportedCalendarInstanceDao {

    @Query("SELECT EXISTS(SELECT 1 FROM imported_calendar_instances WHERE instanceKey = :instanceKey)")
    suspend fun exists(instanceKey: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(instance: ImportedCalendarInstance)

    @Query("DELETE FROM imported_calendar_instances WHERE instanceKey = :instanceKey")
    suspend fun delete(instanceKey: String)

    @Query("SELECT * FROM imported_calendar_instances")
    suspend fun getAll(): List<ImportedCalendarInstance>

    @Query("DELETE FROM imported_calendar_instances")
    suspend fun deleteAll()
}
