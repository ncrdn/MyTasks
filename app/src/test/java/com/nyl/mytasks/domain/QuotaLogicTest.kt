package com.nyl.mytasks.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class QuotaLogicTest {
    private fun work(id: String, points: Int, year: Int?, status: TaskStatus = TaskStatus.TODO) = Task(
        id = id, typeId = "worksite", title = id, createdAt = LocalDate.of(2026, 1, 1),
        effort = points, dueDate = year?.let { LocalDate.of(it, 12, 31) }, status = status,
    )

    @Test fun example_45points_tenTasksOf4AndOneOf5() {
        val tasks = (1..10).map { work("t$it", 4, 2026) } + work("big", 5, 2026)
        val s = QuotaLogic.summary(2026, 45, tasks)
        assertEquals(45, s.committed)
        assertEquals(0, s.remaining)
        assertEquals(11, s.tasks.size)
        assertFalse(s.isOver)
        // Plus aucun rattachement possible : même une tâche d'1 point.
        assertTrue(QuotaLogic.yearsWithRoom(1, 2026, 2026, emptyList(), 45, tasks).isEmpty())
    }

    @Test fun closedTask_keepsItsPoints_butAbandonedFreesThem() {
        val tasks = listOf(work("a", 10, 2026, TaskStatus.DONE), work("b", 6, 2026, TaskStatus.ABANDONED), work("c", 4, 2026))
        val s = QuotaLogic.summary(2026, 45, tasks)
        assertEquals(14, s.committed)
        assertEquals(10, s.done)
        assertEquals(31, s.remaining)
    }

    @Test fun movingOrDeletingATask_freesPointsOnTheOriginYear() {
        val tasks = listOf(work("a", 20, 2026), work("b", 20, 2026))
        assertEquals(5, QuotaLogic.summary(2026, 45, tasks).remaining)
        val moved = listOf(work("a", 20, 2027), work("b", 20, 2026))
        assertEquals(25, QuotaLogic.summary(2026, 45, moved).remaining)
        assertEquals(25, QuotaLogic.summary(2026, 45, tasks.drop(1)).remaining)
    }

    @Test fun pendingTasks_areNotCountedOnAnyYear() {
        val tasks = listOf(work("p", 8, null))
        assertEquals(0, QuotaLogic.summary(2026, 45, tasks).committed)
        assertEquals(null, QuotaLogic.yearOf(tasks.single()))
    }

    @Test fun editingATask_doesNotCountItselfAgainstTheQuota() {
        val tasks = listOf(work("a", 30, 2026), work("b", 10, 2026))
        assertEquals(5, QuotaLogic.available(2026, 45, tasks))
        assertEquals(35, QuotaLogic.available(2026, 45, tasks, excludingTaskId = "a"))
    }

    @Test fun quotaFor_usesDefinedValueThenDefault() {
        val quotas = listOf(YearQuota(2026, 60))
        assertEquals(60, QuotaLogic.quotaFor(2026, quotas, 45))
        assertEquals(45, QuotaLogic.quotaFor(2027, quotas, 45))
    }

    @Test fun yearsWithRoom_listsOnlyYearsThatCanTakeTheTask() {
        val tasks = (1..11).map { work("t$it", 4, 2026) } // 44/45
        val years = QuotaLogic.yearsWithRoom(4, 2026, 2028, emptyList(), 45, tasks)
        assertEquals(listOf(2027, 2028), years)
    }

    @Test fun edgeCase_remainderOfOnePoint_acceptsATaskOfOnePoint() {
        // Quota 47, 46 points engagés (dont une tâche terminée) : il reste 1 point.
        val tasks = listOf(
            work("done", 10, 2026, TaskStatus.DONE), work("a", 10, 2026), work("b", 10, 2026), work("c", 10, 2026), work("d", 6, 2026),
        )
        val quotas = listOf(YearQuota(2026, 47))
        val summary = QuotaLogic.summary(2026, 47, tasks)
        assertEquals(46, summary.committed)
        assertEquals(1, summary.remaining)
        val free = QuotaLogic.available(2026, 47, tasks)
        assertEquals(summary.remaining, free) // l'encart et le blocage reposent sur le même calcul
        // Une tâche de 1 point (reliquat = coût) se rattache ; une tâche de 2 points non.
        assertTrue(QuotaLogic.canAttach(1, free))
        assertFalse(QuotaLogic.canAttach(2, free))
        assertTrue(2026 in QuotaLogic.yearsWithRoom(1, 2026, 2027, quotas, 45, tasks))
        assertFalse(2026 in QuotaLogic.yearsWithRoom(2, 2026, 2027, quotas, 45, tasks))
    }

    @Test fun edgeCase_exactRemainder_forEveryWeight() {
        for (cost in 1..10) {
            val tasks = listOf(work("x", 45 - cost, 2026))
            val free = QuotaLogic.available(2026, 45, tasks)
            assertEquals(cost, free)
            assertTrue("une tâche de $cost pt(s) doit tenir dans un reliquat de $free", QuotaLogic.canAttach(cost, free))
            assertFalse(QuotaLogic.canAttach(cost + 1, free))
        }
    }

    @Test fun editingATaskOfOnePoint_inAFullYear_canStayAttached() {
        // Année pleine (45/45) dont une tâche de 1 point : en la modifiant, ses propres points ne comptent pas contre elle.
        val tasks = listOf(work("big", 44, 2026), work("small", 1, 2026))
        val free = QuotaLogic.available(2026, 45, tasks, excludingTaskId = "small")
        assertTrue(QuotaLogic.canAttach(1, free))
    }

    @Test fun backup_roundTrip_keepsQuotas() {
        val data = BackupData(types = emptyList(), rooms = emptyList(), tasks = emptyList(), events = emptyList(), yearQuotas = listOf(YearQuota(2026, 45)))
        assertEquals(listOf(YearQuota(2026, 45)), BackupCodec.decode(BackupCodec.encode(data)).yearQuotas)
    }

    @Test fun backup_version1_withoutQuotas_stillImports() {
        val v1 = """{"version":1,"exportedAt":"","types":[],"rooms":[],"tasks":[],"events":[]}"""
        assertTrue(BackupCodec.decode(v1).yearQuotas.isEmpty())
    }
}
