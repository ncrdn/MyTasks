package com.nyl.mytasks.ui.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nyl.mytasks.R
import com.nyl.mytasks.domain.ProjectLogic
import com.nyl.mytasks.domain.QuotaLogic
import com.nyl.mytasks.ui.common.SectionTitle
import com.nyl.mytasks.ui.common.toColor
import com.nyl.mytasks.domain.Task
import com.nyl.mytasks.domain.TaskKind
import com.nyl.mytasks.domain.TaskStatus
import com.nyl.mytasks.domain.weekEnd
import com.nyl.mytasks.ui.AppViewModel
import com.nyl.mytasks.ui.common.CompletionDialogs
import com.nyl.mytasks.ui.common.DropdownChip
import com.nyl.mytasks.ui.common.EmptyState
import com.nyl.mytasks.ui.common.PendingCompletion
import com.nyl.mytasks.ui.common.RoundAction
import com.nyl.mytasks.ui.common.ScreenHeader
import androidx.compose.foundation.shape.RoundedCornerShape
import com.nyl.mytasks.ui.common.TaskCard
import com.nyl.mytasks.ui.common.TypeBadge
import java.time.LocalDate

private enum class StatusFilter(val label: Int) {
    ACTIVE(R.string.filter_active), ALL(R.string.filter_all), DONE(R.string.filter_done), ABANDONED(R.string.filter_abandoned)
}

private enum class PeriodFilter(val label: Int) {
    ALL(R.string.period_all), OVERDUE(R.string.period_overdue), TODAY(R.string.period_today),
    WEEK(R.string.period_week), MONTH(R.string.period_month),
    THIS_YEAR(R.string.period_this_year), FUTURE_YEARS(R.string.period_future_years)
}

/** Les travaux sont répartis par année : « Aujourd'hui / Cette semaine / Ce mois-ci » n'y ont pas de sens. */
private fun periodOptions(kind: TaskKind): List<PeriodFilter> = when (kind) {
    TaskKind.WORKSITE -> listOf(PeriodFilter.ALL, PeriodFilter.THIS_YEAR, PeriodFilter.FUTURE_YEARS, PeriodFilter.OVERDUE)
    else -> listOf(PeriodFilter.ALL, PeriodFilter.OVERDUE, PeriodFilter.TODAY, PeriodFilter.WEEK, PeriodFilter.MONTH)
}

private enum class SortMode(val label: Int) {
    BY_DUE(R.string.sort_due), BY_ROOM(R.string.sort_room), BY_PRIORITY(R.string.sort_priority),
    BY_YEAR(R.string.sort_year), BY_GOAL(R.string.sort_goal)
}

