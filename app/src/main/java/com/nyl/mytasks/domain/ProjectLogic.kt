package com.nyl.mytasks.domain

import java.time.LocalDate
import java.time.YearMonth

/**
 * Avancement mensuel d'un projet.
 *
 * @property goal objectif mensuel (minutes)
 * @property carryOver retard cumulé des mois passés à rattraper (minutes)
 * @property doneThisMonth temps réalisé durant le mois en cours
 * @property lateSince fin du premier mois du retard en cours, ou null si à jour
 */
data class ProjectMonth(
    val goal: Int,
    val carryOver: Int,
    val doneThisMonth: Int,
    val lateSince: LocalDate?,
) {
    /** Objectif du mois, rattrapage inclus. */
    val target: Int get() = goal + carryOver
    val remaining: Int get() = (target - doneThisMonth).coerceAtLeast(0)
    val isLate: Boolean get() = carryOver > 0
    val reached: Boolean get() = remaining == 0
    val progress: Float get() = if (target <= 0) 1f else (doneThisMonth.toFloat() / target).coerceIn(0f, 1f)
}

/** Temps consacré à un projet durant un mois (pour le mini-graphique). */
data class MonthTotal(val month: YearMonth, val minutes: Int)

object ProjectLogic {

    private fun minutesByMonth(sessions: List<TaskEvent>): Map<YearMonth, Int> =
        sessions.filter { it.kind == EventKind.SESSION }
            .groupBy { YearMonth.from(it.date) }
            .mapValues { (_, list) -> list.sumOf { it.minutes ?: 0 } }

    /**
     * Pour chaque mois terminé depuis la création du projet, la différence entre l'objectif et le
     * temps réalisé est reportée (le surplus d'un mois résorbe le retard accumulé, jamais l'inverse).
     * Un projet est « en retard » dès la fin d'un mois dont l'objectif n'est pas atteint.
     */
    fun month(task: Task, sessions: List<TaskEvent>, today: LocalDate): ProjectMonth {
        val goal = task.monthlyGoalMinutes ?: 0
        val byMonth = minutesByMonth(sessions)

        val thisMonth = YearMonth.from(today)
        var balance = 0
        var lateSince: LocalDate? = null
        var m = YearMonth.from(task.createdAt)
        while (m.isBefore(thisMonth)) {
            val done = byMonth[m] ?: 0
            val before = balance
            balance = (balance + goal - done).coerceAtLeast(0)
            if (balance > 0 && before == 0) lateSince = m.atEndOfMonth()
            if (balance == 0) lateSince = null
            m = m.plusMonths(1)
        }
        return ProjectMonth(goal, balance, byMonth[thisMonth] ?: 0, if (balance > 0) lateSince else null)
    }

    /** Temps consacré mois par mois sur les [months] derniers mois (le mois en cours en dernier). */
    fun history(sessions: List<TaskEvent>, today: LocalDate, months: Int = 12): List<MonthTotal> {
        val byMonth = minutesByMonth(sessions)
        val current = YearMonth.from(today)
        return (months - 1 downTo 0).map { back ->
            val m = current.minusMonths(back.toLong())
            MonthTotal(m, byMonth[m] ?: 0)
        }
    }
}
