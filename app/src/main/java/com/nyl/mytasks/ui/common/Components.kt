package com.nyl.mytasks.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.Yard
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nyl.mytasks.R
import com.nyl.mytasks.domain.ProjectWeek
import com.nyl.mytasks.domain.Task
import com.nyl.mytasks.domain.TaskKind
import com.nyl.mytasks.domain.TaskStatus
import com.nyl.mytasks.domain.TaskType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import androidx.compose.material.icons.filled.CheckCircle

// --- Catalogue d'icônes et de couleurs proposées pour les types ---

object IconCatalog {
    val icons: Map<String, ImageVector> = linkedMapOf(
        "cleaning" to Icons.Filled.CleaningServices,
        "event" to Icons.Filled.Event,
        "construction" to Icons.Filled.Construction,
        "flag" to Icons.Filled.Flag,
        "home" to Icons.Filled.Home,
        "build" to Icons.Filled.Build,
        "handyman" to Icons.Filled.Handyman,
        "school" to Icons.Filled.School,
        "cart" to Icons.Filled.ShoppingCart,
        "star" to Icons.Filled.Star,
        "work" to Icons.Filled.Work,
        "favorite" to Icons.Filled.Favorite,
        "fitness" to Icons.Filled.FitnessCenter,
        "book" to Icons.Filled.MenuBook,
        "kitchen" to Icons.Filled.Kitchen,
        "yard" to Icons.Filled.Yard,
        "pets" to Icons.Filled.Pets,
        "brush" to Icons.Filled.Brush,
        "checklist" to Icons.Filled.Checklist,
    )

    fun get(key: String): ImageVector = icons[key] ?: Icons.Filled.Checklist

    val colors: List<Long> = listOf(
        0xFF00897B, 0xFFEF6C00, 0xFF6D4C41, 0xFF5E35B1, 0xFF1E88E5, 0xFFD81B60,
        0xFF43A047, 0xFFF9A825, 0xFF546E7A, 0xFFC62828,
    )
}

fun Long.toColor(): Color = Color(this.toInt())

