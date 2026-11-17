package com.nyl.mytasks.domain

import java.time.LocalDate

/**
 * Avancement hebdomadaire d'un projet.
 *
 * @property goal objectif hebdomadaire (minutes)
 * @property carryOver retard cumulé des semaines passées à rattraper (minutes)
 * @property doneThisWeek temps réalisé durant la semaine en cours (lundi → dimanche)
 * @property lateSince fin de la première semaine du retard en cours, ou null si à jour
 */
data class ProjectWeek(
    val goal: Int,
    val carryOver: Int,
    val doneThisWeek: Int,
    val lateSince: LocalDate?,
) {
    /** Objectif de la semaine, rattrapage inclus. */
    val target: Int get() = goal + carryOver
    val remaining: Int get() = (target - doneThisWeek).coerceAtLeast(0)
    val isLate: Boolean get() = carryOver > 0
    val reached: Boolean get() = remaining == 0
    val progress: Float get() = if (target <= 0) 1f else (doneThisWeek.toFloat() / target).coerceIn(0f, 1f)
}

object ProjectLogic {

    /**
     * Pour chaque semaine terminée depuis la création du projet, la différence entre l'objectif et le
     * temps réalisé est reportée (le surplus d'une semaine résorbe le retard accumulé, jamais l'inverse).
     */
    fun week(task: Task, sessions: List<TaskEvent>, today: LocalDate): ProjectWeek {
        val goal = task.weeklyGoalMinutes ?: 0
        val minutesByWeek = sessions
            .filter { it.kind == EventKind.SESSION }
            .groupBy { it.date.weekStart() }
            .mapValues { (_, list) -> list.sumOf { it.minutes ?: 0 } }

        val thisWeek = today.weekStart()
        var balance = 0
        var lateSince: LocalDate? = null
        var w = task.createdAt.weekStart()
        while (w.isBefore(thisWeek)) {
            val done = minutesByWeek[w] ?: 0
            val before = balance
            balance = (balance + goal - done).coerceAtLeast(0)
            if (balance > 0 && before == 0) lateSince = w.plusDays(6)
            if (balance == 0) lateSince = null
            w = w.plusWeeks(1)
        }
        return ProjectWeek(goal, balance, minutesByWeek[thisWeek] ?: 0, if (balance > 0) lateSince else null)
    }
}
