package com.semtrack.evaluations

data class CourseEvaluationUi(
    val id: Long,
    val name: String,
    val totalEvaluatedWeightage: Double = 0.0,
    val totalObtainedPercentage: Double = 0.0
)
