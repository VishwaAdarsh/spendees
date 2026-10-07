package com.example.spendwise.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.spendwise.data.local.entity.ExpenseWithCategory
import com.example.spendwise.data.local.entity.PersonalExpense
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonalExpenseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: PersonalExpense): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(expenses: List<PersonalExpense>)

    @Update
    suspend fun update(expense: PersonalExpense)

    @Delete
    suspend fun delete(expense: PersonalExpense)

    @Query("DELETE FROM personal_expenses WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM personal_expenses WHERE id = :id")
    suspend fun getExpenseById(id: Long): PersonalExpense?

    @Transaction
    @Query("SELECT * FROM personal_expenses ORDER BY timestamp DESC")
    fun getAllExpensesWithCategory(): Flow<List<ExpenseWithCategory>>

    @Transaction
    @Query("SELECT * FROM personal_expenses WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    fun getExpensesBetweenWithCategory(startTime: Long, endTime: Long): Flow<List<ExpenseWithCategory>>

    @Transaction
    @Query("SELECT * FROM personal_expenses WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    suspend fun getExpensesBetweenSync(startTime: Long, endTime: Long): List<ExpenseWithCategory>

    @Transaction
    @Query("SELECT * FROM personal_expenses ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentExpensesWithCategory(limit: Int): Flow<List<ExpenseWithCategory>>

    @Query("SELECT SUM(amountPaise) FROM personal_expenses WHERE timestamp >= :startTime AND timestamp <= :endTime")
    fun getTotalSpentBetween(startTime: Long, endTime: Long): Flow<Long?>

    @Query("DELETE FROM personal_expenses")
    suspend fun clearAll()
}
