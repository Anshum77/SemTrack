package com.semtrack.data

import com.semtrack.data.local.AttendanceEntryDao
import com.semtrack.data.local.AttendanceEntryEntity
import com.semtrack.data.local.CourseDao
import com.semtrack.data.local.CourseEntity
import kotlinx.coroutines.flow.Flow

class AttendanceRepository(
    private val courseDao: CourseDao,
    private val attendanceEntryDao: AttendanceEntryDao
) {
    fun observeCourses(): Flow<List<CourseEntity>> = courseDao.observeCourses()

    fun observeEntries(courseId: Long) = attendanceEntryDao.observeEntries(courseId)

    suspend fun addCourse(name: String): Boolean {
        if (courseDao.countByName(name) > 0) return false
        val order = courseDao.getMaxSortOrder() + 1
        courseDao.insert(CourseEntity(name = name, sortOrder = order))
        return true
     }

    suspend fun renameCourse(courseId: Long, name: String): Boolean {
        if (courseDao.countByNameExcluding(name, courseId) > 0) return false
        courseDao.renameCourse(courseId, name)
        return true
    }

    suspend fun deleteCourse(courseId: Long) {
        courseDao.deleteCourse(courseId)
    }

    suspend fun getEntry(courseId: Long, dateEpochDay: Long): AttendanceEntryEntity? {
        return attendanceEntryDao.getEntry(courseId, dateEpochDay)
    }

    suspend fun insertEntry(courseId: Long, dateEpochDay: Long, status: Int) {
        attendanceEntryDao.insert(
            AttendanceEntryEntity(
                courseId = courseId,
                dateEpochDay = dateEpochDay,
                status = status
            )
        )
    }

    suspend fun updateEntryStatus(entryId: Long, status: Int) {
        attendanceEntryDao.updateStatus(entryId, status)
    }

    suspend fun deleteEntry(entryId: Long) {
        attendanceEntryDao.deleteEntry(entryId)
    }
 }
