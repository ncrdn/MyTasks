package com.nyl.mytasks.domain

import kotlinx.serialization.Serializable

/** Quota annuel de points d'effort pour les travaux (ex. : 45 points pour 2026). */
@Serializable
data class YearQuota(val year: Int, val points: Int)

/**
 * Bilan d'une année : quota, points engagés par les travaux rattachés (ceux d'une tâche terminée restent
 * consommés), points restants et liste des tâches rattachées.
 */
data class YearSummary(
    val year: Int,
    val quota: Int,
    val committed: Int,
    /** Part des points engagés qui correspond à des travaux terminés. */
    val done: Int,
    val tasks: List<Task>,
) {
    val remaining: Int get() = (quota - committed).coerceAtLeast(0)
    val isOver: Boolean get() = committed > quota
    val progress: Float get() = if (quota <= 0) 0f else (committed.toFloat() / quota).coerceIn(0f, 1f)
}

object QuotaLogic {
    /** Quota proposé tant qu'aucune valeur n'a été définie pour l'année. */
    const val DEFAULT_QUOTA = 45

    /** Poids d'une tâche en points d'effort (l'« effort estimé » de la tâche). */
    fun points(task: Task): Int = task.effort ?: 0

    /** Année de réalisation à laquelle le travail est rattaché ; null = en attente. */
    fun yearOf(task: Task): Int? = task.dueDate?.year

    /** Un travail abandonné libère ses points ; en cours ou terminé, il les consomme. */
    fun consumes(task: Task): Boolean = task.status != TaskStatus.ABANDONED

    fun quotaFor(year: Int, quotas: List<YearQuota>, default: Int = DEFAULT_QUOTA): Int =
        quotas.firstOrNull { it.year == year }?.points ?: default

    fun summary(year: Int, quota: Int, worksites: List<Task>): YearSummary {
        val attached = worksites.filter { yearOf(it) == year && consumes(it) }
        return YearSummary(
            year = year,
            quota = quota,
            committed = attached.sumOf(::points),
            done = attached.filter { it.status == TaskStatus.DONE }.sumOf(::points),
            tasks = attached,
        )
    }

    /** Points encore libres sur [year] pour y rattacher une tâche ([excludingTaskId] : la tâche qu'on modifie). */
    fun available(year: Int, quota: Int, worksites: List<Task>, excludingTaskId: String? = null): Int =
        quota - worksites.filter { it.id != excludingTaskId && yearOf(it) == year && consumes(it) }.sumOf(::points)

    /**
     * Rattachement possible tant que le poids de la tâche est inférieur ou égal au reliquat (bornes incluses :
     * un reliquat de 1 point accepte une tâche de 1 point). Seule règle de décision, partagée par l'encart
     * d'année, la liste des années proposées et le message de refus.
     */
    fun canAttach(points: Int, free: Int): Boolean = points <= free

    /** Années (à partir de [fromYear]) où il reste au moins [points] points : celles où le rattachement est possible. */
    fun yearsWithRoom(
        points: Int,
        fromYear: Int,
        toYear: Int,
        quotas: List<YearQuota>,
        default: Int,
        worksites: List<Task>,
        excludingTaskId: String? = null,
    ): List<Int> = (fromYear..toYear).filter { y ->
        canAttach(points, available(y, quotaFor(y, quotas, default), worksites, excludingTaskId))
    }
}
