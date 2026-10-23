package com.ssk.chhupanchhupai.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssk.chhupanchhupai.ChhupanApp
import com.ssk.chhupanchhupai.data.GameState
import com.ssk.chhupanchhupai.data.Loc
import com.ssk.chhupanchhupai.data.Phase
import com.ssk.chhupanchhupai.data.PlayerInfo
import com.ssk.chhupanchhupai.data.Role
import com.ssk.chhupanchhupai.data.Status
import com.ssk.chhupanchhupai.ui.components.PermissionState
import com.ssk.chhupanchhupai.ui.components.RolePill
import com.ssk.chhupanchhupai.ui.components.rememberServerNow
import com.ssk.chhupanchhupai.ui.formatAgo
import com.ssk.chhupanchhupai.ui.formatClock
import com.ssk.chhupanchhupai.ui.formatDistance
import com.ssk.chhupanchhupai.ui.map.FALLBACK_CENTER
import com.ssk.chhupanchhupai.ui.map.MapLayer
import com.ssk.chhupanchhupai.ui.map.OsmMap
import com.ssk.chhupanchhupai.ui.map.dotMarker
import com.ssk.chhupanchhupai.ui.map.zoneCircle
import com.ssk.chhupanchhupai.ui.theme.CountdownStyle
import com.ssk.chhupanchhupai.ui.theme.HiderTeal
import com.ssk.chhupanchhupai.ui.theme.Marigold
import com.ssk.chhupanchhupai.ui.theme.Night
import com.ssk.chhupanchhupai.ui.theme.Saffron
import com.ssk.chhupanchhupai.ui.theme.SeekerRed
import com.ssk.chhupanchhupai.ui.theme.roleColor
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay

private val MeBlue = Color(0xFF4DA3FF)
private const val ROLE_REVEAL_MS = 6_000L
private const val ROLE_REVEAL_FLIP_MS = 2_500L

