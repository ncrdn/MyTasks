package com.nyl.mytasks.data

import androidx.room.withTransaction
import com.nyl.mytasks.data.db.AppDatabase
import com.nyl.mytasks.data.db.toDomain
import com.nyl.mytasks.data.db.toEntity
import com.nyl.mytasks.domain.BackupData
import com.nyl.mytasks.domain.EventKind
import com.nyl.mytasks.domain.HouseRoom
import com.nyl.mytasks.domain.NewEvent
import com.nyl.mytasks.domain.Outcome
import com.nyl.mytasks.domain.Task
import com.nyl.mytasks.domain.TaskEvent
import com.nyl.mytasks.domain.TaskLogic
import com.nyl.mytasks.domain.TaskType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

/** Point d'accès unique aux données : applique la logique métier puis persiste dans une transaction. */
class TaskRepository(private val db: AppDatabase, private val clock: AppClock) {
    private val tasks = db.taskDao()
    private val events = db.eventDao()
    private val types = db.typeDao()
    private val rooms = db.roomDao()

    val tasksFlow: Flow<List<Task>> = tasks.observeAll().map { list -> list.map { it.toDomain() } }
    val eventsFlow: Flow<List<TaskEvent>> = events.observeAll().map { list -> list.map { it.toDomain() } }
    val typesFlow: Flow<List<TaskType>> = types.observeAll().map { list -> list.map { it.toDomain() } }
    val roomsFlow: Flow<List<HouseRoom>> = rooms.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun seedIfEmpty(defaultTypes: List<TaskType>, defaultRooms: List<HouseRoom>) {
        db.withTransaction {
            if (types.count() == 0) {
                types.upsertAll(defaultTypes.map { it.toEntity() })
                if (rooms.getAll().isEmpty()) rooms.upsertAll(defaultRooms.map { it.toEntity() })
            }
        }
    }

    suspend fun getTask(id: String): Task? = tasks.getById(id)?.toDomain()

    suspend fun createTask(task: Task) {
        db.withTransaction {
            tasks.upsert(task.toEntity())
            events.upsert(newEvent(task.id, NewEvent(EventKind.CREATED, task.createdAt)).toEntity())
        }
    }

    suspend fun updateTask(task: Task) = tasks.upsert(task.toEntity())

    suspend fun deleteTask(id: String) {
        db.withTransaction {
            events.deleteForTask(id)
            tasks.delete(id)
        }
    }

    suspend fun apply(outcome: Outcome) {
        db.withTransaction {
            tasks.upsert(outcome.task.toEntity())
            outcome.events.forEach { events.upsert(newEvent(outcome.task.id, it).toEntity()) }
        }
    }

    private suspend fun act(id: String, block: (Task, LocalDate) -> Outcome) {
        val task = getTask(id) ?: return
        apply(block(task, clock.now()))
    }

    suspend fun complete(id: String) = act(id, TaskLogic::complete)
    suspend fun skip(id: String) = act(id, TaskLogic::skip)
    suspend fun abandon(id: String) = act(id, TaskLogic::abandon)
    suspend fun reopen(id: String) = act(id, TaskLogic::reopen)
    suspend fun pause(id: String) = act(id, TaskLogic::pause)
    suspend fun resume(id: String) = act(id, TaskLogic::resume)
    suspend fun postpone(id: String, newDue: LocalDate) = act(id) { t, today -> TaskLogic.postpone(t, newDue, today) }

    suspend fun addSession(id: String, date: LocalDate, minutes: Int?, note: String) =
        act(id) { t, _ -> TaskLogic.session(t, date, minutes, note) }

    /** Supprime une entrée d'historique et remet la tâche dans son état précédent (voir [TaskLogic.undo]). */
    suspend fun deleteEvent(eventId: String) {
        db.withTransaction {
            val event = events.getById(eventId)?.toDomain() ?: return@withTransaction
            val task = getTask(event.taskId)
            if (task != null) {
                val restored = TaskLogic.undo(task, event, events.getForTask(task.id).map { it.toDomain() })
                if (restored != task) tasks.upsert(restored.toEntity())
            }
            events.delete(eventId)
        }
    }

    suspend fun saveType(type: TaskType) = types.upsert(type.toEntity())

    /** Ordre par défaut des onglets : Projets avant Travaux (ordre initial inchangé → on l'échange). */
    suspend fun swapDefaultWorksiteAndProject() {
        db.withTransaction {
            val all = types.getAll()
            val worksite = all.firstOrNull { it.id == "worksite" }
            val project = all.firstOrNull { it.id == "project" }
            if (worksite != null && project != null && worksite.sortOrder == 2 && project.sortOrder == 3) {
                types.upsert(worksite.copy(sortOrder = 3))
                types.upsert(project.copy(sortOrder = 2))
            }
        }
    }

    /** Applique un nouvel intitulé par défaut aux données créées avant le renommage (si l'utilisateur ne l'a pas personnalisé). */
    suspend fun renameTypeIfUnchanged(id: String, oldLabel: String, newLabel: String) {
        val type = types.getAll().firstOrNull { it.id == id } ?: return
        if (type.label == oldLabel) types.upsert(type.copy(label = newLabel))
    }

    suspend fun saveRoom(room: HouseRoom) = rooms.upsert(room.toEntity())

    suspend fun deleteRoom(id: String) {
        db.withTransaction {
            tasks.clearRoom(id)
            rooms.delete(id)
        }
    }

    // --- Sauvegarde JSON ---

    suspend fun snapshot(): BackupData = BackupData(
        exportedAt = LocalDateTime.now().toString(),
        types = types.getAll().map { it.toDomain() },
        rooms = rooms.getAll().map { it.toDomain() },
        tasks = tasks.getAll().map { it.toDomain() },
        events = events.getAll().map { it.toDomain() },
    )

    /** Remplace toutes les données par celles de la sauvegarde. */
    suspend fun restore(data: BackupData) {
        db.withTransaction {
            events.deleteAll()
            tasks.deleteAll()
            rooms.deleteAll()
            types.deleteAll()
            types.upsertAll(data.types.map { it.toEntity() })
            rooms.upsertAll(data.rooms.map { it.toEntity() })
            tasks.upsertAll(data.tasks.map { it.toEntity() })
            events.upsertAll(data.events.map { it.toEntity() })
        }
    }

    suspend fun typesNow(): List<TaskType> = typesFlow.first()

    private fun newEvent(taskId: String, e: NewEvent) = TaskEvent(
        id = UUID.randomUUID().toString(),
        taskId = taskId,
        kind = e.kind,
        date = e.date,
        minutes = e.minutes,
        note = e.note,
        timestamp = System.currentTimeMillis(),
    )
}
