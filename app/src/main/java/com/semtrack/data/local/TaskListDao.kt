package com.semtrack.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskListDao {
    @Query("SELECT * FROM task_lists ORDER BY sortOrder ASC")
    fun observeLists(): Flow<List<TaskListEntity>>

    @Insert
    suspend fun insert(list: TaskListEntity): Long

    @Update
    suspend fun update(list: TaskListEntity)

    @Query("UPDATE task_lists SET name = :name WHERE id = :listId")
    suspend fun renameList(listId: Long, name: String)

    @Query("UPDATE task_lists SET isCompletedExpanded = :isExpanded WHERE id = :listId")
    suspend fun setCompletedExpanded(listId: Long, isExpanded: Boolean)

    @Query("DELETE FROM task_lists WHERE id = :listId")
    suspend fun deleteList(listId: Long)

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM task_lists")
    suspend fun getMaxSortOrder(): Int

    @Query("SELECT COUNT(*) FROM task_lists")
    suspend fun getCount(): Int
}