@Composable
fun GameScreen(game: GameState, permissions: PermissionState, onLeave: () -> Unit) {
    val app = LocalContext.current.applicationContext as ChhupanApp
    val now by rememberServerNow()
    val myLoc by app.myLocation.collectAsStateWithLifecycle()
    var map by remember { mutableStateOf<MapView?>(null) }
    var layer by remember { mutableStateOf<MapLayer?>(null) }
    var centred by remember { mutableStateOf(false) }
    var confirmLeave by remember { mutableStateOf(false) }
    var confirmEnd by remember { mutableStateOf(false) }
    var showCaughtDialog by remember { mutableStateOf(false) }
    var tagTarget by remember { mutableStateOf<PlayerInfo?>(null) }
    var answeredRequests by remember { mutableStateOf(setOf<String>()) }

    BackHandler { confirmLeave = true }

    val me: Loc? = myLoc ?: game.you.loc
    val start = game.zone?.let { GeoPoint(it.lat, it.lng) } ?: me?.let { GeoPoint(it.lat, it.lng) } ?: FALLBACK_CENTER

    Box(Modifier.fillMaxSize()) {
        OsmMap(
            modifier = Modifier.fillMaxSize(),
            center = start,
            zoom = 16.0,
            onReady = {
                map = it
                layer = MapLayer(it)
            },
        )

        Column(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Hud(game, now, onEnd = { confirmEnd = true })
            ZoneBanner(game, now)
            if (game.isActiveHider && game.dhappaBase != null && game.players.any { it.role == Role.HIDER && it.status == Status.CAUGHT }) {
                Banner("Friends are in jail! Reach the BASE to shout DHAPPA and free them.", HiderTeal)
            }
            if (!permissions.granted) {
                Banner("Location is off — the game can't see you. Tap to allow.", SeekerRed, onClick = permissions.request)
            }
        }

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FloatingActionButton(
                onClick = { me?.let { map?.controller?.animateTo(GeoPoint(it.lat, it.lng)) } },
                modifier = Modifier.align(Alignment.End),
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                Icon(Icons.Filled.LocationOn, contentDescription = "Centre on me", tint = MeBlue)
            }
            ActionPanel(
                game = game,
                onCaught = { showCaughtDialog = true },
                onTag = { tagTarget = it },
            )
        }

        if (game.isSeeker && game.phase == Phase.HEADSTART) {
            SeekerWaitCover(game, now)
        }
        // Everyone's phone flips from "?" to their role at the same moment.
        val startedAt = game.startedAt
        if (game.phase == Phase.HEADSTART && startedAt != null && now - startedAt < ROLE_REVEAL_MS) {
            RoleReveal(game, now - startedAt)
        }
    }

    // Draw zone, pings, live players and me.
    LaunchedEffect(map, game, me, now / 10_000) {
        val m = map ?: return@LaunchedEffect
        layer?.replace(buildOverlays(m, game, me, now))
        if (!centred && me != null && game.zone == null) {
            m.controller.setCenter(GeoPoint(me.lat, me.lng))
            centred = true
        }
    }

    // A seeker says they tagged me.
    val request = game.you.pendingTagRequests.firstOrNull { it.requestId !in answeredRequests && it.expiresAt > now }
    if (request != null && game.isActiveHider) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("${request.fromName} says they tagged you!") },
            text = { Text("Were you really caught? Be honest — the game relies on it. (${formatClock(request.expiresAt - now)} to answer)") },
            confirmButton = {
                Button(
                    onClick = {
                        answeredRequests = answeredRequests + request.requestId
                        app.connection.replyToTag(request.requestId, true)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SeekerRed),
                ) { Text("Yes, I'm caught") }
            },
            dismissButton = {
                TextButton(onClick = {
                    answeredRequests = answeredRequests + request.requestId
                    app.connection.replyToTag(request.requestId, false)
                }) { Text("No, I got away") }
            },
        )
    }

    tagTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { tagTarget = null },
            title = { Text("Tag ${target.name}?") },
            text = { Text("Only if you touched them in person. ${target.name} will be asked to confirm on their phone.") },
            confirmButton = {
                Button(onClick = {
                    app.connection.tag(target.id)
                    tagTarget = null
                }) { Text("Tag!") }
            },
            dismissButton = { TextButton(onClick = { tagTarget = null }) { Text("Cancel") } },
        )
    }

    if (showCaughtDialog) {
        CaughtDialog(
            seekers = game.players.filter { it.role == Role.SEEKER },
            onConfirm = { byId ->
                app.connection.reportCaught(byId)
                showCaughtDialog = false
            },
            onDismiss = { showCaughtDialog = false },
        )
    }

    if (confirmEnd) {
        AlertDialog(
            onDismissRequest = { confirmEnd = false },
            title = { Text("End the round now?") },
            text = { Text("Everyone goes to the results screen. Nobody wins.") },
            confirmButton = {
                TextButton(onClick = {
                    app.connection.endRound()
                    confirmEnd = false
                }) { Text("End round") }
            },
            dismissButton = { TextButton(onClick = { confirmEnd = false }) { Text("Keep playing") } },
        )
    }

    if (confirmLeave) {
        AlertDialog(
            onDismissRequest = { confirmLeave = false },
            title = { Text("Leave the game?") },
            text = {
                Text(
                    if (game.isActiveHider) "You'll be out of this round and can't come back to it."
                    else "You'll leave this game for good.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmLeave = false
                    onLeave()
                }) { Text("Leave") }
            },
            dismissButton = { TextButton(onClick = { confirmLeave = false }) { Text("Stay") } },
        )
    }
}

