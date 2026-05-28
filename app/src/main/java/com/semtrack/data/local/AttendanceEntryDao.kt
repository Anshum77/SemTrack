package com.semtrack.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceEntryDao {
    @Query("SELECT * FROM attendance_entries WHERE courseId = :courseId ORDER BY dateEpochDay DESC")
    fun observeEntries(courseId: Long): Flow<List<AttendanceEntryEntity>>

    @Query("SELECT * FROM attendance_entries WHERE courseId = :courseId AND dateEpochDay = :dateEpochDay LIMIT 1")
    suspend fun getEntry(courseId: Long, dateEpochDay: Long): AttendanceEntryEntity?

    @Insert
    suspend fun insert(entry: AttendanceEntryEntity): Long

    @Query("UPDATE attendance_entries SET status = :status WHERE id = :entryId")
    suspend fun updateStatus(entryId: Long, status: Int)

    @Query("DELETE FROM attendance_entries WHERE id = :entryId")
    suspend fun deleteEntry(entryId: Long)

    @Query("DELETE FROM attendance_entries WHERE courseId = :courseId")
    suspend fun deleteByCourse(courseId: Long)

    @Query("SELECT * FROM attendance_entries")
    fun observeAllEntries(): Flow<List<AttendanceEntryEntity>>
}