@Composable
fun TypeBadge(type: TaskType?, size: Int = 40) {
    val color = (type?.color ?: 0xFF607D8B).toColor()
    Box(
        modifier = Modifier.size(size.dp).clip(CircleShape).background(color.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(IconCatalog.get(type?.iconKey ?: ""), contentDescription = null, tint = color, modifier = Modifier.size((size * 0.55f).dp))
    }
}

@Composable
fun Pill(text: String, container: Color, content: Color, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = content,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.clip(RoundedCornerShape(50)).background(container).padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
fun OverduePill(text: String) =
    Pill(text, MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)

@Composable
fun SectionTitle(text: String, count: Int? = null, modifier: Modifier = Modifier) {
    Row(modifier.padding(top = 20.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        if (count != null) {
            Spacer(Modifier.size(8.dp))
            Pill("$count", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

// --- Sous-titre descriptif d'une tâche ---

@Composable
fun taskSubtitle(task: Task, kind: TaskKind, roomName: String?): String {
    val parts = ArrayList<String>()
    when (kind) {
        TaskKind.DOMESTIC -> {
            roomName?.let(parts::add)
            task.estimatedMinutes?.let { parts += formatMinutes(it) }
            task.recurrence?.let { parts += describeRecurrence(androidx.compose.ui.platform.LocalContext.current, it) }
            if (task.paused) parts += stringResource(R.string.paused)
        }
        TaskKind.ONE_OFF -> {
            task.place?.takeIf { it.isNotBlank() }?.let(parts::add)
            task.estimatedMinutes?.let { parts += formatMinutes(it) }
        }
        TaskKind.WORKSITE -> {
            roomName?.let(parts::add)
            task.effort?.let { parts += stringResource(R.string.effort_short, it) }
            task.priority?.let { parts += stringResource(R.string.priority_short, it) }
            task.dueDate?.let { parts += stringResource(R.string.before_end_of, it.year) }
        }
        TaskKind.PROJECT -> task.weeklyGoalMinutes?.let { parts += stringResource(R.string.per_week, formatMinutes(it)) }
    }
    return parts.joinToString(" · ")
}

@Composable
fun statusLabel(status: TaskStatus): String = when (status) {
    TaskStatus.TODO -> stringResource(R.string.status_todo)
    TaskStatus.DONE -> stringResource(R.string.status_done)
    TaskStatus.POSTPONED -> stringResource(R.string.status_postponed)
    TaskStatus.ABANDONED -> stringResource(R.string.status_abandoned)
}

// --- Carte de tâche ---

@Composable
fun TaskCard(
    task: Task,
    kind: TaskKind,
    type: TaskType?,
    roomName: String?,
    today: LocalDate,
    project: ProjectWeek?,
    onClick: () -> Unit,
    onComplete: (() -> Unit)?,
    daysLateOverride: Int? = null,
    showType: Boolean = false,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val active = task.status.isActive
    val due = task.dueDate
    val late = when {
        kind == TaskKind.PROJECT -> project?.isLate == true
        else -> active && !task.paused && due != null && due.isBefore(today)
    }
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (active) MaterialTheme.colorScheme.surfaceContainerLow else MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
    ) {
        Row(
            Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TypeBadge(type)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(
                    task.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (active) null else TextDecoration.LineThrough,
                    color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (showType && type != null) {
                    Text(type.label, style = MaterialTheme.typography.labelSmall, color = type.color.toColor())
                }
                val sub = taskSubtitle(task, kind, roomName)
                if (sub.isNotBlank()) {
                    Text(
                        sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                    )
                }
                if (kind == TaskKind.PROJECT && project != null && active) {
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(progress = { project.progress }, modifier = Modifier.fillMaxWidth())
                    Text(
                        buildString {
                            append(stringResource(R.string.progress_minutes, formatMinutes(project.doneThisWeek), formatMinutes(project.target)))
                            if (project.carryOver > 0) append(" · ").append(stringResource(R.string.catch_up, formatMinutes(project.carryOver)))
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (!active) {
                    Text(statusLabel(task.status), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                if (late) {
                    val days = daysLateOverride ?: due?.let { java.time.temporal.ChronoUnit.DAYS.between(it, today).toInt() } ?: 0
                    OverduePill(if (kind == TaskKind.PROJECT) stringResource(R.string.late_label) else stringResource(R.string.due_late_days, days.coerceAtLeast(1)))
                } else if (active && due != null && kind != TaskKind.PROJECT && kind != TaskKind.WORKSITE) {
                    Text(describeDue(context, due, today), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else if (active && kind == TaskKind.WORKSITE && due != null) {
                    Text(stringResource(R.string.before_end_of, due.year), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (onComplete != null && active) {
                val desc = stringResource(R.string.action_mark_done)
                IconButton(onClick = onComplete, modifier = Modifier.size(48.dp).semantics { contentDescription = desc }) {
                    Icon(Icons.Filled.RadioButtonUnchecked, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            } else if (!active && task.status == TaskStatus.DONE) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 12.dp))
            }
        }
    }
}

// --- Champs de formulaire ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDialogFor(initial: LocalDate?, onDismiss: () -> Unit, onPick: (LocalDate) -> Unit) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = (initial ?: LocalDate.now()).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                onDismiss()
            }) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    ) { DatePicker(state = state) }
}

@Composable
fun DateField(label: String, value: LocalDate?, onChange: (LocalDate) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    OutlinedButton(
        onClick = { open = true },
        modifier = modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Icon(Icons.Filled.CalendarMonth, contentDescription = null)
        Spacer(Modifier.size(8.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text(value?.formatLong() ?: "—", style = MaterialTheme.typography.bodyLarge)
        }
    }
    if (open) DatePickerDialogFor(value, { open = false }, onChange)
}

@Composable
fun NumberField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    suffix: String? = null,
    isError: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(it.filter(Char::isDigit).take(5)) },
        label = { Text(label) },
        suffix = suffix?.let { { Text(it) } },
        isError = isError,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
        modifier = modifier,
    )
}

/** Liste déroulante générique sous forme de bouton. */
@Composable
fun <T> DropdownField(
    label: String,
    selectedText: String,
    options: List<T>,
    optionText: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(
            onClick = { open = true },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(4.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelSmall)
                Text(selectedText, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { opt ->
                DropdownMenuItem(text = { Text(optionText(opt)) }, onClick = { open = false; onSelect(opt) })
            }
        }
    }
}

/** Puce de filtre avec menu déroulant. */
@Composable
fun <T> DropdownChip(
    label: String,
    options: List<T>,
    optionText: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    selected: Boolean,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        FilterChip(
            selected = selected,
            onClick = { open = true },
            label = { Text(label, maxLines = 1) },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp)) },
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { opt ->
                DropdownMenuItem(text = { Text(optionText(opt)) }, onClick = { open = false; onSelect(opt) })
            }
        }
    }
}

@Composable
fun ConfirmDialog(title: String, message: String, confirmText: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = { onConfirm(); onDismiss() }) { Text(confirmText) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

// --- Flux de complétion (section 5.4) ---

data class PendingCompletion(val task: Task, val kind: TaskKind, val suggestedMinutes: Int)

/**
 * Projet : saisie de temps préremplie ; chantier : « fait entièrement » ou « enregistrer une session ».
 * Les autres types sont complétés directement par l'appelant.
 */
@Composable
fun CompletionDialogs(
    pending: PendingCompletion?,
    today: LocalDate,
    onDismiss: () -> Unit,
    onFinish: (Task) -> Unit,
    onSession: (Task, LocalDate, Int?, String) -> Unit,
) {
    if (pending == null) return
    var sessionMode by remember(pending) { mutableStateOf(pending.kind == TaskKind.PROJECT) }
    if (!sessionMode) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(pending.task.title) },
            text = { Text(stringResource(R.string.worksite_complete_prompt)) },
            confirmButton = {
                TextButton(onClick = { onFinish(pending.task); onDismiss() }) { Text(stringResource(R.string.finish_entirely)) }
            },
            dismissButton = { TextButton(onClick = { sessionMode = true }) { Text(stringResource(R.string.log_session)) } },
        )
    } else {
        SessionDialog(
            title = if (pending.kind == TaskKind.PROJECT) stringResource(R.string.log_time) else stringResource(R.string.log_session),
            minutesRequired = pending.kind == TaskKind.PROJECT,
            initialMinutes = pending.suggestedMinutes,
            today = today,
            onDismiss = onDismiss,
            onConfirm = { date, minutes, note -> onSession(pending.task, date, minutes, note); onDismiss() },
        )
    }
}

@Composable
fun SessionDialog(
    title: String,
    minutesRequired: Boolean,
    initialMinutes: Int,
    today: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate, Int?, String) -> Unit,
) {
    var date by remember { mutableStateOf(today) }
    var minutes by remember { mutableStateOf(if (initialMinutes > 0) initialMinutes.toString() else "") }
    var note by remember { mutableStateOf("") }
    val minutesValue = minutes.toIntOrNull()
    val valid = !minutesRequired || (minutesValue != null && minutesValue > 0)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DateField(stringResource(R.string.field_date), date, { date = it })
                NumberField(
                    label = stringResource(if (minutesRequired) R.string.field_minutes else R.string.field_minutes_optional),
                    value = minutes, onChange = { minutes = it }, suffix = "min", modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = note, onValueChange = { note = it }, label = { Text(stringResource(R.string.field_comment)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = { onConfirm(date, minutesValue?.takeIf { it > 0 }, note.trim()) }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
fun EmptyState(text: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Ligne cliquable sans carte, utilisée dans les réglages. */
@Composable
fun ClickableRow(modifier: Modifier = Modifier, onClick: () -> Unit, content: @Composable () -> Unit) {
    Row(
        modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) { content() }
}
