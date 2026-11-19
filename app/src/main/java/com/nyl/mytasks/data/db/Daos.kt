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

@Database(
    entities = [TaskTypeEntity::class, RoomEntity::class, TaskEntity::class, EventEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun eventDao(): EventDao
    abstract fun typeDao(): TypeDao
    abstract fun roomDao(): RoomDao
}
