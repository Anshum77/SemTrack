package com.semtrack.data

import com.semtrack.data.local.CategoryWithItems
import com.semtrack.data.local.CourseDao
import com.semtrack.data.local.CourseEntity
import com.semtrack.data.local.EvaluationCategoryEntity
import com.semtrack.data.local.EvaluationDao
import com.semtrack.data.local.EvaluationItemEntity
import kotlinx.coroutines.flow.Flow

class EvaluationRepository(
    private val courseDao: CourseDao,
    private val evaluationDao: EvaluationDao
) {
    fun observeCourses(): Flow<List<CourseEntity>> = courseDao.observeCourses()

    suspend fun addCourse(name: String) {
        val maxSort = courseDao.getMaxSortOrder()
        courseDao.insert(CourseEntity(name = name, sortOrder = maxSort + 1))
    }

    suspend fun updateCourseOrders(courseIds: List<Long>) {
        courseIds.forEachIndexed { index, id ->
            courseDao.updateSortOrder(id, index)
        }
    }

    suspend fun renameCourse(courseId: Long, name: String) {
        courseDao.renameCourse(courseId, name)
    }

    suspend fun deleteCourse(courseId: Long) {
        courseDao.deleteCourse(courseId)
    }

    fun observeAllCategoriesWithItems(): Flow<List<CategoryWithItems>> = evaluationDao.observeAllCategoriesWithItems()

    fun observeCategoriesWithItems(courseId: Long): Flow<List<CategoryWithItems>> = evaluationDao.observeCategoriesWithItems(courseId)

    suspend fun addCategoryWithItems(
        courseId: Long,
        name: String,
        weightage: Double,
        itemCount: Int,
        bestOf: Int?
    ): Boolean {
        return try {
            val category = EvaluationCategoryEntity(
                courseId = courseId,
                name = name,
                weightage = weightage,
                itemCount = itemCount,
                bestOf = bestOf
            )
            val categoryId = evaluationDao.insertCategory(category)
            
            val items = (1..itemCount).map { i ->
                EvaluationItemEntity(
                    categoryId = categoryId,
                    name = "$name $i",
                    totalMarks = null,
                    marksObtained = null
                )
            }
            evaluationDao.insertItems(items)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun updateItemMarks(
        itemId: Long,
        categoryId: Long,
        name: String,
        totalMarks: Double?,
        marksObtained: Double?
    ): Boolean {
        return try {
            val item = EvaluationItemEntity(
                id = itemId,
                categoryId = categoryId,
                name = name,
                totalMarks = totalMarks,
                marksObtained = marksObtained
            )
            evaluationDao.updateItem(item)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteCategory(categoryId: Long) {
        evaluationDao.deleteCategory(categoryId)
    }
}
