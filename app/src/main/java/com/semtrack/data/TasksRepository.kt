package com.semtrack.data

import com.semtrack.data.local.TaskDao
import com.semtrack.data.local.TaskEntity
import com.semtrack.data.local.TaskListDao
import com.semtrack.data.local.TaskListEntity
import kotlinx.coroutines.flow.Flow

class TasksRepository(
    private val taskListDao: TaskListDao,
    private val taskDao: TaskDao
) {
    fun observeLists(): Flow<List<TaskListEntity>> = taskListDao.observeLists()

    fun observeTasks(): Flow<List<TaskEntity>> = taskDao.observeAllTasks()

    suspend fun ensureDefaultList() {
        if (taskListDao.getCount() == 0) {
            taskListDao.insert(
                TaskListEntity(
                    name = "My Tasks",
                    sortOrder = 0,
                    isCompletedExpanded = false
                )
            )
        }
    }

    suspend fun addList(name: String) {
        val order = taskListDao.getMaxSortOrder() + 1
        taskListDao.insert(
            TaskListEntity(
                name = name,
                sortOrder = order,
                isCompletedExpanded = false
            )
        )
    }

    suspend fun renameList(listId: Long, newName: String) {
        taskListDao.renameList(listId, newName)
    }

    suspend fun setCompletedExpanded(listId: Long, isExpanded: Boolean) {
        taskListDao.setCompletedExpanded(listId, isExpanded)
    }

    suspend fun addTask(listId: Long, title: String) {
        val order = taskDao.getMaxSortOrder(listId, false) + 1
        taskDao.insert(
            TaskEntity(
                listId = listId,
                title = title,
                isStarred = false,
                isCompleted = false,
                sortOrder = order
            )
        )
    }

    suspend fun setTaskCompleted(taskId: Long, listId: Long, isCompleted: Boolean) {
        val order = taskDao.getMaxSortOrder(listId, isCompleted) + 1
        taskDao.setCompleted(taskId, isCompleted, order)
    }

    suspend fun setTaskStarred(taskId: Long, isStarred: Boolean) {
        taskDao.setStarred(taskId, isStarred)
    }

    suspend fun deleteCompletedTasks(listId: Long) {
        taskDao.deleteCompleted(listId)
    }
}
