package com.nyl.mytasks.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class ProjectAndDashboardTest {
    private fun d(s: String) = LocalDate.parse(s)

    private val spanish = Task(
        id = "p1", typeId = "project", title = "Apprendre l'espagnol",
        createdAt = d("2026-11-02"), weeklyGoalMinutes = 90,
    )

    private fun session(date: String, minutes: Int, task: String = "p1") = TaskEvent(
        id = "$task-$date-$minutes", taskId = task, kind = EventKind.SESSION, date = d(date), minutes = minutes,
    )

    @Test fun project_currentWeekOnly_isNotLate() {
        val w = ProjectLogic.week(spanish, listOf(session("2026-11-03", 30)), d("2026-11-04"))
        assertFalse(w.isLate)
        assertEquals(30, w.doneThisWeek)
        assertEquals(60, w.remaining)
    }

    @Test fun project_missedWeeks_accumulateCarryOver() {
        val sessions = listOf(session("2026-11-03", 60), session("2026-11-16", 30))
        val w = ProjectLogic.week(spanish, sessions, d("2026-11-17"))
        // semaine du 2/11 : 30 de retard ; semaine du 9/11 : 90 de retard => 120 à rattraper
        assertEquals(120, w.carryOver)
        assertEquals(210, w.target)
        assertEquals(180, w.remaining)
        assertTrue(w.isLate)
        assertEquals(d("2026-11-08"), w.lateSince)
    }

    @Test fun project_surplusAbsorbsLateness() {
        val sessions = listOf(session("2026-11-03", 30), session("2026-11-10", 150))
        val w = ProjectLogic.week(spanish, sessions, d("2026-11-17"))
        assertEquals(0, w.carryOver)
        assertFalse(w.isLate)
        assertNull(w.lateSince)
    }

    @Test fun project_goalReachedThisWeek() {
        val w = ProjectLogic.week(spanish, listOf(session("2026-11-02", 90)), d("2026-11-05"))
        assertTrue(w.reached)
        assertEquals(1f, w.progress, 0.0001f)
    }

    private val kinds = mapOf(
        "domestic" to TaskKind.DOMESTIC, "oneoff" to TaskKind.ONE_OFF,
        "worksite" to TaskKind.WORKSITE, "project" to TaskKind.PROJECT,
    )

    private val trash = Task(
        id = "t1", typeId = "domestic", title = "Sortir les poubelles", createdAt = d("2026-11-01"),
        dueDate = d("2026-11-17"), recurrence = Recurrence.Weekly(1, setOf(DayOfWeek.TUESDAY)),
    )
    private val parcel = Task(
        id = "t2", typeId = "oneoff", title = "Poster le colis", createdAt = d("2026-11-01"),
        dueDate = d("2026-11-20"),
    )
    private val salon = Task(
        id = "w1", typeId = "worksite", title = "Peindre le salon", createdAt = d("2026-01-01"),
        dueDate = d("2027-12-31"), effort = 7, priority = 4,
    )

    private fun dash(tasks: List<Task>, events: List<TaskEvent>, today: String) =
        DashboardCalculator.compute(tasks, events, { kinds[it] }, d(today))

    @Test fun dashboard_recurringTaskAppearsOnItsDay() {
        val s = dash(listOf(trash), emptyList(), "2026-11-17")
        assertEquals(listOf("t1"), s.todayItems.filter { it.kind == TaskKind.DOMESTIC }.map { it.task.id })
        assertEquals(0, s.overdueCount)
        assertEquals(0, s.domesticDone)
        assertEquals(1, s.domesticPlanned)
    }

    @Test fun dashboard_weeklyProgressAfterCompletion() {
        val out = TaskLogic.complete(trash, d("2026-11-17"))
        val ev = TaskEvent("e1", "t1", EventKind.DONE, d("2026-11-17"))
        val s = dash(listOf(out.task), listOf(ev), "2026-11-17")
        assertEquals(1, s.domesticDone)
        assertEquals(1, s.domesticPlanned)
        assertTrue(s.todayItems.none { it.kind == TaskKind.DOMESTIC })
    }

    @Test fun dashboard_oneOffBecomesOverdueTheDayAfter() {
        assertEquals(0, dash(listOf(parcel), emptyList(), "2026-11-20").overdueCount)
        assertEquals(1, dash(listOf(parcel), emptyList(), "2026-11-20").todayItems.size)
        val s = dash(listOf(parcel), emptyList(), "2026-11-21")
        assertEquals(1, s.overdueCount)
        assertEquals(1, s.overdueItems.single().daysLate)
    }

    @Test fun dashboard_overdueSortedOldestFirst() {
        val older = parcel.copy(id = "t3", dueDate = d("2026-11-10"))
        val s = dash(listOf(parcel, older), emptyList(), "2026-11-21")
        assertEquals(listOf("t3", "t2"), s.overdueItems.map { it.task.id })
    }

    @Test fun dashboard_projectLateAppearsInOverdue() {
        val s = dash(listOf(spanish), emptyList(), "2026-11-17")
        assertEquals(1, s.overdueCount)
        assertEquals(1, s.projects.size)
    }

    @Test fun dashboard_worksiteDeadlineLevels() {
        val soon = salon.copy(id = "w2", dueDate = d("2027-01-31"))
        val mid = salon.copy(id = "w3", dueDate = d("2027-04-30"))
        val s = dash(listOf(salon, soon, mid), emptyList(), "2026-11-17")
        val levels = s.worksiteDeadlines.associate { it.task.id to it.level }
        assertEquals(DeadlineLevel.UNDER_3_MONTHS, levels["w2"])
        assertEquals(DeadlineLevel.UNDER_6_MONTHS, levels["w3"])
        assertNull(levels["w1"])
        assertTrue(s.todayItems.any { it.task.id == "w1" }) // priorité 4 => « chantier prioritaire »
    }

    @Test fun streakAndCompletionRate() {
        val events = listOf(
            TaskEvent("a", "t1", EventKind.DONE, d("2026-11-17")),
            TaskEvent("b", "t1", EventKind.DONE, d("2026-11-16")),
            TaskEvent("c", "w1", EventKind.SESSION, d("2026-11-15")),
            TaskEvent("d", "t1", EventKind.DONE, d("2026-11-12")),
        )
        assertEquals(3, DashboardCalculator.streak(events, d("2026-11-17")))
        assertEquals(2, DashboardCalculator.streak(events.drop(1), d("2026-11-17")))
        val rate = DashboardCalculator.completionRate(emptyList(), events, { null }, d("2026-11-17"), 7)
        assertNotNull(rate)
        assertEquals(100, rate)
    }

    @Test fun backup_roundTripIsLossless() {
        val data = BackupData(
            exportedAt = "2026-11-17T10:00:00",
            types = listOf(TaskType("domestic", TaskKind.DOMESTIC, "Corvées", "cleaning", 0xFF00897B, 0)),
            rooms = listOf(HouseRoom("r1", "Cuisine", 0)),
            tasks = listOf(trash.copy(roomId = "r1", estimatedMinutes = 5), parcel, salon, spanish),
            events = listOf(TaskEvent("e1", "t1", EventKind.DONE, d("2026-11-17"), note = "ok", timestamp = 42L)),
        )
        val decoded = BackupCodec.decode(BackupCodec.encode(data))
        assertEquals(data, decoded)
    }
}
