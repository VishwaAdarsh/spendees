package com.example.spendwise.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Model representing a monthly budget allocation.
 * monthKey is stored as "YYYY-MM" (e.g., "2026-10").
 */
@Entity(tableName = "budgets")
data class MonthlyBudget(
    @PrimaryKey
    val monthKey: String,
    val amountPaise: Long
)
