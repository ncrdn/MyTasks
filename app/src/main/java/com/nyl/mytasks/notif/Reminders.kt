package com.nyl.mytasks.notif

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.nyl.mytasks.R
import com.nyl.mytasks.container
import com.nyl.mytasks.data.AppSettings
import com.nyl.mytasks.domain.DashboardCalculator
import com.nyl.mytasks.domain.EventKind
import com.nyl.mytasks.domain.TaskKind
import com.nyl.mytasks.ui.common.formatMinutes
import kotlinx.coroutines.flow.first
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

/** Planifie les rappels locaux (aucun serveur) : une chaîne de travaux uniques par type de rappel. */
object ReminderScheduler {
    const val KIND_DAILY = "daily"
    const val KIND_MONTHLY = "monthly"
    private const val LEGACY_WEEKLY = "weekly"
    private const val KEY_KIND = "kind"

    fun schedule(context: Context, settings: AppSettings, replace: Boolean) {
        val wm = WorkManager.getInstance(context)
        wm.cancelUniqueWork(LEGACY_WEEKLY) // ancienne alerte hebdomadaire des projets
        if (!settings.notificationsEnabled) {
            wm.cancelUniqueWork(KIND_DAILY)
            wm.cancelUniqueWork(KIND_MONTHLY)
            return
        }
        enqueue(wm, KIND_DAILY, delayUntilDaily(settings, LocalDateTime.now()), replace)
        if (settings.weeklyEnabled) {
            enqueue(wm, KIND_MONTHLY, delayUntilMonthly(settings, LocalDateTime.now()), replace)
        } else {
            wm.cancelUniqueWork(KIND_MONTHLY)
        }
    }

    /** Appelé par le travail lui-même pour programmer sa prochaine exécution. */
    fun scheduleNext(context: Context, settings: AppSettings, kind: String) {
        if (!settings.notificationsEnabled) return
        val now = LocalDateTime.now()
        when (kind) {
            KIND_DAILY -> enqueueAfterSelf(context, KIND_DAILY, delayUntilDaily(settings, now))
            KIND_MONTHLY -> if (settings.weeklyEnabled) {
                enqueueAfterSelf(context, KIND_MONTHLY, delayUntilMonthly(settings, now))
            }
        }
    }

    private fun enqueueAfterSelf(context: Context, kind: String, delay: Duration) =
        enqueue(WorkManager.getInstance(context), kind, delay, replace = false, append = true)

    private fun enqueue(wm: WorkManager, kind: String, delay: Duration, replace: Boolean, append: Boolean = false) {
        val req = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(KEY_KIND to kind))
            .build()
        val policy = when {
            append -> ExistingWorkPolicy.APPEND_OR_REPLACE
            replace -> ExistingWorkPolicy.REPLACE
            else -> ExistingWorkPolicy.KEEP
        }
        wm.enqueueUniqueWork(kind, policy, req)
    }

    internal fun delayUntilDaily(s: AppSettings, now: LocalDateTime): Duration {
        var target = now.toLocalDate().atTime(LocalTime.of(s.dailyHour, s.dailyMinute))
        if (!target.isAfter(now.plusMinutes(1))) target = target.plusDays(1)
        return Duration.between(now, target)
    }

    /** Alerte de fin de mois (objectifs mensuels des projets) : l'avant-avant-dernier jour du mois, à l'heure choisie. */
    internal fun delayUntilMonthly(s: AppSettings, now: LocalDateTime): Duration {
        val time = LocalTime.of(s.weeklyHour, s.weeklyMinute)
        var month = java.time.YearMonth.from(now)
        var target = month.atEndOfMonth().minusDays(2).atTime(time)
        while (!target.isAfter(now.plusMinutes(1))) {
            month = month.plusMonths(1)
            target = month.atEndOfMonth().minusDays(2).atTime(time)
        }
        return Duration.between(now, target)
    }
}

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val kind = inputData.getString("kind") ?: ReminderScheduler.KIND_DAILY
        val settings = applicationContext.container.settings.flow.first()
        if (settings.notificationsEnabled) {
            runCatching {
                if (kind == ReminderScheduler.KIND_MONTHLY) monthlyAlert() else dailyReminders(settings)
            }
        }
        ReminderScheduler.scheduleNext(applicationContext, settings, kind)
        return Result.success()
    }

    private suspend fun dailyReminders(settings: AppSettings) {
        val ctx = applicationContext
        val repo = ctx.container.repository
        val today = ctx.container.clock.now()
        val tasks = repo.tasksFlow.first()
        val events = repo.eventsFlow.first()
        val types = repo.typesFlow.first().associateBy { it.id }
        val state = DashboardCalculator.compute(tasks, events, { types[it]?.kind }, today)

        // Rappel du jour : tâches par type (avec les intitulés personnalisés) + retards.
        val lines = ArrayList<String>()
        state.todayItems.groupBy { it.task.typeId }.forEach { (typeId, items) ->
            lines += ctx.getString(R.string.notif_line_type, types[typeId]?.label.orEmpty(), items.size)
        }
        if (state.overdueCount > 0) lines += ctx.getString(R.string.notif_line_overdue, state.overdueCount)
        if (lines.isNotEmpty()) {
            Notifier.show(ctx, Notifier.ID_DAILY, ctx.getString(R.string.notif_daily_title, state.todayItems.size), lines)
        }

        // Veille de l'échéance des tâches ponctuelles (J-1).
        if (settings.dueEveEnabled) {
            val tomorrow = today.plusDays(1)
            val eve = tasks.filter {
                types[it.typeId]?.kind == TaskKind.ONE_OFF && it.status.isActive && it.dueDate == tomorrow
            }
            if (eve.isNotEmpty()) {
                Notifier.show(ctx, Notifier.ID_EVE, ctx.getString(R.string.notif_eve_title), eve.map { it.title })
            }
        }

        // Chantiers dont l'année visée approche sans session récente (contrôle le lundi).
        if (today.dayOfWeek == DayOfWeek.MONDAY) {
            val lastSession = events.filter { it.kind == EventKind.SESSION }
                .groupBy { it.taskId }.mapValues { (_, l) -> l.maxOf { it.date } }
            val stale = state.worksiteDeadlines.filter { d ->
                val last = lastSession[d.task.id] ?: d.task.createdAt
                ChronoUnit.DAYS.between(last, today) >= 30
            }
            if (stale.isNotEmpty()) {
                Notifier.show(
                    ctx, Notifier.ID_WORKSITE, ctx.getString(R.string.notif_worksite_title),
                    stale.map { ctx.getString(R.string.notif_worksite_line, it.task.title, it.daysLeft) },
                )
            }
        }
    }

    private suspend fun monthlyAlert() {
        val ctx = applicationContext
        val repo = ctx.container.repository
        val today: LocalDate = ctx.container.clock.now()
        val types = repo.typesFlow.first().associateBy { it.id }
        val state = DashboardCalculator.compute(
            repo.tasksFlow.first(), repo.eventsFlow.first(), { types[it]?.kind }, today,
        )
        val late = state.projects.filter { (it.project?.remaining ?: 0) > 0 }
        if (late.isNotEmpty()) {
            Notifier.show(
                ctx, Notifier.ID_WEEKLY, ctx.getString(R.string.notif_weekly_title),
                late.map { ctx.getString(R.string.notif_weekly_line, it.task.title, formatMinutes(it.project!!.remaining)) },
            )
        }
    }
}
