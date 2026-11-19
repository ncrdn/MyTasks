package com.nyl.mytasks.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nyl.mytasks.R
import com.nyl.mytasks.domain.DashItem
import com.nyl.mytasks.domain.DashboardState
import com.nyl.mytasks.domain.DeadlineLevel
import com.nyl.mytasks.domain.TaskKind
import com.nyl.mytasks.domain.TaskType
import com.nyl.mytasks.ui.AppViewModel
import com.nyl.mytasks.ui.common.CompletionDialogs
import com.nyl.mytasks.ui.common.EmptyState
import com.nyl.mytasks.ui.common.OverduePill
import com.nyl.mytasks.ui.common.PendingCompletion
import com.nyl.mytasks.ui.common.RoundAction
import com.nyl.mytasks.ui.common.ScreenHeader
import com.nyl.mytasks.ui.theme.Palette
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.nyl.mytasks.ui.common.SectionTitle
import com.nyl.mytasks.ui.common.TaskCard
import com.nyl.mytasks.ui.common.formatLong
import com.nyl.mytasks.ui.common.formatMinutes
import com.nyl.mytasks.ui.common.toColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    vm: AppViewModel,
    onOpenTask: (String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val state by vm.dashboard.collectAsStateWithLifecycle()
    val types by vm.types.collectAsStateWithLifecycle()
    val rooms by vm.rooms.collectAsStateWithLifecycle()
    val today by vm.today.collectAsStateWithLifecycle()
    var pending by remember { mutableStateOf<PendingCompletion?>(null) }
    val typeById = remember(types) { types.associateBy { it.id } }
    val roomNames = remember(rooms) { rooms.associate { it.id to it.name } }

    Scaffold(
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            ScreenHeader(
                title = stringResource(R.string.tab_dashboard),
                subtitle = today.formatLong(),
                actions = { RoundAction(Icons.Filled.Settings, stringResource(R.string.settings), onOpenSettings) },
            )
        },
    ) { padding ->
        val s = state
        if (s == null) {
            EmptyState(stringResource(R.string.loading), Modifier.padding(padding))
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // --- Statistiques (carte principale en tête) ---
            item { Stats(s) }

            // --- Aujourd'hui ---
            item { SectionTitle(stringResource(R.string.dash_today), s.todayItems.size) }
            if (s.todayItems.isEmpty()) item { EmptyState(stringResource(R.string.dash_today_empty)) }
            items(s.todayItems, key = { "today-" + it.task.id }) { item ->
                DashRow(item, typeById, roomNames, s, onOpenTask) { pending = vm.beginComplete(item.task) }
            }

            // --- Retards ---
            item { SectionTitle(stringResource(R.string.dash_overdue), s.overdueCount) }
            if (s.overdueItems.isEmpty()) item { EmptyState(stringResource(R.string.dash_overdue_empty)) }
            items(s.overdueItems, key = { "late-" + it.task.id }) { item ->
                DashRow(item, typeById, roomNames, s, onOpenTask) { pending = vm.beginComplete(item.task) }
            }

            // --- Avancement de la semaine ---
            item { SectionTitle(stringResource(R.string.dash_week_progress)) }
            item { WeekProgress(s, types, onOpenTask) }
        }
    }

    CompletionDialogs(
        pending = pending, today = today, onDismiss = { pending = null },
        onFinish = { vm.complete(it.id) },
        onSession = { t, d, m, n -> vm.addSession(t.id, d, m, n) },
    )
}

@Composable
private fun DashRow(
    item: DashItem,
    typeById: Map<String, TaskType>,
    roomNames: Map<String, String>,
    s: DashboardState,
    onOpenTask: (String) -> Unit,
    onComplete: () -> Unit,
) {
    TaskCard(
        task = item.task, kind = item.kind, type = typeById[item.task.typeId],
        roomName = item.task.roomId?.let(roomNames::get), today = s.today, project = item.project,
        onClick = { onOpenTask(item.task.id) }, onComplete = onComplete,
        daysLateOverride = item.daysLate.takeIf { it > 0 }, showType = true,
    )
}

