package com.example.spendwise.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Model representing an expense category.
 */
@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val icon: String,
    val isDefault: Boolean = true,
    val isActive: Boolean = true,
    val displayOrder: Int = 0
)
