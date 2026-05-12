package com.semtrack.attendance

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.semtrack.data.AttendanceRepository
import com.semtrack.data.local.CourseEntity
import com.semtrack.data.local.SemTrackDatabase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AttendanceViewModel(
    private val repository: AttendanceRepository
) : ViewModel() {

    val courses: StateFlow<List<CourseUi>> = repository.observeCourses()
        .map { list -> list.map { it.toUi() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addCourse(name: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.addCourse(name)
            onResult(success)
        }
    }

    fun renameCourse(courseId: Long, name: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.renameCourse(courseId, name)
            onResult(success)
        }
    }

    fun deleteCourse(courseId: Long) {
        viewModelScope.launch {
            repository.deleteCourse(courseId)
        }
    }

    private fun CourseEntity.toUi(): CourseUi {
        return CourseUi(id = id, name = name)
    }

    class Factory(private val appContext: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val database = SemTrackDatabase.getInstance(appContext)
            val repository = AttendanceRepository(
                courseDao = database.courseDao(),
                attendanceEntryDao = database.attendanceEntryDao()
            )
            return AttendanceViewModel(repository) as T
        }
    }
}
