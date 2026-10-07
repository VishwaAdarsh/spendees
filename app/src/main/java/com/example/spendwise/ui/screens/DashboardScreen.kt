package com.example.spendwise.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spendwise.domain.model.CurrencyFormatter
import com.example.spendwise.domain.model.DateUtils
import com.example.spendwise.presentation.CategoryShare
import com.example.spendwise.presentation.NavigationTab
import com.example.spendwise.presentation.SpendWiseViewModel
import com.example.spendwise.ui.components.BudgetCard
import com.example.spendwise.ui.components.CategoryAnalyticsView
import com.example.spendwise.ui.components.ExpenseItemRow
import java.util.Calendar

@Composable
fun DashboardScreen(
    viewModel: SpendWiseViewModel,
    modifier: Modifier = Modifier
) {
    val selectedMonthKey by viewModel.selectedMonthKey.collectAsState()
    val budgetPaise by viewModel.selectedMonthBudget.collectAsState()
    val currency by viewModel.currency.collectAsState()
    val allExpenses by viewModel.allExpenses.collectAsState()
    val categories by viewModel.allCategories.collectAsState()

    // Calculate monthly expenses
    val (startMonth, endMonth) = remember(selectedMonthKey) {
        DateUtils.getStartAndEndOfMonth(selectedMonthKey)
    }

    val (startToday, endToday) = remember {
        DateUtils.getStartAndEndOfToday()
    }

    val (startWeek, endWeek) = remember {
        DateUtils.getStartAndEndOfWeek()
    }

    val monthExpenses = remember(allExpenses, startMonth, endMonth) {
        allExpenses.filter { it.expense.timestamp in startMonth..endMonth }
    }

    val totalSpentPaise = remember(monthExpenses) {
        monthExpenses.sumOf { it.expense.amountPaise }
    }

    val todaySpentPaise = remember(allExpenses, startToday, endToday) {
        allExpenses.filter { it.expense.timestamp in startToday..endToday }
            .sumOf { it.expense.amountPaise }
    }

    val weekSpentPaise = remember(allExpenses, startWeek, endWeek) {
        allExpenses.filter { it.expense.timestamp in startWeek..endWeek }
            .sumOf { it.expense.amountPaise }
    }

    // Category breakdown
    val categoryShares = remember(monthExpenses, categories, totalSpentPaise) {
        if (totalSpentPaise <= 0L || monthExpenses.isEmpty()) {
            emptyList()
        } else {
            val grouped = monthExpenses.groupBy { it.expense.categoryId }
            grouped.mapNotNull { (catId, expList) ->
                val cat = categories.find { it.id == catId }
                    ?: expList.firstOrNull()?.category
                val catTotal = expList.sumOf { it.expense.amountPaise }
                val pct = CurrencyFormatter.calculatePercentage(catTotal, totalSpentPaise)
                if (cat != null) {
                    CategoryShare(category = cat, amountPaise = catTotal, percentage = pct)
                } else null
            }.sortedByDescending { it.amountPaise }
        }
    }

    // Recent 5 expenses in month
    val recentMonthExpenses = remember(monthExpenses) {
        monthExpenses.take(5)
    }

    val monthTitle = remember(selectedMonthKey) {
        DateUtils.formatMonthTitle(selectedMonthKey)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Month Navigation Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = { viewModel.previousMonth() },
                    modifier = Modifier.testTag("dashboard_prev_month_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "Previous Month"
                    )
                }

                Text(
                    text = monthTitle,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag("dashboard_month_title")
                )

                IconButton(
                    onClick = { viewModel.nextMonth() },
                    modifier = Modifier.testTag("dashboard_next_month_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Next Month"
                    )
                }
            }

            // 1. Budget & Spending Summary Hero Card
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                BudgetCard(
                    monthTitle = monthTitle,
                    totalSpentPaise = totalSpentPaise,
                    budgetPaise = budgetPaise,
                    currencySymbol = currency.symbol,
                    onEditBudget = { viewModel.openBudgetDialog() }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Quick Stat Tiles: This Week & Today
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "This Week",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = CurrencyFormatter.formatPaise(weekSpentPaise, currency.symbol),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Today,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Today",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = CurrencyFormatter.formatPaise(todaySpentPaise, currency.symbol),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Weekly & Monthly Spending Sheet & Invoice Banner Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clickable { viewModel.openSpendingReport() },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Receipt,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "Weekly & Monthly Statement",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Invoice-style sheet, CSV export & share",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Button(
                        onClick = { viewModel.openSpendingReport() },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("open_spending_sheet_button")
                    ) {
                        Text("View Sheet", fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Category Analytics Breakdown
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                CategoryAnalyticsView(
                    categoryShares = categoryShares,
                    currencySymbol = currency.symbol
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Chronological Recent Expenses
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Transactions",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (monthExpenses.isNotEmpty()) {
                        TextButton(
                            onClick = { viewModel.navigateTo(NavigationTab.HISTORY) },
                            modifier = Modifier.testTag("view_all_history_button")
                        ) {
                            Text("View All")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (recentMonthExpenses.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No expenses yet",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Start tracking your spending by adding your first expense.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        recentMonthExpenses.forEach { item ->
                            ExpenseItemRow(
                                item = item,
                                currencySymbol = currency.symbol,
                                onEdit = { viewModel.startEditingExpense(item) },
                                onDelete = { viewModel.startDeletingExpense(item) }
                            )
                        }
                    }
                }
            }
        }
    }
}
