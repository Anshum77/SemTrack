package com.semtrack.attendance

data class CourseUi(
    val id: Long,
    val name: String,
    val present: Int = 0,
    val absent: Int = 0,
    val total: Int = 0,
    val percent: Double = 0.0
)
