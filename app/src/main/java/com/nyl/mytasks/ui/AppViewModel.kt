package com.nyl.mytasks.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nyl.mytasks.AppContainer
import com.nyl.mytasks.data.AppSettings
import com.nyl.mytasks.domain.BackupCodec
import com.nyl.mytasks.domain.DashboardCalculator
import com.nyl.mytasks.domain.DashboardState
import com.nyl.mytasks.domain.HouseRoom
import com.nyl.mytasks.domain.ProjectLogic
import com.nyl.mytasks.domain.Task
import com.nyl.mytasks.domain.TaskEvent
import com.nyl.mytasks.ui.common.PendingCompletion
import com.nyl.mytasks.domain.TaskKind
import com.nyl.mytasks.domain.TaskType
import com.nyl.mytasks.notif.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.UUID

/** État et actions partagés par tous les écrans (une seule source de vérité : le dépôt). */
class AppViewModel(private val container: AppContainer) : ViewModel() {
    private val repo = container.repository
    private val eager = SharingStarted.Eagerly

    val types: StateFlow<List<TaskType>> = repo.typesFlow.stateIn(viewModelScope, eager, emptyList())
    val rooms: StateFlow<List<HouseRoom>> = repo.roomsFlow.stateIn(viewModelScope, eager, emptyList())
    val tasks: StateFlow<List<Task>> = repo.tasksFlow.stateIn(viewModelScope, eager, emptyList())
    val events: StateFlow<List<TaskEvent>> = repo.eventsFlow.stateIn(viewModelScope, eager, emptyList())
    val today: StateFlow<LocalDate> = container.clock.today
    val settings: StateFlow<AppSettings> = container.settings.flow.stateIn(viewModelScope, eager, AppSettings())

    val eventsByTask: StateFlow<Map<String, List<TaskEvent>>> =
        events.map { list -> list.groupBy { it.taskId } }.stateIn(viewModelScope, eager, emptyMap())

    val dashboard: StateFlow<DashboardState?> = combine(tasks, events, types, today) { t, e, ty, d ->
        val kinds = ty.associate { it.id to it.kind }
        DashboardCalculator.compute(t, e, { kinds[it] }, d)
    }.stateIn(viewModelScope, eager, null)

    fun kindOf(typeId: String): TaskKind? = types.value.firstOrNull { it.id == typeId }?.kind

    /** À appeler à la reprise de l'app : met à jour la date du jour (changement de jour). */
    fun refreshToday() { container.clock.now() }

    private fun launch(block: suspend () -> Unit) { viewModelScope.launch { block() } }

    /**
     * Flux de complétion (section 5.4) : domestique/ponctuelle sont terminées immédiatement ; chantier et
     * projet renvoient une demande de saisie (session ou temps) que l'UI doit présenter à l'utilisateur.
     */
    fun beginComplete(task: Task): PendingCompletion? {
        val kind = kindOf(task.typeId) ?: return null
        return when (kind) {
            TaskKind.DOMESTIC, TaskKind.ONE_OFF -> { complete(task.id); null }
            TaskKind.WORKSITE -> PendingCompletion(task, kind, 0)
            TaskKind.PROJECT -> {
                val week = ProjectLogic.week(task, eventsByTask.value[task.id].orEmpty(), today.value)
                val goal = task.weeklyGoalMinutes ?: 30
                PendingCompletion(task, kind, if (week.remaining > 0) minOf(week.remaining, goal) else goal)
            }
        }
    }

    // --- Tâches ---
    fun newId(): String = UUID.randomUUID().toString()
    fun saveNew(task: Task) = launch { repo.createTask(task) }
    fun saveEdited(task: Task) = launch { repo.updateTask(task) }
    fun delete(id: String) = launch { repo.deleteTask(id) }
    fun complete(id: String) = launch { repo.complete(id) }
    fun skip(id: String) = launch { repo.skip(id) }
    fun abandon(id: String) = launch { repo.abandon(id) }
    fun reopen(id: String) = launch { repo.reopen(id) }
    fun pause(id: String) = launch { repo.pause(id) }
    fun resume(id: String) = launch { repo.resume(id) }
    fun postpone(id: String, newDue: LocalDate) = launch { repo.postpone(id, newDue) }
    fun addSession(id: String, date: LocalDate, minutes: Int?, note: String) =
        launch { repo.addSession(id, date, minutes, note) }
    fun deleteEvent(eventId: String) = launch { repo.deleteEvent(eventId) }

    // --- Catalogue ---
    fun saveType(type: TaskType) = launch { repo.saveType(type) }
    fun saveRoom(room: HouseRoom) = launch { repo.saveRoom(room) }
    fun deleteRoom(id: String) = launch { repo.deleteRoom(id) }

    // --- Réglages ---
    fun setFontScale(scale: Float) = launch { container.settings.update { it.copy(fontScale = scale) } }

    fun updateSettings(context: Context, transform: (AppSettings) -> AppSettings) = launch {
        container.settings.update(transform)
        ReminderScheduler.schedule(context.applicationContext, container.settings.flow.first(), replace = true)
    }

    // --- Sauvegarde JSON ---
    suspend fun export(context: Context, uri: Uri): Result<Unit> = runCatching {
        val text = BackupCodec.encode(repo.snapshot())
        withContext(Dispatchers.IO) {
            context.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(text.toByteArray()) }
        }
    }

    suspend fun import(context: Context, uri: Uri): Result<Unit> = runCatching {
        val text = withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() }
        }
        repo.restore(BackupCodec.decode(text))
    }
}
