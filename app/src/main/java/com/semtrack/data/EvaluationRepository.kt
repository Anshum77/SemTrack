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
        weightage: Double?,
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

    suspend fun editCategoryWithItems(
        categoryId: Long,
        name: String,
        weightage: Double?,
        itemCount: Int,
        bestOf: Int?
    ): Boolean {
        return try {
            val category = evaluationDao.getCategoryById(categoryId) ?: return false
            val items = evaluationDao.getItemsForCategory(categoryId)

            // Update category
            val updatedCategory = category.copy(
                name = name,
                weightage = weightage,
                itemCount = itemCount,
                bestOf = bestOf
            )
            evaluationDao.updateCategory(updatedCategory)

            // Handle items
            val currentCount = items.size
            if (itemCount > currentCount) {
                // Add new items
                val newItems = (currentCount + 1..itemCount).map { i ->
                    EvaluationItemEntity(
                        categoryId = categoryId,
                        name = "$name $i",
                        totalMarks = null,
                        marksObtained = null
                    )
                }
                evaluationDao.insertItems(newItems)
            } else if (itemCount < currentCount) {
                // Delete items from the end, but stop if we hit one with marks
                var itemsToDelete = currentCount - itemCount
                for (i in items.indices.reversed()) {
                    if (itemsToDelete <= 0) break
                    val item = items[i]
                    if (item.totalMarks == null && item.marksObtained == null) {
                        evaluationDao.deleteItem(item.id)
                        itemsToDelete--
                    } else {
                        // Stop deleting if we hit an item with marks
                        break
                    }
                }
                
                // If we couldn't delete enough items because they had marks,
                // update the category's itemCount to reflect the actual remaining count
                val actualCount = currentCount - (currentCount - itemCount - itemsToDelete)
                if (actualCount != itemCount) {
                    evaluationDao.updateCategory(updatedCategory.copy(itemCount = actualCount))
                }
            }

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
