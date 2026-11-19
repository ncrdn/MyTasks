package com.nyl.mytasks

import android.app.Application
import androidx.room.Room
import com.nyl.mytasks.data.AppClock
import com.nyl.mytasks.data.SettingsRepository
import com.nyl.mytasks.data.TaskRepository
import com.nyl.mytasks.data.db.AppDatabase
import com.nyl.mytasks.domain.HouseRoom
import com.nyl.mytasks.domain.TaskKind
import com.nyl.mytasks.domain.TaskType
import com.nyl.mytasks.notif.ReminderScheduler
import com.nyl.mytasks.notif.Notifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Conteneur de dépendances (injection manuelle). */
class AppContainer(private val app: Application) {
    val clock = AppClock()
    private val db: AppDatabase = Room.databaseBuilder(app, AppDatabase::class.java, "mytasks.db").build()
    val repository = TaskRepository(db, clock)
    val settings = SettingsRepository(app)

    /** Intitulés par défaut renommés dans une version ultérieure (données déjà enregistrées ou importées). */
    suspend fun upgradeLegacyLabels() {
        repository.renameTypeIfUnchanged("domestic", "Tâches domestiques", app.getString(R.string.type_domestic))
    }
}

class MyTasksApp : Application() {
    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Notifier.createChannel(this)
        appScope.launch {
            container.repository.seedIfEmpty(defaultTypes(), defaultRooms())
            container.upgradeLegacyLabels()
            ReminderScheduler.schedule(this@MyTasksApp, container.settings.flow.first(), replace = false)
        }
    }

    /** Catalogue initial : les intitulés viennent des ressources (modifiables ensuite par l'utilisateur). */
    private fun defaultTypes() = listOf(
        TaskType("domestic", TaskKind.DOMESTIC, getString(R.string.type_domestic), "cleaning", 0xFF0F9D8A, 0),
        TaskType("oneoff", TaskKind.ONE_OFF, getString(R.string.type_oneoff), "event", 0xFFE08A00, 1),
        TaskType("worksite", TaskKind.WORKSITE, getString(R.string.type_worksite), "construction", 0xFFD6409F, 2),
        TaskType("project", TaskKind.PROJECT, getString(R.string.type_project), "flag", 0xFF1D8FE1, 3),
    )

    private fun defaultRooms() = resources.getStringArray(R.array.default_rooms)
        .mapIndexed { i, name -> HouseRoom("room-$i", name, i) }
}

val android.content.Context.container: AppContainer
    get() = (applicationContext as MyTasksApp).container
