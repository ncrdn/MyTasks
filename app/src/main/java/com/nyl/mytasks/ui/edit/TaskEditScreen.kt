package com.nyl.mytasks.ui.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nyl.mytasks.R
import com.nyl.mytasks.domain.QuotaLogic
import com.nyl.mytasks.domain.RecurrenceEngine
import com.nyl.mytasks.domain.Task
import com.nyl.mytasks.domain.TaskKind
import com.nyl.mytasks.ui.AppViewModel
import com.nyl.mytasks.ui.common.ChipRow
import com.nyl.mytasks.ui.common.ChoiceChip
import com.nyl.mytasks.ui.common.DateField
import com.nyl.mytasks.ui.common.DropdownField
import com.nyl.mytasks.ui.common.FormCard
import com.nyl.mytasks.ui.common.FormTextField
import com.nyl.mytasks.ui.common.NumberField
import com.nyl.mytasks.ui.common.PointsBadge
import com.nyl.mytasks.ui.common.RoundAction
import com.nyl.mytasks.ui.common.ScreenHeader
import com.nyl.mytasks.ui.common.SegmentedPicker
import com.nyl.mytasks.ui.common.formatMinutes
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import java.time.LocalDate

/** Création et modification d'une tâche ; les champs affichés dépendent du comportement du type. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskEditScreen(
    vm: AppViewModel,
    typeId: String,
    taskId: String?,
    onClose: () -> Unit,
) {
    val types by vm.types.collectAsStateWithLifecycle()
    val rooms by vm.rooms.collectAsStateWithLifecycle()
    val today by vm.today.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    val type = types.firstOrNull { it.id == typeId } ?: return
    val kind = type.kind
    val existing = remember(taskId) { taskId?.let { id -> vm.tasks.value.firstOrNull { it.id == id } } }
    val isNew = existing == null

    var title by remember { mutableStateOf(existing?.title ?: "") }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var roomId by remember { mutableStateOf(existing?.roomId) }
    var minutes by remember { mutableStateOf(existing?.estimatedMinutes?.toString() ?: "") }
    var place by remember { mutableStateOf(existing?.place ?: "") }
    var dueDate by remember { mutableStateOf(existing?.dueDate ?: today) }
    var dueTouched by remember { mutableStateOf(!isNew) }
    var anchorTheoretical by remember { mutableStateOf(existing?.anchorOnTheoretical ?: false) }
    var effort by remember { mutableStateOf((existing?.effort ?: 5).toFloat()) }
    var priority by remember { mutableStateOf(existing?.priority ?: 3) }
    // Année de réalisation (null = en attente) ; une année n'est proposée que s'il y reste assez de points d'effort.
    var targetYear by remember { mutableStateOf<Int?>(if (existing != null) existing.dueDate?.year else today.year) }
    val allTasks by vm.tasks.collectAsStateWithLifecycle()
    val quotas by vm.yearQuotas.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val worksites = remember(allTasks, typeId) { allTasks.filter { it.typeId == typeId } }
    var goalMinutes by remember { mutableStateOf(existing?.monthlyGoalMinutes?.toString() ?: "") }
    var showErrors by remember { mutableStateOf(false) }
    val recForm = remember { RecurrenceForm(existing?.dueDate ?: today).also { f -> if (existing != null) f.load(existing.recurrence) } }

    // Pour une nouvelle tâche récurrente, la première échéance suit la règle tant que l'utilisateur ne l'a pas fixée.
    val rule = recForm.toRule()
    LaunchedEffect(rule, isNew) {
        if (kind == TaskKind.DOMESTIC && !dueTouched) {
            dueDate = rule?.let { RecurrenceEngine.firstOnOrAfter(it, today) } ?: today
        }
    }

    val titleBlank = title.isBlank()
    val goalValue = goalMinutes.toIntOrNull() ?: 0

    // Années où le poids de la tâche tient dans le quota restant (hors la tâche elle-même si on la modifie).
    val points = effort.toInt()
    val firstYear = minOf(today.year, existing?.dueDate?.year ?: today.year)
    val yearsWithRoom = remember(worksites, quotas, settings, points, existing?.id, firstYear) {
        if (kind != TaskKind.WORKSITE) emptyList() else
            QuotaLogic.yearsWithRoom(points, firstYear, today.year + 10, quotas, settings.defaultYearQuota, worksites, existing?.id)
    }
    // Nouvelle tâche : si l'année proposée n'a plus assez de points, on choisit la première qui en a (ou l'attente).
    LaunchedEffect(yearsWithRoom) {
        if (kind == TaskKind.WORKSITE && isNew && targetYear != null && targetYear !in yearsWithRoom) {
            targetYear = yearsWithRoom.firstOrNull()
        }
    }
    fun freeIn(y: Int) = QuotaLogic.available(y, QuotaLogic.quotaFor(y, quotas, settings.defaultYearQuota), worksites, existing?.id)
    val yearOk = kind != TaskKind.WORKSITE || targetYear == null || targetYear in yearsWithRoom

    val valid = !titleBlank &&
        (kind != TaskKind.PROJECT || goalValue > 0) &&
        (kind != TaskKind.DOMESTIC || recForm.valid) &&
        yearOk

    fun save() {
        if (!valid) { showErrors = true; return }
        val est = minutes.toIntOrNull()?.takeIf { it > 0 }
        val base = existing ?: Task(id = vm.newId(), typeId = typeId, title = "", createdAt = today)
        val task = when (kind) {
            TaskKind.DOMESTIC -> base.copy(
                recurrence = rule, dueDate = dueDate, roomId = roomId, estimatedMinutes = est,
                anchorOnTheoretical = anchorTheoretical,
            )
            TaskKind.ONE_OFF -> base.copy(dueDate = dueDate, place = place.trim().ifBlank { null }, estimatedMinutes = est)
            TaskKind.WORKSITE -> base.copy(
                roomId = roomId, effort = effort.toInt(), priority = priority, dueDate = targetYear?.let { LocalDate.of(it, 12, 31) },
            )
            TaskKind.PROJECT -> base.copy(monthlyGoalMinutes = goalValue)
        }.copy(title = title.trim(), notes = notes.trim())
        if (isNew) vm.saveNew(task) else vm.saveEdited(task)
        onClose()
    }

    Scaffold(
        modifier = Modifier.imePadding(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            ScreenHeader(
                title = stringResource(if (isNew) R.string.form_new else R.string.form_edit),
                subtitle = type.label,
                leading = { RoundAction(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back), onClose) },
            )
        },
        // Bouton d'enregistrement toujours visible, en bas de l'écran.
        bottomBar = {
            Surface(shadowElevation = 8.dp, color = MaterialTheme.colorScheme.surface) {
                Button(
                    onClick = ::save,
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp).heightIn(min = 56.dp),
                    shape = RoundedCornerShape(28.dp),
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.save), style = MaterialTheme.typography.titleMedium)
                }
            }
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            FormCard(stringResource(R.string.form_general)) {
                FormTextField(
                    value = title, onValueChange = { title = it }, capitalize = true,
                    label = stringResource(if (kind == TaskKind.PROJECT) R.string.field_project_name else R.string.field_object),
                    leadingIcon = Icons.Filled.Edit,
                    isError = showErrors && titleBlank, errorText = stringResource(R.string.error_required),
                )
                if (kind == TaskKind.DOMESTIC || kind == TaskKind.WORKSITE) {
                    DropdownField(
                        label = stringResource(R.string.field_room),
                        selectedText = rooms.firstOrNull { it.id == roomId }?.name ?: stringResource(R.string.no_room),
                        options = listOf<String?>(null) + rooms.map { it.id },
                        optionText = { id -> rooms.firstOrNull { it.id == id }?.name ?: stringResource(R.string.no_room) },
                        onSelect = { roomId = it },
                        icon = Icons.Filled.Home,
                    )
                }
            }

            when (kind) {
                TaskKind.DOMESTIC -> {
                    FormCard(stringResource(R.string.form_planning)) {
                        RecurrenceEditor(recForm)
                        DateField(
                            label = stringResource(R.string.field_next_due), value = dueDate,
                            onChange = { dueDate = it; dueTouched = true },
                        )
                        if (rule != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(stringResource(R.string.anchor_option), style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        stringResource(R.string.anchor_option_help), style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Switch(checked = anchorTheoretical, onCheckedChange = { anchorTheoretical = it })
                            }
                        }
                    }
                    FormCard(stringResource(R.string.form_duration)) { DurationField(minutes) { minutes = it } }
                }
                TaskKind.ONE_OFF -> {
                    FormCard(stringResource(R.string.form_due)) {
                        DateField(label = stringResource(R.string.field_due), value = dueDate, onChange = { dueDate = it })
                        FormTextField(
                            value = place, onValueChange = { place = it }, capitalize = true,
                            label = stringResource(R.string.field_place), leadingIcon = Icons.Filled.Place,
                        )
                    }
                    FormCard(stringResource(R.string.form_duration)) { DurationField(minutes) { minutes = it } }
                }
                TaskKind.WORKSITE -> {
                    FormCard(stringResource(R.string.form_weight)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(R.string.effort_title), style = MaterialTheme.typography.bodyLarge)
                                Text(stringResource(R.string.effort_help), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            PointsBadge(points, MaterialTheme.colorScheme.primary)
                        }
                        Slider(
                            value = effort, onValueChange = { effort = it.toInt().toFloat() }, valueRange = 1f..10f, steps = 8,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(stringResource(R.string.priority_label, priority), style = MaterialTheme.typography.bodyLarge)
                            Text(stringResource(R.string.priority_help), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            SegmentedPicker(1..5, priority, { priority = it }, Modifier.padding(top = 4.dp))
                        }
                    }
                    FormCard(stringResource(R.string.form_year)) {
                        val selectedYear = targetYear
                        DropdownField(
                            label = stringResource(R.string.field_target_year),
                            selectedText = if (selectedYear == null) stringResource(R.string.year_not_attached)
                            else stringResource(R.string.year_option, selectedYear, (freeIn(selectedYear) - points).coerceAtLeast(0)),
                            options = listOf<Int?>(null) + yearsWithRoom,
                            optionText = { y -> if (y == null) stringResource(R.string.year_not_attached) else stringResource(R.string.year_option, y, freeIn(y) - points) },
                            onSelect = { targetYear = it },
                            icon = Icons.Filled.CalendarMonth,
                        )
                        Text(
                            if (yearsWithRoom.isEmpty()) stringResource(R.string.year_attach_none)
                            else stringResource(
                                R.string.year_room_hint,
                                yearsWithRoom.take(4).joinToString(" · ") { y -> context.getString(R.string.year_room_item, y, freeIn(y)) } + if (yearsWithRoom.size > 4) " …" else "",
                            ),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (!yearOk) {
                            Text(
                                stringResource(R.string.year_no_room, selectedYear?.let { freeIn(it) } ?: 0),
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
                TaskKind.PROJECT -> {
                    FormCard(stringResource(R.string.form_goal)) {
                        NumberField(
                            label = stringResource(R.string.field_monthly_goal_min), value = goalMinutes, onChange = { goalMinutes = it },
                            suffix = "min", modifier = Modifier.fillMaxWidth(), isError = showErrors && goalValue <= 0,
                        )
                        ChipRow {
                            listOf(60, 120, 240, 360, 600).forEach { m ->
                                ChoiceChip(goalValue == m, formatMinutes(m), { goalMinutes = m.toString() })
                            }
                        }
                    }
                }
            }

            FormCard(stringResource(R.string.form_notes)) {
                FormTextField(
                    value = notes, onValueChange = { notes = it }, singleLine = false, minLines = 3, capitalize = true,
                    label = stringResource(R.string.field_notes),
                )
            }
            Spacer(Modifier.size(8.dp))
        }
    }
}

@Composable
private fun DurationField(minutes: String, onChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        NumberField(
            label = stringResource(R.string.field_duration), value = minutes, onChange = onChange,
            suffix = "min", modifier = Modifier.fillMaxWidth(),
        )
        ChipRow {
            listOf(5, 15, 30, 60, 120).forEach { m ->
                ChoiceChip(minutes == m.toString(), formatMinutes(m), { onChange(m.toString()) })
            }
        }
    }
}
