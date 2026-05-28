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
        TaskListEntity::class,
        EvaluationCategoryEntity::class,
        EvaluationItemEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class SemTrackDatabase : RoomDatabase() {
    abstract fun attendanceEntryDao(): AttendanceEntryDao
    abstract fun courseDao(): CourseDao
    abstract fun taskDao(): TaskDao
    abstract fun taskListDao(): TaskListDao
    abstract fun evaluationDao(): EvaluationDao

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

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS evaluations (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "courseId INTEGER NOT NULL, " +
                        "name TEXT NOT NULL, " +
                        "weightage REAL NOT NULL, " +
                        "totalMarks REAL, " +
                        "marksObtained REAL, " +
                        "FOREIGN KEY(courseId) REFERENCES courses(id) ON DELETE CASCADE" +
                    ")"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_evaluations_courseId ON evaluations(courseId)"
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS evaluations")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS evaluation_categories (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "courseId INTEGER NOT NULL, " +
                        "name TEXT NOT NULL, " +
                        "weightage REAL NOT NULL, " +
                        "itemCount INTEGER NOT NULL, " +
                        "bestOf INTEGER, " +
                        "FOREIGN KEY(courseId) REFERENCES courses(id) ON DELETE CASCADE" +
                    ")"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_evaluation_categories_courseId ON evaluation_categories(courseId)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS evaluation_items (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "categoryId INTEGER NOT NULL, " +
                        "name TEXT NOT NULL, " +
                        "totalMarks REAL, " +
                        "marksObtained REAL, " +
                        "FOREIGN KEY(categoryId) REFERENCES evaluation_categories(id) ON DELETE CASCADE" +
                    ")"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_evaluation_items_categoryId ON evaluation_items(categoryId)"
                )
            }
        }

        fun getInstance(context: Context): SemTrackDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    SemTrackDatabase::class.java,
                    "semtrack.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build().also { instance = it }
            }
        }
    }
}
