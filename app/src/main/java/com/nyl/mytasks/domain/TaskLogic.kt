package com.nyl.mytasks.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Événement à ajouter à l'historique, produit par la logique métier. */
data class NewEvent(
    val kind: EventKind,
    val date: LocalDate,
    val minutes: Int? = null,
    val note: String = "",
)

/** Résultat d'une action : la tâche mise à jour et les événements d'historique à enregistrer. */
data class Outcome(val task: Task, val events: List<NewEvent>)

/**
 * Règles métier sur une tâche (sections 3, 4 et 5.4). Fonctions pures : la date du jour est
 * toujours fournie en paramètre.
 */
object TaskLogic {

    /** Tâche active dont l'échéance est dépassée (les récurrences en pause ne sont jamais en retard). */
    fun isOverdue(task: Task, kind: TaskKind, today: LocalDate): Boolean {
        if (!task.status.isActive || task.paused) return false
        if (kind == TaskKind.PROJECT) return false // géré par ProjectLogic
        val due = task.dueDate ?: return false
        return due.isBefore(today)
    }

    fun daysLate(task: Task, today: LocalDate): Int {
        val due = task.dueDate ?: return 0
        return ChronoUnit.DAYS.between(due, today).toInt().coerceAtLeast(0)
    }

    /**
     * Marque la tâche (ou son occurrence) comme faite. Si elle est récurrente, l'occurrence suivante
     * est calculée à partir de la date de complétion (jamais avant l'échéance prévue), sauf si
     * [Task.anchorOnTheoretical] impose de caler sur la date théorique.
     */
    fun complete(task: Task, today: LocalDate): Outcome {
        val rule = task.recurrence
        val due = task.dueDate
        if (rule != null && due != null) {
            val base = if (task.anchorOnTheoretical) due else maxOf(today, due)
            val next = RecurrenceEngine.next(rule, base)
            return Outcome(
                task.copy(dueDate = next, status = TaskStatus.TODO),
                listOf(NewEvent(EventKind.DONE, today)),
            )
        }
        return Outcome(
            task.copy(status = TaskStatus.DONE, closedAt = today),
            listOf(NewEvent(EventKind.DONE, today)),
        )
    }

    /** « Ignorer cette fois » : l'occurrence suivante est générée à la date théorique. */
    fun skip(task: Task, today: LocalDate): Outcome {
        val rule = task.recurrence
        val due = task.dueDate
        require(rule != null && due != null) { "Seule une occurrence récurrente peut être ignorée" }
        return Outcome(
            task.copy(dueDate = RecurrenceEngine.next(rule, due), status = TaskStatus.TODO),
            listOf(NewEvent(EventKind.SKIPPED, today, note = due.toString())),
        )
    }

    /** Déplace l'échéance ; le retard est recalculé à partir de la nouvelle date. */
    fun postpone(task: Task, newDue: LocalDate, today: LocalDate): Outcome {
        val status = if (task.recurrence != null) task.status else TaskStatus.POSTPONED
        return Outcome(
            task.copy(dueDate = newDue, status = status),
            listOf(NewEvent(EventKind.POSTPONED, today, note = newDue.toString())),
        )
    }

    fun abandon(task: Task, today: LocalDate): Outcome = Outcome(
        task.copy(status = TaskStatus.ABANDONED, closedAt = today),
        listOf(NewEvent(EventKind.ABANDONED, today)),
    )

    /** Rouvre une tâche terminée ou abandonnée. */
    fun reopen(task: Task, today: LocalDate): Outcome = Outcome(
        task.copy(status = TaskStatus.TODO, closedAt = null),
        listOf(NewEvent(EventKind.REOPENED, today)),
    )

    fun pause(task: Task, today: LocalDate): Outcome =
        Outcome(task.copy(paused = true), listOf(NewEvent(EventKind.PAUSED, today)))

    /** Réactive une récurrence : une échéance passée est replacée à la prochaine occurrence. */
    fun resume(task: Task, today: LocalDate): Outcome {
        val rule = task.recurrence
        val due = task.dueDate
        val newDue = if (rule != null && (due == null || due.isBefore(today))) {
            RecurrenceEngine.firstOnOrAfter(rule, today)
        } else {
            due
        }
        return Outcome(task.copy(paused = false, dueDate = newDue), listOf(NewEvent(EventKind.RESUMED, today)))
    }

    /** Enregistre une session de travail (chantier) ou un temps passé (projet). */
    fun session(task: Task, date: LocalDate, minutes: Int?, note: String): Outcome =
        Outcome(task, listOf(NewEvent(EventKind.SESSION, date, minutes, note)))
}
