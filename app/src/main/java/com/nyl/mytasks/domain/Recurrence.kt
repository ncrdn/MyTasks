package com.nyl.mytasks.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

/** Motifs de récurrence supportés (section 4 du cahier des charges). */
@Serializable
sealed interface Recurrence {
    /** Toutes les [everyWeeks] semaines, les jours [days]. */
    @Serializable
    @SerialName("weekly")
    data class Weekly(val everyWeeks: Int = 1, val days: Set<DayOfWeek>) : Recurrence

    /** Le [dayOfMonth] du mois (ramené au dernier jour si le mois est plus court), tous les [everyMonths] mois. */
    @Serializable
    @SerialName("monthly_day")
    data class MonthlyDay(val everyMonths: Int = 1, val dayOfMonth: Int) : Recurrence

    /** Le [nth]e [dayOfWeek] du mois (nth = 1..4, ou -1 pour le dernier). */
    @Serializable
    @SerialName("monthly_nth")
    data class MonthlyNthWeekday(val everyMonths: Int = 1, val nth: Int, val dayOfWeek: DayOfWeek) : Recurrence

    /** Chaque année à date fixe (le 29 février devient le 28 les années non bissextiles). */
    @Serializable
    @SerialName("yearly")
    data class Yearly(val month: Int, val day: Int) : Recurrence

    /** Tous les [days] jours. */
    @Serializable
    @SerialName("custom")
    data class EveryDays(val days: Int) : Recurrence
}

/**
 * Moteur de calcul des occurrences : fonctions pures sur [LocalDate] (aucune dépendance au
 * fuseau horaire ni à l'horloge), donc déterministes et testables unitairement.
 */
object RecurrenceEngine {

    /** Première occurrence située à la date [from] ou après. */
    fun firstOnOrAfter(rule: Recurrence, from: LocalDate): LocalDate = when (rule) {
        is Recurrence.Weekly -> {
            val days = rule.days.sortedBy { it.value }.ifEmpty { listOf(from.dayOfWeek) }
            val sameWeek = days.firstOrNull { it.value >= from.dayOfWeek.value }
            if (sameWeek != null) {
                from.with(TemporalAdjusters.nextOrSame(sameWeek))
            } else {
                from.weekStart().plusWeeks(1).with(TemporalAdjusters.nextOrSame(days.first()))
            }
        }
        is Recurrence.MonthlyDay, is Recurrence.MonthlyNthWeekday -> {
            val inMonth = monthlyCandidate(rule, YearMonth.from(from))
            if (!inMonth.isBefore(from)) inMonth else monthlyCandidate(rule, YearMonth.from(from).plusMonths(1))
        }
        is Recurrence.Yearly -> {
            val c = yearlyCandidate(rule, from.year)
            if (!c.isBefore(from)) c else yearlyCandidate(rule, from.year + 1)
        }
        is Recurrence.EveryDays -> from
    }

    /** Première occurrence strictement postérieure à [after]. */
    fun next(rule: Recurrence, after: LocalDate): LocalDate = when (rule) {
        is Recurrence.Weekly -> {
            val days = rule.days.sortedBy { it.value }.ifEmpty { listOf(after.dayOfWeek) }
            val laterThisWeek = days.firstOrNull { it.value > after.dayOfWeek.value }
            if (laterThisWeek != null) {
                after.with(TemporalAdjusters.nextOrSame(laterThisWeek))
            } else {
                after.weekStart().plusWeeks(rule.everyWeeks.coerceAtLeast(1).toLong())
                    .with(TemporalAdjusters.nextOrSame(days.first()))
            }
        }
        is Recurrence.MonthlyDay, is Recurrence.MonthlyNthWeekday -> {
            val ym = YearMonth.from(after)
            val inMonth = monthlyCandidate(rule, ym)
            if (inMonth.isAfter(after)) inMonth else monthlyCandidate(rule, ym.plusMonths(everyMonths(rule).toLong()))
        }
        is Recurrence.Yearly -> {
            val c = yearlyCandidate(rule, after.year)
            if (c.isAfter(after)) c else yearlyCandidate(rule, after.year + 1)
        }
        is Recurrence.EveryDays -> after.plusDays(rule.days.coerceAtLeast(1).toLong())
    }

    /** Occurrences comprises entre [from] et [to] (bornes incluses). */
    fun occurrencesBetween(rule: Recurrence, from: LocalDate, to: LocalDate, limit: Int = 400): List<LocalDate> {
        val out = ArrayList<LocalDate>()
        var d = firstOnOrAfter(rule, from)
        while (!d.isAfter(to) && out.size < limit) {
            out += d
            d = next(rule, d)
        }
        return out
    }

    private fun everyMonths(rule: Recurrence): Int = when (rule) {
        is Recurrence.MonthlyDay -> rule.everyMonths
        is Recurrence.MonthlyNthWeekday -> rule.everyMonths
        else -> 1
    }.coerceAtLeast(1)

    private fun monthlyCandidate(rule: Recurrence, ym: YearMonth): LocalDate = when (rule) {
        is Recurrence.MonthlyDay -> ym.atDay(rule.dayOfMonth.coerceIn(1, ym.lengthOfMonth()))
        is Recurrence.MonthlyNthWeekday -> {
            val first = ym.atDay(1)
            if (rule.nth < 0) {
                ym.atEndOfMonth().with(TemporalAdjusters.previousOrSame(rule.dayOfWeek))
            } else {
                first.with(TemporalAdjusters.nextOrSame(rule.dayOfWeek))
                    .plusWeeks((rule.nth.coerceIn(1, 4) - 1).toLong())
            }
        }
        else -> error("not a monthly rule")
    }

    private fun yearlyCandidate(rule: Recurrence.Yearly, year: Int): LocalDate {
        val ym = YearMonth.of(year, rule.month.coerceIn(1, 12))
        return ym.atDay(rule.day.coerceIn(1, ym.lengthOfMonth()))
    }
}
