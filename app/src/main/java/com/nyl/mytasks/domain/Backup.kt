package com.nyl.mytasks.domain

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Instantané complet des données, exporté/importé en JSON (sauvegarde manuelle). */
@Serializable
data class BackupData(
    val version: Int = CURRENT_VERSION,
    val exportedAt: String = "",
    val types: List<TaskType>,
    val rooms: List<HouseRoom>,
    val tasks: List<Task>,
    val events: List<TaskEvent>,
) {
    companion object {
        const val CURRENT_VERSION = 1
    }
}

object BackupCodec {
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    fun encode(data: BackupData): String = json.encodeToString(BackupData.serializer(), data)

    fun decode(text: String): BackupData {
        val data = json.decodeFromString(BackupData.serializer(), text)
        require(data.version <= BackupData.CURRENT_VERSION) { "Version de sauvegarde non supportée : ${data.version}" }
        return data
    }

    /** Encodage compact d'une récurrence pour le stockage en base. */
    fun encodeRecurrence(r: Recurrence?): String? =
        r?.let { json.encodeToString(Recurrence.serializer(), it) }

    fun decodeRecurrence(text: String?): Recurrence? =
        text?.let { runCatching { json.decodeFromString(Recurrence.serializer(), it) }.getOrNull() }
}
