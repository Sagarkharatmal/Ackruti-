package com.ssk.chhupanchhupai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssk.chhupanchhupai.ChhupanApp
import com.ssk.chhupanchhupai.data.ApiException
import com.ssk.chhupanchhupai.data.EventStatus
import com.ssk.chhupanchhupai.data.EventSummary
import com.ssk.chhupanchhupai.data.MyEvents
import com.ssk.chhupanchhupai.ui.components.BigButton
import com.ssk.chhupanchhupai.ui.components.SectionCard
import com.ssk.chhupanchhupai.ui.components.rememberGamePermissions
import com.ssk.chhupanchhupai.ui.formatDay
import com.ssk.chhupanchhupai.ui.formatEventTime
import com.ssk.chhupanchhupai.ui.formatMonth
import com.ssk.chhupanchhupai.ui.theme.HiderTeal
import com.ssk.chhupanchhupai.ui.theme.Marigold
import com.ssk.chhupanchhupai.ui.theme.Saffron
import com.ssk.chhupanchhupai.ui.theme.SeekerRed

private const val NEARBY_KM = 50

/** Discover public events nearby, see your own, open one by code, or host one. */
@Composable
fun EventsScreen(onBack: () -> Unit, onOpen: (String) -> Unit, onHost: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as ChhupanApp
    val permissions = rememberGamePermissions()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var nearby by remember { mutableStateOf<List<EventSummary>?>(null) }
    var mine by remember { mutableStateOf<MyEvents?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableIntStateOf(0) }
    var name by rememberSaveable { mutableStateOf(app.prefs.playerName) }
    var code by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(refreshKey, tab, permissions.granted) {
        loading = true
        error = null
        try {
            if (tab == 0) {
                val here = if (permissions.granted) currentLocation(context, timeoutMs = 4000) else null
                nearby = app.api.listEvents(here?.latitude, here?.longitude, if (here != null) NEARBY_KM else null)
            } else if (app.prefs.playerName.isNotBlank()) {
                mine = app.api.myEvents()
            }
        } catch (e: ApiException) {
            error = e.message
        } finally {
            loading = false
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                Text("Chhupan Chhupai Nights", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                IconButton(onClick = { refreshKey++ }) { Icon(Icons.Filled.Refresh, contentDescription = "Refresh") }
            }
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Near you") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("My events") })
            }
            LazyColumn(
                Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (app.prefs.playerName.isBlank()) {
                    item {
                        SectionCard {
                            Text("What should people call you?", style = MaterialTheme.typography.titleMedium)
                            NameField(name) { name = it }
                            BigButton("Save", onClick = {
                                app.prefs.playerName = name.trim()
                                refreshKey++
                            }, enabled = name.isNotBlank())
                        }
                    }
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = code,
                            onValueChange = { raw -> code = raw.uppercase().filter { it.isLetterOrDigit() }.take(6) },
                            label = { Text("Have an event code?") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedButton(onClick = { onOpen(code) }, enabled = code.length == 6) { Text("Open") }
                    }
                }
                if (loading) item { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
                error?.let { item { Text(it, color = SeekerRed) } }
                if (tab == 0) {
                    val list = nearby.orEmpty()
                    if (!loading && nearby != null && list.isEmpty()) {
                        item { EmptyEvents("No public events nearby yet. Be the first — host one for your mohalla, college or run club!") }
                    }
                    items(list, key = { it.id }) { EventCard(it) { onOpen(it.id) } }
                } else {
                    val m = mine
                    if (m != null) {
                        if (m.hosting.isEmpty() && m.attending.isEmpty()) item { EmptyEvents("You haven't joined any events yet.") }
                        if (m.hosting.isNotEmpty()) item { SectionTitle("Hosting") }
                        items(m.hosting, key = { "h" + it.id }) { EventCard(it) { onOpen(it.id) } }
                        if (m.attending.isNotEmpty()) item { SectionTitle("Going") }
                        items(m.attending, key = { "a" + it.id }) { EventCard(it) { onOpen(it.id) } }
                    }
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = onHost,
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text("Host an event") },
            containerColor = Saffron,
            contentColor = MaterialTheme.colorScheme.background,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = Marigold)
}

@Composable
private fun EmptyEvents(text: String) {
    SectionCard { Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
}

@Composable
fun EventCard(e: EventSummary, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(
                Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Saffron.copy(alpha = 0.15f)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(formatDay(e.startsAt), fontSize = 26.sp, fontWeight = FontWeight.Black, color = Saffron)
                Text(formatMonth(e.startsAt), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Saffron)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        e.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    StatusChip(e.status)
                }
                Text(
                    listOf(e.community, "by ${e.hostName}").filter { it.isNotBlank() }.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(formatEventTime(e.startsAt), style = MaterialTheme.typography.bodyMedium)
                Text(
                    "📍 ${e.place.name}" + (e.distanceKm?.let { " · $it km" } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row {
                    Text("${e.going}/${e.capacity} going", style = MaterialTheme.typography.labelMedium, color = HiderTeal)
                    e.sponsor?.let {
                        Text(" · with ${it.name}", style = MaterialTheme.typography.labelMedium, color = Marigold, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
fun StatusChip(status: String) {
    val (label, color) = when (status) {
        EventStatus.LIVE -> "LIVE" to SeekerRed
        EventStatus.ENDED -> "ENDED" to MaterialTheme.colorScheme.onSurfaceVariant
        EventStatus.CANCELLED -> "CANCELLED" to MaterialTheme.colorScheme.onSurfaceVariant
        else -> return
    }
    Spacer(Modifier.width(6.dp))
    Text(
        label,
        color = color,
        fontWeight = FontWeight.Black,
        fontSize = 11.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.18f))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}
