package com.example.spendwise.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.spendwise.data.local.entity.Category
import com.example.spendwise.data.local.entity.ExpenseWithCategory
import com.example.spendwise.data.local.entity.PersonalExpense
import com.example.spendwise.data.repository.SpendWiseRepository
import com.example.spendwise.domain.model.BudgetStatus
import com.example.spendwise.domain.model.Currency
import com.example.spendwise.domain.model.CurrencyFormatter
import com.example.spendwise.domain.model.DateUtils
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CategoryShare(
    val category: Category,
    val amountPaise: Long,
    val percentage: Float
)

data class QuickAddUiState(
    val amountInput: String = "",
    val selectedCategoryId: Long? = 1L,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val errorMessage: String? = null,
    val isSaving: Boolean = false,
    val saveSuccessAnimation: Boolean = false
)

data class DashboardUiState(
    val selectedMonthKey: String = DateUtils.currentMonthKey(),
    val monthTitle: String = DateUtils.formatMonthTitle(DateUtils.currentMonthKey()),
    val totalSpentPaise: Long = 0L,
    val budgetPaise: Long = 0L,
    val remainingPaise: Long = 0L,
    val isOverBudget: Boolean = false,
    val overBudgetPaise: Long = 0L,
    val budgetStatus: BudgetStatus = BudgetStatus.SAFE,
    val progressRatio: Float = 0f,
    val todaySpentPaise: Long = 0L,
    val categoryBreakdown: List<CategoryShare> = emptyList(),
    val recentExpenses: List<ExpenseWithCategory> = emptyList()
)

data class HistoryUiState(
    val searchQuery: String = "",
    val selectedCategoryIdFilter: Long? = null,
    val selectedMonthFilter: String? = null,
    val filteredExpenses: List<ExpenseWithCategory> = emptyList()
)

class SpendWiseViewModel(application: Application) : AndroidViewModel(application) {

    val repository = SpendWiseRepository(application)

    // Current navigation tab
    private val _currentTab = MutableStateFlow(NavigationTab.QUICK_ADD)
    val currentTab: StateFlow<NavigationTab> = _currentTab.asStateFlow()

    // Currency
    val currency: StateFlow<Currency> = repository.currencyFlow

    // Categories
    val activeCategories: StateFlow<List<Category>> = repository.getActiveCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCategories: StateFlow<List<Category>> = repository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All expenses stream
    val allExpenses: StateFlow<List<ExpenseWithCategory>> = repository.getAllExpenses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Quick Add State
    private val _quickAddState = MutableStateFlow(QuickAddUiState())
    val quickAddState: StateFlow<QuickAddUiState> = _quickAddState.asStateFlow()

    // Dashboard Month Key
    private val _selectedMonthKey = MutableStateFlow(DateUtils.currentMonthKey())
    val selectedMonthKey: StateFlow<String> = _selectedMonthKey.asStateFlow()

    // Budget for selected month
    private val _selectedMonthBudget = MutableStateFlow(0L)
    val selectedMonthBudget: StateFlow<Long> = _selectedMonthBudget.asStateFlow()

    // History filter states
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategoryFilter = MutableStateFlow<Long?>(null)
    val selectedCategoryFilter: StateFlow<Long?> = _selectedCategoryFilter.asStateFlow()

    // Toast / Feedback events
    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    // Editing Expense state for Dialog
    private val _editingExpense = MutableStateFlow<ExpenseWithCategory?>(null)
    val editingExpense: StateFlow<ExpenseWithCategory?> = _editingExpense.asStateFlow()

    // Deleting Expense state for Dialog
    private val _deletingExpense = MutableStateFlow<ExpenseWithCategory?>(null)
    val deletingExpense: StateFlow<ExpenseWithCategory?> = _deletingExpense.asStateFlow()

    // Budget setting dialog state
    private val _showBudgetDialog = MutableStateFlow(false)
    val showBudgetDialog: StateFlow<Boolean> = _showBudgetDialog.asStateFlow()