private fun sortOptions(kind: TaskKind): List<SortMode> = when (kind) {
    TaskKind.DOMESTIC -> listOf(SortMode.BY_DUE, SortMode.BY_ROOM)
    TaskKind.ONE_OFF -> listOf(SortMode.BY_DUE)
    TaskKind.WORKSITE -> listOf(SortMode.BY_PRIORITY, SortMode.BY_YEAR, SortMode.BY_ROOM)
    TaskKind.PROJECT -> listOf(SortMode.BY_GOAL)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TypeTabScreen(
    vm: AppViewModel,
    typeId: String,
    onOpenTask: (String) -> Unit,
    onAdd: (String) -> Unit,
) {
    val types by vm.types.collectAsStateWithLifecycle()
    val allTasks by vm.tasks.collectAsStateWithLifecycle()
    val rooms by vm.rooms.collectAsStateWithLifecycle()
    val eventsByTask by vm.eventsByTask.collectAsStateWithLifecycle()
    val today by vm.today.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current

    val type = types.firstOrNull { it.id == typeId } ?: return
    val kind = type.kind
    val roomNames = remember(rooms) { rooms.associate { it.id to it.name } }

    var query by remember(typeId) { mutableStateOf("") }
    var searching by remember(typeId) { mutableStateOf(false) }
    var status by remember(typeId) { mutableStateOf(StatusFilter.ACTIVE) }
    var period by remember(typeId) { mutableStateOf(PeriodFilter.ALL) }
    var roomFilter by remember(typeId) { mutableStateOf<String?>(null) }
    var minPriority by remember(typeId) { mutableStateOf(0) }
    var sort by remember(typeId) { mutableStateOf(sortOptions(kind).first()) }
    var showHistory by remember(typeId) { mutableStateOf(false) }
    var pending by remember { mutableStateOf<PendingCompletion?>(null) }

    val matching = remember(allTasks, typeId, query, period, roomFilter, minPriority, today) {
        allTasks.filter { t ->
            t.typeId == typeId &&
                (query.isBlank() || t.title.contains(query.trim(), ignoreCase = true)) &&
                (roomFilter == null || t.roomId == roomFilter) &&
                (t.priority ?: 5) >= minPriority &&
                matchesPeriod(t, period, today)
        }
    }
    val comparator = remember(sort, eventsByTask, today, roomNames) {
        comparatorFor(sort, roomNames) { t -> ProjectLogic.week(t, eventsByTask[t.id].orEmpty(), today).remaining }
    }
    val active = remember(matching, comparator) { matching.filter { it.status.isActive }.sortedWith(comparator) }
    val inactive = remember(matching) { matching.filter { !it.status.isActive }.sortedByDescending { it.closedAt } }

    val (mainList, historyList) = when (status) {
        StatusFilter.ACTIVE -> active to inactive
        StatusFilter.ALL -> (active + inactive) to emptyList()
        StatusFilter.DONE -> inactive.filter { it.status == TaskStatus.DONE } to emptyList()
        StatusFilter.ABANDONED -> inactive.filter { it.status == TaskStatus.ABANDONED } to emptyList()
    }

    // --- Travaux : vue par année avec quota de points d'effort ---
    val quotas by vm.yearQuotas.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    var year by remember(typeId) { mutableStateOf(today.year) }
    var editingQuota by remember(typeId) { mutableStateOf(false) }
    val isWorksite = kind == TaskKind.WORKSITE
    val worksiteAll = remember(allTasks, typeId) { allTasks.filter { it.typeId == typeId } }
    val yearQuota = QuotaLogic.quotaFor(year, quotas, settings.defaultYearQuota)
    val summary = remember(year, yearQuota, worksiteAll) { QuotaLogic.summary(year, yearQuota, worksiteAll) }
    // Les points d'une tâche terminée restent consommés : elles restent donc listées avec l'année.
    fun inStatus(t: Task) = when (status) {
        StatusFilter.ACTIVE -> t.status != TaskStatus.ABANDONED
        StatusFilter.ALL -> true
        StatusFilter.DONE -> t.status == TaskStatus.DONE
        StatusFilter.ABANDONED -> t.status == TaskStatus.ABANDONED
    }
    val attachedList = remember(matching, comparator, year, status) {
        matching.filter { QuotaLogic.yearOf(it) == year && inStatus(it) }.sortedWith(comparator)
    }
    val pendingList = remember(matching, comparator, status) {
        matching.filter { QuotaLogic.yearOf(it) == null && inStatus(it) && (status != StatusFilter.ACTIVE || it.status.isActive) }
            .sortedWith(comparator)
    }
    val futureGroups = remember(matching, comparator, status) {
        matching.filter { QuotaLogic.yearOf(it) != null && inStatus(it) }
            .sortedWith(comparator)
            .groupBy { QuotaLogic.yearOf(it)!! }
            .toSortedMap()
            .toList()
    }
    val roomYears = remember(worksiteAll, quotas, settings, today) {
        if (!isWorksite) emptyList() else (today.year..today.year + 10).mapNotNull { y ->
            val q = QuotaLogic.quotaFor(y, quotas, settings.defaultYearQuota)
            val free = QuotaLogic.available(y, q, worksiteAll)
            if (free > 0) y to free else null
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            ScreenHeader(
                title = type.label,
                leading = { TypeBadge(type, size = 46) },
                actions = {
                    RoundAction(
                        if (searching) Icons.Filled.Close else Icons.Filled.Search,
                        stringResource(R.string.search),
                    ) { searching = !searching; if (!searching) query = "" }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onAdd(typeId) },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.add)) },
                shape = RoundedCornerShape(22.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        },
    ) { padding ->
        Column2(padding) {
            if (searching) {
                OutlinedTextField(
                    value = query, onValueChange = { query = it }, singleLine = true,
                    label = { Text(stringResource(R.string.search_by_title)) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    DropdownChip(
                        label = stringResource(status.label), options = StatusFilter.entries,
                        optionText = { stringResource(it.label) }, onSelect = { status = it },
                        selected = status != StatusFilter.ACTIVE,
                    )
                }
                if (kind != TaskKind.PROJECT) item {
                    DropdownChip(
                        label = stringResource(period.label), options = periodOptions(kind),
                        optionText = { stringResource(it.label) },
                        onSelect = { period = it; if (it == PeriodFilter.THIS_YEAR) year = today.year },
                        selected = period != PeriodFilter.ALL,
                    )
                }
                if (kind == TaskKind.DOMESTIC || kind == TaskKind.WORKSITE) item {
                    val options = listOf<String?>(null) + rooms.map { it.id }
                    DropdownChip(
                        label = roomFilter?.let { roomNames[it] } ?: stringResource(R.string.filter_room),
                        options = options,
                        optionText = { id -> id?.let { roomNames[it] } ?: stringResource(R.string.all_rooms) },
                        onSelect = { roomFilter = it },
                        selected = roomFilter != null,
                    )
                }
                if (kind == TaskKind.WORKSITE) item {
                    DropdownChip(
                        label = if (minPriority == 0) stringResource(R.string.filter_priority) else stringResource(R.string.priority_at_least, minPriority),
                        options = listOf(0, 1, 2, 3, 4, 5),
                        optionText = { if (it == 0) stringResource(R.string.all_priorities) else stringResource(R.string.priority_at_least, it) },
                        onSelect = { minPriority = it },
                        selected = minPriority != 0,
                    )
                }
                val sorts = sortOptions(kind)
                if (sorts.size > 1) item {
                    DropdownChip(
                        label = stringResource(R.string.sort_by, stringResource(sort.label)),
                        options = sorts, optionText = { stringResource(it.label) }, onSelect = { sort = it },
                        selected = false,
                    )
                }
            }

            if (!isWorksite && mainList.isEmpty() && historyList.isEmpty()) {
                EmptyState(stringResource(R.string.empty_list))
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (isWorksite && (period == PeriodFilter.FUTURE_YEARS || period == PeriodFilter.OVERDUE)) {
                    // Années à venir / en retard : liste groupée par année, sans la vue de quota d'une année précise.
                    if (futureGroups.isEmpty()) item(key = "groups-empty") { EmptyState(stringResource(R.string.empty_list)) }
                    futureGroups.forEach { (y, list) ->
                        item(key = "g-title-$y") { SectionTitle(stringResource(R.string.year_title, y), list.size) }
                        items(list, key = { "g-" + it.id }) { task ->
                            TaskRow(vm, task, kind, type, roomNames, today, eventsByTask, onOpenTask) { pending = vm.beginComplete(task) }
                        }
                    }
                } else if (isWorksite) {
                    item(key = "year-card") {
                        YearQuotaCard(
                            summary = summary, accent = type.color.toColor(),
                            onPrevious = { year-- }, onNext = { year++ }, onEditQuota = { editingQuota = true },
                        )
                    }
                    item(key = "year-title") { SectionTitle(stringResource(R.string.year_attached, year), attachedList.size) }
                    if (attachedList.isEmpty()) item(key = "year-empty") { EmptyState(stringResource(R.string.year_attached_empty)) }
                    items(attachedList, key = { "y-" + it.id }) { task ->
                        TaskRow(vm, task, kind, type, roomNames, today, eventsByTask, onOpenTask) { pending = vm.beginComplete(task) }
                    }
                    item(key = "pending-title") { SectionTitle(stringResource(R.string.year_pending), pendingList.size) }
                    item(key = "pending-help") {
                        Text(
                            if (roomYears.isEmpty()) stringResource(R.string.year_pending_help_none)
                            else stringResource(
                                R.string.year_pending_help,
                                roomYears.joinToString(" · ") { (y, free) -> context.getString(R.string.year_room_item, y, free) },
                            ),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    items(pendingList, key = { "p-" + it.id }) { task ->
                        TaskRow(vm, task, kind, type, roomNames, today, eventsByTask, onOpenTask) { pending = vm.beginComplete(task) }
                    }
                }
                items(if (isWorksite) emptyList() else mainList, key = { it.id }) { task ->
                    TaskRow(vm, task, kind, type, roomNames, today, eventsByTask, onOpenTask) { pending = vm.beginComplete(task) }
                }
                if (!isWorksite && historyList.isNotEmpty()) {
                    item(key = "history-header") {
                        Row(
                            Modifier.fillMaxWidth().clickable { showHistory = !showHistory }.padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                stringResource(R.string.history_collapsible, historyList.size),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                            )
                            Icon(if (showHistory) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null)
                        }
                    }
                    if (showHistory) {
                        items(historyList, key = { "h-" + it.id }) { task ->
                            TaskRow(vm, task, kind, type, roomNames, today, eventsByTask, onOpenTask, onComplete = null)
                        }
                    }
                }
            }
        }
    }

    if (editingQuota) {
        YearQuotaDialog(
            year = year, current = yearQuota, onDismiss = { editingQuota = false },
            onSave = { points, asDefault -> vm.setYearQuota(year, points, asDefault) },
        )
    }

    CompletionDialogs(
        pending = pending, today = today, onDismiss = { pending = null },
        onFinish = { vm.complete(it.id) },
        onSession = { t, d, m, n -> vm.addSession(t.id, d, m, n) },
    )
}

@Composable
private fun Column2(padding: PaddingValues, content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.Column(Modifier.padding(padding).fillMaxSize()) { content() }
}

@Composable
private fun TaskRow(
    vm: AppViewModel,
    task: Task,
    kind: TaskKind,
    type: com.nyl.mytasks.domain.TaskType,
    roomNames: Map<String, String>,
    today: LocalDate,
    eventsByTask: Map<String, List<com.nyl.mytasks.domain.TaskEvent>>,
    onOpenTask: (String) -> Unit,
    onComplete: (() -> Unit)?,
) {
    val project = if (kind == TaskKind.PROJECT) ProjectLogic.week(task, eventsByTask[task.id].orEmpty(), today) else null
    TaskCard(
        task = task, kind = kind, type = type, roomName = task.roomId?.let(roomNames::get), today = today,
        project = project, onClick = { onOpenTask(task.id) }, onComplete = onComplete,
    )
}

private fun matchesPeriod(t: Task, p: PeriodFilter, today: LocalDate): Boolean {
    if (p == PeriodFilter.ALL) return true
    val due = t.dueDate ?: return false
    return when (p) {
        PeriodFilter.ALL -> true
        PeriodFilter.OVERDUE -> t.status.isActive && due.isBefore(today)
        PeriodFilter.TODAY -> due == today
        PeriodFilter.WEEK -> !due.isBefore(today) && !due.isAfter(today.weekEnd())
        PeriodFilter.MONTH -> due.year == today.year && due.month == today.month
        PeriodFilter.THIS_YEAR -> due.year == today.year
        PeriodFilter.FUTURE_YEARS -> due.year > today.year
    }
}

private fun comparatorFor(
    sort: SortMode,
    roomNames: Map<String, String>,
    projectRemaining: (Task) -> Int,
): Comparator<Task> {
    val byDue = compareBy<Task, LocalDate?>(nullsLast()) { it.dueDate }
    val byTitle = compareBy<Task> { it.title.lowercase() }
    return when (sort) {
        SortMode.BY_DUE -> byDue.then(byTitle)
        SortMode.BY_ROOM -> compareBy<Task, String?>(nullsLast()) { it.roomId?.let(roomNames::get)?.lowercase() }.then(byDue).then(byTitle)
        SortMode.BY_PRIORITY -> compareByDescending<Task> { it.priority ?: 0 }.then(byDue).then(byTitle)
        SortMode.BY_YEAR -> byDue.then(compareByDescending { it.priority ?: 0 }).then(byTitle)
        SortMode.BY_GOAL -> compareByDescending<Task> { projectRemaining(it) }.then(byTitle)
    }
}
