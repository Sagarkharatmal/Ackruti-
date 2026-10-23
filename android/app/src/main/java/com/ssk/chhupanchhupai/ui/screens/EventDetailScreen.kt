package com.ssk.chhupanchhupai.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ssk.chhupanchhupai.ChhupanApp
import com.ssk.chhupanchhupai.Reminders
import com.ssk.chhupanchhupai.data.ApiException
import com.ssk.chhupanchhupai.data.ChatMessage
import com.ssk.chhupanchhupai.data.EventDetail
import com.ssk.chhupanchhupai.data.EventStatus
import com.ssk.chhupanchhupai.data.Role
import com.ssk.chhupanchhupai.ui.components.BigButton
import com.ssk.chhupanchhupai.ui.components.SectionCard
import com.ssk.chhupanchhupai.ui.components.SwitchRow
import com.ssk.chhupanchhupai.ui.formatClock
import com.ssk.chhupanchhupai.ui.formatEventTime
import com.ssk.chhupanchhupai.ui.formatUntil
import com.ssk.chhupanchhupai.ui.map.MapLayer
import com.ssk.chhupanchhupai.ui.map.OsmMap
import com.ssk.chhupanchhupai.ui.map.dotMarker
import com.ssk.chhupanchhupai.ui.map.zoneCircle
import com.ssk.chhupanchhupai.ui.theme.HiderTeal
import com.ssk.chhupanchhupai.ui.theme.Marigold
import com.ssk.chhupanchhupai.ui.theme.Saffron
import com.ssk.chhupanchhupai.ui.theme.SeekerRed
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

private const val CHECKIN_OPENS_BEFORE_MS = 2 * 60 * 60_000L

@Composable
fun EventDetailScreen(eventId: String, onBack: () -> Unit, onEdit: (EventDetail) -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as ChhupanApp
    val scope = rememberCoroutineScope()
    var event by remember { mutableStateOf<EventDetail?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var refreshKey by remember { mutableIntStateOf(0) }
    var menuOpen by remember { mutableStateOf(false) }
    var confirmCancel by remember { mutableStateOf(false) }
    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }

    fun act(block: suspend () -> Unit) {
        scope.launch {
            busy = true
            try {
                block()
            } catch (e: ApiException) {
                Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
            } finally {
                busy = false
            }
        }
    }

    // Event page refreshes every 10 s (attendees, open rounds, leaderboard).
    LaunchedEffect(eventId, refreshKey) {
        while (true) {
            try {
                event = app.api.event(eventId)
                error = null
            } catch (e: ApiException) {
                if (event == null) error = e.message
            }
            delay(10_000)
        }
    }

    // Chat refreshes every 3 s while the chat tab is open.
    val canChat = event?.me?.going == true
    LaunchedEffect(eventId, tab, canChat) {
        if (tab != 1 || !canChat) return@LaunchedEffect
        while (true) {
            try {
                val since = messages.lastOrNull()?.at ?: 0L
                val fresh = app.api.messages(eventId, since)
                if (fresh.isNotEmpty()) messages = (messages + fresh).distinctBy { it.id }
            } catch (_: ApiException) {
            }
            delay(3_000)
        }
    }

    val ev = event
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Text(
                ev?.title ?: "Event",
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (ev != null) {
                IconButton(onClick = { shareInvite(context, ev) }) { Icon(Icons.Filled.Share, contentDescription = "Invite friends") }
                if (ev.me.isHost) {
                    Box {
                        IconButton(onClick = { menuOpen = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "Host menu") }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(text = { Text("Edit event") }, onClick = {
                                menuOpen = false
                                onEdit(ev)
                            })
                            if (ev.status != EventStatus.CANCELLED) {
                                DropdownMenuItem(text = { Text("Cancel event", color = SeekerRed) }, onClick = {
                                    menuOpen = false
                                    confirmCancel = true
                                })
                            }
                        }
                    }
                }
            }
        }

        if (ev == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (error != null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                        Text(error ?: "", color = SeekerRed)
                        TextButton(onClick = { refreshKey++ }) { Text("Try again") }
                    }
                } else {
                    CircularProgressIndicator()
                }
            }
            return@Column
        }

        TabRow(selectedTabIndex = tab) {
            listOf("Details", "Chat", "People", "Ranking").forEachIndexed { i, label ->
                Tab(selected = tab == i, onClick = { tab = i }, text = { Text(label) })
            }
        }
        Box(Modifier.weight(1f)) {
            when (tab) {
                0 -> DetailsTab(ev)
                1 -> ChatTab(ev, messages, onSend = { text, announce ->
                    act {
                        if (announce) app.api.announce(ev.id, text) else app.api.postMessage(ev.id, text)
                        val fresh = app.api.messages(ev.id, messages.lastOrNull()?.at ?: 0L)
                        messages = (messages + fresh).distinctBy { it.id }
                    }
                })
                2 -> PeopleTab(ev, onHostCheckIn = { userId -> act { event = app.api.hostCheckIn(ev.id, userId) } })
                else -> RankingTab(ev)
            }
        }
        ActionBar(ev, busy, onAction = { action ->
            when (action) {
                EventAction.RSVP -> act {
                    val updated = app.api.rsvp(ev.id)
                    event = updated
                    Reminders.schedule(context, updated.id, updated.title, updated.startsAt)
                    Toast.makeText(context, "You're in! We'll remind you before it starts.", Toast.LENGTH_LONG).show()
                }
                EventAction.CANCEL_RSVP -> act {
                    event = app.api.cancelRsvp(ev.id)
                    Reminders.cancel(context, ev.id)
                }
                EventAction.CHECK_IN -> act {
                    val here = currentLocation(context)
                        ?: throw ApiException("Couldn't get your location. Turn on location and try again.")
                    event = app.api.checkIn(ev.id, here.latitude, here.longitude, if (here.hasAccuracy()) here.accuracy else null)
                    Toast.makeText(context, "Checked in! Have fun 🙈", Toast.LENGTH_SHORT).show()
                }
                EventAction.START_ROUND -> act {
                    val code = app.api.startRound(ev.id)
                    app.activeEvent.value = ev
                    app.connection.join(app.prefs.serverUrl, code, app.prefs.playerName)
                }
                EventAction.JOIN_ROUND -> {
                    val code = ev.currentRound?.code ?: return@ActionBar
                    app.activeEvent.value = ev
                    app.connection.join(app.prefs.serverUrl, code, app.prefs.playerName)
                }
            }
        })
    }

    if (confirmCancel && ev != null) {
        AlertDialog(
            onDismissRequest = { confirmCancel = false },
            title = { Text("Cancel this event?") },
            text = { Text("Everyone who RSVPed will see it as cancelled. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmCancel = false
                    act { event = app.api.cancelEvent(ev.id) }
                }) { Text("Cancel event", color = SeekerRed) }
            },
            dismissButton = { TextButton(onClick = { confirmCancel = false }) { Text("Keep it") } },
        )
    }
}

