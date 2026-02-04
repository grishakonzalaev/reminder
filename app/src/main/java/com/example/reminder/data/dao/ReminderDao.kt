package com.example.reminder.data.dao

import androidx.room.*
import com.example.reminder.data.model.Reminder
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    @Query("SELECT * FROM reminders ORDER BY timeMillis ASC")
    fun getAllFlow(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders ORDER BY timeMillis ASC")
    suspend fun getAll(): List<Reminder>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getById(id: Long): Reminder?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: Reminder): Long

    @Update
    suspend fun update(reminder: Reminder)

    @Delete
    suspend fun delete(reminder: Reminder)

    @Query("DELETE FROM reminders WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM reminders WHERE timeMillis < :timestampMillis")
    suspend fun deletePastReminders(timestampMillis: Long): Int

    @Query("SELECT * FROM reminders WHERE timeMillis < :timestampMillis")
    suspend fun getPastReminders(timestampMillis: Long): List<Reminder>
}
