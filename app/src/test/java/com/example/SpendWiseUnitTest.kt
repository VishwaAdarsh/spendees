package com.example

import com.example.spendwise.domain.model.BudgetStatus
import com.example.spendwise.domain.model.CurrencyFormatter
import com.example.spendwise.domain.model.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpendWiseUnitTest {

    @Test
    fun parseInputToPaise_calculatesAccurately() {
        assertEquals(10000L, CurrencyFormatter.parseInputToPaise("100"))
        assertEquals(10000L, CurrencyFormatter.parseInputToPaise("100.00"))
        assertEquals(25050L, CurrencyFormatter.parseInputToPaise("250.50"))
        assertEquals(25005L, CurrencyFormatter.parseInputToPaise("250.05"))
        assertEquals(199999L, CurrencyFormatter.parseInputToPaise("1999.99"))
        assertEquals(0L, CurrencyFormatter.parseInputToPaise(""))
        assertEquals(0L, CurrencyFormatter.parseInputToPaise("0"))
    }

    @Test
    fun formatPaise_producesExpectedRepresentation() {
        assertEquals("₹250", CurrencyFormatter.formatPaise(25000L, "₹"))
        assertEquals("₹250.50", CurrencyFormatter.formatPaise(25050L, "₹"))
        assertEquals("₹12,450", CurrencyFormatter.formatPaise(1245000L, "₹"))
        assertEquals("$99.99", CurrencyFormatter.formatPaise(9999L, "$"))
    }

    @Test
    fun categoryPercentage_calculatedCorrectly() {
        // Food: 4,500 on Total: 12,450 -> ~36.14%
        val pct = CurrencyFormatter.calculatePercentage(450000L, 1245000L)
        assertTrue("Percentage should be approx 36.14%", pct in 36.1f..36.2f)

        // Zero total spent should return 0% safely
        assertEquals(0f, CurrencyFormatter.calculatePercentage(1000L, 0L), 0.001f)
    }

    @Test
    fun budgetStatus_thresholdsComplyWithPRD() {
        val budget = 2000000L // ₹20,000

        // 0 - 69% is SAFE (Green)
        assertEquals(BudgetStatus.SAFE, CurrencyFormatter.getBudgetStatus(1200000L, budget)) // 60%

        // 70 - 89% is WARNING (Yellow)
        assertEquals(BudgetStatus.WARNING, CurrencyFormatter.getBudgetStatus(1500000L, budget)) // 75%

        // 90 - 99% is DANGER (Red)
        assertEquals(BudgetStatus.DANGER, CurrencyFormatter.getBudgetStatus(1900000L, budget)) // 95%

        // 100%+ is EXCEEDED
        assertEquals(BudgetStatus.EXCEEDED, CurrencyFormatter.getBudgetStatus(2120000L, budget)) // 106%
    }

    @Test
    fun dateUtils_monthKeyAndShifting() {
        assertEquals("2026-11", DateUtils.shiftMonth("2026-10", 1))
        assertEquals("2026-09", DateUtils.shiftMonth("2026-10", -1))
        assertEquals("October 2026", DateUtils.formatMonthTitle("2026-10"))
    }

    @Test
    fun dateUtils_weekRangeCalculation() {
        val (start, end) = DateUtils.getStartAndEndOfWeek()
        assertTrue("End of week should be after start of week", end > start)
        val diffDays = (end - start) / (1000 * 60 * 60 * 24)
        assertEquals(6L, diffDays)
    }
}
