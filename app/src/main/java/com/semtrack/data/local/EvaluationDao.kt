package com.semtrack.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Transaction
import androidx.room.Embedded
import androidx.room.Relation
import kotlinx.coroutines.flow.Flow

data class CategoryWithItems(
    @Embedded val category: EvaluationCategoryEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "categoryId"
    )
    val items: List<EvaluationItemEntity>
)

@Dao
interface EvaluationDao {
    @Transaction
    @Query("SELECT * FROM evaluation_categories WHERE courseId = :courseId ORDER BY id ASC")
    fun observeCategoriesWithItems(courseId: Long): Flow<List<CategoryWithItems>>

    @Transaction
    @Query("SELECT * FROM evaluation_categories")
    fun observeAllCategoriesWithItems(): Flow<List<CategoryWithItems>>

    @Insert
    suspend fun insertCategory(category: EvaluationCategoryEntity): Long

    @Insert
    suspend fun insertItems(items: List<EvaluationItemEntity>)

    @Update
    suspend fun updateCategory(category: EvaluationCategoryEntity)

    @Update
    suspend fun updateItem(item: EvaluationItemEntity)

    @Query("DELETE FROM evaluation_categories WHERE id = :categoryId")
    suspend fun deleteCategory(categoryId: Long)

    @Query("DELETE FROM evaluation_items WHERE id = :itemId")
    suspend fun deleteItem(itemId: Long)
}
