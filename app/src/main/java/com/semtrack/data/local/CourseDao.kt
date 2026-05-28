package com.semtrack.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseDao {
    @Query("SELECT * FROM courses ORDER BY sortOrder ASC")
    fun observeCourses(): Flow<List<CourseEntity>>

    @Insert
    suspend fun insert(course: CourseEntity): Long

    @Query("UPDATE courses SET name = :name WHERE id = :courseId")
    suspend fun renameCourse(courseId: Long, name: String)

    @Query("DELETE FROM courses WHERE id = :courseId")
    suspend fun deleteCourse(courseId: Long)

    @Query("UPDATE courses SET sortOrder = :order WHERE id = :courseId")
    suspend fun updateSortOrder(courseId: Long, order: Int)

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM courses")
    suspend fun getMaxSortOrder(): Int

    @Query("SELECT COUNT(*) FROM courses WHERE name = :name COLLATE NOCASE")
    suspend fun countByName(name: String): Int

    @Query("SELECT COUNT(*) FROM courses WHERE name = :name COLLATE NOCASE AND id != :excludeId")
    suspend fun countByNameExcluding(name: String, excludeId: Long): Int
}
