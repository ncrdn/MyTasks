package com.nyl.mytasks.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Élément affiché sur le tableau de bord. */
data class DashItem(
    val task: Task,
    val kind: TaskKind,
    /** Jours de retard (0 si pas en retard). */
    val daysLate: Int = 0,
    val project: ProjectMonth? = null,
)

enum class DeadlineLevel { UNDER_3_MONTHS, UNDER_6_MONTHS }

data class WorksiteDeadline(val task: Task, val level: DeadlineLevel, val daysLeft: Int)

/**
 * Avancement d'un type sur sa période (semaine, mois ou année) : éléments faits / prévus et temps
 * consacré / estimé (en minutes, selon les données disponibles).
 */
data class Progress(
    val done: Int,
    val planned: Int,
    val doneMinutes: Int,
    val plannedMinutes: Int,
) {
    /** Taux d'accomplissement 0..1 sur le nombre d'éléments (1 si rien n'est prévu). */
    val ratio: Float get() = if (planned <= 0) 1f else (done.toFloat() / planned).coerceIn(0f, 1f)

    /** Taux d'accomplissement 0..1 sur le temps : durées faites / durées prévues (0 si aucune durée n'est connue). */
    val timeRatio: Float get() = if (plannedMinutes <= 0) 0f else (doneMinutes.toFloat() / plannedMinutes).coerceIn(0f, 1f)
}

data class DashboardState(
    val today: LocalDate,
    val todayItems: List<DashItem>,
    val overdueItems: List<DashItem>,
    /** À faire plus tard cette semaine (tâches récurrentes et ponctuelles, après aujourd'hui). */
    val weekItems: List<DashItem>,
    /** Tâches récurrentes : occurrences de la semaine. */
    val recurring: Progress,
    /** Tâches ponctuelles : échéances de la semaine. */
    val oneOff: Progress,
    /** Projets : temps du mois en cours (minutes réalisées / objectif du mois, rattrapage inclus). */
    val projectsMonth: Progress,
    /** Travaux : points d'effort consommés / quota de l'année ; minutes = temps de sessions sur l'année. */
    val worksitesYear: Progress,
    val projects: List<DashItem>,
    val worksiteSessionsThisWeek: Int,
    val worksiteDeadlines: List<WorksiteDeadline>,
) {
    val overdueCount: Int get() = overdueItems.size
    val domesticDone: Int get() = recurring.done
    val domesticPlanned: Int get() = recurring.planned
}

object DashboardCalculator {