@Composable
private fun WeekProgress(s: DashboardState, types: List<TaskType>, onOpenTask: (String) -> Unit) {
    fun label(kind: TaskKind) = types.firstOrNull { it.kind == kind }?.label.orEmpty()
    fun color(kind: TaskKind) = (types.firstOrNull { it.kind == kind }?.color ?: 0xFF607D8B).toColor()

    Card(
        Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            // Récurrentes : X occurrences faites / Y prévues
            Column {
                Text(label(TaskKind.DOMESTIC), style = MaterialTheme.typography.titleSmall, color = color(TaskKind.DOMESTIC))
                Text(stringResource(R.string.dash_domestic_progress, s.domesticDone, s.domesticPlanned))
                LinearProgressIndicator(
                    progress = { if (s.domesticPlanned == 0) 1f else s.domesticDone.toFloat() / s.domesticPlanned },
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    color = color(TaskKind.DOMESTIC),
                )
            }

            // Projets : temps réalisé / objectif hebdo
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(label(TaskKind.PROJECT), style = MaterialTheme.typography.titleSmall, color = color(TaskKind.PROJECT))
                if (s.projects.isEmpty()) Text(stringResource(R.string.dash_no_project), color = MaterialTheme.colorScheme.onSurfaceVariant)
                s.projects.forEach { p ->
                    val w = p.project ?: return@forEach
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(p.task.title, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                            if (w.isLate) OverduePill(stringResource(R.string.late_label))
                        }
                        LinearProgressIndicator(
                            progress = { w.progress }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            color = color(TaskKind.PROJECT),
                        )
                        Text(
                            buildString {
                                append(stringResource(R.string.progress_minutes, formatMinutes(w.doneThisWeek), formatMinutes(w.target)))
                                if (w.carryOver > 0) append(" · ").append(stringResource(R.string.catch_up, formatMinutes(w.carryOver)))
                            },
                            style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Chantiers : sessions de la semaine, échéances annuelles qui approchent
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(label(TaskKind.WORKSITE), style = MaterialTheme.typography.titleSmall, color = color(TaskKind.WORKSITE))
                Text(pluralStringResource(R.plurals.dash_sessions, s.worksiteSessionsThisWeek, s.worksiteSessionsThisWeek))
                if (s.worksiteDeadlines.isEmpty()) {
                    Text(stringResource(R.string.dash_no_deadline), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                s.worksiteDeadlines.forEach { d ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.dash_deadline_line, d.task.title, d.task.dueDate?.year ?: 0),
                            Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium,
                        )
                        OverduePill(
                            stringResource(
                                if (d.level == DeadlineLevel.UNDER_3_MONTHS) R.string.deadline_under_3 else R.string.deadline_under_6,
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Stats(s: DashboardState) {
    // Carte principale : indigo plein, texte blanc (contraste conservé en mode sombre via les couleurs fixes).
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Palette.Indigo, contentColor = Color.White),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(stringResource(R.string.dash_stats), style = MaterialTheme.typography.labelLarge, color = Palette.OnIndigoMuted)
            Row(Modifier.padding(top = 12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatBlock(s.rate7?.let { "$it %" } ?: "—", stringResource(R.string.stat_7_days), Modifier.weight(1f))
                StatBlock(s.rate30?.let { "$it %" } ?: "—", stringResource(R.string.stat_30_days), Modifier.weight(1f))
                StatBlock("${s.streak}", pluralStringResource(R.plurals.stat_streak, s.streak), Modifier.weight(1.1f), highlighted = true)
            }
        }
    }
}

@Composable
private fun StatBlock(value: String, label: String, modifier: Modifier = Modifier, highlighted: Boolean = false) {
    Column(
        modifier.then(
            if (highlighted) Modifier.clip(RoundedCornerShape(18.dp)).background(Color.White.copy(alpha = 0.16f)).padding(horizontal = 12.dp, vertical = 8.dp)
            else Modifier.padding(vertical = 8.dp),
        ),
    ) {
        Text(value, style = MaterialTheme.typography.headlineMedium, color = Color.White, maxLines = 1)
        Text(label, style = MaterialTheme.typography.labelMedium, color = Palette.OnIndigoMuted, maxLines = 2)
    }
}
