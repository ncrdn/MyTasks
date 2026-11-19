package com.nyl.mytasks.ui.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nyl.mytasks.R
import com.nyl.mytasks.domain.YearSummary
import com.nyl.mytasks.ui.common.NumberField

/** Vue de l'année : quota total, points engagés, points restants, avec navigation d'une année à l'autre. */
@Composable
fun YearQuotaCard(
    summary: YearSummary,
    accent: Color,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onEditQuota: () -> Unit,
) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPrevious) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.year_previous))
                }
                Text(
                    stringResource(R.string.year_title, summary.year),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onEditQuota) {
                    Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.year_edit_quota))
                }
                IconButton(onClick = onNext) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.year_next))
                }
            }
            Column(Modifier.padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Figure(stringResource(R.string.year_quota), "${summary.quota}", Modifier.weight(1f))
                    Figure(
                        stringResource(R.string.year_committed), "${summary.committed}", Modifier.weight(1f),
                        valueColor = if (summary.isOver) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    )
                    Figure(stringResource(R.string.year_remaining), "${summary.remaining}", Modifier.weight(1f), valueColor = accent)
                }
                LinearProgressIndicator(
                    progress = { summary.progress },
                    modifier = Modifier.fillMaxWidth().height(10.dp),
                    color = if (summary.isOver) MaterialTheme.colorScheme.error else accent,
                    trackColor = accent.copy(alpha = 0.16f),
                    gapSize = 0.dp,
                    drawStopIndicator = {},
                )
                if (summary.isOver) {
                    Text(
                        stringResource(R.string.year_over_quota), style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun Figure(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Column(modifier) {
        Text(value, style = MaterialTheme.typography.headlineMedium, color = valueColor)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Saisie du quota d'une année, avec option « valeur par défaut » pour les années sans quota propre. */
@Composable
fun YearQuotaDialog(year: Int, current: Int, onDismiss: () -> Unit, onSave: (points: Int, asDefault: Boolean) -> Unit) {
    var text by remember(year) { mutableStateOf(current.toString()) }
    var asDefault by remember(year) { mutableStateOf(false) }
    val value = text.toIntOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.year_quota_dialog_title, year)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                NumberField(
                    label = stringResource(R.string.year_quota), value = text, onChange = { text = it },
                    suffix = stringResource(R.string.points_unit), modifier = Modifier.fillMaxWidth(),
                    isError = value == null,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = asDefault, onCheckedChange = { asDefault = it })
                    Text(stringResource(R.string.year_quota_as_default), Modifier.padding(start = 4.dp))
                }
            }
        },
        confirmButton = {
            TextButton(enabled = value != null, onClick = { onSave(value ?: 0, asDefault); onDismiss() }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