private enum class EventAction { RSVP, CANCEL_RSVP, CHECK_IN, START_ROUND, JOIN_ROUND }

@Composable
private fun ActionBar(ev: EventDetail, busy: Boolean, onAction: (EventAction) -> Unit) {
    val now = System.currentTimeMillis()
    val open = ev.status == EventStatus.UPCOMING || ev.status == EventStatus.LIVE
    val checkInOpen = open && now >= ev.startsAt - CHECKIN_OPENS_BEFORE_MS
    Column(
        Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        when {
            ev.status == EventStatus.CANCELLED -> Text("This event was cancelled.", color = SeekerRed)
            ev.status == EventStatus.ENDED -> Text("This event has ended — see the final standings in Ranking.")
            else -> {
                val round = ev.currentRound
                if (round != null && ev.me.going) {
                    BigButton(
                        "Join round ${ev.rounds.size + 1} now (${round.players} in)",
                        onClick = { onAction(EventAction.JOIN_ROUND) },
                        enabled = !busy,
                        color = HiderTeal,
                    )
                }
                if (!ev.me.going) {
                    BigButton(
                        if (ev.going >= ev.capacity) "Event is full" else "I'm in! (${ev.going}/${ev.capacity} going)",
                        onClick = { onAction(EventAction.RSVP) },
                        enabled = !busy && ev.going < ev.capacity,
                    )
                } else if (!ev.me.checkedIn && checkInOpen) {
                    BigButton("I'm here — check in", onClick = { onAction(EventAction.CHECK_IN) }, enabled = !busy, color = Marigold)
                }
                if (ev.me.isHost && round == null) {
                    BigButton(
                        "Start round ${ev.rounds.size + 1} (${ev.checkedIn} checked in)",
                        onClick = { onAction(EventAction.START_ROUND) },
                        enabled = !busy,
                        color = Saffron,
                    )
                }
                if (ev.me.going && !ev.me.isHost) {
                    TextButton(onClick = { onAction(EventAction.CANCEL_RSVP) }, enabled = !busy) { Text("Can't make it any more") }
                }
            }
        }
    }
}