    fun compute(
        tasks: List<Task>,
        events: List<TaskEvent>,
        kindOf: (String) -> TaskKind?,
        today: LocalDate,
        /** Quota de points d'effort de l'année en cours (travaux). */
        yearQuota: Int = QuotaLogic.DEFAULT_QUOTA,
    ): DashboardState {
        val weekStart = today.weekStart()
        val weekEnd = today.weekEnd()
        val eventsByTask = events.groupBy { it.taskId }

        val todayItems = ArrayList<DashItem>()
        val overdue = ArrayList<DashItem>()
        val weekItems = ArrayList<DashItem>()
        val projects = ArrayList<DashItem>()
        val deadlines = ArrayList<WorksiteDeadline>()
        var recurring = Progress(0, 0, 0, 0)
        var oneOff = Progress(0, 0, 0, 0)

        for (task in tasks) {
            val kind = kindOf(task.typeId) ?: continue
            val taskEvents = eventsByTask[task.id].orEmpty()
            val est = task.estimatedMinutes ?: 0

            // Jauges en temps : une tâche sans durée estimée est exclue du calcul (et du décompte affiché avec).
            if ((kind == TaskKind.DOMESTIC || kind == TaskKind.ONE_OFF) && est > 0) {
                val done = taskEvents.count { it.kind == EventKind.DONE && it.date in weekStart..weekEnd }
                val pending = pendingThisWeek(task, weekEnd)
                val add = Progress(done, done + pending, done * est, (done + pending) * est)
                if (kind == TaskKind.DOMESTIC) recurring = recurring + add else oneOff = oneOff + add
            }
            if (!task.status.isActive) continue

            when (kind) {
                TaskKind.PROJECT -> {
                    val month = ProjectLogic.month(task, taskEvents, today)
                    val item = DashItem(task, kind, project = month)
                    projects += item
                    if (month.isLate) {
                        val since = month.lateSince
                        overdue += item.copy(daysLate = if (since == null) 1 else ChronoUnit.DAYS.between(since, today).toInt().coerceAtLeast(1))
                    }
                    if (month.remaining > 0) weekItems += item
                }
                else -> {
                    if (task.paused) continue
                    val due = task.dueDate
                    if (TaskLogic.isOverdue(task, kind, today)) {
                        overdue += DashItem(task, kind, daysLate = TaskLogic.daysLate(task, today))
                    } else if (kind == TaskKind.WORKSITE) {
                        if ((task.priority ?: 0) >= 4) weekItems += DashItem(task, kind)
                    } else if (due == today) {
                        todayItems += DashItem(task, kind)
                    } else if (due != null && due.isAfter(today) && !due.isAfter(weekEnd)) {
                        weekItems += DashItem(task, kind)
                    }
                    if (kind == TaskKind.WORKSITE && due != null && !due.isBefore(today)) {
                        val left = ChronoUnit.DAYS.between(today, due).toInt()
                        when {
                            left <= 92 -> deadlines += WorksiteDeadline(task, DeadlineLevel.UNDER_3_MONTHS, left)
                            left <= 183 -> deadlines += WorksiteDeadline(task, DeadlineLevel.UNDER_6_MONTHS, left)
                        }
                    }
                }
            }
        }

        val worksites = tasks.filter { kindOf(it.typeId) == TaskKind.WORKSITE }
        val worksiteIds = worksites.map { it.id }.toSet()
        val sessionsThisWeek = events.count {
            it.kind == EventKind.SESSION && it.taskId in worksiteIds && it.date in weekStart..weekEnd
        }
        val yearSummary = QuotaLogic.summary(today.year, yearQuota, worksites)
        val worksiteYearMinutes = events.filter {
            it.kind == EventKind.SESSION && it.taskId in worksiteIds && it.date.year == today.year
        }.sumOf { it.minutes ?: 0 }

        val projectsMonth = Progress(
            done = projects.count { (it.project?.remaining ?: 1) == 0 },
            planned = projects.size,
            doneMinutes = projects.sumOf { it.project?.doneThisMonth ?: 0 },
            plannedMinutes = projects.sumOf { it.project?.target ?: 0 },
        )

        return DashboardState(
            today = today,
            todayItems = todayItems.sortedWith(compareBy({ it.kind.ordinal }, { it.task.title.lowercase() })),
            overdueItems = overdue.sortedByDescending { it.daysLate },
            // Récurrentes et ponctuelles par échéance, puis les projets, puis les travaux (sans date du jour).
            weekItems = weekItems.sortedWith(
                compareBy<DashItem>({ weekRank(it.kind) }, { it.task.dueDate?.toEpochDay() ?: Long.MAX_VALUE }, { it.task.title.lowercase() }),
            ),
            recurring = recurring,
            oneOff = oneOff,
            projectsMonth = projectsMonth,
            worksitesYear = Progress(yearSummary.committed, yearQuota, worksiteYearMinutes, 0),
            projects = projects.sortedBy { it.task.title.lowercase() },
            worksiteSessionsThisWeek = sessionsThisWeek,
            worksiteDeadlines = deadlines.sortedBy { it.daysLeft },
        )
    }

    private fun weekRank(kind: TaskKind) = when (kind) {
        TaskKind.DOMESTIC, TaskKind.ONE_OFF -> 0
        TaskKind.PROJECT -> 1
        TaskKind.WORKSITE -> 2
    }

    private operator fun Progress.plus(o: Progress) =
        Progress(done + o.done, planned + o.planned, doneMinutes + o.doneMinutes, plannedMinutes + o.plannedMinutes)

    /** Occurrences restant à faire d'ici la fin de la semaine (retards inclus). */
    private fun pendingThisWeek(task: Task, weekEnd: LocalDate): Int {
        if (!task.status.isActive || task.paused) return 0
        val due = task.dueDate ?: return 0
        val rule = task.recurrence
        if (due.isAfter(weekEnd)) return 0
        if (rule == null) return 1
        var count = 0
        var d = due
        while (!d.isAfter(weekEnd) && count < 20) {
            count++
            d = RecurrenceEngine.next(rule, d)
        }
        return count
    }
}
