package com.nyl.mytasks.domain

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject

/** Instantané complet des données, exporté/importé en JSON (sauvegarde manuelle). */
@Serializable
data class BackupData(
    val version: Int = CURRENT_VERSION,
    val exportedAt: String = "",
    val types: List<TaskType>,
    val rooms: List<HouseRoom>,
    val tasks: List<Task>,
    val events: List<TaskEvent>,
    /** Quotas annuels de points d'effort (absent des sauvegardes de la version 1). */
    val yearQuotas: List<YearQuota> = emptyList(),
) {
    companion object {
        /** 1 : version initiale · 2 : quotas annuels · 3 : objectifs de projet mensuels (et non plus hebdomadaires). */
        const val CURRENT_VERSION = 3
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
        val root = json.parseToJsonElement(text).jsonObject
        val version = (root["version"] as? JsonPrimitive)?.intOrNull ?: 1
        require(version <= BackupData.CURRENT_VERSION) { "Version de sauvegarde non supportée : $version" }
        val migrated = if (version < 3) migrateWeeklyGoals(root) else root
        return json.decodeFromJsonElement(BackupData.serializer(), migrated)
    }

    /** Anciennes sauvegardes : un objectif hebdomadaire devient un objectif mensuel (× 4 : 1 h 30 / semaine → 6 h / mois). */
    private fun migrateWeeklyGoals(root: JsonObject): JsonObject {
        val tasks = root["tasks"] as? JsonArray ?: return root
        val fixed = JsonArray(
            tasks.map { element ->
                val task = element as? JsonObject ?: return@map element
                val weekly = (task["weeklyGoalMinutes"] as? JsonPrimitive)?.intOrNull
                if (weekly == null) task
                else JsonObject(task.filterKeys { it != "weeklyGoalMinutes" } + ("monthlyGoalMinutes" to JsonPrimitive(weekly * 4)))
            },
        )
        return JsonObject(root + ("tasks" to fixed))
    }

    /** Encodage compact d'une récurrence pour le stockage en base. */
    fun encodeRecurrence(r: Recurrence?): String? =
        r?.let { json.encodeToString(Recurrence.serializer(), it) }

    fun decodeRecurrence(text: String?): Recurrence? =
        text?.let { runCatching { json.decodeFromString(Recurrence.serializer(), it) }.getOrNull() }
}
