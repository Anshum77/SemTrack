package com.semtrack.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        AttendanceEntryEntity::class,
        CourseEntity::class,
        TaskEntity::class,
        TaskListEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class SemTrackDatabase : RoomDatabase() {
    abstract fun attendanceEntryDao(): AttendanceEntryDao
    abstract fun courseDao(): CourseDao
    abstract fun taskDao(): TaskDao
    abstract fun taskListDao(): TaskListDao

    companion object {
        @Volatile
        private var instance: SemTrackDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS courses (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "name TEXT NOT NULL COLLATE NOCASE, " +
                        "sortOrder INTEGER NOT NULL" +
                    ")"
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_courses_name ON courses(name)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS attendance_entries (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "courseId INTEGER NOT NULL, " +
                        "dateEpochDay INTEGER NOT NULL, " +
                        "status INTEGER NOT NULL, " +
                        "FOREIGN KEY(courseId) REFERENCES courses(id) ON DELETE CASCADE" +
                    ")"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_attendance_entries_courseId ON attendance_entries(courseId)"
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_attendance_entries_courseId_dateEpochDay " +
                        "ON attendance_entries(courseId, dateEpochDay)"
                )
            }
        }

        fun getInstance(context: Context): SemTrackDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    SemTrackDatabase::class.java,
                    "semtrack.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build().also { instance = it }
            }
        }
    }
}
