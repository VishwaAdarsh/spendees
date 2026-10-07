package com.example.spendwise.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.spendwise.domain.model.DateUtils
import com.example.spendwise.presentation.NavigationTab
import com.example.spendwise.presentation.SpendWiseViewModel
import com.example.spendwise.ui.components.DeleteConfirmDialog
import com.example.spendwise.ui.components.EditExpenseDialog
import com.example.spendwise.ui.components.OnboardingDialog
import com.example.spendwise.ui.components.SetBudgetDialog
import com.example.spendwise.ui.components.SpendingReportDialog
import com.example.spendwise.ui.screens.DashboardScreen
import com.example.spendwise.ui.screens.HistoryScreen
import com.example.spendwise.ui.screens.QuickAddScreen
import com.example.spendwise.ui.screens.SettingsScreen
import kotlinx.coroutines.flow.collectLatest

@Composable
fun MainScreen(
    viewModel: SpendWiseViewModel,
    modifier: Modifier = Modifier
) {
    val currentTab by viewModel.currentTab.collectAsState()
    val currency by viewModel.currency.collectAsState()
    val allCategories by viewModel.allCategories.collectAsState()
    val selectedMonthKey by viewModel.selectedMonthKey.collectAsState()
    val selectedMonthBudget by viewModel.selectedMonthBudget.collectAsState()

    val editingExpense by viewModel.editingExpense.collectAsState()
    val deletingExpense by viewModel.deletingExpense.collectAsState()
    val showBudgetDialog by viewModel.showBudgetDialog.collectAsState()
    val showSpendingReport by viewModel.showSpendingReport.collectAsState()
    val showOnboarding by viewModel.showOnboarding.collectAsState()
    val allExpenses by viewModel.allExpenses.collectAsState()

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Toast and Snackbar events
    LaunchedEffect(Unit) {
        viewModel.toastEvent.collectLatest { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    // Predictive back / BackHandler: if on Dashboard, History, or Settings, pressing back takes user to Quick Add!
    if (currentTab != NavigationTab.QUICK_ADD) {
        BackHandler {
            viewModel.navigateTo(NavigationTab.QUICK_ADD)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 3.dp,
                modifier = Modifier.testTag("bottom_navigation_bar")
            ) {
                // 1. Quick Add
                NavigationBarItem(
                    selected = currentTab == NavigationTab.QUICK_ADD,
                    onClick = { viewModel.navigateTo(NavigationTab.QUICK_ADD) },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == NavigationTab.QUICK_ADD) Icons.Filled.AddCircle else Icons.Outlined.AddCircleOutline,
                            contentDescription = "Quick Add"
                        )
                    },
                    label = {
                        Text(
                            text = "Add",
                            fontWeight = if (currentTab == NavigationTab.QUICK_ADD) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedIconColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("nav_item_quick_add")
                )

                // 2. Dashboard
                NavigationBarItem(
                    selected = currentTab == NavigationTab.DASHBOARD,
                    onClick = { viewModel.navigateTo(NavigationTab.DASHBOARD) },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == NavigationTab.DASHBOARD) Icons.Filled.BarChart else Icons.Outlined.BarChart,
                            contentDescription = "Dashboard"
                        )
                    },
                    label = {
                        Text(
                            text = "Dashboard",
                            fontWeight = if (currentTab == NavigationTab.DASHBOARD) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedIconColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("nav_item_dashboard")
                )

                // 3. History
                NavigationBarItem(
                    selected = currentTab == NavigationTab.HISTORY,
                    onClick = { viewModel.navigateTo(NavigationTab.HISTORY) },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == NavigationTab.HISTORY) Icons.Filled.History else Icons.Outlined.History,
                            contentDescription = "History"
                        )
                    },
                    label = {
                        Text(
                            text = "History",
                            fontWeight = if (currentTab == NavigationTab.HISTORY) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedIconColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("nav_item_history")
                )

                // 4. Settings
                NavigationBarItem(
                    selected = currentTab == NavigationTab.SETTINGS,
                    onClick = { viewModel.navigateTo(NavigationTab.SETTINGS) },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == NavigationTab.SETTINGS) Icons.Filled.Settings else Icons.Outlined.Settings,
                            contentDescription = "Settings"
                        )
                    },
                    label = {
                        Text(
                            text = "Settings",
                            fontWeight = if (currentTab == NavigationTab.SETTINGS) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedIconColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("nav_item_settings")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Crossfade(
                targetState = currentTab,
                label = "screen_transition"
            ) { tab ->
                when (tab) {
                    NavigationTab.QUICK_ADD -> QuickAddScreen(viewModel = viewModel)
                    NavigationTab.DASHBOARD -> DashboardScreen(viewModel = viewModel)
                    NavigationTab.HISTORY -> HistoryScreen(viewModel = viewModel)
                    NavigationTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
                }
            }
        }
    }

    // Edit Expense Dialog
    editingExpense?.let { expenseItem ->
        EditExpenseDialog(
            item = expenseItem,
            categories = allCategories,
            currencySymbol = currency.symbol,
            onDismiss = { viewModel.dismissEditingExpense() },
            onSave = { id, amountPaise, categoryId, note, timestamp ->
                viewModel.updateExpense(id, amountPaise, categoryId, note, timestamp)
            }
        )
    }

    // Delete Expense Confirmation Dialog
    deletingExpense?.let { expenseItem ->
        DeleteConfirmDialog(
            item = expenseItem,
            currencySymbol = currency.symbol,
            onDismiss = { viewModel.dismissDeletingExpense() },
            onConfirm = { viewModel.confirmDeleteExpense() }
        )
    }

    // Monthly Budget Dialog
    if (showBudgetDialog) {
        val monthTitle = DateUtils.formatMonthTitle(selectedMonthKey)
        SetBudgetDialog(
            monthTitle = monthTitle,
            currentBudgetPaise = selectedMonthBudget,
            currencySymbol = currency.symbol,
            onDismiss = { viewModel.closeBudgetDialog() },
            onSave = { amountPaise ->
                viewModel.saveMonthlyBudget(amountPaise)
            }
        )
    }

    // Weekly & Monthly Spending Report Sheet / Invoice Dialog
    if (showSpendingReport) {
        SpendingReportDialog(
            viewModel = viewModel,
            allExpenses = allExpenses,
            currency = currency,
            onDismiss = { viewModel.closeSpendingReport() }
        )
    }

    // First launch onboarding dialog
    if (showOnboarding) {
        OnboardingDialog(
            currentCurrency = currency,
            onComplete = { cur, budgetPaise ->
                viewModel.completeOnboarding(cur, budgetPaise)
            },
            onSkip = { viewModel.dismissOnboarding() }
        )
    }
}
