package com.nyl.mytasks.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class RecurrenceEngineTest {
    private fun d(s: String) = LocalDate.parse(s)

    @Test fun weekly_everyTuesday_nextIsFollowingTuesday() {
        val rule = Recurrence.Weekly(1, setOf(DayOfWeek.TUESDAY))
        assertEquals(d("2026-11-24"), RecurrenceEngine.next(rule, d("2026-11-17")))
        assertEquals(d("2026-11-24"), RecurrenceEngine.next(rule, d("2026-11-19")))
        assertEquals(d("2026-11-24"), RecurrenceEngine.next(rule, d("2026-11-23")))
    }

    @Test fun weekly_everyTwoWeeks_monday() {
        val rule = Recurrence.Weekly(2, setOf(DayOfWeek.MONDAY))
        assertEquals(d("2026-11-30"), RecurrenceEngine.next(rule, d("2026-11-16")))
    }

    @Test fun weekly_multipleDays_staysInWeekThenJumps() {
        val rule = Recurrence.Weekly(1, setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY))
        assertEquals(d("2026-11-19"), RecurrenceEngine.next(rule, d("2026-11-17")))
        assertEquals(d("2026-11-24"), RecurrenceEngine.next(rule, d("2026-11-19")))
        val biweekly = Recurrence.Weekly(2, setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY))
        assertEquals(d("2026-12-01"), RecurrenceEngine.next(biweekly, d("2026-11-19")))
    }

    @Test fun weekly_acrossYearBoundary() {
        val rule = Recurrence.Weekly(1, setOf(DayOfWeek.MONDAY))
        assertEquals(d("2027-01-04"), RecurrenceEngine.next(rule, d("2026-12-28")))
    }

    @Test fun weekly_firstOnOrAfter() {
        val rule = Recurrence.Weekly(1, setOf(DayOfWeek.TUESDAY))
        assertEquals(d("2026-11-17"), RecurrenceEngine.firstOnOrAfter(rule, d("2026-11-17")))
        assertEquals(d("2026-11-24"), RecurrenceEngine.firstOnOrAfter(rule, d("2026-11-18")))
    }

    @Test fun monthlyDay_clampsToShortMonths() {
        val rule = Recurrence.MonthlyDay(1, 31)
        assertEquals(d("2026-02-28"), RecurrenceEngine.next(rule, d("2026-01-31")))
        assertEquals(d("2026-03-31"), RecurrenceEngine.next(rule, d("2026-02-28")))
        assertEquals(d("2026-04-30"), RecurrenceEngine.next(rule, d("2026-03-31")))
    }

    @Test fun monthlyDay_leapYear() {
        val rule = Recurrence.MonthlyDay(1, 31)
        assertEquals(d("2024-02-29"), RecurrenceEngine.next(rule, d("2024-01-31")))
    }

    @Test fun monthlyDay_acrossYear_andInterval() {
        assertEquals(d("2027-01-01"), RecurrenceEngine.next(Recurrence.MonthlyDay(1, 1), d("2026-12-15")))
        assertEquals(d("2027-02-01"), RecurrenceEngine.next(Recurrence.MonthlyDay(2, 1), d("2026-12-01")))
    }

    @Test fun monthlyNthWeekday_thirdSaturday() {
        val rule = Recurrence.MonthlyNthWeekday(1, 3, DayOfWeek.SATURDAY)
        assertEquals(d("2026-11-21"), RecurrenceEngine.next(rule, d("2026-11-17")))
        assertEquals(d("2026-12-19"), RecurrenceEngine.next(rule, d("2026-11-21")))
    }

    @Test fun monthlyNthWeekday_lastFriday() {
        val rule = Recurrence.MonthlyNthWeekday(1, -1, DayOfWeek.FRIDAY)
        assertEquals(d("2026-11-27"), RecurrenceEngine.next(rule, d("2026-11-01")))
    }

    @Test fun yearly_fixedDate() {
        val rule = Recurrence.Yearly(3, 15)
        assertEquals(d("2027-03-15"), RecurrenceEngine.next(rule, d("2026-03-15")))
        assertEquals(d("2026-03-15"), RecurrenceEngine.next(rule, d("2026-01-01")))
    }

    @Test fun yearly_feb29_fallsBackOnNonLeapYears() {
        val rule = Recurrence.Yearly(2, 29)
        assertEquals(d("2025-02-28"), RecurrenceEngine.next(rule, d("2024-02-29")))
        assertEquals(d("2028-02-29"), RecurrenceEngine.next(rule, d("2027-12-31")))
    }

    @Test fun custom_every40Days() {
        assertEquals(d("2026-12-27"), RecurrenceEngine.next(Recurrence.EveryDays(40), d("2026-11-17")))
    }

    @Test fun occurrencesBetween_listsAll() {
        val rule = Recurrence.Weekly(1, setOf(DayOfWeek.TUESDAY))
        assertEquals(
            listOf(d("2026-11-17"), d("2026-11-24"), d("2026-12-01")),
            RecurrenceEngine.occurrencesBetween(rule, d("2026-11-16"), d("2026-12-01")),
        )
    }
}
