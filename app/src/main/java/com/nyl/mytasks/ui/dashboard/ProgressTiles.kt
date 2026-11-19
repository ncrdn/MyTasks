package com.nyl.mytasks.ui.dashboard

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.nyl.mytasks.R
import com.nyl.mytasks.domain.DashboardState
import com.nyl.mytasks.domain.Progress
import com.nyl.mytasks.domain.TaskKind
import com.nyl.mytasks.domain.TaskType
import com.nyl.mytasks.ui.common.formatMinutes
import com.nyl.mytasks.ui.common.toColor

/** Une jauge du bloc « Avancement » : partagée par le tableau de bord et le widget d'écran d'accueil. */
data class ProgressTile(
    val kind: TaskKind,
    val label: String,
    /** « Cette semaine », « Ce mois-ci » ou « Année 2026 ». */
    val period: String,
    val color: Color,
    /** Taux d'accomplissement 0..1. */
    val ratio: Float,
    /** Rien de prévu sur la période : la jauge est vide et sans pourcentage. */
    val empty: Boolean,
    val count: String,
    /** Décompte des heures (temps consacré / estimé), quand il est connu. */
    val hours: String?,
)

object ProgressTiles {

    /**
     * Quatre jauges : tâches récurrentes et ponctuelles sur la semaine, projets sur le mois (en temps),
     * travaux sur l'année (points d'effort consommés / quota).
     */
    fun build(context: Context, s: DashboardState, types: List<TaskType>): List<ProgressTile> {
        fun typeOf(kind: TaskKind) = types.firstOrNull { it.kind == kind }
        fun label(kind: TaskKind) = typeOf(kind)?.label.orEmpty()
        fun color(kind: TaskKind) = (typeOf(kind)?.color ?: 0xFF607D8B).toColor()

        fun hoursOf(p: Progress): String? = when {
            p.plannedMinutes > 0 -> context.getString(R.string.progress_minutes, formatMinutes(p.doneMinutes), formatMinutes(p.plannedMinutes))
            p.doneMinutes > 0 -> context.getString(R.string.dash_time_spent, formatMinutes(p.doneMinutes))
            else -> null
        }

        val week = context.getString(R.string.this_week)
        val pm = s.projectsMonth
        val wy = s.worksitesYear
        return listOf(
            ProgressTile(
                TaskKind.DOMESTIC, label(TaskKind.DOMESTIC), week, color(TaskKind.DOMESTIC),
                s.recurring.ratio, s.recurring.planned == 0,
                context.getString(R.string.dash_count, s.recurring.done, s.recurring.planned), hoursOf(s.recurring),
            ),
            ProgressTile(
                TaskKind.ONE_OFF, label(TaskKind.ONE_OFF), week, color(TaskKind.ONE_OFF),
                s.oneOff.ratio, s.oneOff.planned == 0,
                context.getString(R.string.dash_count, s.oneOff.done, s.oneOff.planned), hoursOf(s.oneOff),
            ),
            ProgressTile(
                TaskKind.PROJECT, label(TaskKind.PROJECT), context.getString(R.string.this_month), color(TaskKind.PROJECT),
                if (pm.plannedMinutes <= 0) 0f else (pm.doneMinutes.toFloat() / pm.plannedMinutes).coerceIn(0f, 1f),
                pm.planned == 0,
                if (pm.planned == 0) context.getString(R.string.dash_no_project)
                else context.getString(R.string.progress_minutes, formatMinutes(pm.doneMinutes), formatMinutes(pm.plannedMinutes)),
                null,
            ),
            ProgressTile(
                TaskKind.WORKSITE, label(TaskKind.WORKSITE), context.getString(R.string.year_title, s.today.year), color(TaskKind.WORKSITE),
                wy.ratio, wy.planned == 0,
                context.getString(R.string.dash_points_count, wy.done, wy.planned),
                if (wy.doneMinutes > 0) context.getString(R.string.dash_time_spent, formatMinutes(wy.doneMinutes)) else null,
            ),
        )
    }
}
