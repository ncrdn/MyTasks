package com.nyl.mytasks.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.graphics.compositeOver
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
import androidx.compose.foundation.layout.statusBarsPadding
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
        0xFF0F9D8A, 0xFFE08A00, 0xFFD6409F, 0xFF1D8FE1, 0xFF4F46E5, 0xFFD92D3A,
        0xFF43A047, 0xFF8E5CF7, 0xFF546E7A, 0xFF8D6E63,
    )
}

fun Long.toColor(): Color = Color(this.toInt())

@Composable
fun TypeBadge(type: TaskType?, size: Int = 46) {
    val color = (type?.color ?: 0xFF607D8B).toColor()
    Box(
        modifier = Modifier.size(size.dp).clip(RoundedCornerShape((size * 0.32f).dp)).background(color.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(IconCatalog.get(type?.iconKey ?: ""), contentDescription = null, tint = color, modifier = Modifier.size((size * 0.52f).dp))
    }
}

/** En-tête d'écran moderne : sous-titre discret, grand titre et actions dans des pastilles rondes. */
@Composable
fun ScreenHeader(
    title: String,
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null,
    actions: @Composable () -> Unit = {},
) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.size(12.dp))
        }
        Column(Modifier.weight(1f)) {
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(title, style = com.nyl.mytasks.ui.theme.ScreenTitleStyle, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        actions()
    }
}

/** Bouton d'action rond posé sur une pastille blanche. */
@Composable
fun RoundAction(icon: ImageVector, description: String, onClick: () -> Unit) {
    androidx.compose.material3.FilledTonalIconButton(
        onClick = onClick,
        modifier = Modifier.size(46.dp),
        colors = androidx.compose.material3.IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) { Icon(icon, contentDescription = description) }
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
    Row(modifier.padding(start = 4.dp, top = 22.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
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
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (active) 2.dp else 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                late -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f).compositeOver(MaterialTheme.colorScheme.surfaceContainerLow)
                active -> MaterialTheme.colorScheme.surfaceContainerLow
                else -> MaterialTheme.colorScheme.surfaceContainerLowest
            },
        ),
    ) {
        Row(
            Modifier.padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 8.dp),
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
                Box(
                    Modifier.size(48.dp).clip(CircleShape).clickable(onClick = onComplete).semantics { contentDescription = desc },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier.size(34.dp).clip(CircleShape).border(2.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
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
