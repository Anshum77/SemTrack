package com.semtrack.attendance

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.semtrack.data.AttendanceRepository
import com.semtrack.data.local.AttendanceEntryEntity
import com.semtrack.data.local.SemTrackDatabase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AttendanceDetailViewModel(
    private val courseId: Long,
    private val repository: AttendanceRepository
) : ViewModel() {

    private val entriesFlow = repository.observeEntries(courseId)

    val entries: StateFlow<List<AttendanceEntryUi>> = entriesFlow
        .map { list -> list.map { it.toUi() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stats: StateFlow<AttendanceStatsUi> = entriesFlow
        .map { list -> list.toStats() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AttendanceStatsUi(0, 0, 0, 0))

    fun markAttendance(dateEpochDay: Long, status: Int, onResult: (MarkResult) -> Unit) {
        viewModelScope.launch {
            val existing = repository.getEntry(courseId, dateEpochDay)
            when {
                existing == null -> {
                    repository.insertEntry(courseId, dateEpochDay, status)
                    onResult(MarkResult.Inserted)
                }
                existing.status == status -> {
                    onResult(MarkResult.AlreadyMarked)
                }
                else -> {
                    onResult(MarkResult.DifferentStatus(existing.id, existing.status))
                }
            }
        }
    }

    fun updateAttendance(entryId: Long, status: Int) {
        viewModelScope.launch {
            repository.updateEntryStatus(entryId, status)
        }
    }

    fun deleteEntry(entryId: Long) {
        viewModelScope.launch {
            repository.deleteEntry(entryId)
        }
    }

    private fun AttendanceEntryEntity.toUi(): AttendanceEntryUi {
        return AttendanceEntryUi(
            id = id,
            dateEpochDay = dateEpochDay,
            status = status
        )
    }

    private fun List<AttendanceEntryEntity>.toStats(): AttendanceStatsUi {
        val presentCount = count { it.status == AttendanceStatus.PRESENT }
        val absentCount = count { it.status == AttendanceStatus.ABSENT }
        val totalCount = size
        val percent = if (totalCount == 0) 0 else (presentCount * 100) / totalCount
        return AttendanceStatsUi(
            present = presentCount,
            absent = absentCount,
            total = totalCount,
            percent = percent
        )
    }

    sealed class MarkResult {
        data object Inserted : MarkResult()
        data object AlreadyMarked : MarkResult()
        data class DifferentStatus(val entryId: Long, val existingStatus: Int) : MarkResult()
    }

    class Factory(
        private val appContext: Context,
        private val courseId: Long
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val database = SemTrackDatabase.getInstance(appContext)
            val repository = AttendanceRepository(
                courseDao = database.courseDao(),
                attendanceEntryDao = database.attendanceEntryDao()
            )
            return AttendanceDetailViewModel(courseId, repository) as T
        }
    }
}
