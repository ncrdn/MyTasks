package com.nyl.mytasks.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.nyl.mytasks.MainActivity
import com.nyl.mytasks.R
import com.nyl.mytasks.ui.dashboard.ProgressTile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Widget d'écran d'accueil : le bloc « Avancement » du tableau de bord (récurrentes et ponctuelles sur la
 * semaine, projets sur le mois, travaux sur l'année). Un appui ouvre l'application.
 */
class ProgressWidget : GlanceAppWidget() {
    // Taille exacte : on adapte la disposition (4 jauges en ligne ou grille 2 × 2) à la place disponible.
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = WidgetData.load(context)
        provideContent { ProgressContent(data) }
    }
}

class ProgressWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ProgressWidget()
}

/** Changement de date, d'heure ou de fuseau : « cette semaine » et « ce mois-ci » peuvent avoir changé. */
class WidgetRefreshReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                ProgressWidget().updateAll(context)
            } finally {
                pending.finish()
            }
        }
    }
}

/**
 * Propose d'épingler le widget sur l'écran d'accueil (le lanceur affiche sa propre confirmation).
 * Renvoie false si le lanceur ne le permet pas : l'utilisateur ajoute alors le widget depuis la liste des widgets.
 */
fun requestPinProgressWidget(context: Context): Boolean {
    val manager = android.appwidget.AppWidgetManager.getInstance(context)
    if (!manager.isRequestPinAppWidgetSupported) return false
    return manager.requestPinAppWidget(android.content.ComponentName(context, ProgressWidgetReceiver::class.java), null, null)
}

// --- Couleurs (clair / sombre) ---

private val TextMain = ColorProvider(day = Color(0xFF1B1B21), night = Color(0xFFECECF1))
private val TextMuted = ColorProvider(day = Color(0xFF5F5F6B), night = Color(0xFFA6A6B2))

private fun tint(color: Color) = ColorProvider(
    day = lerp(color, Color.Black, 0.35f),
    night = lerp(color, Color.White, 0.3f),
)

// --- Disposition ---

@Composable
private fun ProgressContent(data: WidgetData) {
    val size = LocalSize.current
    val width = size.width
    val height = size.height
    val pad = 10.dp
    val wide = width >= 240.dp
    val showTitle = height >= 150.dp
    val titleHeight = if (showTitle) 22.dp else 0.dp

    // Taille de l'anneau : limitée par la largeur d'une jauge et par la hauteur qui reste pour le texte.
    val ring: Dp = if (wide) {
        minOf((width - pad * 2) / 4 - 6.dp, height - pad * 2 - titleHeight - 44.dp)
    } else {
        minOf((width - pad * 2) / 2 - 6.dp, (height - pad * 2 - titleHeight) / 2 - 36.dp)
    }.coerceIn(30.dp, 84.dp)
    val showHours = wide && height >= 190.dp

    Column(
        GlanceModifier.fillMaxSize()
            .background(ImageProvider(R.drawable.widget_background))
            .padding(pad)
            .clickable(actionStartActivity<MainActivity>()),
    ) {
        if (showTitle) {
            Text(
                LocalContext.current.getString(R.string.dash_progress),
                modifier = GlanceModifier.fillMaxWidth().height(titleHeight),
                style = TextStyle(color = TextMain, fontSize = 15.sp, fontWeight = FontWeight.Bold),
            )
        }
        if (wide) {
            Row(GlanceModifier.fillMaxSize()) {
                data.tiles.forEach { tile -> Gauge(tile, ring, showHours, GlanceModifier.defaultWeight().fillMaxHeight()) }
            }
        } else {
            Column(GlanceModifier.fillMaxSize()) {
                data.tiles.chunked(2).forEach { pair ->
                    Row(GlanceModifier.fillMaxWidth().defaultWeight()) {
                        pair.forEach { tile -> Gauge(tile, ring, false, GlanceModifier.defaultWeight().fillMaxHeight()) }
                    }
                }
            }
        }
    }
}

@Composable
private fun Gauge(tile: ProgressTile, ring: Dp, showHours: Boolean, modifier: GlanceModifier) {
    val context = LocalContext.current
    val density = context.resources.displayMetrics.density
    val bitmap = RingBitmap.draw((ring.value * density).toInt(), tile.color.toArgb(), tile.ratio, tile.empty)
    Column(
        modifier.padding(horizontal = 3.dp),
        verticalAlignment = Alignment.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(GlanceModifier.size(ring), contentAlignment = Alignment.Center) {
            Image(ImageProvider(bitmap), contentDescription = null, modifier = GlanceModifier.size(ring), contentScale = ContentScale.Fit)
            Text(
                if (tile.empty) "—" else "${(tile.ratio * 100).toInt()} %",
                style = TextStyle(
                    color = TextMain, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                    fontSize = if (ring >= 58.dp) 15.sp else 11.sp,
                ),
            )
        }
        Spacer(GlanceModifier.height(2.dp))
        Text(
            tile.label, maxLines = 2,
            style = TextStyle(color = tint(tile.color), fontSize = 10.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center),
        )
        Text(
            tile.count, maxLines = 1,
            style = TextStyle(color = TextMain, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
        )
        if (showHours && tile.hours != null) {
            Text(
                tile.hours, maxLines = 1,
                style = TextStyle(color = TextMuted, fontSize = 10.sp, textAlign = TextAlign.Center),
            )
        }
    }
}
