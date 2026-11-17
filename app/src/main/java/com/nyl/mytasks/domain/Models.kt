package com.nyl.mytasks.domain

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * Comportement d'un type de tâche. Le catalogue des types ([TaskType]) est piloté par des
 * données : ajouter un type = ajouter une ligne qui référence l'un de ces comportements.
 */
enum class TaskKind { DOMESTIC, ONE_OFF, WORKSITE, PROJECT }

enum class TaskStatus {
    TODO, DONE, POSTPONED, ABANDONED;

    val isActive: Boolean get() = this == TODO || this == POSTPONED
}

enum class EventKind { CREATED, DONE, SKIPPED, POSTPONED, ABANDONED, REOPENED, SESSION, PAUSED, RESUMED }

object LocalDateSerializer : KSerializer<LocalDate> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LocalDate", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: LocalDate) = encoder.encodeString(value.toString())
    override fun deserialize(decoder: Decoder): LocalDate = LocalDate.parse(decoder.decodeString())
}

typealias SerializableDate = @Serializable(with = LocalDateSerializer::class) LocalDate

@Serializable
data class TaskType(
    val id: String,
    val kind: TaskKind,
    val label: String,
    val iconKey: String,
    val color: Long,
    val sortOrder: Int,
)

@Serializable
data class HouseRoom(
    val id: String,
    val name: String,
    val sortOrder: Int,
)

/**
 * Tâche commune à tous les types. Les champs spécifiques sont optionnels et utilisés selon
 * le [TaskKind] du type de la tâche :
 *  - `dueDate` : prochaine occurrence (domestique), échéance (ponctuelle), 31/12 de l'année visée (chantier)
 *  - `title` : l'« objet » de la tâche, ou le nom du projet.
 */
@Serializable
data class Task(
    val id: String,
    val typeId: String,
    val title: String,
    val notes: String = "",
    val status: TaskStatus = TaskStatus.TODO,
    val createdAt: SerializableDate,
    val closedAt: SerializableDate? = null,
    val roomId: String? = null,
    val estimatedMinutes: Int? = null,
    val dueDate: SerializableDate? = null,
    val place: String? = null,
    val effort: Int? = null,
    val priority: Int? = null,
    val recurrence: Recurrence? = null,
    val anchorOnTheoretical: Boolean = false,
    val paused: Boolean = false,
    val weeklyGoalMinutes: Int? = null,
)

/** Événement d'historique : complétion, report, session de travail (avec durée), etc. */
@Serializable
data class TaskEvent(
    val id: String,
    val taskId: String,
    val kind: EventKind,
    val date: SerializableDate,
    val minutes: Int? = null,
    val note: String = "",
    val timestamp: Long = 0L,
)

fun LocalDate.weekStart(): LocalDate = with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
fun LocalDate.weekEnd(): LocalDate = weekStart().plusDays(6)
