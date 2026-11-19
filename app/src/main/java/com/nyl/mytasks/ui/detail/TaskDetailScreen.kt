package com.nyl.mytasks.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nyl.mytasks.R
import com.nyl.mytasks.domain.EventKind
import com.nyl.mytasks.domain.ProjectLogic
import com.nyl.mytasks.domain.TaskEvent
import com.nyl.mytasks.domain.TaskLogic
import com.nyl.mytasks.domain.TaskKind
import com.nyl.mytasks.domain.TaskStatus
import com.nyl.mytasks.ui.AppViewModel
import com.nyl.mytasks.ui.common.CompletionDialogs
import com.nyl.mytasks.ui.common.ConfirmDialog
import com.nyl.mytasks.ui.common.DatePickerDialogFor
import com.nyl.mytasks.ui.common.OverduePill
import com.nyl.mytasks.ui.common.PendingCompletion
import com.nyl.mytasks.ui.common.Pill
import com.nyl.mytasks.ui.common.SectionTitle
import com.nyl.mytasks.ui.common.SessionDialog
import com.nyl.mytasks.ui.common.TypeBadge
import com.nyl.mytasks.ui.common.describeDue
import com.nyl.mytasks.ui.common.describeRecurrence
import com.nyl.mytasks.ui.common.formatLong
import com.nyl.mytasks.ui.common.formatMinutes
import com.nyl.mytasks.ui.common.statusLabel
import com.nyl.mytasks.ui.common.toColor

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskDetailScreen(
    vm: AppViewModel,
    taskId: String,
    onBack: () -> Unit,
    onEdit: (typeId: String, taskId: String) -> Unit,
) {
    val tasks by vm.tasks.collectAsStateWithLifecycle()
    val types by vm.types.collectAsStateWithLifecycle()
    val rooms by vm.rooms.collectAsStateWithLifecycle()
    val eventsByTask by vm.eventsByTask.collectAsStateWithLifecycle()
    val today by vm.today.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val task = tasks.firstOrNull { it.id == taskId }
    val type = types.firstOrNull { it.id == task?.typeId }
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmAbandon by remember { mutableStateOf(false) }
    var eventToDelete by remember { mutableStateOf<TaskEvent?>(null) }
    var postponing by remember { mutableStateOf(false) }
    var sessionDialog by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf<PendingCompletion?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(type?.label.orEmpty()) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    if (task != null) {
                        IconButton(onClick = { onEdit(task.typeId, task.id) }) {
                            Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.action_edit))
                        }
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete))
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (task == null || type == null) {
            Text(stringResource(R.string.task_not_found), Modifier.padding(padding).padding(16.dp))
            return@Scaffold
        }
        val kind = type.kind
        val events = eventsByTask[task.id].orEmpty()
        val sessions = events.filter { it.kind == EventKind.SESSION }
        val project = if (kind == TaskKind.PROJECT) ProjectLogic.week(task, events, today) else null
        val active = task.status.isActive
        val due = task.dueDate
        val overdue = active && !task.paused && (
            if (kind == TaskKind.PROJECT) project?.isLate == true else due != null && due.isBefore(today)
            )

        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TypeBadge(type, 48)
                Column(Modifier.padding(start = 12.dp).weight(1f)) {
                    Text(task.title, style = MaterialTheme.typography.headlineSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                        Pill(statusLabel(task.status), MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
                        if (task.paused) Pill(stringResource(R.string.paused), MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
                        if (overdue) OverduePill(stringResource(R.string.late_label))
                    }
                }
            }

            // --- Champs ---
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    task.roomId?.let { id -> rooms.firstOrNull { it.id == id }?.let { Field(stringResource(R.string.field_room), it.name) } }
                    when (kind) {
                        TaskKind.DOMESTIC -> {
                            task.recurrence?.let { Field(stringResource(R.string.field_recurrence), describeRecurrence(context, it)) }
                            due?.let { Field(stringResource(R.string.field_next_due), "${it.formatLong()} (${describeDue(context, it, today)})") }
                            task.estimatedMinutes?.let { Field(stringResource(R.string.field_duration), formatMinutes(it)) }
                            if (task.recurrence != null) {
                                Field(
                                    stringResource(R.string.field_next_calc),
                                    stringResource(if (task.anchorOnTheoretical) R.string.anchor_theoretical else R.string.anchor_completion),
                                )
                            }
                        }
                        TaskKind.ONE_OFF -> {
                            due?.let { Field(stringResource(R.string.field_due), "${it.formatLong()} (${describeDue(context, it, today)})") }
                            task.place?.takeIf { it.isNotBlank() }?.let { Field(stringResource(R.string.field_place), it) }
                            task.estimatedMinutes?.let { Field(stringResource(R.string.field_duration), formatMinutes(it)) }
                        }
                        TaskKind.WORKSITE -> {
                            task.effort?.let { Field(stringResource(R.string.field_effort), stringResource(R.string.effort_value, it)) }
                            task.priority?.let { Field(stringResource(R.string.field_priority), stringResource(R.string.priority_value, it)) }
                            due?.let { Field(stringResource(R.string.field_target_year), stringResource(R.string.before_end_of, it.year)) }
                            Field(stringResource(R.string.field_sessions), "${sessions.size}")
                        }
                        TaskKind.PROJECT -> task.weeklyGoalMinutes?.let {
                            Field(stringResource(R.string.field_weekly_goal), stringResource(R.string.per_week, formatMinutes(it)))
                        }
                    }
                    Field(stringResource(R.string.field_created), task.createdAt.formatLong())
                    task.closedAt?.let { Field(stringResource(R.string.field_closed), it.formatLong()) }
                    if (task.notes.isNotBlank()) Field(stringResource(R.string.field_notes), task.notes)
                }
            }

            if (project != null && active) {
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.this_week), style = MaterialTheme.typography.titleSmall)
                        LinearProgressIndicator(
                            progress = { project.progress }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            color = type.color.toColor(),
                        )
                        Text(stringResource(R.string.progress_minutes, formatMinutes(project.doneThisWeek), formatMinutes(project.target)))
                        if (project.carryOver > 0) {
                            Text(
                                stringResource(R.string.catch_up, formatMinutes(project.carryOver)),
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }

            // --- Actions ---
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (active) {
                    Button(onClick = { pending = vm.beginComplete(task) }) { Text(stringResource(R.string.action_mark_done)) }
                    if (kind == TaskKind.WORKSITE || kind == TaskKind.PROJECT) {
                        OutlinedButton(onClick = { sessionDialog = true }) {
                            Text(stringResource(if (kind == TaskKind.PROJECT) R.string.log_time else R.string.log_session))
                        }
                    }
                    if (kind == TaskKind.PROJECT) {
                        OutlinedButton(onClick = { vm.complete(task.id) }) { Text(stringResource(R.string.finish_project)) }
                    }
                    if (kind != TaskKind.PROJECT && due != null) {
                        OutlinedButton(onClick = { postponing = true }) { Text(stringResource(R.string.action_postpone)) }
                    }
                    if (kind == TaskKind.DOMESTIC && task.recurrence != null && !task.paused) {
                        OutlinedButton(onClick = { vm.skip(task.id) }) { Text(stringResource(R.string.action_skip)) }
                    }
                    if (task.recurrence != null) {
                        OutlinedButton(onClick = { if (task.paused) vm.resume(task.id) else vm.pause(task.id) }) {
                            Text(stringResource(if (task.paused) R.string.action_resume else R.string.action_pause))
                        }
                    }
                    OutlinedButton(onClick = { confirmAbandon = true }) { Text(stringResource(R.string.action_abandon)) }
                } else {
                    Button(onClick = { vm.reopen(task.id) }) { Text(stringResource(R.string.action_reopen)) }
                }
            }

            // --- Historique ---
            SectionTitle(stringResource(R.string.history), events.size)
            if (events.isEmpty()) Text(stringResource(R.string.history_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
            events.forEachIndexed { i, e ->
                if (i > 0) HorizontalDivider()
                EventRow(e, onDelete = if (TaskLogic.isUndoable(e.kind)) ({ eventToDelete = e }) else null)
            }
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.delete_title), message = stringResource(R.string.delete_message),
            confirmText = stringResource(R.string.action_delete),
            onConfirm = { vm.delete(taskId); onBack() }, onDismiss = { confirmDelete = false },
        )
    }
    eventToDelete?.let { e ->
        ConfirmDialog(
            title = stringResource(R.string.delete_event_title),
            message = stringResource(if (e.kind == EventKind.SESSION) R.string.delete_event_message_session else R.string.delete_event_message),
            confirmText = stringResource(R.string.action_delete),
            onConfirm = { vm.deleteEvent(e.id) }, onDismiss = { eventToDelete = null },
        )
    }
    if (confirmAbandon) {
        ConfirmDialog(
            title = stringResource(R.string.abandon_title), message = stringResource(R.string.abandon_message),
            confirmText = stringResource(R.string.action_abandon),
            onConfirm = { vm.abandon(taskId) }, onDismiss = { confirmAbandon = false },
        )
    }
    if (postponing) {
        DatePickerDialogFor(task?.dueDate, onDismiss = { postponing = false }, onPick = { vm.postpone(taskId, it) })
    }
    if (sessionDialog && task != null) {
        val isProject = type?.kind == TaskKind.PROJECT
        SessionDialog(
            title = stringResource(if (isProject) R.string.log_time else R.string.log_session),
            minutesRequired = isProject,
            initialMinutes = if (isProject) (task.weeklyGoalMinutes ?: 30) else 0,
            today = today,
            onDismiss = { sessionDialog = false },
            onConfirm = { d, m, n -> vm.addSession(taskId, d, m, n); sessionDialog = false },
        )
    }
    CompletionDialogs(
        pending = pending, today = today, onDismiss = { pending = null },
        onFinish = { vm.complete(it.id) },
        onSession = { t, d, m, n -> vm.addSession(t.id, d, m, n) },
    )
}

