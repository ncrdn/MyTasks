package com.nyl.mytasks.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class TaskLogicTest {
    private fun d(s: String) = LocalDate.parse(s)

    private fun trash(due: String, anchor: Boolean = false) = Task(
        id = "t1", typeId = "domestic", title = "Sortir les poubelles", createdAt = d("2026-11-01"),
        dueDate = d(due), recurrence = Recurrence.Weekly(1, setOf(DayOfWeek.TUESDAY)),
        anchorOnTheoretical = anchor,
    )

    private fun parcel(due: String) = Task(
        id = "t2", typeId = "oneoff", title = "Poster le colis", createdAt = d("2026-11-01"), dueDate = d(due),
    )

    @Test fun complete_recurring_generatesNextTuesday() {
        val out = TaskLogic.complete(trash("2026-11-17"), d("2026-11-17"))
        assertEquals(d("2026-11-24"), out.task.dueDate)
        assertEquals(TaskStatus.TODO, out.task.status)
        assertEquals(EventKind.DONE, out.events.single().kind)
    }

    @Test fun complete_late_anchorsOnCompletionDate() {
        val out = TaskLogic.complete(trash("2026-11-03"), d("2026-11-17"))
        assertEquals(d("2026-11-24"), out.task.dueDate) // pas d'empilement de retards
    }

    @Test fun complete_late_withTheoreticalAnchor() {
        val out = TaskLogic.complete(trash("2026-11-03", anchor = true), d("2026-11-17"))
        assertEquals(d("2026-11-10"), out.task.dueDate)
    }

    @Test fun complete_early_doesNotGenerateOccurrenceBeforeDue() {
        val out = TaskLogic.complete(trash("2026-11-24"), d("2026-11-23"))
        assertEquals(d("2026-12-01"), out.task.dueDate)
    }

    @Test fun skip_generatesNextOnTheoreticalDate() {
        val out = TaskLogic.skip(trash("2026-11-17"), d("2026-11-19"))
        assertEquals(d("2026-11-24"), out.task.dueDate)
        assertEquals(EventKind.SKIPPED, out.events.single().kind)
    }

    @Test fun complete_oneOff_closesTask() {
        val out = TaskLogic.complete(parcel("2026-11-20"), d("2026-11-19"))
        assertEquals(TaskStatus.DONE, out.task.status)
        assertEquals(d("2026-11-19"), out.task.closedAt)
    }

    @Test fun oneOff_overdueFromTheDayAfterDue() {
        val t = parcel("2026-11-20")
        assertFalse(TaskLogic.isOverdue(t, TaskKind.ONE_OFF, d("2026-11-20")))
        assertTrue(TaskLogic.isOverdue(t, TaskKind.ONE_OFF, d("2026-11-21")))
        assertEquals(1, TaskLogic.daysLate(t, d("2026-11-21")))
    }

    @Test fun postpone_movesDueDateAndRecomputesLateness() {
        val out = TaskLogic.postpone(parcel("2026-11-20"), d("2026-11-25"), d("2026-11-21"))
        assertEquals(TaskStatus.POSTPONED, out.task.status)
        assertFalse(TaskLogic.isOverdue(out.task, TaskKind.ONE_OFF, d("2026-11-21")))
    }

    @Test fun done_task_isNeverOverdue() {
        val done = TaskLogic.complete(parcel("2026-11-20"), d("2026-11-22")).task
        assertFalse(TaskLogic.isOverdue(done, TaskKind.ONE_OFF, d("2026-12-30")))
    }

    @Test fun pausedRecurrence_isNotOverdue_andResumeMovesDueDate() {
        val paused = TaskLogic.pause(trash("2026-11-03"), d("2026-11-04")).task
        assertFalse(TaskLogic.isOverdue(paused, TaskKind.DOMESTIC, d("2026-11-17")))
        val resumed = TaskLogic.resume(paused, d("2026-11-18")).task
        assertEquals(d("2026-11-24"), resumed.dueDate)
        assertFalse(resumed.paused)
    }
}
