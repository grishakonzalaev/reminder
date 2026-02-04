package com.example.reminder.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.reminder.data.dao.CalendarEventMappingDao
import com.example.reminder.data.dao.ImportedCalendarInstanceDao
import com.example.reminder.data.dao.ReminderDao
import com.example.reminder.data.model.CalendarEventMapping
import com.example.reminder.data.model.ImportedCalendarInstance
import com.example.reminder.data.model.Reminder

@Database(
    entities = [
        Reminder::class,
        CalendarEventMapping::class,
        ImportedCalendarInstance::class
    ],
    version = 1,
    exportSchema = false
)
abstract class ReminderDatabase : RoomDatabase() {

    abstract fun reminderDao(): ReminderDao
    abstract fun calendarEventMappingDao(): CalendarEventMappingDao
    abstract fun importedCalendarInstanceDao(): ImportedCalendarInstanceDao

    companion object {
        @Volatile
        private var INSTANCE: ReminderDatabase? = null

        fun getDatabase(context: Context): ReminderDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ReminderDatabase::class.java,
                    "reminder_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