@Composable
private fun DetailsTab(ev: EventDetail) {
    val context = LocalContext.current
    var reminder by remember(ev.id) { mutableStateOf(Reminders.isScheduled(context, ev.id)) }
    var map by remember { mutableStateOf<MapView?>(null) }
    var layer by remember { mutableStateOf<MapLayer?>(null) }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionCard {
            if (ev.community.isNotBlank()) Text(ev.community.uppercase(), style = MaterialTheme.typography.labelLarge, color = Marigold)
            Text(ev.title, style = MaterialTheme.typography.headlineSmall)
            Text("Hosted by ${ev.hostName}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text("🗓  ${formatEventTime(ev.startsAt)} (${ev.durationMin / 60}h${if (ev.durationMin % 60 != 0) " ${ev.durationMin % 60}m" else ""})")
            if (ev.status == EventStatus.UPCOMING) {
                Text("Starts ${formatUntil(ev.startsAt - System.currentTimeMillis())}", color = HiderTeal)
            }
            if (ev.status == EventStatus.LIVE) Text("Happening now!", color = SeekerRed, fontWeight = FontWeight.Bold)
            Text("📍  ${ev.place.name}")
            Text("👥  ${ev.going}/${ev.capacity} going · ${ev.checkedIn} checked in")
            ev.sponsor?.let { Text("✨  Presented by ${it.name}", color = Marigold, fontWeight = FontWeight.SemiBold) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    val p = ev.place
                    val uri = Uri.parse("geo:${p.lat},${p.lng}?q=${p.lat},${p.lng}(${Uri.encode(p.name)})")
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                    } catch (_: Exception) {
                        Toast.makeText(context, "No maps app found.", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("Directions") }
                if (ev.visibility == "private") {
                    Text("Private · code ${ev.code}", modifier = Modifier.align(Alignment.CenterVertically), color = Marigold)
                }
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(18.dp)),
        ) {
            OsmMap(
                modifier = Modifier.fillMaxSize(),
                center = GeoPoint(ev.place.lat, ev.place.lng),
                zoom = 15.0,
                interactive = false,
                onReady = {
                    map = it
                    layer = MapLayer(it)
                },
            )
        }
        LaunchedEffect(map, ev.rules, ev.place) {
            val m = map ?: return@LaunchedEffect
            val overlays = buildList {
                val z = ev.rules.zone
                if (ev.rules.zoneEnabled && z != null) add(zoneCircle(m, z.lat, z.lng, z.radiusM.toDouble(), Saffron))
                add(dotMarker(m, GeoPoint(ev.place.lat, ev.place.lng), Marigold, "Meet here"))
            }
            layer?.replace(overlays)
        }
        if (ev.me.going && ev.status == EventStatus.UPCOMING) {
            SectionCard {
                SwitchRow("Remind me before it starts", "1 hour before (or 10 minutes if it's sooner)", reminder) { on ->
                    if (on) Reminders.schedule(context, ev.id, ev.title, ev.startsAt) else Reminders.cancel(context, ev.id)
                    reminder = Reminders.isScheduled(context, ev.id)
                }
            }
        }
        if (ev.announcements.isNotEmpty()) {
            SectionCard {
                Text("📣 From the host", style = MaterialTheme.typography.titleMedium, color = Saffron)
                for (a in ev.announcements.asReversed()) {
                    Text(a.text, style = MaterialTheme.typography.bodyLarge)
                    Text(formatEventTime(a.at), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (ev.description.isNotBlank()) {
            SectionCard {
                Text("About", style = MaterialTheme.typography.titleMedium)
                Text(ev.description, style = MaterialTheme.typography.bodyMedium)
            }
        }
        SectionCard {
            Text("Game rules", style = MaterialTheme.typography.titleMedium)
            RulesSummary(ev.rules)
            if (ev.rules.dhappaEnabled) {
                Text("Dhappa! is on: reach the base to free caught hiders.", color = Marigold, style = MaterialTheme.typography.bodySmall)
            }
        }
        SectionCard {
            Text("Bring along", style = MaterialTheme.typography.titleMedium)
            Text(
                "• A charged phone (and a power bank)\n• Running shoes and water\n• Location and notifications switched on\n" +
                    "• Agree on safe zones: no roads, no private property, look out for younger players",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun ChatTab(ev: EventDetail, messages: List<ChatMessage>, onSend: (String, Boolean) -> Unit) {
    if (!ev.me.going) {
        Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Text("RSVP to join the group chat.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    var input by rememberSaveable { mutableStateOf("") }
    var asAnnouncement by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }
    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.weight(1f),
            state = listState,
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (messages.isEmpty()) item { Text("Say hi to the group! 👋", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(messages, key = { it.id }) { ChatBubble(it) }
        }
        if (ev.me.isHost) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp)) {
                Checkbox(checked = asAnnouncement, onCheckedChange = { asAnnouncement = it })
                Text("Send as a pinned announcement", style = MaterialTheme.typography.bodySmall)
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it.take(500) },
                placeholder = { Text("Message the group") },
                modifier = Modifier.weight(1f),
                maxLines = 4,
            )
            IconButton(
                onClick = {
                    onSend(input.trim(), asAnnouncement)
                    input = ""
                    asAnnouncement = false
                },
                enabled = input.isNotBlank(),
            ) { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Saffron) }
        }
    }
}

@Composable
private fun ChatBubble(m: ChatMessage) {
    when (m.kind) {
        "system" -> Text(
            m.text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        )
        else -> SectionCard {
            Row {
                Text(
                    if (m.kind == "announcement") "📣 ${m.name} (host)" else m.name,
                    fontWeight = FontWeight.Bold,
                    color = if (m.kind == "announcement") Saffron else HiderTeal,
                    modifier = Modifier.weight(1f),
                )
                Text(formatEventTime(m.at).substringAfter("· "), style = MaterialTheme.typography.labelSmall)
            }
            Text(m.text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun PeopleTab(ev: EventDetail, onHostCheckIn: (String) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Text(
                "${ev.going} going · ${ev.checkedIn} checked in",
                style = MaterialTheme.typography.titleMedium,
            )
        }
        items(ev.attendees, key = { it.userId }) { a ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(a.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                if (a.isHost) {
                    Icon(Icons.Filled.Star, contentDescription = "Host", tint = Marigold)
                    Spacer(Modifier.width(8.dp))
                }
                when {
                    a.checkedIn -> Text("✓ here", color = HiderTeal, fontWeight = FontWeight.Bold)
                    ev.me.isHost -> TextButton(onClick = { onHostCheckIn(a.userId) }) { Text("Check in") }
                    else -> Text("coming", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun RankingTab(ev: EventDetail) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (ev.leaderboard.isEmpty()) {
            item {
                SectionCard {
                    Text("No rounds played yet.", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Points: hiders get 1 per minute survived after the head start and +10 if never found. " +
                            "Seekers get 5 per catch and +5 when the seekers win. Every Dhappa rescue is +2.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        items(ev.leaderboard.withIndex().toList(), key = { (it.value.userId ?: it.value.name) }) { (i, row) ->
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        when (i) {
                            0 -> "🥇"
                            1 -> "🥈"
                            2 -> "🥉"
                            else -> "#${i + 1}"
                        },
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(row.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${row.rounds} round(s) · ${row.catches} catches · found ${row.timesFound}× · hid ${formatClock(row.survivedMs)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text("${row.points}", style = MaterialTheme.typography.headlineSmall, color = Saffron)
                    Text(" pts", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        if (ev.rounds.isNotEmpty()) {
            item { Text("Rounds", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp)) }
            items(ev.rounds, key = { "r${it.number}" }) { r ->
                Text(
                    "Round ${r.number}: " + when (r.winner) {
                        Role.SEEKER -> "seekers won"
                        Role.HIDER -> "hiders won"
                        else -> "ended early"
                    } + " · ${r.players} players",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

private fun shareInvite(context: android.content.Context, ev: EventDetail) {
    val text = buildString {
        append("🙈 ${ev.title}\n")
        append("🗓 ${formatEventTime(ev.startsAt)}\n")
        append("📍 ${ev.place.name}\n")
        ev.sponsor?.let { append("✨ Presented by ${it.name}\n") }
        append("\nReal-life hide & seek with Chhupan Chhupai. In the app: Events → \"Have an event code?\" → ${ev.code}")
    }
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(send, "Invite friends"))
}
