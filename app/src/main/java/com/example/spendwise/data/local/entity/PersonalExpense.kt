package com.example.spendwise.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Model representing a single expense record.
 * Uses amountPaise (Long) to store currency in cents/paise and avoid
 * floating point rounding errors.
 */
@Entity(tableName = "personal_expenses")
data class PersonalExpense(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amountPaise: Long,
    val categoryId: Long,
    val note: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