private fun buildOverlays(m: MapView, game: GameState, me: Loc?, now: Long): List<Overlay> {
    val overlays = mutableListOf<Overlay>()
    game.zone?.let { z ->
        val current = (z.currentRadiusM ?: z.radiusM).toDouble()
        if (current < z.radiusM) {
            overlays += zoneCircle(m, z.lat, z.lng, z.radiusM.toDouble(), Saffron.copy(alpha = 0.35f), fillAlpha = 0f)
        }
        overlays += zoneCircle(m, z.lat, z.lng, current, Saffron)
    }
    val seesEverything = game.you.spectator || game.phase == Phase.ENDED
    game.pings.forEachIndexed { index, ping ->
        val latest = index == game.pings.lastIndex
        for (mark in ping.marks) {
            val lat = mark.lat ?: continue
            val lng = mark.lng ?: continue
            val player = game.player(mark.playerId)
            if (!seesEverything && player?.status != Status.ACTIVE) continue
            val label = if (latest) "${player?.name ?: "?"} · ${formatAgo(now - (mark.locAt ?: ping.at))}" else null
            overlays += dotMarker(m, GeoPoint(lat, lng), HiderTeal, label, alpha = if (latest) 0.95f else 0.4f, small = !latest)
        }
    }
    for (live in game.live) {
        val player = game.player(live.playerId)
        val name = player?.name ?: "?"
        val (color, label) = when (live.kind) {
            "revealed" -> Marigold to "$name · LIVE"
            "teammate" -> SeekerRed to name
            else -> roleColor(player?.role) to if (player?.status == Status.CAUGHT) "$name (out)" else name
        }
        overlays += dotMarker(m, GeoPoint(live.lat, live.lng), color, label)
    }
    game.dhappaBase?.let { b ->
        overlays += zoneCircle(m, b.lat, b.lng, b.radiusM.toDouble(), Marigold, fillAlpha = 0.35f)
        overlays += dotMarker(m, GeoPoint(b.lat, b.lng), Marigold, "BASE · Dhappa!")
    }
    if (me != null) overlays += dotMarker(m, GeoPoint(me.lat, me.lng), MeBlue, "You")
    return overlays
}

@Composable
private fun Hud(game: GameState, now: Long, onEnd: () -> Unit) {
    val spectator = game.you.spectator
    val (title, target) = when (game.phase) {
        Phase.HEADSTART -> when {
            game.isActiveHider -> "Go hide! Seekers released in" to game.huntStartsAt
            else -> "Hiders are hiding… hunt starts in" to game.huntStartsAt
        }
        else -> "Time left" to game.endsAt
    }
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RolePill(game.you.role, caught = spectator)
                Spacer(Modifier.width(8.dp))
                Text(
                    if (spectator) "You're out — spectating" else "Code ${game.code}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                if (game.you.isHost) {
                    TextButton(onClick = onEnd) { Text("End", color = SeekerRed) }
                }
            }
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(formatClock((target ?: now) - now), style = CountdownStyle, color = Saffron)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                if (game.phase == Phase.HUNT && game.nextPingAt != null) {
                    Stat("Next ping", formatClock(game.nextPingAt - now))
                }
                Stat("Hidden", "${game.hidersLeft} / ${game.players.count { it.role == Role.HIDER }}")
                game.zone?.let { Stat("Zone", formatDistance((it.currentRadiusM ?: it.radiusM) * 2)) }
                if (game.isActiveHider && game.lastPingAt != null) {
                    Stat("Pinged", formatAgo(now - game.lastPingAt))
                }
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Banner(text: String, color: Color, onClick: (() -> Unit)? = null) {
    Text(
        text,
        color = Night,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .fillMaxWidth()
            .background(color, RoundedCornerShape(14.dp))
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(12.dp),
    )
}

@Composable
private fun ZoneBanner(game: GameState, now: Long) {
    val outSince = game.you.outSince ?: return
    if (!game.isActiveHider) return
    when {
        game.you.revealed -> Banner("OUT OF ZONE — seekers can see you LIVE! Get back inside.", SeekerRed)
        game.phase == Phase.HEADSTART -> Banner("You're outside the zone. Get back in before the hunt starts!", Marigold)
        else -> {
            val left = game.settings.outOfZoneGraceSec * 1000L - (now - outSince)
            Banner("Outside the zone! You'll be revealed in ${formatClock(left)}", Marigold)
        }
    }
}

