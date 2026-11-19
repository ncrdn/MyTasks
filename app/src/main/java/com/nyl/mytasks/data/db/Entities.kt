package com.nyl.mytasks.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.nyl.mytasks.domain.BackupCodec
import com.nyl.mytasks.domain.EventKind
import com.nyl.mytasks.domain.HouseRoom
import com.nyl.mytasks.domain.Task
import com.nyl.mytasks.domain.TaskEvent
import com.nyl.mytasks.domain.TaskKind
import com.nyl.mytasks.domain.TaskStatus
import com.nyl.mytasks.domain.TaskType
import com.nyl.mytasks.domain.YearQuota
import java.time.LocalDate

@Entity(tableName = "task_types")
data class TaskTypeEntity(
    @PrimaryKey val id: String,
    val kind: String,
    val label: String,
    val iconKey: String,
    val color: Long,
    val sortOrder: Int,
)

@Entity(tableName = "rooms")
data class RoomEntity(
    @PrimaryKey val id: String,
    val name: String,
    val sortOrder: Int,
)

@Entity(tableName = "tasks", indices = [Index("typeId"), Index("dueDate")])
data class TaskEntity(
    @PrimaryKey val id: String,
    val typeId: String,
    val title: String,
    val notes: String,
    val status: String,
    val createdAt: Long,
    val closedAt: Long?,
    val roomId: String?,
    val estimatedMinutes: Int?,
    val dueDate: Long?,
    val place: String?,
    val effort: Int?,
    val priority: Int?,
    val recurrenceJson: String?,
    val anchorOnTheoretical: Boolean,
    val paused: Boolean,
    val monthlyGoalMinutes: Int?,
)

@Entity(tableName = "task_events", indices = [Index("taskId")])
data class EventEntity(
    @PrimaryKey val id: String,
    val taskId: String,
    val kind: String,
    val date: Long,
    val minutes: Int?,
    val note: String,
    val timestamp: Long,
)

@Entity(tableName = "year_quotas")
data class YearQuotaEntity(
    @PrimaryKey val year: Int,
    val points: Int,
)

// --- Mappers entité <-> modèle de domaine ---

fun YearQuotaEntity.toDomain() = YearQuota(year, points)
fun YearQuota.toEntity() = YearQuotaEntity(year, points)

fun TaskTypeEntity.toDomain() = TaskType(id, TaskKind.valueOf(kind), label, iconKey, color, sortOrder)
fun TaskType.toEntity() = TaskTypeEntity(id, kind.name, label, iconKey, color, sortOrder)

fun RoomEntity.toDomain() = HouseRoom(id, name, sortOrder)
fun HouseRoom.toEntity() = RoomEntity(id, name, sortOrder)

fun TaskEntity.toDomain() = Task(
    id = id,
    typeId = typeId,
    title = title,
    notes = notes,
    status = TaskStatus.valueOf(status),
    createdAt = LocalDate.ofEpochDay(createdAt),
    closedAt = closedAt?.let(LocalDate::ofEpochDay),
    roomId = roomId,
    estimatedMinutes = estimatedMinutes,
    dueDate = dueDate?.let(LocalDate::ofEpochDay),
    place = place,
    effort = effort,
    priority = priority,
    recurrence = BackupCodec.decodeRecurrence(recurrenceJson),
    anchorOnTheoretical = anchorOnTheoretical,
    paused = paused,
    monthlyGoalMinutes = monthlyGoalMinutes,
)

fun Task.toEntity() = TaskEntity(
    id = id,
    typeId = typeId,
    title = title,
    notes = notes,
    status = status.name,
    createdAt = createdAt.toEpochDay(),
    closedAt = closedAt?.toEpochDay(),
    roomId = roomId,
    estimatedMinutes = estimatedMinutes,
    dueDate = dueDate?.toEpochDay(),
    place = place,
    effort = effort,
    priority = priority,
    recurrenceJson = BackupCodec.encodeRecurrence(recurrence),
    anchorOnTheoretical = anchorOnTheoretical,
    paused = paused,
    monthlyGoalMinutes = monthlyGoalMinutes,
)

fun EventEntity.toDomain() = TaskEvent(
    id, taskId, EventKind.valueOf(kind), LocalDate.ofEpochDay(date), minutes, note, timestamp,
)

fun TaskEvent.toEntity() = EventEntity(id, taskId, kind.name, date.toEpochDay(), minutes, note, timestamp)
