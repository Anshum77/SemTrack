package com.semtrack

data class TaskUi(
    val id: Long,
    val listId: Long,
    val title: String,
    val isStarred: Boolean,
    val isCompleted: Boolean
)

data class TaskListUiState(
    val id: Long,
    val name: String,
    val isCompletedExpanded: Boolean,
    val active: List<TaskUi>,
    val completed: List<TaskUi>
)
