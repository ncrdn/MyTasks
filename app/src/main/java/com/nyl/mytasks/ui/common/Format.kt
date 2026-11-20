package com.nyl.mytasks.ui.common

import android.content.Context
import com.nyl.mytasks.R
import com.nyl.mytasks.domain.Recurrence
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

private val FR = Locale.FRANCE

private val dateFmt = DateTimeFormatter.ofPattern("EEE d MMM yyyy", FR)
private val shortDateFmt = DateTimeFormatter.ofPattern("EEE d MMM", FR)

fun LocalDate.formatLong(): String = format(dateFmt)
fun LocalDate.formatShort(): String = format(shortDateFmt)

/** « 45 min », « 1 h », « 1 h 30 ». */
fun formatMinutes(minutes: Int): String {
    if (minutes < 60) return "$minutes min"
    val h = minutes / 60
    val m = minutes % 60
    return if (m == 0) "$h h" else "$h h ${m.toString().padStart(2, '0')}"
}

fun dayName(day: DayOfWeek, short: Boolean = false): String =
    day.getDisplayName(if (short) TextStyle.SHORT else TextStyle.FULL, FR)

/** Libellé relatif d'une échéance : « Aujourd'hui », « Demain », « En retard de 3 j »… */
fun describeDue(context: Context, due: LocalDate, today: LocalDate): String {
    val days = ChronoUnit.DAYS.between(today, due).toInt()
    return when {
        days == 0 -> context.getString(R.string.due_today)
        days == 1 -> context.getString(R.string.due_tomorrow)
        days > 1 -> context.getString(R.string.due_in_days, days, due.formatShort())
        else -> context.getString(R.string.due_late_days, -days)
    }
}

fun describeRecurrence(context: Context, r: Recurrence): String = when (r) {
    is Recurrence.Weekly -> {
        val days = r.days.sortedBy { it.value }.joinToString(", ") { dayName(it, short = true) }
        if (r.everyWeeks <= 1) context.getString(R.string.rec_weekly_desc, days)
        else context.getString(R.string.rec_weekly_n_desc, r.everyWeeks, days)
    }
    is Recurrence.MonthlyDay ->
        if (r.everyMonths <= 1) context.getString(R.string.rec_monthly_day_desc, r.dayOfMonth)
        else context.getString(R.string.rec_monthly_day_n_desc, r.everyMonths, r.dayOfMonth)
    is Recurrence.MonthlyNthWeekday -> {
        val nth = if (r.nth < 0) context.getString(R.string.rec_last) else context.getString(R.string.rec_nth_value, r.nth)
        if (r.everyMonths <= 1) context.getString(R.string.rec_monthly_nth_desc, nth, dayName(r.dayOfWeek))
        else context.getString(R.string.rec_monthly_nth_n_desc, r.everyMonths, nth, dayName(r.dayOfWeek))
    }
    is Recurrence.Yearly -> context.getString(
        R.string.rec_yearly_desc, r.day,
        java.time.Month.of(r.month).getDisplayName(TextStyle.FULL, FR),
    )
    is Recurrence.EveryDays -> context.getString(R.string.rec_custom_desc, r.days)
}
