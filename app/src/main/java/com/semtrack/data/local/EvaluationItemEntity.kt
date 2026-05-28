package com.semtrack.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "evaluation_items",
    foreignKeys = [
        ForeignKey(
            entity = EvaluationCategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("categoryId")
    ]
)
data class EvaluationItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long,
    val name: String,
    val totalMarks: Double? = null,
    val marksObtained: Double? = null
)
