package com.ssk.chhupanchhupai.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssk.chhupanchhupai.ChhupanApp
import com.ssk.chhupanchhupai.data.GameState
import com.ssk.chhupanchhupai.data.ResultPlayer
import com.ssk.chhupanchhupai.data.Role
import com.ssk.chhupanchhupai.data.Status
import com.ssk.chhupanchhupai.ui.components.BigButton
import com.ssk.chhupanchhupai.ui.components.RolePill
import com.ssk.chhupanchhupai.ui.components.SectionCard
import com.ssk.chhupanchhupai.ui.formatClock
import com.ssk.chhupanchhupai.ui.formatDistance
import com.ssk.chhupanchhupai.ui.map.MapLayer
import com.ssk.chhupanchhupai.ui.map.OsmMap
import com.ssk.chhupanchhupai.ui.map.dotMarker
import com.ssk.chhupanchhupai.ui.map.fitPoints
import com.ssk.chhupanchhupai.ui.map.trailLine
import com.ssk.chhupanchhupai.ui.map.zoneCircle
import com.ssk.chhupanchhupai.ui.theme.HiderTeal
import com.ssk.chhupanchhupai.ui.theme.Saffron
import com.ssk.chhupanchhupai.ui.theme.SeekerRed
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay

private val TrailColors = listOf(
    Color(0xFF2EC4B6), Color(0xFFFF4D6D), Color(0xFFFFC145), Color(0xFF4DA3FF),
    Color(0xFFB388FF), Color(0xFF7CFC9A), Color(0xFFFF8A65), Color(0xFFF06292),
)

@Composable
fun ResultsScreen(game: GameState, onLeave: () -> Unit) {
    val app = LocalContext.current.applicationContext as ChhupanApp
    val results = game.results
    var map by remember { mutableStateOf<MapView?>(null) }
    var layer by remember { mutableStateOf<MapLayer?>(null) }
    BackHandler { onLeave() }

    val players = results?.players.orEmpty().sortedWith(
        compareBy<ResultPlayer> { it.role }.thenByDescending { it.survivedMs ?: 0L }.thenByDescending { it.catches },
    )
    val colorFor = players.mapIndexed { i, p -> p.id to TrailColors[i % TrailColors.size] }.toMap()

    val (headline, headlineColor) = when (results?.winner) {
        Role.SEEKER -> "Seekers win!" to SeekerRed
        Role.HIDER -> "Hiders win!" to HiderTeal
        else -> "Round over" to Saffron
    }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(headline, fontSize = 40.sp, fontWeight = FontWeight.Black, color = headlineColor)
            Text(results?.reason.orEmpty(), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box(
            Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .height(260.dp)
                .clip(RoundedCornerShape(18.dp)),
        ) {
            OsmMap(
                modifier = Modifier.fillMaxSize(),
                center = game.zone?.let { GeoPoint(it.lat, it.lng) } ?: com.ssk.chhupanchhupai.ui.map.FALLBACK_CENTER,
                zoom = 15.0,
                onReady = {
                    map = it
                    layer = MapLayer(it)
                },
            )
        }
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            for (p in players) PlayerResult(p, colorFor[p.id] ?: Saffron, results?.players.orEmpty())
            if (players.isEmpty()) Text("No results for this round.")
        }
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (game.you.isHost) {
                BigButton("Play again with the same group", onClick = { app.connection.rematch() })
            } else {
                Text(
                    "The host can start another round from here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedButton(onClick = onLeave, modifier = Modifier.fillMaxWidth()) { Text("Leave game") }
        }
    }

    LaunchedEffect(map, results) {
        val m = map ?: return@LaunchedEffect
        val overlays = mutableListOf<Overlay>()
        val all = mutableListOf<GeoPoint>()
        game.zone?.let { overlays += zoneCircle(m, it.lat, it.lng, it.radiusM.toDouble(), Saffron, fillAlpha = 0.06f) }
        for (p in players) {
            val points = p.trail.mapNotNull { if (it.size >= 2) GeoPoint(it[0], it[1]) else null }
            if (points.isEmpty()) continue
            val color = colorFor[p.id] ?: Saffron
            if (points.size > 1) overlays += trailLine(m, points, color)
            overlays += dotMarker(m, points.last(), color, p.name)
            all += points
        }
        layer?.replace(overlays)
        if (all.isNotEmpty()) m.fitPoints(all)
    }
}

@Composable
private fun PlayerResult(p: ResultPlayer, color: Color, everyone: List<ResultPlayer>) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(color),
            )
            Spacer(Modifier.width(10.dp))
            Text(p.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            RolePill(p.role)
        }
        val detail = when {
            p.role == Role.SEEKER -> "${p.catches} catch${if (p.catches == 1) "" else "es"}"
            p.status == Status.CAUGHT -> {
                val by = everyone.firstOrNull { it.id == p.caughtBy }?.name
                "Caught${if (by != null) " by $by" else ""} after ${formatClock(p.survivedMs ?: 0)}"
            }
            p.status == Status.LEFT -> "Left after ${formatClock(p.survivedMs ?: 0)}"
            else -> "Never found! Survived ${formatClock(p.survivedMs ?: 0)}"
        }
        Text(detail, style = MaterialTheme.typography.bodyMedium)
        Text(
            "Moved ${formatDistance(p.distanceM)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
