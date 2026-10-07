package com.example.spendwise.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Relation model combining PersonalExpense with its associated Category.
 */
data class ExpenseWithCategory(
    @Embedded
    val expense: PersonalExpense,

    @Relation(
        parentColumn = "categoryId",
        entityColumn = "id"
    )
    val category: Category?
)
