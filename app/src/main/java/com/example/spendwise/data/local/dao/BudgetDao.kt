package com.example.spendwise.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.spendwise.data.local.entity.MonthlyBudget
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    @Query("SELECT * FROM budgets WHERE monthKey = :monthKey")
    fun getBudgetForMonth(monthKey: String): Flow<MonthlyBudget?>

    @Query("SELECT * FROM budgets WHERE monthKey = :monthKey")
    suspend fun getBudgetSync(monthKey: String): MonthlyBudget?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setBudget(budget: MonthlyBudget)

    @Query("SELECT * FROM budgets ORDER BY monthKey DESC")
    fun getAllBudgets(): Flow<List<MonthlyBudget>>

    @Query("DELETE FROM budgets")
    suspend fun clearAll()
}
