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

private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        try {
            db.execSQL("ALTER TABLE reminders ADD COLUMN repeatType TEXT")
        } catch (e: Exception) {
            // колонка уже есть (например, миграция прервалась ранее)
        }
        db.execSQL("UPDATE reminders SET repeatType = 'none' WHERE repeatType IS NULL")
    }
}

@Database(
    entities = [
        Reminder::class,
        CalendarEventMapping::class,
        ImportedCalendarInstance::class
    ],
    version = 2,
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
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
