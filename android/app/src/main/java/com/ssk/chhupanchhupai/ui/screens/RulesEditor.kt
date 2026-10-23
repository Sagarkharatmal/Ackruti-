package com.ssk.chhupanchhupai.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.ssk.chhupanchhupai.data.GameSettings
import com.ssk.chhupanchhupai.data.Zone
import com.ssk.chhupanchhupai.location.GameLocationService
import com.ssk.chhupanchhupai.ui.components.LabeledSlider
import com.ssk.chhupanchhupai.ui.components.SectionCard
import com.ssk.chhupanchhupai.ui.components.SwitchRow
import com.ssk.chhupanchhupai.ui.formatDistance
import com.ssk.chhupanchhupai.ui.formatSeconds
import com.ssk.chhupanchhupai.ui.map.FALLBACK_CENTER
import com.ssk.chhupanchhupai.ui.map.MapLayer
import com.ssk.chhupanchhupai.ui.map.OsmMap
import com.ssk.chhupanchhupai.ui.map.zoneCircle
import com.ssk.chhupanchhupai.ui.theme.Saffron
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import kotlin.math.roundToInt

/** One-shot GPS fix for centring the zone picker. */
@SuppressLint("MissingPermission")
fun fetchCurrentLocation(context: Context, onResult: (GeoPoint) -> Unit) {
    if (!GameLocationService.hasLocationPermission(context)) return
    LocationServices.getFusedLocationProviderClient(context)
        .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
        .addOnSuccessListener { location -> if (location != null) onResult(GeoPoint(location.latitude, location.longitude)) }
}

/**
 * Map where the host drags the play zone into place: the zone is always centred
 * on the crosshair in the middle of the map.
 */
