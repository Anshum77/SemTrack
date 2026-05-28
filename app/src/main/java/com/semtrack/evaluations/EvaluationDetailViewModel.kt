package com.semtrack.evaluations

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.semtrack.data.EvaluationRepository
import com.semtrack.data.local.CategoryWithItems
import com.semtrack.data.local.SemTrackDatabase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class EvaluationDetailUi(
    val courseId: Long,
    val totalEvaluatedWeightage: Double = 0.0,
    val totalObtainedPercentage: Double = 0.0,
    val categories: List<CategoryWithItems> = emptyList()
)

class EvaluationDetailViewModel(
    private val courseId: Long,
    private val repository: EvaluationRepository
) : ViewModel() {

    val uiState: StateFlow<EvaluationDetailUi> = repository.observeCategoriesWithItems(courseId)
        .map { categories ->
            var totalEvaluatedWeightage = 0.0
            var totalObtainedPercentage = 0.0

            for (catWithItems in categories) {
                val category = catWithItems.category
                val items = catWithItems.items

                val consideredCount = category.bestOf ?: category.itemCount
                val itemWeightage = if (consideredCount > 0) category.weightage / consideredCount else 0.0

                val evaluatedItems = items.filter { it.totalMarks != null && it.totalMarks > 0 }
                    .map { item ->
                        val obt = item.marksObtained ?: 0.0
                        val max = item.totalMarks ?: 1.0
                        (obt / max) * itemWeightage
                    }
                    .sortedDescending()

                val itemsToCount = minOf(evaluatedItems.size, consideredCount)
                
                if (itemsToCount > 0) {
                    totalEvaluatedWeightage += (itemsToCount * itemWeightage)
                    totalObtainedPercentage += evaluatedItems.take(itemsToCount).sum()
                }
            }

            EvaluationDetailUi(
                courseId = courseId,
                totalEvaluatedWeightage = totalEvaluatedWeightage,
                totalObtainedPercentage = totalObtainedPercentage,
                categories = categories
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), EvaluationDetailUi(courseId))

    fun addCategory(name: String, weightage: Double, itemCount: Int, bestOf: Int?) {
        viewModelScope.launch {
            repository.addCategoryWithItems(courseId, name, weightage, itemCount, bestOf)
        }
    }

    fun updateItemMarks(itemId: Long, categoryId: Long, name: String, totalMarks: Double?, marksObtained: Double?) {
        viewModelScope.launch {
            repository.updateItemMarks(itemId, categoryId, name, totalMarks, marksObtained)
        }
    }

    fun deleteCategory(id: Long) {
        viewModelScope.launch {
            repository.deleteCategory(id)
        }
    }

    class Factory(
        private val appContext: Context,
        private val courseId: Long
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val database = SemTrackDatabase.getInstance(appContext)
            val repository = EvaluationRepository(
                courseDao = database.courseDao(),
                evaluationDao = database.evaluationDao()
            )
            return EvaluationDetailViewModel(courseId, repository) as T
        }
    }
}
