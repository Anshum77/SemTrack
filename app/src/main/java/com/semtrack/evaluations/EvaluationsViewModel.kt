package com.semtrack.evaluations

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.semtrack.data.EvaluationRepository
import com.semtrack.data.local.SemTrackDatabase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class EvaluationsViewModel(
    private val repository: EvaluationRepository
) : ViewModel() {

    val courses: StateFlow<List<CourseEvaluationUi>> = repository.observeCourses()
        .combine(repository.observeAllCategoriesWithItems()) { courseList, allCategories ->
            val categoriesByCourse = allCategories.groupBy { it.category.courseId }
            courseList.map { course ->
                val categories = categoriesByCourse[course.id].orEmpty()
                
                var totalEvaluatedWeightage = 0.0
                var totalObtainedPercentage = 0.0

                for (catWithItems in categories) {
                    val category = catWithItems.category
                    val items = catWithItems.items

                    val consideredCount = category.bestOf ?: category.itemCount
                    val itemWeightage = if (consideredCount > 0) category.weightage / consideredCount else 0.0

                    // Filter items that have been evaluated (totalMarks > 0)
                    val evaluatedItems = items.filter { it.totalMarks != null && it.totalMarks > 0 }
                        .map { item ->
                            val obt = item.marksObtained ?: 0.0
                            val max = item.totalMarks ?: 1.0
                            (obt / max) * itemWeightage
                        }
                        .sortedDescending() // sort highest to lowest

                    // We only consider up to `consideredCount` items
                    val itemsToCount = minOf(evaluatedItems.size, consideredCount)
                    
                    if (itemsToCount > 0) {
                        totalEvaluatedWeightage += (itemsToCount * itemWeightage)
                        totalObtainedPercentage += evaluatedItems.take(itemsToCount).sum()
                    }
                }

                CourseEvaluationUi(
                    id = course.id,
                    name = course.name,
                    totalEvaluatedWeightage = totalEvaluatedWeightage,
                    totalObtainedPercentage = totalObtainedPercentage
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addCourse(name: String) {
        viewModelScope.launch {
            repository.addCourse(name)
        }
    }

    fun updateCourseOrders(courseIds: List<Long>) {
        viewModelScope.launch {
            repository.updateCourseOrders(courseIds)
        }
    }

    fun renameCourse(courseId: Long, name: String) {
        viewModelScope.launch {
            repository.renameCourse(courseId, name)
        }
    }

    fun deleteCourse(courseId: Long) {
        viewModelScope.launch {
            repository.deleteCourse(courseId)
        }
    }

    class Factory(private val appContext: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val database = SemTrackDatabase.getInstance(appContext)
            val repository = EvaluationRepository(
                courseDao = database.courseDao(),
                evaluationDao = database.evaluationDao()
            )
            return EvaluationsViewModel(repository) as T
        }
    }
}
