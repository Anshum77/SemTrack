package com.semtrack

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.semtrack.data.TasksRepository
import com.semtrack.data.local.SemTrackDatabase
import com.semtrack.data.local.TaskEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TasksViewModel(
    private val repository: TasksRepository
) : ViewModel() {

    val uiState: StateFlow<List<TaskListUiState>> = combine(
        repository.observeLists(),
        repository.observeTasks()
    ) { lists, tasks ->
        val tasksByList = tasks.groupBy { it.listId }
        lists.map { list ->
            val listTasks = tasksByList[list.id].orEmpty()
            val active = listTasks.filter { !it.isCompleted }
                .sortedByDescending { it.sortOrder }
            val completed = listTasks.filter { it.isCompleted }
                .sortedByDescending { it.sortOrder }
            TaskListUiState(
                id = list.id,
                name = list.name,
                isCompletedExpanded = list.isCompletedExpanded,
                active = active.map { it.toUi() },
                completed = completed.map { it.toUi() }
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.ensureDefaultList()
        }
    }

    fun addList(name: String) {
        viewModelScope.launch {
            repository.addList(name)
        }
    }

    fun renameList(listId: Long, name: String) {
        viewModelScope.launch {
            repository.renameList(listId, name)
        }
    }

    fun addTask(listId: Long, title: String) {
        viewModelScope.launch {
            repository.addTask(listId, title)
        }
    }

    fun setCompletedExpanded(listId: Long, isExpanded: Boolean) {
        viewModelScope.launch {
            repository.setCompletedExpanded(listId, isExpanded)
        }
    }

    fun completeTask(task: TaskUi) {
        viewModelScope.launch {
            repository.setTaskCompleted(task.id, task.listId, true)
        }
    }

    fun restoreTask(task: TaskUi) {
        viewModelScope.launch {
            repository.setTaskCompleted(task.id, task.listId, false)
        }
    }

    fun toggleStar(task: TaskUi) {
        viewModelScope.launch {
            repository.setTaskStarred(task.id, !task.isStarred)
        }
    }

    fun updateTaskTitle(taskId: Long, title: String) {
        viewModelScope.launch {
            repository.updateTaskTitle(taskId, title)
        }
    }

    fun deleteCompletedTasks(listId: Long) {
        viewModelScope.launch {
            repository.deleteCompletedTasks(listId)
        }
    }

    private fun TaskEntity.toUi(): TaskUi {
        return TaskUi(
            id = id,
            listId = listId,
            title = title,
            isStarred = isStarred,
            isCompleted = isCompleted
        )
    }

    class Factory(private val appContext: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val database = SemTrackDatabase.getInstance(appContext)
            val repository = TasksRepository(
                taskListDao = database.taskListDao(),
                taskDao = database.taskDao()
            )
            return TasksViewModel(repository) as T
        }
    }
}
