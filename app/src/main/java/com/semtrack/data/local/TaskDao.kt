package com.semtrack.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY sortOrder DESC")
    fun observeAllTasks(): Flow<List<TaskEntity>>

    @Insert
    suspend fun insert(task: TaskEntity): Long

    @Query("UPDATE tasks SET isStarred = :isStarred WHERE id = :taskId")
    suspend fun setStarred(taskId: Long, isStarred: Boolean)

    @Query("UPDATE tasks SET title = :title WHERE id = :taskId")
    suspend fun updateTitle(taskId: Long, title: String)

    @Query("UPDATE tasks SET sortOrder = :sortOrder WHERE id = :taskId")
    suspend fun updateSortOrder(taskId: Long, sortOrder: Int)

    @Query("UPDATE tasks SET isCompleted = :isCompleted, sortOrder = :sortOrder WHERE id = :taskId")
    suspend fun setCompleted(taskId: Long, isCompleted: Boolean, sortOrder: Int)

    @Query("DELETE FROM tasks WHERE listId = :listId AND isCompleted = 1")
    suspend fun deleteCompleted(listId: Long)

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM tasks WHERE listId = :listId AND isCompleted = :isCompleted")
    suspend fun getMaxSortOrder(listId: Long, isCompleted: Boolean): Int
}