@Composable
private fun ActionPanel(game: GameState, onCaught: () -> Unit, onTag: (PlayerInfo) -> Unit) {
    when {
        game.isActiveHider -> Button(
            onClick = onCaught,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SeekerRed, contentColor = Night),
        ) { Text("I've been caught", style = MaterialTheme.typography.labelLarge) }

        game.isSeeker && game.phase == Phase.HUNT -> {
            val hiders = game.players.filter { it.role == Role.HIDER && it.status == Status.ACTIVE }
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)),
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text("Caught someone? Tag them:", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(hiders, key = { it.id }) { hider ->
                            Button(
                                onClick = { onTag(hider) },
                                colors = ButtonDefaults.buttonColors(containerColor = HiderTeal, contentColor = Night),
                            ) { Text(hider.name) }
                        }
                    }
                }
            }
        }

        game.you.inJail -> Banner(
            "Caught! Walk to the BASE and wait there. If a free hider reaches it — DHAPPA! — you're back in the game.",
            Marigold,
        )

        game.you.spectator -> Banner("You've been caught. You can now watch everyone live — no telling!", MaterialTheme.colorScheme.onSurfaceVariant)
        else -> Unit
    }
}

@Composable
private fun SeekerWaitCover(game: GameState, now: Long) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Night.copy(alpha = 0.96f))
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        RolePill(Role.SEEKER)
        Spacer(Modifier.height(24.dp))
        Text("Eyes closed!", fontSize = 40.sp, fontWeight = FontWeight.Black, color = SeekerRed)
        Spacer(Modifier.height(8.dp))
        Text(
            "The hiders are running to hide. Count out loud — no peeking!",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(32.dp))
        Text("The hunt starts in", style = MaterialTheme.typography.titleMedium)
        Text(formatClock((game.huntStartsAt ?: now) - now), style = CountdownStyle.copy(fontSize = 64.sp), color = Saffron)
        Spacer(Modifier.height(16.dp))
        Text(
            "${game.players.count { it.role == Role.HIDER }} hiders · ${game.players.count { it.role == Role.SEEKER }} seekers",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CaughtDialog(seekers: List<PlayerInfo>, onConfirm: (String?) -> Unit, onDismiss: () -> Unit) {
    var chosen by remember { mutableStateOf(seekers.singleOrNull()?.id) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Were you caught?") },
        text = {
            Column {
                Text("You'll be out of the round and become a spectator. Who got you?")
                Spacer(Modifier.height(8.dp))
                for (s in seekers) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { chosen = s.id },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = chosen == s.id, onClick = { chosen = s.id })
                        Text(s.name)
                    }
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { chosen = null },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = chosen == null, onClick = { chosen = null })
                    Text("Not sure")
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(chosen) }, colors = ButtonDefaults.buttonColors(containerColor = SeekerRed)) {
                Text("I'm caught")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** "Huddle up" moment: phones held together show "?" then flip to each player's role. */
@Composable
private fun RoleReveal(game: GameState, elapsedMs: Long) {
    val flipped = elapsedMs >= ROLE_REVEAL_FLIP_MS
    val seeker = game.isSeeker
    val color = if (!flipped) Saffron else if (seeker) SeekerRed else HiderTeal
    Column(
        Modifier
            .fillMaxSize()
            .background(if (flipped) color else Night),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (!flipped) {
            // A pulsing question mark while everyone holds their phones together.
            val pulse = 1f + 0.12f * kotlin.math.sin(elapsedMs / 120.0).toFloat()
            Text("?", fontSize = (220 * pulse).sp, fontWeight = FontWeight.Black, color = Saffron)
            Text("Huddle up… who's hiding?", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
        } else {
            Text("YOU ARE A", style = MaterialTheme.typography.titleLarge, color = Night, fontWeight = FontWeight.Black)
            Text(
                if (seeker) "SEEKER" else "HIDER",
                fontSize = 72.sp,
                fontWeight = FontWeight.Black,
                color = Night,
            )
            Text(
                if (seeker) "Close your eyes and count!" else "RUN! Go hide!",
                style = MaterialTheme.typography.titleLarge,
                color = Night,
            )
        }
    }
}
