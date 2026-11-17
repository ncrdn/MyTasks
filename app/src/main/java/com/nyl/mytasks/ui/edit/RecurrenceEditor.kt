package com.nyl.mytasks.ui.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nyl.mytasks.R
import com.nyl.mytasks.domain.Recurrence
import com.nyl.mytasks.ui.common.DropdownField
import com.nyl.mytasks.ui.common.NumberField
import com.nyl.mytasks.ui.common.dayName
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

enum class RecurrenceMode(val label: Int) {
    NONE(R.string.rec_none), WEEKLY(R.string.rec_weekly), MONTHLY_DAY(R.string.rec_monthly_day),
    MONTHLY_NTH(R.string.rec_monthly_nth), YEARLY(R.string.rec_yearly), CUSTOM(R.string.rec_custom)
}

/** État du formulaire de récurrence ; [toRule] produit la règle (ou null si incomplète / aucune). */
class RecurrenceForm(start: LocalDate) {
    var mode by mutableStateOf(RecurrenceMode.WEEKLY)
    var everyWeeks by mutableStateOf("1")
    var days by mutableStateOf(setOf(start.dayOfWeek))
    var monthDay by mutableStateOf(start.dayOfMonth.toString())
    var nth by mutableStateOf(((start.dayOfMonth - 1) / 7 + 1).coerceAtMost(4))
    var nthDay by mutableStateOf(start.dayOfWeek)
    var yearDay by mutableStateOf(start.dayOfMonth.toString())
    var yearMonth by mutableStateOf(start.monthValue)
    var customDays by mutableStateOf("40")

    fun load(rule: Recurrence?) {
        when (rule) {
            null -> mode = RecurrenceMode.NONE
            is Recurrence.Weekly -> { mode = RecurrenceMode.WEEKLY; everyWeeks = rule.everyWeeks.toString(); days = rule.days }
            is Recurrence.MonthlyDay -> { mode = RecurrenceMode.MONTHLY_DAY; monthDay = rule.dayOfMonth.toString() }
            is Recurrence.MonthlyNthWeekday -> { mode = RecurrenceMode.MONTHLY_NTH; nth = rule.nth; nthDay = rule.dayOfWeek }
            is Recurrence.Yearly -> { mode = RecurrenceMode.YEARLY; yearDay = rule.day.toString(); yearMonth = rule.month }
            is Recurrence.EveryDays -> { mode = RecurrenceMode.CUSTOM; customDays = rule.days.toString() }
        }
    }

    /** Règle valide, ou null pour « ne se répète pas » ; [valid] indique si la saisie est exploitable. */
    fun toRule(): Recurrence? = when (mode) {
        RecurrenceMode.NONE -> null
        RecurrenceMode.WEEKLY ->
            if (days.isEmpty()) null else Recurrence.Weekly((everyWeeks.toIntOrNull() ?: 1).coerceAtLeast(1), days)
        RecurrenceMode.MONTHLY_DAY -> monthDay.toIntOrNull()?.takeIf { it in 1..31 }?.let { Recurrence.MonthlyDay(1, it) }
        RecurrenceMode.MONTHLY_NTH -> Recurrence.MonthlyNthWeekday(1, nth, nthDay)
        RecurrenceMode.YEARLY -> yearDay.toIntOrNull()?.takeIf { it in 1..31 }?.let { Recurrence.Yearly(yearMonth, it) }
        RecurrenceMode.CUSTOM -> customDays.toIntOrNull()?.takeIf { it >= 1 }?.let { Recurrence.EveryDays(it) }
    }

    val valid: Boolean get() = mode == RecurrenceMode.NONE || toRule() != null
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecurrenceEditor(form: RecurrenceForm) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        DropdownField(
            label = stringResource(R.string.field_recurrence),
            selectedText = stringResource(form.mode.label),
            options = RecurrenceMode.entries,
            optionText = { stringResource(it.label) },
            onSelect = { form.mode = it },
        )
        when (form.mode) {
            RecurrenceMode.NONE -> Unit
            RecurrenceMode.WEEKLY -> {
                NumberField(
                    label = stringResource(R.string.rec_every_n_weeks), value = form.everyWeeks,
                    onChange = { form.everyWeeks = it }, modifier = Modifier.fillMaxWidth(),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DayOfWeek.entries.forEach { d ->
                        FilterChip(
                            selected = d in form.days,
                            onClick = { form.days = if (d in form.days) form.days - d else form.days + d },
                            label = { Text(dayName(d, short = true)) },
                        )
                    }
                }
            }
            RecurrenceMode.MONTHLY_DAY -> NumberField(
                label = stringResource(R.string.rec_day_of_month), value = form.monthDay,
                onChange = { form.monthDay = it }, modifier = Modifier.fillMaxWidth(),
                isError = form.monthDay.toIntOrNull()?.let { it !in 1..31 } ?: true,
            )
            RecurrenceMode.MONTHLY_NTH -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DropdownField(
                    label = stringResource(R.string.rec_nth),
                    selectedText = nthLabel(form.nth), options = listOf(1, 2, 3, 4, -1), optionText = { nthLabel(it) },
                    onSelect = { form.nth = it }, modifier = Modifier.weight(1f),
                )
                DropdownField(
                    label = stringResource(R.string.rec_weekday),
                    selectedText = dayName(form.nthDay), options = DayOfWeek.entries, optionText = { dayName(it) },
                    onSelect = { form.nthDay = it }, modifier = Modifier.weight(1f),
                )
            }
            RecurrenceMode.YEARLY -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(
                    label = stringResource(R.string.rec_day), value = form.yearDay, onChange = { form.yearDay = it },
                    modifier = Modifier.weight(1f),
                    isError = form.yearDay.toIntOrNull()?.let { it !in 1..31 } ?: true,
                )
                DropdownField(
                    label = stringResource(R.string.rec_month),
                    selectedText = monthName(form.yearMonth), options = (1..12).toList(), optionText = { monthName(it) },
                    onSelect = { form.yearMonth = it }, modifier = Modifier.weight(1f),
                )
            }
            RecurrenceMode.CUSTOM -> NumberField(
                label = stringResource(R.string.rec_every_n_days), value = form.customDays,
                onChange = { form.customDays = it }, modifier = Modifier.fillMaxWidth(), suffix = stringResource(R.string.days_unit),
                isError = form.customDays.toIntOrNull()?.let { it < 1 } ?: true,
            )
        }
    }
}

@Composable
private fun nthLabel(n: Int): String =
    if (n < 0) stringResource(R.string.rec_last) else stringResource(R.string.rec_nth_value, n)

private fun monthName(m: Int): String = Month.of(m).getDisplayName(TextStyle.FULL, Locale.FRANCE)
