package com.nyl.mytasks.data.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks")
    fun observeAll(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks")
    suspend fun getAll(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: String): TaskEntity?

    @Upsert
    suspend fun upsert(task: TaskEntity)

    @Upsert
    suspend fun upsertAll(tasks: List<TaskEntity>)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM tasks")
    suspend fun deleteAll()

    @Query("UPDATE tasks SET roomId = NULL WHERE roomId = :roomId")
    suspend fun clearRoom(roomId: String)
}

@Dao
interface EventDao {
    @Query("SELECT * FROM task_events ORDER BY date DESC, timestamp DESC")
    fun observeAll(): Flow<List<EventEntity>>

    @Query("SELECT * FROM task_events")
    suspend fun getAll(): List<EventEntity>

    @Query("SELECT * FROM task_events WHERE taskId = :taskId")
    suspend fun getForTask(taskId: String): List<EventEntity>

    @Query("SELECT * FROM task_events WHERE id = :id")
    suspend fun getById(id: String): EventEntity?

    @Upsert
    suspend fun upsert(event: EventEntity)

    @Upsert
    suspend fun upsertAll(events: List<EventEntity>)

    @Query("DELETE FROM task_events WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM task_events WHERE taskId = :taskId")
    suspend fun deleteForTask(taskId: String)

    @Query("DELETE FROM task_events")
    suspend fun deleteAll()
}

@Dao
interface TypeDao {
    @Query("SELECT * FROM task_types ORDER BY sortOrder")
    fun observeAll(): Flow<List<TaskTypeEntity>>

    @Query("SELECT * FROM task_types ORDER BY sortOrder")
    suspend fun getAll(): List<TaskTypeEntity>

    @Query("SELECT COUNT(*) FROM task_types")
    suspend fun count(): Int

    @Upsert
    suspend fun upsert(type: TaskTypeEntity)

    @Upsert
    suspend fun upsertAll(types: List<TaskTypeEntity>)

    @Query("DELETE FROM task_types")
    suspend fun deleteAll()
}

@Dao
interface RoomDao {
    @Query("SELECT * FROM rooms ORDER BY sortOrder, name")
    fun observeAll(): Flow<List<RoomEntity>>

    @Query("SELECT * FROM rooms ORDER BY sortOrder, name")
    suspend fun getAll(): List<RoomEntity>

    @Upsert
    suspend fun upsert(room: RoomEntity)

    @Upsert
    suspend fun upsertAll(rooms: List<RoomEntity>)

    @Query("DELETE FROM rooms WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM rooms")
    suspend fun deleteAll()
}

@Dao
interface YearQuotaDao {
    @Query("SELECT * FROM year_quotas ORDER BY year")
    fun observeAll(): Flow<List<YearQuotaEntity>>

    @Query("SELECT * FROM year_quotas ORDER BY year")
    suspend fun getAll(): List<YearQuotaEntity>

    @Upsert
    suspend fun upsert(quota: YearQuotaEntity)

    @Upsert
    suspend fun upsertAll(quotas: List<YearQuotaEntity>)

    @Query("DELETE FROM year_quotas")
    suspend fun deleteAll()
}

@Database(
    entities = [TaskTypeEntity::class, RoomEntity::class, TaskEntity::class, EventEntity::class, YearQuotaEntity::class],
    version = 3,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun eventDao(): EventDao
    abstract fun typeDao(): TypeDao
    abstract fun roomDao(): RoomDao
    abstract fun yearQuotaDao(): YearQuotaDao
}

/**
 * v2 → v3 : l'objectif d'un projet devient mensuel. La colonne `weeklyGoalMinutes` est remplacée par
 * `monthlyGoalMinutes` (table recréée : `RENAME COLUMN` n'existe pas avant SQLite 3.25) ; les objectifs existants
 * sont multipliés par 4 (1 h 30 par semaine → 6 h par mois).
 */
val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `tasks_new` (`id` TEXT NOT NULL, `typeId` TEXT NOT NULL, `title` TEXT NOT NULL, " +
                "`notes` TEXT NOT NULL, `status` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `closedAt` INTEGER, " +
                "`roomId` TEXT, `estimatedMinutes` INTEGER, `dueDate` INTEGER, `place` TEXT, `effort` INTEGER, " +
                "`priority` INTEGER, `recurrenceJson` TEXT, `anchorOnTheoretical` INTEGER NOT NULL, " +
                "`paused` INTEGER NOT NULL, `monthlyGoalMinutes` INTEGER, PRIMARY KEY(`id`))",
        )
        db.execSQL(
            "INSERT INTO `tasks_new` SELECT `id`, `typeId`, `title`, `notes`, `status`, `createdAt`, `closedAt`, `roomId`, " +
                "`estimatedMinutes`, `dueDate`, `place`, `effort`, `priority`, `recurrenceJson`, `anchorOnTheoretical`, " +
                "`paused`, `weeklyGoalMinutes` * 4 FROM `tasks`",
        )
        db.execSQL("DROP TABLE `tasks`")
        db.execSQL("ALTER TABLE `tasks_new` RENAME TO `tasks`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_typeId` ON `tasks` (`typeId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_dueDate` ON `tasks` (`dueDate`)")
    }
}

/** v1 → v2 : quotas annuels de points d'effort. */
val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `year_quotas` (`year` INTEGER NOT NULL, `points` INTEGER NOT NULL, PRIMARY KEY(`year`))")
    }
}
