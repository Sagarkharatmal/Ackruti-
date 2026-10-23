package com.ssk.chhupanchhupai.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssk.chhupanchhupai.ChhupanApp
import com.ssk.chhupanchhupai.data.GameSettings
import com.ssk.chhupanchhupai.data.GameState
import com.ssk.chhupanchhupai.data.Role
import com.ssk.chhupanchhupai.ui.components.BigButton
import com.ssk.chhupanchhupai.ui.components.PermissionState
import com.ssk.chhupanchhupai.ui.components.RolePill
import com.ssk.chhupanchhupai.ui.components.ScreenScaffold
import com.ssk.chhupanchhupai.ui.components.SectionCard
import com.ssk.chhupanchhupai.ui.components.StatusDot
import com.ssk.chhupanchhupai.ui.formatDistance
import com.ssk.chhupanchhupai.ui.formatSeconds
import com.ssk.chhupanchhupai.ui.theme.HiderTeal
import com.ssk.chhupanchhupai.ui.theme.Marigold
import com.ssk.chhupanchhupai.ui.theme.Saffron
import com.ssk.chhupanchhupai.ui.theme.SeekerRed

@Composable
fun LobbyScreen(game: GameState, permissions: PermissionState, onLeave: () -> Unit) {
    val app = LocalContext.current.applicationContext as ChhupanApp
    var editing by remember { mutableStateOf<GameSettings?>(null) }
    var confirmLeave by remember { mutableStateOf(false) }

    val draft = editing
    if (draft != null) {
        RulesEditScreen(
            settings = draft,
            onChange = { editing = it },
            locationGranted = permissions.granted,
            onCancel = { editing = null },
            onSave = {
                app.connection.updateSettings(draft)
                app.prefs.lastSettings = draft
                editing = null
            },
        )
        return
    }

    val playerCount = game.players.size
    val zoneMissing = game.settings.zoneEnabled && game.settings.zone == null
    ScreenScaffold(
        title = "Lobby",
        onBack = { confirmLeave = true },
        bottomBar = {
            if (game.you.isHost) {
                BigButton(
                    text = when {
                        playerCount < 2 -> "Waiting for players…"
                        zoneMissing -> "Set the zone first"
                        else -> "Start the round ($playerCount players)"
                    },
                    onClick = { app.connection.start() },
                    enabled = playerCount >= 2 && !zoneMissing,
                )
            } else {
                Text(
                    "Waiting for ${game.player(game.hostId)?.name ?: "the host"} to start…",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    ) {
        GameCodeCard(game.code)
        if (!permissions.granted) {
            SectionCard {
                Text("Location needed", style = MaterialTheme.typography.titleMedium, color = SeekerRed)
                Text(
                    "The game can't see where you are, so you can't be pinged or play the zone. " +
                        "Allow location (\"While using the app\") to play.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                TextButton(onClick = permissions.request) { Text("Allow location") }
            }
        }
        SectionCard {
            Text("I want to be…", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for ((pref, label) in listOf(Role.HIDER to "Hider", Role.RANDOM to "Random", Role.SEEKER to "Seeker")) {
                    FilterChip(
                        selected = game.you.rolePref == pref,
                        onClick = { app.connection.setRolePreference(pref) },
                        label = { Text(label) },
                    )
                }
            }
            Text(
                "The app balances teams: about one seeker for every four players.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        SectionCard {
            Text("Players ($playerCount)", style = MaterialTheme.typography.titleMedium)
            for (p in game.players) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusDot(p.connected)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        p.name + if (p.id == game.you.id) " (you)" else "",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    if (p.id == game.hostId) {
                        Icon(Icons.Filled.Star, contentDescription = "Host", tint = Marigold)
                        Spacer(Modifier.width(6.dp))
                    }
                    RolePill(p.rolePref)
                    if (game.you.isHost && p.id != game.you.id) {
                        IconButton(onClick = { app.connection.kick(p.id) }) {
                            Icon(Icons.Filled.Close, contentDescription = "Remove ${p.name}")
                        }
                    }
                }
            }
            if (playerCount < 3) {
                Text(
                    "Share the code — 3 or more players is the most fun.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        SectionCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Rules", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (game.you.isHost) {
                    OutlinedButton(onClick = { editing = game.settings }) { Text("Edit") }
                }
            }
            RulesSummary(game.settings)
        }
        Spacer(Modifier.padding(4.dp))
    }

    if (confirmLeave) {
        AlertDialog(
            onDismissRequest = { confirmLeave = false },
            title = { Text("Leave the lobby?") },
            text = { Text(if (game.you.isHost) "Someone else will become the host." else "You can rejoin with the code.") },
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

@Composable
private fun GameCodeCard(code: String) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    SectionCard {
        Text("Game code", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                code,
                fontSize = 40.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 6.sp,
                color = Saffron,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = {
                clipboard.setText(AnnotatedString(code))
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(
                        Intent.EXTRA_TEXT,
                        "Let's play Chhupan Chhupai! Open the app, tap \"Join with a code\" and enter: $code",
                    )
                }
                context.startActivity(Intent.createChooser(send, "Share game code"))
            }) {
                Icon(Icons.Filled.Share, contentDescription = "Share code")
            }
        }
    }
}

@Composable
fun RulesSummary(s: GameSettings) {
    val lines = buildList {
        add("Round" to "${s.durationMin} min")
        add("Head start" to formatSeconds(s.headStartSec))
        add("Ping every" to formatSeconds(s.pingIntervalSec))
        if (s.zoneEnabled) {
            add("Zone" to (s.zone?.let { "${formatDistance(it.radiusM * 2)} across" } ?: "not placed yet"))
            if (s.shrinkEnabled) add("Shrinks to" to "${s.shrinkToPct}%")
            add("Out-of-zone grace" to formatSeconds(s.outOfZoneGraceSec))
        } else {
            add("Zone" to "off — play anywhere")
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for ((k, v) in lines) {
            Row {
                Text(k, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(v, fontWeight = FontWeight.SemiBold, color = HiderTeal)
            }
        }
    }
}

@Composable
private fun RulesEditScreen(
    settings: GameSettings,
    onChange: (GameSettings) -> Unit,
    locationGranted: Boolean,
    onCancel: () -> Unit,
    onSave: () -> Unit,
) {
    ScreenScaffold(
        title = "Edit rules",
        onBack = onCancel,
        scroll = false,
        bottomBar = {
            BigButton(
                "Save rules",
                onClick = onSave,
                enabled = !settings.zoneEnabled || settings.zone != null,
            )
        },
    ) {
        if (settings.zoneEnabled) ZonePicker(settings, onChange, locationGranted)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            RulesForm(settings, onChange)
        }
    }
}
