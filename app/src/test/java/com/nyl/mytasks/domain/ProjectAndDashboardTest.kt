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

    // Objectif : 6 h par mois (360 min).
    private val spanish = Task(
        id = "p1", typeId = "project", title = "Apprendre l'espagnol",
        createdAt = d("2026-11-02"), monthlyGoalMinutes = 360,
    )
    // Même projet, commencé en septembre : deux mois complets sont écoulés en novembre.
    private val spanishSince = spanish.copy(createdAt = d("2026-09-10"))

    private fun session(date: String, minutes: Int, task: String = "p1") = TaskEvent(
        id = "$task-$date-$minutes", taskId = task, kind = EventKind.SESSION, date = d(date), minutes = minutes,
    )

    @Test fun project_currentMonthOnly_isNotLate() {
        val m = ProjectLogic.month(spanish, listOf(session("2026-11-03", 120)), d("2026-11-20"))
        assertFalse(m.isLate)
        assertEquals(120, m.doneThisMonth)
        assertEquals(240, m.remaining)
    }

    @Test fun project_missedMonths_accumulateCarryOver() {
        val sessions = listOf(session("2026-09-12", 120), session("2026-11-16", 60))
        val m = ProjectLogic.month(spanishSince, sessions, d("2026-11-17"))
        // septembre : 240 de retard ; octobre (rien fait) : 360 de plus => 600 à rattraper
        assertEquals(600, m.carryOver)
        assertEquals(960, m.target)
        assertEquals(900, m.remaining)
        assertTrue(m.isLate)
        assertEquals(d("2026-09-30"), m.lateSince) // en retard dès la fin du mois non atteint
    }

    @Test fun project_surplusAbsorbsLateness() {
        val sessions = listOf(session("2026-09-12", 120), session("2026-10-10", 800))
        val m = ProjectLogic.month(spanishSince, sessions, d("2026-11-17"))
        assertEquals(0, m.carryOver)
        assertFalse(m.isLate)
        assertNull(m.lateSince)
    }

    @Test fun project_goalReachedThisMonth() {
        val m = ProjectLogic.month(spanish, listOf(session("2026-11-02", 200), session("2026-11-15", 160)), d("2026-11-20"))
        assertTrue(m.reached)
        assertEquals(1f, m.progress, 0.0001f)
    }

    @Test fun project_notLateUntilTheMonthEnds() {
        // Le 30 novembre l'objectif n'est pas atteint mais le mois n'est pas fini ; le 1er décembre il est en retard.
        assertFalse(ProjectLogic.month(spanish, emptyList(), d("2026-11-30")).isLate)
        assertTrue(ProjectLogic.month(spanish, emptyList(), d("2026-12-01")).isLate)
    }

    @Test fun project_history_lastTwelveMonthsWithTotals() {
        val sessions = listOf(session("2026-11-03", 100), session("2026-11-20", 50), session("2026-09-01", 30), session("2025-10-01", 999))
        val h = ProjectLogic.history(sessions, d("2026-11-20"))
        assertEquals(12, h.size)
        assertEquals(java.time.YearMonth.of(2025, 12), h.first().month) // 12 derniers mois : déc. 2025 → nov. 2026
        assertEquals(java.time.YearMonth.of(2026, 11), h.last().month)
        assertEquals(150, h.last().minutes)
        assertEquals(30, h.first { it.month == java.time.YearMonth.of(2026, 9) }.minutes)
        assertTrue(h.none { it.minutes == 999 }) // hors fenêtre
    }

    @Test fun backup_oldWeeklyGoals_areConvertedToMonthly() {
        val v2 = """{"version":2,"types":[],"rooms":[],"events":[],"tasks":[
            {"id":"p","typeId":"project","title":"X","createdAt":"2026-11-02","weeklyGoalMinutes":90}]}"""
        val task = BackupCodec.decode(v2).tasks.single()
        assertEquals(360, task.monthlyGoalMinutes) // 1 h 30 / semaine → 6 h / mois
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

    @Test fun dashboard_laterThisWeekListsRecurringAndOneOffTasksAfterToday() {
        // Mardi 17 : le colis (vendredi 20) est « à faire cette semaine » ; la poubelle (aujourd'hui) est dans « aujourd'hui ».
        val s = dash(listOf(trash, parcel), emptyList(), "2026-11-17")
        assertEquals(listOf("t2"), s.weekItems.map { it.task.id })
        assertEquals(listOf("t1"), s.todayItems.map { it.task.id })
        // Dimanche prochain : la semaine suivante n'apparaît pas.
        val next = trash.copy(id = "t9", dueDate = d("2026-11-24"))
        assertTrue(dash(listOf(next), emptyList(), "2026-11-17").weekItems.isEmpty())
    }

    @Test fun dashboard_progressGaugesWithHours() {
        val cooked = trash.copy(estimatedMinutes = 30) // fait 1 fois cette semaine, plus une prévue avant dimanche
        val parcel2 = parcel.copy(estimatedMinutes = 20)
        val ev = TaskEvent("e1", "t1", EventKind.DONE, d("2026-11-16"))
        val s = dash(listOf(cooked, parcel2), listOf(ev), "2026-11-17")
        assertEquals(1, s.recurring.done)
        assertEquals(2, s.recurring.planned)
        assertEquals(30, s.recurring.doneMinutes)
        assertEquals(60, s.recurring.plannedMinutes)
        assertEquals(0.5f, s.recurring.ratio, 0.0001f)
        assertEquals(0, s.oneOff.done)
        assertEquals(1, s.oneOff.planned)
        assertEquals(20, s.oneOff.plannedMinutes)
    }

    @Test fun dashboard_worksiteYearGauge_pointsOverQuota() {
        val a = salon.copy(id = "wa", dueDate = d("2026-12-31"), effort = 4)
        val b = salon.copy(id = "wb", dueDate = d("2026-12-31"), effort = 5, status = TaskStatus.DONE)
        val other = salon.copy(id = "wc", dueDate = d("2027-12-31"), effort = 9)
        val ev = TaskEvent("s1", "wa", EventKind.SESSION, d("2026-11-10"), minutes = 90)
        val s = DashboardCalculator.compute(listOf(a, b, other), listOf(ev), { kinds[it] }, d("2026-11-17"), yearQuota = 45)
        assertEquals(9, s.worksitesYear.done)   // points engagés sur 2026 (la tâche terminée reste consommée)
        assertEquals(45, s.worksitesYear.planned)
        assertEquals(90, s.worksitesYear.doneMinutes)
    }

    @Test fun dashboard_projectsMonthGauge_sumsMinutesOverGoal() {
        val s = dash(listOf(spanish), listOf(session("2026-11-03", 120)), "2026-11-17")
        assertEquals(120, s.projectsMonth.doneMinutes)
        assertEquals(360, s.projectsMonth.plannedMinutes)
        assertEquals(1, s.projectsMonth.planned)
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
        val s = dash(listOf(spanishSince), emptyList(), "2026-11-17")
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
