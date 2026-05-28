package com.semtrack.attendance

data class AttendanceEntryUi(
    val id: Long,
    val dateEpochDay: Long,
    val status: Int
)

data class AttendanceStatsUi(
    val present: Int,
    val absent: Int,
    val total: Int,
    val percent: Double
)

object AttendanceStatus {
    const val PRESENT = 1
    const val ABSENT = 0
}
