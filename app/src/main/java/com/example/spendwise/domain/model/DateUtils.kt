package com.example.spendwise.domain.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {

    private val monthKeyFormat = SimpleDateFormat("yyyy-MM", Locale.US)
    private val monthTitleFormat = SimpleDateFormat("MMMM yyyy", Locale.US)
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.US)
    private val fullDateFormat = SimpleDateFormat("d MMM yyyy, hh:mm a", Locale.US)
    private val dayGroupFormat = SimpleDateFormat("EEE, d MMM yyyy", Locale.US)
    private val shortDateFormat = SimpleDateFormat("d MMM", Locale.US)
    private val standardDateFormat = SimpleDateFormat("dd MMM yyyy", Locale.US)

    fun currentMonthKey(): String {
        return monthKeyFormat.format(Date())
    }

    fun formatMonthTitle(monthKey: String): String {
        return try {
            val date = monthKeyFormat.parse(monthKey)
            if (date != null) monthTitleFormat.format(date) else monthKey
        } catch (_: Exception) {
            monthKey
        }
    }

    fun shiftMonth(monthKey: String, deltaMonths: Int): String {
        return try {
            val date = monthKeyFormat.parse(monthKey) ?: Date()
            val cal = Calendar.getInstance().apply {
                time = date
                add(Calendar.MONTH, deltaMonths)
            }
            monthKeyFormat.format(cal.time)
        } catch (_: Exception) {
            monthKey
        }
    }

    /**
     * Returns (startTimeMs, endTimeMs) for a given monthKey "yyyy-MM"
     */
    fun getStartAndEndOfMonth(monthKey: String): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        try {
            val date = monthKeyFormat.parse(monthKey)
            if (date != null) {
                cal.time = date
            }
        } catch (_: Exception) {
            // fallback to current
        }
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startTime = cal.timeInMillis

        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val endTime = cal.timeInMillis

        return Pair(startTime, endTime)
    }

    /**
     * Returns (startTimeMs, endTimeMs) for the current week (Monday to Sunday)
     */
    fun getStartAndEndOfWeek(timestamp: Long = System.currentTimeMillis()): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply {
            timeInMillis = timestamp
            firstDayOfWeek = Calendar.MONDAY
        }
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val daysFromMonday = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY

        cal.add(Calendar.DAY_OF_MONTH, -daysFromMonday)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfWeek = cal.timeInMillis

        cal.add(Calendar.DAY_OF_MONTH, 6)
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val endOfWeek = cal.timeInMillis

        return Pair(startOfWeek, endOfWeek)
    }

    fun formatWeekTitle(startMs: Long, endMs: Long): String {
        val startStr = shortDateFormat.format(Date(startMs))
        val endStr = standardDateFormat.format(Date(endMs))
        return "$startStr – $endStr"
    }

    fun getStartAndEndOfToday(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startTime = cal.timeInMillis

        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val endTime = cal.timeInMillis

        return Pair(startTime, endTime)
    }

    /**
     * Formats timestamp into smart relative day labels:
     * "Today • Wednesday, 7 Oct", "Yesterday • Tuesday, 6 Oct", etc.
     */
    fun formatRelativeDate(timestamp: Long): String {
        val now = Calendar.getInstance()
        val expenseCal = Calendar.getInstance().apply { timeInMillis = timestamp }

        val isSameYear = now.get(Calendar.YEAR) == expenseCal.get(Calendar.YEAR)
        val dayDiff = now.get(Calendar.DAY_OF_YEAR) - expenseCal.get(Calendar.DAY_OF_YEAR)

        return if (isSameYear && dayDiff == 0) {
            "Today • " + SimpleDateFormat("EEEE, d MMM", Locale.US).format(Date(timestamp))
        } else if (isSameYear && dayDiff == 1) {
            "Yesterday • " + SimpleDateFormat("EEEE, d MMM", Locale.US).format(Date(timestamp))
        } else {
            dayGroupFormat.format(Date(timestamp))
        }
    }

    fun formatTime(timestamp: Long): String {
        return timeFormat.format(Date(timestamp))
    }

    fun formatFullDateTime(timestamp: Long): String {
        return fullDateFormat.format(Date(timestamp))
    }

    fun formatDateOnly(timestamp: Long): String {
        return standardDateFormat.format(Date(timestamp))
    }
}