@Composable
private fun Field(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun EventRow(e: TaskEvent, onDelete: (() -> Unit)?) {
    val label = when (e.kind) {
        EventKind.CREATED -> stringResource(R.string.event_created)
        EventKind.DONE -> stringResource(R.string.event_done)
        EventKind.SKIPPED -> stringResource(R.string.event_skipped)
        EventKind.POSTPONED -> stringResource(R.string.event_postponed)
        EventKind.ABANDONED -> stringResource(R.string.event_abandoned)
        EventKind.REOPENED -> stringResource(R.string.event_reopened)
        EventKind.SESSION -> stringResource(R.string.event_session)
        EventKind.PAUSED -> stringResource(R.string.event_paused)
        EventKind.RESUMED -> stringResource(R.string.event_resumed)
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                buildString {
                    append(label)
                    e.minutes?.let { append(" · ").append(formatMinutes(it)) }
                },
                style = MaterialTheme.typography.bodyLarge,
            )
            val extra = when (e.kind) {
                EventKind.POSTPONED, EventKind.SKIPPED -> TaskLogic.noteDate(e.note)?.formatLong()
                EventKind.DONE -> null // la note garde l'échéance traitée (technique)
                else -> e.note.takeIf { it.isNotBlank() }
            }
            if (extra != null) Text(extra, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(e.date.formatLong(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (onDelete != null) {
            IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete), modifier = Modifier.size(20.dp))
            }
        }
    }
}
