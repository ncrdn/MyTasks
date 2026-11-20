package com.nyl.mytasks.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nyl.mytasks.R
import com.nyl.mytasks.domain.DashItem
import com.nyl.mytasks.domain.DashboardState
import com.nyl.mytasks.domain.DeadlineLevel
import com.nyl.mytasks.domain.Progress
import com.nyl.mytasks.domain.TaskKind
import com.nyl.mytasks.domain.TaskType
import com.nyl.mytasks.ui.AppViewModel
import com.nyl.mytasks.ui.common.CompletionDialogs
import com.nyl.mytasks.ui.common.EmptyState
import com.nyl.mytasks.ui.common.OverduePill
import com.nyl.mytasks.ui.common.PendingCompletion
import com.nyl.mytasks.ui.common.RoundAction
import com.nyl.mytasks.ui.common.ScreenHeader
import com.nyl.mytasks.ui.common.SectionTitle
import com.nyl.mytasks.ui.common.TaskCard
import com.nyl.mytasks.ui.common.formatLong
import com.nyl.mytasks.ui.common.formatMinutes
import com.nyl.mytasks.ui.common.toColor
import com.nyl.mytasks.ui.theme.Palette

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
            // --- Avancement : jauges par type, tout en haut ---
            item(key = "progress") { ProgressBlock(s, types) }

            // --- En retard ---
            item(key = "late-title") { SectionTitle(stringResource(R.string.dash_overdue), s.overdueCount) }
            if (s.overdueItems.isEmpty()) item(key = "late-empty") { EmptyState(stringResource(R.string.dash_overdue_empty)) }
            items(s.overdueItems, key = { "late-" + it.task.id }) { item ->
                DashRow(item, typeById, roomNames, s, onOpenTask) { pending = vm.beginComplete(item.task) }
            }

            // --- À faire aujourd'hui ---
            item(key = "today-title") { SectionTitle(stringResource(R.string.dash_today), s.todayItems.size) }
            if (s.todayItems.isEmpty()) item(key = "today-empty") { EmptyState(stringResource(R.string.dash_today_empty)) }
            items(s.todayItems, key = { "today-" + it.task.id }) { item ->
                DashRow(item, typeById, roomNames, s, onOpenTask) { pending = vm.beginComplete(item.task) }
            }

            // --- À faire cette semaine : récurrentes et ponctuelles ---
            item(key = "week-title") { SectionTitle(stringResource(R.string.dash_later_this_week), s.weekItems.size) }
            if (s.weekItems.isEmpty()) item(key = "week-empty") { EmptyState(stringResource(R.string.dash_week_empty)) }
            items(s.weekItems, key = { "week-" + it.task.id }) { item ->
                DashRow(item, typeById, roomNames, s, onOpenTask) { pending = vm.beginComplete(item.task) }
            }

            // --- Projets du mois : une tuile par projet actif, avec son avancement du mois ---
            if (s.projects.isNotEmpty()) {
                item(key = "projects-title") { SectionTitle(stringResource(R.string.dash_projects_month), s.projects.size) }
                items(s.projects, key = { "project-" + it.task.id }) { item ->
                    DashRow(item, typeById, roomNames, s, onOpenTask) { pending = vm.beginComplete(item.task) }
                }
            }

            // --- Travaux : récapitulatif seul (sessions de la semaine, échéances annuelles qui approchent) ---
            if (s.worksiteSessionsThisWeek > 0 || s.worksiteDeadlines.isNotEmpty()) {
                item(key = "worksites") { WorksiteCard(s, types.firstOrNull { it.kind == TaskKind.WORKSITE }) }
            }
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

// --- Bloc « Avancement » ---

/**
 * Quatre jauges d'accomplissement : tâches récurrentes et ponctuelles sur la semaine, projets sur le mois,
 * travaux sur l'année (points d'effort consommés / quota), avec le décompte des heures quand il est connu.
 */
@Composable
private fun ProgressBlock(s: DashboardState, types: List<TaskType>) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val tiles = remember(s, types) { ProgressTiles.build(context, s, types) }

    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.dash_progress), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 4.dp))
            tiles.chunked(2).forEach { pair ->
                Row(Modifier.height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    pair.forEach { tile -> GaugeTile(tile, Modifier.weight(1f).fillMaxHeight()) }
                }
            }
        }
    }
}

@Composable
private fun GaugeTile(tile: ProgressTile, modifier: Modifier = Modifier) {
    val color = tile.color
    val ink = lerp(color, MaterialTheme.colorScheme.onSurface, 0.45f)
    val ratio = tile.ratio
    val percent = (ratio * 100).toInt()
    val summary = buildString {
        append(tile.label).append(", ").append(tile.period).append(" : ").append(tile.count)
        tile.hours?.let { append(", ").append(it) }
    }
    Column(
        modifier.clip(RoundedCornerShape(22.dp)).background(color.copy(alpha = 0.10f)).padding(12.dp)
            .semantics(mergeDescendants = true) { contentDescription = summary },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            tile.label, style = MaterialTheme.typography.labelLarge, color = ink, maxLines = 2,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, textAlign = TextAlign.Center,
        )
        Text(tile.period, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        Box(Modifier.padding(vertical = 8.dp).size(84.dp), contentAlignment = Alignment.Center) {
            val track = color.copy(alpha = 0.2f)
            Canvas(Modifier.size(84.dp)) {
                val stroke = 10.dp.toPx()
                val inset = stroke / 2
                val arcSize = Size(size.width - stroke, size.height - stroke)
                drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
                if (!tile.empty && ratio > 0f) {
                    drawArc(color, -90f, 360f * ratio, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                }
            }
            Text(
                if (tile.empty) "—" else "$percent %",
                style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Text(tile.count, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center, maxLines = 2)
        if (tile.hours != null) {
            Text(
                tile.hours, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center, maxLines = 2,
            )
        }
    }
}

// --- Travaux : sessions et échéances ---

@Composable
private fun WorksiteCard(s: DashboardState, type: TaskType?) {
    Card(
        Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(type?.label.orEmpty(), style = MaterialTheme.typography.titleMedium, color = (type?.color ?: 0xFF607D8B).toColor())
            Text(pluralStringResource(R.plurals.dash_sessions, s.worksiteSessionsThisWeek, s.worksiteSessionsThisWeek))
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
