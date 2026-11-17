package com.nyl.mytasks.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate
import java.time.ZoneId

/** Source unique de « la date du jour », observable pour que l'UI se mette à jour au changement de jour. */
class AppClock(private val zone: ZoneId = ZoneId.systemDefault()) {
    private val _today = MutableStateFlow(LocalDate.now(zone))
    val today: StateFlow<LocalDate> = _today

    fun now(): LocalDate = LocalDate.now(zone).also { _today.value = it }
}
