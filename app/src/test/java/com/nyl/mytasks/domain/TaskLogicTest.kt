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

    // --- Suppression d'une entrée d'historique ---

    private var clock = 0L
    private fun ev(task: Task, n: NewEvent) = TaskEvent("e${clock}", task.id, n.kind, n.date, n.minutes, n.note, clock++)

    @Test fun undo_done_oneOff_restoresTodo() {
        val t = parcel("2026-11-20")
        val out = TaskLogic.complete(t, d("2026-11-18"))
        val e = ev(t, out.events.single())
        val back = TaskLogic.undo(out.task, e, listOf(e))
        assertEquals(TaskStatus.TODO, back.status)
        assertEquals(null, back.closedAt)
    }

    @Test fun undo_done_recurring_restoresPreviousOccurrence() {
        val t = trash("2026-11-03") // en retard, fait le 18 → prochaine au 24
        val out = TaskLogic.complete(t, d("2026-11-18"))
        assertEquals(d("2026-11-24"), out.task.dueDate)
        val e = ev(t, out.events.single())
        val back = TaskLogic.undo(out.task, e, listOf(e))
        assertEquals(d("2026-11-03"), back.dueDate) // l'échéance en retard revient
        assertEquals(TaskStatus.TODO, back.status)
    }

    @Test fun undo_done_recurring_withoutNote_fallsBackOnPreviousOccurrence() {
        val t = trash("2026-11-24")
        val old = TaskEvent("x", t.id, EventKind.DONE, d("2026-11-17"), null, "", 1L)
        assertEquals(d("2026-11-17"), TaskLogic.undo(t, old, listOf(old)).dueDate)
    }

    @Test fun undo_skip_restoresTheSkippedOccurrence() {
        val t = trash("2026-11-17")
        val out = TaskLogic.skip(t, d("2026-11-17"))
        val e = ev(t, out.events.single())
        assertEquals(d("2026-11-17"), TaskLogic.undo(out.task, e, listOf(e)).dueDate)
    }

    @Test fun undo_postpone_restoresPreviousDueDate() {
        val t = parcel("2026-11-20")
        val out = TaskLogic.postpone(t, d("2026-11-30"), d("2026-11-18"))
        assertEquals(TaskStatus.POSTPONED, out.task.status)
        val e = ev(t, out.events.single())
        val back = TaskLogic.undo(out.task, e, listOf(e))
        assertEquals(d("2026-11-20"), back.dueDate)
        assertEquals(TaskStatus.TODO, back.status)
    }

    @Test fun undo_olderEntry_onlyRemovesHistory() {
        val t = trash("2026-11-10")
        val first = TaskLogic.complete(t, d("2026-11-10"))
        val e1 = ev(t, first.events.single())
        val second = TaskLogic.complete(first.task, d("2026-11-17"))
        val e2 = ev(first.task, second.events.single())
        // Supprimer la plus ancienne ne doit pas toucher à la prochaine échéance courante.
        assertEquals(second.task, TaskLogic.undo(second.task, e1, listOf(e1, e2)))
        // Supprimer la dernière restaure l'occurrence du 17.
        assertEquals(d("2026-11-17"), TaskLogic.undo(second.task, e2, listOf(e1, e2)).dueDate)
    }

    @Test fun undo_abandon_reopensTask() {
        val t = parcel("2026-11-20")
        val out = TaskLogic.abandon(t, d("2026-11-18"))
        val e = ev(t, out.events.single())
        assertEquals(TaskStatus.TODO, TaskLogic.undo(out.task, e, listOf(e)).status)
    }

    @Test fun undo_session_leavesTaskUntouched() {
        val t = parcel("2026-11-20")
        val e = ev(t, NewEvent(EventKind.SESSION, d("2026-11-18"), 30))
        assertEquals(t, TaskLogic.undo(t, e, listOf(e)))
    }
}
