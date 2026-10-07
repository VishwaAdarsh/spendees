package com.example.spendwise.domain.model

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

enum class BudgetStatus {
    SAFE,       // 0 - 69%
    WARNING,    // 70 - 89%
    DANGER,     // 90 - 99%
    EXCEEDED    // 100%+
}

object CurrencyFormatter {

    private val numberFormat = DecimalFormat("#,##,##0.00", DecimalFormatSymbols(Locale.US)).apply {
        groupingSize = 3
    }

    private val integerFormat = DecimalFormat("#,##,##0", DecimalFormatSymbols(Locale.US)).apply {
        groupingSize = 3
    }

    /**
     * Formats an amount in paise to a display string with currency symbol.
     * e.g., 25050L -> "₹250.50", 1245000L -> "₹12,450" (or optionally with decimals if paise > 0)
     */
    fun formatPaise(
        amountPaise: Long,
        currencySymbol: String = "₹",
        alwaysShowDecimals: Boolean = false
    ): String {
        val paisePart = Math.abs(amountPaise % 100)
        val units = amountPaise / 100
        val isNegative = amountPaise < 0

        val formatted = if (paisePart != 0L || alwaysShowDecimals) {
            val totalAsDecimal = Math.abs(amountPaise).toDouble() / 100.0
            numberFormat.format(totalAsDecimal)
        } else {
            integerFormat.format(Math.abs(units))
        }

        return if (isNegative) {
            "-$currencySymbol$formatted"
        } else {
            "$currencySymbol$formatted"
        }
    }

    /**
     * Converts a user input string (e.g., "250.50", "250") to paise as Long.
     * Returns 0L if invalid or empty.
     */
    fun parseInputToPaise(input: String): Long {
        val cleaned = input.trim().replace(",", "")
        if (cleaned.isEmpty()) return 0L

        return try {
            val parts = cleaned.split(".")
            val whole = parts[0].toLongOrNull() ?: 0L
            val fraction = if (parts.size > 1) {
                val fracStr = parts[1].take(2).padEnd(2, '0')
                fracStr.toLongOrNull() ?: 0L
            } else {
                0L
            }
            whole * 100L + fraction
        } catch (_: Exception) {
            0L
        }
    }

    /**
     * Converts paise to clean input string for numeric editor, e.g. 25050 -> "250.50"
     */
    fun paiseToInputString(amountPaise: Long): String {
        if (amountPaise == 0L) return ""
        val units = amountPaise / 100
        val paise = amountPaise % 100
        return if (paise == 0L) {
            units.toString()
        } else {
            val frac = paise.toString().padStart(2, '0')
            "$units.$frac"
        }
    }

    /**
     * Calculates category percentage against total monthly spending.
     * Handles 0 total spending safely.
     */
    fun calculatePercentage(categoryPaise: Long, totalPaise: Long): Float {
        if (totalPaise <= 0L || categoryPaise <= 0L) return 0f
        val percentage = (categoryPaise.toDouble() / totalPaise.toDouble()) * 100.0
        return percentage.toFloat().coerceIn(0f, 100f)
    }

    /**
     * Determines budget state and health based on PRD thresholds:
     * Green: 0-69%
     * Yellow: 70-89%
     * Red: 90%+
     * Exceeded: > 100%
     */
    fun getBudgetStatus(spentPaise: Long, budgetPaise: Long): BudgetStatus {
        if (budgetPaise <= 0L) return BudgetStatus.SAFE
        val ratio = spentPaise.toDouble() / budgetPaise.toDouble()
        return when {
            ratio >= 1.0 -> BudgetStatus.EXCEEDED
            ratio >= 0.90 -> BudgetStatus.DANGER
            ratio >= 0.70 -> BudgetStatus.WARNING
            else -> BudgetStatus.SAFE
        }
    }

    /**
     * Budget progress ratio clamped between 0.0 and 1.0 for progress bars
     */
    fun getBudgetProgressRatio(spentPaise: Long, budgetPaise: Long): Float {
        if (budgetPaise <= 0L) return 0f
        val ratio = spentPaise.toFloat() / budgetPaise.toFloat()
        return ratio.coerceIn(0f, 1f)
    }
}