@Composable
fun ZonePicker(settings: GameSettings, onChange: (GameSettings) -> Unit, locationGranted: Boolean) {
    val context = LocalContext.current
    val latest by rememberUpdatedState(settings)
    val latestOnChange by rememberUpdatedState(onChange)
    var map by remember { mutableStateOf<MapView?>(null) }
    var layer by remember { mutableStateOf<MapLayer?>(null) }
    val zone = settings.zone
    val radius = zone?.radiusM ?: DEFAULT_RADIUS_M
    val startedWithoutZone = remember { settings.zone == null }

    fun report(m: MapView) {
        val c = m.mapCenter
        val r = latest.zone?.radiusM ?: DEFAULT_RADIUS_M
        latestOnChange(latest.copy(zone = Zone(c.latitude, c.longitude, r)))
    }

    Box(
        Modifier
            .fillMaxWidth()
            .height(280.dp)
            .clip(RoundedCornerShape(18.dp)),
    ) {
        OsmMap(
            modifier = Modifier.fillMaxSize(),
            center = zone?.let { GeoPoint(it.lat, it.lng) } ?: FALLBACK_CENTER,
            zoom = if (zone != null) 15.0 else 5.0,
            onReady = { m ->
                map = m
                layer = MapLayer(m)
                m.addMapListener(object : MapListener {
                    override fun onScroll(event: ScrollEvent?): Boolean {
                        report(m)
                        return false
                    }

                    override fun onZoom(event: ZoomEvent?): Boolean {
                        report(m)
                        return false
                    }
                })
            },
        )
        Box(
            Modifier
                .align(Alignment.Center)
                .size(16.dp)
                .border(3.dp, Saffron, CircleShape),
        )
        SmallFloatingActionButton(
            onClick = {
                fetchCurrentLocation(context) { p ->
                    map?.controller?.setZoom(15.0)
                    map?.controller?.animateTo(p)
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(10.dp),
        ) {
            Icon(Icons.Filled.LocationOn, contentDescription = "Centre on me")
        }
        Text(
            "Drag the map to place the zone",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(8.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.8f))
                .padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }

    // First time (no zone placed yet): jump to where the host is standing.
    LaunchedEffect(map, locationGranted) {
        val m = map ?: return@LaunchedEffect
        if (!startedWithoutZone) return@LaunchedEffect
        report(m)
        if (locationGranted) {
            fetchCurrentLocation(context) { p ->
                m.controller.setZoom(15.0)
                m.controller.setCenter(p)
                report(m)
            }
        }
    }
    LaunchedEffect(map, zone?.lat, zone?.lng, radius) {
        val m = map ?: return@LaunchedEffect
        val z = zone ?: return@LaunchedEffect
        layer?.replace(listOf(zoneCircle(m, z.lat, z.lng, radius.toDouble(), Saffron)))
    }
}

/** All the round's rules. The map sits above (outside) the scrolling form. */
@Composable
fun RulesForm(settings: GameSettings, onChange: (GameSettings) -> Unit) {
    Column {
        SectionCard {
            Text("Play zone", style = MaterialTheme.typography.titleMedium)
            SwitchRow(
                "Use a play zone",
                "Hiders who leave it for ${settings.outOfZoneGraceSec}s are shown live to seekers.",
                settings.zoneEnabled,
            ) { onChange(settings.copy(zoneEnabled = it)) }
            if (settings.zoneEnabled) {
                val radius = settings.zone?.radiusM ?: DEFAULT_RADIUS_M
                LabeledSlider(
                    label = "Zone size",
                    valueText = "${formatDistance(radius * 2)} across",
                    value = radius.toFloat(),
                    range = 50f..1500f,
                    steps = 28,
                    onChange = { v ->
                        val r = (v / 50f).roundToInt() * 50
                        val z = settings.zone
                        onChange(settings.copy(zone = z?.copy(radiusM = r) ?: Zone(FALLBACK_CENTER.latitude, FALLBACK_CENTER.longitude, r)))
                    },
                )
                SwitchRow(
                    "Shrinking zone",
                    "The zone slowly closes in until the end of the round.",
                    settings.shrinkEnabled,
                ) { onChange(settings.copy(shrinkEnabled = it)) }
                if (settings.shrinkEnabled) {
                    LabeledSlider(
                        label = "Shrinks to",
                        valueText = "${settings.shrinkToPct}% of its size",
                        value = settings.shrinkToPct.toFloat(),
                        range = 10f..90f,
                        steps = 15,
                        onChange = { onChange(settings.copy(shrinkToPct = (it / 5f).roundToInt() * 5)) },
                    )
                }
                LabeledSlider(
                    label = "Out-of-zone grace",
                    valueText = formatSeconds(settings.outOfZoneGraceSec),
                    value = settings.outOfZoneGraceSec.toFloat(),
                    range = 10f..120f,
                    steps = 10,
                    onChange = { onChange(settings.copy(outOfZoneGraceSec = (it / 10f).roundToInt() * 10)) },
                )
            }
        }
        androidx.compose.foundation.layout.Spacer(Modifier.height(12.dp))
        SectionCard {
            Text("Timing", style = MaterialTheme.typography.titleMedium)
            LabeledSlider(
                label = "Round length",
                valueText = "${settings.durationMin} min",
                value = settings.durationMin.toFloat(),
                range = 5f..120f,
                steps = 22,
                onChange = { onChange(settings.copy(durationMin = (it / 5f).roundToInt() * 5)) },
            )
            LabeledSlider(
                label = "Head start",
                valueText = formatSeconds(settings.headStartSec),
                value = settings.headStartSec.toFloat(),
                range = 30f..900f,
                steps = 28,
                onChange = { onChange(settings.copy(headStartSec = (it / 30f).roundToInt() * 30)) },
            )
            LabeledSlider(
                label = "Ping every",
                valueText = formatSeconds(settings.pingIntervalSec),
                value = settings.pingIntervalSec.toFloat(),
                range = 30f..900f,
                steps = 28,
                onChange = { onChange(settings.copy(pingIntervalSec = (it / 30f).roundToInt() * 30)) },
            )
            SwitchRow(
                "Ping when the hunt starts",
                "Seekers get a first look the moment the head start ends.",
                settings.pingOnHuntStart,
            ) { onChange(settings.copy(pingOnHuntStart = it)) }
        }
    }
}

const val DEFAULT_RADIUS_M = 400