    // Spending report / Invoice sheet dialog state
    private val _showSpendingReport = MutableStateFlow(false)
    val showSpendingReport: StateFlow<Boolean> = _showSpendingReport.asStateFlow()

    // Onboarding dialog state
    private val _showOnboarding = MutableStateFlow(!repository.isOnboardingCompleted())
    val showOnboarding: StateFlow<Boolean> = _showOnboarding.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureCategoriesSeeded()
        }
        observeBudgetForSelectedMonth()
    }

    private fun observeBudgetForSelectedMonth() {
        viewModelScope.launch {
            _selectedMonthKey.collect { monthKey ->
                repository.getBudgetForMonth(monthKey).collect { budget ->
                    _selectedMonthBudget.value = budget?.amountPaise ?: 0L
                }
            }
        }
    }

    // Navigation
    fun navigateTo(tab: NavigationTab) {
        _currentTab.value = tab
    }

    // --- QUICK ADD KEYPAD & ACTIONS ---

    fun onKeypadDigit(digit: Char) {
        val current = _quickAddState.value.amountInput
        // Maximum reasonable transaction limit prevention (e.g. max 9 chars)
        if (current.length >= 9) return

        if (digit == '.') {
            if (current.contains('.')) return
            val next = if (current.isEmpty()) "0." else "$current."
            _quickAddState.value = _quickAddState.value.copy(amountInput = next, errorMessage = null)
            return
        }

        if (current == "0" && digit != '.') {
            _quickAddState.value = _quickAddState.value.copy(amountInput = digit.toString(), errorMessage = null)
            return
        }

        // Decimal precision: max 2 decimal places
        if (current.contains('.')) {
            val decimals = current.substringAfter('.')
            if (decimals.length >= 2) return
        }

        _quickAddState.value = _quickAddState.value.copy(
            amountInput = current + digit,
            errorMessage = null
        )
    }

    fun onKeypadBackspace() {
        val current = _quickAddState.value.amountInput
        if (current.isNotEmpty()) {
            _quickAddState.value = _quickAddState.value.copy(
                amountInput = current.dropLast(1),
                errorMessage = null
            )
        }
    }

    fun onKeypadClear() {
        _quickAddState.value = _quickAddState.value.copy(
            amountInput = "",
            errorMessage = null
        )
    }

    fun setQuickAddCategory(categoryId: Long) {
        _quickAddState.value = _quickAddState.value.copy(
            selectedCategoryId = categoryId,
            errorMessage = null
        )
    }

    fun setQuickAddNote(note: String) {
        _quickAddState.value = _quickAddState.value.copy(
            note = note.take(120) // PRD: Maximum character limit
        )
    }

    fun setQuickAddTimestamp(timestamp: Long) {
        _quickAddState.value = _quickAddState.value.copy(
            timestamp = timestamp
        )
    }

    fun saveExpense() {
        val state = _quickAddState.value
        if (state.isSaving) return

        val paise = CurrencyFormatter.parseInputToPaise(state.amountInput)
        if (paise <= 0L) {
            _quickAddState.value = state.copy(errorMessage = "Please enter an amount greater than 0")
            return
        }

        val categoryId = state.selectedCategoryId ?: 1L

        viewModelScope.launch {
            _quickAddState.value = state.copy(isSaving = true)
            try {
                repository.insertExpense(
                    amountPaise = paise,
                    categoryId = categoryId,
                    note = state.note,
                    timestamp = state.timestamp
                )

                // Flash confirmation and reset input ready for next quick add
                val formatted = CurrencyFormatter.formatPaise(paise, currency.value.symbol)
                _toastEvent.emit("Added $formatted")

                _quickAddState.value = QuickAddUiState(
                    amountInput = "",
                    selectedCategoryId = categoryId, // preserve last selected category
                    note = "",
                    timestamp = System.currentTimeMillis(),
                    saveSuccessAnimation = true
                )
            } catch (e: Exception) {
                _quickAddState.value = state.copy(
                    isSaving = false,
                    errorMessage = "Unable to save expense. Please try again."
                )
            }
        }
    }

    fun clearSaveSuccessAnimation() {
        _quickAddState.value = _quickAddState.value.copy(saveSuccessAnimation = false)
    }

    // --- DASHBOARD ACTIONS ---

    fun previousMonth() {
        _selectedMonthKey.value = DateUtils.shiftMonth(_selectedMonthKey.value, -1)
    }

    fun nextMonth() {
        _selectedMonthKey.value = DateUtils.shiftMonth(_selectedMonthKey.value, 1)
    }

    fun setMonth(monthKey: String) {
        _selectedMonthKey.value = monthKey
    }

    fun saveMonthlyBudget(amountPaise: Long) {
        viewModelScope.launch {
            repository.setMonthlyBudget(_selectedMonthKey.value, amountPaise)
            _selectedMonthBudget.value = amountPaise
            _showBudgetDialog.value = false
            _toastEvent.emit("Budget updated")
        }
    }

    fun openBudgetDialog() {
        _showBudgetDialog.value = true
    }

    fun closeBudgetDialog() {
        _showBudgetDialog.value = false
    }

    fun openSpendingReport() {
        _showSpendingReport.value = true
    }

    fun closeSpendingReport() {
        _showSpendingReport.value = false
    }

    // --- EDIT & DELETE EXPENSE ---

    fun startEditingExpense(expenseWithCategory: ExpenseWithCategory) {
        _editingExpense.value = expenseWithCategory
    }

    fun dismissEditingExpense() {
        _editingExpense.value = null
    }

    fun updateExpense(id: Long, amountPaise: Long, categoryId: Long, note: String?, timestamp: Long) {
        viewModelScope.launch {
            repository.updateExpense(id, amountPaise, categoryId, note, timestamp)
            _editingExpense.value = null
            _toastEvent.emit("Expense updated")
        }
    }

    fun startDeletingExpense(expenseWithCategory: ExpenseWithCategory) {
        _deletingExpense.value = expenseWithCategory
    }

    fun dismissDeletingExpense() {
        _deletingExpense.value = null
    }

    fun confirmDeleteExpense() {
        val item = _deletingExpense.value ?: return
        viewModelScope.launch {
            repository.deleteExpense(item.expense)
            _deletingExpense.value = null
            _toastEvent.emit("Expense deleted")
        }
    }

    // --- HISTORY ACTIONS ---

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCategoryFilter(categoryId: Long?) {
        _selectedCategoryFilter.value = categoryId
    }

    // --- SETTINGS ACTIONS ---

    fun setCurrency(currency: Currency) {
        repository.setCurrency(currency)
    }

    fun dismissOnboarding() {
        repository.setOnboardingCompleted()
        _showOnboarding.value = false
    }

    fun completeOnboarding(currency: Currency, initialBudgetPaise: Long) {
        repository.setCurrency(currency)
        if (initialBudgetPaise > 0L) {
            viewModelScope.launch {
                repository.setMonthlyBudget(DateUtils.currentMonthKey(), initialBudgetPaise)
            }
        }
        repository.setOnboardingCompleted()
        _showOnboarding.value = false
    }

    fun toggleCategoryActive(category: Category) {
        viewModelScope.launch {
            repository.updateCategory(category.copy(isActive = !category.isActive))
        }
    }

    fun loadSampleData() {
        viewModelScope.launch {
            repository.seedSampleData()
            _toastEvent.emit("Sample expenses loaded")
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            _toastEvent.emit("All data cleared")
        }
    }
}

enum class NavigationTab {
    QUICK_ADD,
    DASHBOARD,
    HISTORY,
    SETTINGS
}
