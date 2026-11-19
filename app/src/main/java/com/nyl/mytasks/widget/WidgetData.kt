package com.nyl.mytasks.widget

import android.content.Context
import com.nyl.mytasks.container
import com.nyl.mytasks.domain.DashboardCalculator
import com.nyl.mytasks.domain.QuotaLogic
import com.nyl.mytasks.ui.dashboard.ProgressTile
import com.nyl.mytasks.ui.dashboard.ProgressTiles
import kotlinx.coroutines.flow.first

/** Données affichées par le widget : les mêmes jauges que le bloc « Avancement » du tableau de bord. */
data class WidgetData(val tiles: List<ProgressTile>) {
    companion object {
        suspend fun load(context: Context): WidgetData {
            val app = context.container
            val repo = app.repository
            val today = app.clock.now()
            val types = repo.typesFlow.first()
            val kinds = types.associate { it.id to it.kind }
            val quota = QuotaLogic.quotaFor(today.year, repo.quotasFlow.first(), app.settings.flow.first().defaultYearQuota)
            val state = DashboardCalculator.compute(repo.tasksFlow.first(), repo.eventsFlow.first(), { kinds[it] }, today, quota)
            return WidgetData(ProgressTiles.build(context, state, types))
        }
    }
}
