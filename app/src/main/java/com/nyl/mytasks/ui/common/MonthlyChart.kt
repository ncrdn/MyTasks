package com.nyl.mytasks.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nyl.mytasks.R
import com.nyl.mytasks.domain.MonthTotal
import java.time.format.TextStyle
import java.util.Locale

/**
 * Mini-graphique à barres : temps consacré à un projet mois par mois, avec une ligne de repère pour
 * l'objectif mensuel. Les mois où l'objectif est atteint sont en plein, les autres en teinte atténuée.
 */
@Composable
fun MonthlyBars(
    history: List<MonthTotal>,
    goal: Int,
    color: Color,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 56.dp,
) {
    if (history.isEmpty()) return
    val peak = maxOf(goal, history.maxOf { it.minutes }, 1)
    val lineColor = MaterialTheme.colorScheme.onSurfaceVariant
    val total = history.sumOf { it.minutes }
    val summary = stringResource(R.string.chart_description, history.size, formatMinutes(total), formatMinutes(goal))

    Column(modifier.fillMaxWidth().semantics { contentDescription = summary }) {
        Canvas(Modifier.fillMaxWidth().height(chartHeight)) {
            val gap = 4.dp.toPx()
            val barWidth = (size.width - gap * (history.size - 1)) / history.size
            val corner = CornerRadius(3.dp.toPx())
            history.forEachIndexed { i, m ->
                val x = i * (barWidth + gap)
                if (m.minutes <= 0) {
                    // Mois sans temps : simple socle, pour que la durée de l'historique reste lisible.
                    drawRoundRect(color.copy(alpha = 0.18f), Offset(x, size.height - 2.dp.toPx()), Size(barWidth, 2.dp.toPx()), corner)
                } else {
                    val h = (m.minutes.toFloat() / peak) * size.height
                    val reached = goal > 0 && m.minutes >= goal
                    drawRoundRect(
                        if (reached) color else color.copy(alpha = 0.5f),
                        Offset(x, size.height - h), Size(barWidth, h), corner,
                    )
                }
            }
            if (goal > 0) {
                val y = size.height - goal.toFloat() / peak * size.height
                drawLine(
                    lineColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)),
                )
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 2.dp)) {
            history.forEach { m ->
                Text(
                    m.month.month.getDisplayName(TextStyle.NARROW, Locale.FRANCE).uppercase(),
                    modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
                    fontSize = 9.sp, lineHeight = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
