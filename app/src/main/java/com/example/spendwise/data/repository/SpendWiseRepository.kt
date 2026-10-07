package com.example.spendwise.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.spendwise.data.local.SpendWiseDatabase
import com.example.spendwise.data.local.entity.Category
import com.example.spendwise.data.local.entity.ExpenseWithCategory
import com.example.spendwise.data.local.entity.MonthlyBudget
import com.example.spendwise.data.local.entity.PersonalExpense
import com.example.spendwise.domain.model.Currency
import com.example.spendwise.domain.model.CurrencyFormatter
import com.example.spendwise.domain.model.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class SpendWiseRepository(private val context: Context) {

    private val db = SpendWiseDatabase.getInstance(context)
    private val expenseDao = db.personalExpenseDao()
    private val categoryDao = db.categoryDao()
    private val budgetDao = db.budgetDao()

    private val prefs: SharedPreferences =
        context.getSharedPreferences("spendwise_prefs", Context.MODE_PRIVATE)

    private val _currencyFlow = MutableStateFlow(
        Currency.fromCode(prefs.getString("selected_currency", "INR") ?: "INR")
    )
    val currencyFlow: StateFlow<Currency> = _currencyFlow.asStateFlow()

    fun getAllExpenses(): Flow<List<ExpenseWithCategory>> =
        expenseDao.getAllExpensesWithCategory()

    fun getExpensesForMonth(monthKey: String): Flow<List<ExpenseWithCategory>> {
        val (start, end) = DateUtils.getStartAndEndOfMonth(monthKey)
        return expenseDao.getExpensesBetweenWithCategory(start, end)
    }

    fun getRecentExpenses(limit: Int = 10): Flow<List<ExpenseWithCategory>> =
        expenseDao.getRecentExpensesWithCategory(limit)

    fun getActiveCategories(): Flow<List<Category>> =
        categoryDao.getActiveCategories()

    fun getAllCategories(): Flow<List<Category>> =
        categoryDao.getAllCategories()

    fun getBudgetForMonth(monthKey: String): Flow<MonthlyBudget?> =
        budgetDao.getBudgetForMonth(monthKey)

    suspend fun insertExpense(
        amountPaise: Long,
        categoryId: Long,
        note: String?,
        timestamp: Long = System.currentTimeMillis()
    ): Long = withContext(Dispatchers.IO) {
        ensureCategoriesSeeded()
        val expense = PersonalExpense(
            amountPaise = amountPaise,
            categoryId = categoryId,
            note = note?.trim()?.ifEmpty { null },
            timestamp = timestamp
        )
        expenseDao.insert(expense)
    }

    suspend fun updateExpense(
        id: Long,
        amountPaise: Long,
        categoryId: Long,
        note: String?,
        timestamp: Long
    ) = withContext(Dispatchers.IO) {
        val expense = PersonalExpense(
            id = id,
            amountPaise = amountPaise,
            categoryId = categoryId,
            note = note?.trim()?.ifEmpty { null },
            timestamp = timestamp
        )
        expenseDao.update(expense)
    }

    suspend fun deleteExpense(expense: PersonalExpense) = withContext(Dispatchers.IO) {
        expenseDao.delete(expense)
    }

    suspend fun deleteExpenseById(id: Long) = withContext(Dispatchers.IO) {
        expenseDao.deleteById(id)
    }

    suspend fun setMonthlyBudget(monthKey: String, amountPaise: Long) = withContext(Dispatchers.IO) {
        budgetDao.setBudget(MonthlyBudget(monthKey = monthKey, amountPaise = amountPaise))
    }

    suspend fun updateCategory(category: Category) = withContext(Dispatchers.IO) {
        categoryDao.update(category)
    }

    fun setCurrency(currency: Currency) {
        prefs.edit().putString("selected_currency", currency.code).apply()
        _currencyFlow.value = currency
    }

    fun isOnboardingCompleted(): Boolean {
        return prefs.getBoolean("onboarding_completed", false)
    }

    fun setOnboardingCompleted() {
        prefs.edit().putBoolean("onboarding_completed", true).apply()
    }

    suspend fun ensureCategoriesSeeded() = withContext(Dispatchers.IO) {
        val existing = categoryDao.getAllCategoriesSync()
        val existingNames = existing.map { it.name.lowercase() }.toSet()
        val missing = SpendWiseDatabase.DEFAULT_CATEGORIES.filter {
            !existingNames.contains(it.name.lowercase())
        }
        if (missing.isNotEmpty()) {
            categoryDao.insertAll(missing)
        }
    }

    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        expenseDao.clearAll()
        budgetDao.clearAll()
        // Re-seed default categories
        categoryDao.insertAll(SpendWiseDatabase.DEFAULT_CATEGORIES)
    }

    /**
     * Generates CSV format string conforming to PRD Section 50:
     * Date,Time,Amount,Category,Note
     */
    suspend fun generateCsvData(): String = withContext(Dispatchers.IO) {
        val allExpenses = expenseDao.getExpensesBetweenSync(0L, Long.MAX_VALUE)
        generateCsvForExpenses(allExpenses)
    }

    fun generateCsvForExpenses(expenses: List<ExpenseWithCategory>): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.US)

        val sb = StringBuilder()
        sb.append("Date,Time,Amount,Category,Note\n")

        for (item in expenses) {
            val dateStr = dateFormat.format(Date(item.expense.timestamp))
            val timeStr = timeFormat.format(Date(item.expense.timestamp))
            val amountFormatted = String.format(Locale.US, "%.2f", item.expense.amountPaise / 100.0)
            val categoryName = item.category?.name ?: "Other"
            val noteClean = (item.expense.note ?: "").replace("\"", "\"\"")
            sb.append("\"$dateStr\",\"$timeStr\",$amountFormatted,\"$categoryName\",\"$noteClean\"\n")
        }

        return sb.toString()
    }

    /**
     * Generates a formal, beautiful text-based spending invoice / financial statement
     * detailing period, totals, category distribution, and itemized ledger.
     */
    fun generateInvoiceStatement(
        periodTitle: String,
        expenses: List<ExpenseWithCategory>,
        currencySymbol: String
    ): String {
        val totalPaise = expenses.sumOf { it.expense.amountPaise }
        val totalFormatted = CurrencyFormatter.formatPaise(totalPaise, currencySymbol)
        val fullDateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US)
        val issueDate = SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date())

        val sb = StringBuilder()
        sb.append("=========================================\n")
        sb.append("        SPENDWISE EXPENSE STATEMENT      \n")
        sb.append("=========================================\n")
        sb.append("Statement Period : $periodTitle\n")
        sb.append("Generated On     : $issueDate\n")
        sb.append("Total Entries    : ${expenses.size}\n")
        sb.append("TOTAL SPENT      : $totalFormatted\n")
        sb.append("-----------------------------------------\n")
        sb.append("CATEGORY BREAKDOWN:\n")

        val grouped = expenses.groupBy { it.category?.name ?: "Other" }
            .mapValues { (_, list) -> list.sumOf { it.expense.amountPaise } }
            .toList()
            .sortedByDescending { it.second }

        for ((catName, amount) in grouped) {
            val pct = if (totalPaise > 0L) (amount.toDouble() / totalPaise.toDouble()) * 100.0 else 0.0
            val amtStr = CurrencyFormatter.formatPaise(amount, currencySymbol)
            val line = String.format(Locale.US, "• %-18s %12s (%4.1f%%)\n", catName.take(18), amtStr, pct)
            sb.append(line)
        }

        sb.append("-----------------------------------------\n")
        sb.append("ITEMIZED TRANSACTIONS:\n")

        expenses.sortedByDescending { it.expense.timestamp }.forEachIndexed { index, item ->
            val dateStr = fullDateFormat.format(Date(item.expense.timestamp))
            val cat = item.category?.name ?: "Other"
            val amtStr = CurrencyFormatter.formatPaise(item.expense.amountPaise, currencySymbol)
            val note = if (!item.expense.note.isNullOrBlank()) " - ${item.expense.note}" else ""

            sb.append("${index + 1}. $dateStr\n")
            sb.append("   [$cat] $amtStr$note\n")
        }

        sb.append("=========================================\n")
        sb.append("       Thank you for using SpendWise     \n")
        sb.append("         100% Offline & Private          \n")
        sb.append("=========================================\n")

        return sb.toString()
    }

    /**
     * Preloads realistic sample expenses to immediately showcase analytics,
     * category breakdown, and budget progress.
     */
    suspend fun seedSampleData() = withContext(Dispatchers.IO) {
        ensureCategoriesSeeded()
        val currentMonthKey = DateUtils.currentMonthKey()
        // Set sample monthly budget of 20,000 (2000000 paise)
        budgetDao.setBudget(MonthlyBudget(monthKey = currentMonthKey, amountPaise = 2000000L))

        val now = System.currentTimeMillis()
        val oneDay = 86400000L
        val oneHour = 3600000L

        val sampleList = listOf(
            PersonalExpense(amountPaise = 25000L, categoryId = 1, note = "Lunch with team", timestamp = now - 2 * oneHour),
            PersonalExpense(amountPaise = 8000L, categoryId = 2, note = "Metro ticket", timestamp = now - 4 * oneHour),
            PersonalExpense(amountPaise = 120000L, categoryId = 4, note = "New running shoes", timestamp = now - oneDay - 3 * oneHour),
            PersonalExpense(amountPaise = 30000L, categoryId = 1, note = "Groceries & Vegetables", timestamp = now - oneDay - 6 * oneHour),
            PersonalExpense(amountPaise = 300000L, categoryId = 3, note = "Electricity & WiFi bill", timestamp = now - 2 * oneDay),
            PersonalExpense(amountPaise = 45000L, categoryId = 5, note = "Movie night tickets", timestamp = now - 3 * oneDay),
            PersonalExpense(amountPaise = 15000L, categoryId = 8, note = "Music streaming subscription", timestamp = now - 4 * oneDay),
            PersonalExpense(amountPaise = 65000L, categoryId = 6, note = "Dental checkup & medicine", timestamp = now - 5 * oneDay),
            PersonalExpense(amountPaise = 18000L, categoryId = 2, note = "Cab commute", timestamp = now - 6 * oneDay),
            PersonalExpense(amountPaise = 52000L, categoryId = 7, note = "Online course book", timestamp = now - 7 * oneDay)
        )

        expenseDao.insertAll(sampleList)
    }
}
