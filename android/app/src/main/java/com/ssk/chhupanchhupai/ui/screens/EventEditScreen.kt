package com.ssk.chhupanchhupai.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.ssk.chhupanchhupai.ChhupanApp
import com.ssk.chhupanchhupai.data.ApiException
import com.ssk.chhupanchhupai.data.EventDetail
import com.ssk.chhupanchhupai.data.EventInput
import com.ssk.chhupanchhupai.data.Place
import com.ssk.chhupanchhupai.data.Sponsor
import com.ssk.chhupanchhupai.data.Zone
import com.ssk.chhupanchhupai.ui.components.AskOnce
import com.ssk.chhupanchhupai.ui.components.BigButton
import com.ssk.chhupanchhupai.ui.components.LabeledSlider
import com.ssk.chhupanchhupai.ui.components.ScreenScaffold
import com.ssk.chhupanchhupai.ui.components.SectionCard
import com.ssk.chhupanchhupai.ui.components.SwitchRow
import com.ssk.chhupanchhupai.ui.components.rememberGamePermissions
import com.ssk.chhupanchhupai.ui.formatEventTime
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.math.roundToInt

/** Host a new event, or edit one (`existing`). Step 1: where. Step 2: details + rules. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventEditScreen(existing: EventDetail?, onBack: () -> Unit, onSaved: (String) -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as ChhupanApp
    val scope = rememberCoroutineScope()
    val permissions = rememberGamePermissions()
    AskOnce(permissions)

    var step by rememberSaveable { mutableIntStateOf(if (existing == null) 1 else 2) }
    var title by rememberSaveable { mutableStateOf(existing?.title ?: "") }
    var community by rememberSaveable { mutableStateOf(existing?.community ?: "") }
    var description by rememberSaveable { mutableStateOf(existing?.description ?: "") }
    var sponsor by rememberSaveable { mutableStateOf(existing?.sponsor?.name ?: "") }
    var placeName by rememberSaveable { mutableStateOf(existing?.place?.name ?: "") }
    var startsAt by rememberSaveable { mutableLongStateOf(existing?.startsAt ?: defaultStart()) }
    var durationMin by rememberSaveable { mutableIntStateOf(existing?.durationMin ?: 120) }
    var capacity by rememberSaveable { mutableIntStateOf(existing?.capacity ?: 30) }
    var isPublic by rememberSaveable { mutableStateOf(existing?.visibility != "private") }
    // The play zone's centre is the meeting point.
    var rules by remember {
        mutableStateOf(
            if (existing != null) {
                existing.rules.copy(zone = existing.rules.zone ?: Zone(existing.place.lat, existing.place.lng, DEFAULT_RADIUS_M))
            } else {
                app.prefs.lastSettings.copy(zone = null, zoneEnabled = true)
            },
        )
    }
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    if (step == 1) {
        ScreenScaffold(
            title = if (existing == null) "Host an event · 1/2" else "Meeting point",
            onBack = { if (existing == null) onBack() else step = 2 },
            scroll = false,
            bottomBar = {
                BigButton("Next: details", onClick = { step = 2 }, enabled = rules.zone != null && placeName.isNotBlank())
            },
        ) {
            Text(
                "Drag the map so the crosshair is on the meeting point. The circle is the play zone around it.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ZonePicker(rules.copy(zoneEnabled = true), { rules = it.copy(zoneEnabled = rules.zoneEnabled) }, permissions.granted)
            OutlinedTextField(
                value = placeName,
                onValueChange = { placeName = it.take(80) },
                label = { Text("Meeting point name (e.g. Shivaji Park, Gate 5)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth(),
            )
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                LabeledSlider(
                    label = "Zone size",
                    valueText = "${((rules.zone?.radiusM ?: DEFAULT_RADIUS_M) * 2)} m across",
                    value = (rules.zone?.radiusM ?: DEFAULT_RADIUS_M).toFloat(),
                    range = 50f..1500f,
                    steps = 28,
                    onChange = { v -> rules.zone?.let { z -> rules = rules.copy(zone = z.copy(radiusM = (v / 50f).roundToInt() * 50)) } },
                )
            }
        }
        return
    }

    val zone = rules.zone
    ScreenScaffold(
        title = if (existing == null) "Host an event · 2/2" else "Edit event",
        onBack = { if (existing == null) step = 1 else onBack() },
        bottomBar = {
            BigButton(
                when {
                    saving -> "Saving…"
                    existing == null -> "Publish event"
                    else -> "Save changes"
                },
                enabled = !saving && title.trim().length >= 3 && zone != null && placeName.isNotBlank(),
                onClick = {
                    if (zone == null) return@BigButton
                    val input = EventInput(
                        title = title.trim(),
                        description = description.trim(),
                        community = community.trim(),
                        startsAt = startsAt,
                        durationMin = durationMin,
                        place = Place(zone.lat, zone.lng, placeName.trim()),
                        capacity = capacity,
                        visibility = if (isPublic) "public" else "private",
                        sponsor = sponsor.trim().takeIf { it.isNotEmpty() }?.let { Sponsor(it) },
                        rules = rules,
                    )
                    scope.launch {
                        saving = true
                        try {
                            if (app.prefs.playerName.isBlank()) throw ApiException("Set your name in Settings first.")
                            val saved = if (existing == null) app.api.createEvent(input) else app.api.updateEvent(existing.id, input)
                            app.prefs.lastSettings = rules
                            onSaved(saved.id)
                        } catch (e: ApiException) {
                            Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
                        } finally {
                            saving = false
                        }
                    }
                },
            )
        },
    ) {
        SectionCard {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it.take(60) },
                label = { Text("Event name (e.g. Friday Night Chhupan Chhupai)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = community,
                onValueChange = { community = it.take(40) },
                label = { Text("Club / college / society (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it.take(1000) },
                label = { Text("What to expect, what to bring (optional)") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = sponsor,
                onValueChange = { sponsor = it.take(40) },
                label = { Text("Sponsor / partner (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        SectionCard {
            Text("Where", style = MaterialTheme.typography.titleMedium)
            Text("📍 $placeName", style = MaterialTheme.typography.bodyLarge)
            OutlinedButton(onClick = { step = 1 }) { Text("Change on the map") }
        }
        SectionCard {
            Text("When", style = MaterialTheme.typography.titleMedium)
            Text(formatEventTime(startsAt), style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showDate = true }) { Text("Change date") }
                OutlinedButton(onClick = { showTime = true }) { Text("Change time") }
            }
            LabeledSlider(
                label = "Event length",
                valueText = "${durationMin / 60} h${if (durationMin % 60 != 0) " ${durationMin % 60} min" else ""}",
                value = durationMin.toFloat(),
                range = 30f..360f,
                steps = 10,
                onChange = { durationMin = ((it / 30f).roundToInt() * 30).coerceIn(30, 360) },
            )
        }
        SectionCard {
            Text("Who", style = MaterialTheme.typography.titleMedium)
            LabeledSlider(
                label = "Max people",
                valueText = "$capacity",
                value = capacity.toFloat(),
                range = 2f..200f,
                steps = 0,
                onChange = { capacity = it.roundToInt() },
            )
            SwitchRow(
                "Public event",
                if (isPublic) "Anyone nearby can find it in the app." else "Hidden: only people with the code can see it.",
                isPublic,
            ) { isPublic = it }
        }
        Text("Rounds", style = MaterialTheme.typography.titleMedium)
        Text(
            "Every round you start at the event uses these rules (you can change them before each round).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        RulesForm(rules) { rules = it }
    }

    if (showDate) {
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = Instant.ofEpochMilli(startsAt).atZone(ZoneId.systemDefault()).toLocalDate()
                .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let { utcMidnight ->
                        val date = Instant.ofEpochMilli(utcMidnight).atZone(ZoneOffset.UTC).toLocalDate()
                        startsAt = combine(date, localTime(startsAt))
                    }
                    showDate = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDate = false }) { Text("Cancel") } },
        ) {
            DatePicker(state = dateState)
        }
    }
    if (showTime) {
        val t = localTime(startsAt)
        val timeState = rememberTimePickerState(initialHour = t.hour, initialMinute = t.minute, is24Hour = false)
        AlertDialog(
            onDismissRequest = { showTime = false },
            title = { Text("Start time") },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(onClick = {
                    val date = Instant.ofEpochMilli(startsAt).atZone(ZoneId.systemDefault()).toLocalDate()
                    startsAt = combine(date, LocalTime.of(timeState.hour, timeState.minute))
                    showTime = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showTime = false }) { Text("Cancel") } },
        )
    }
}

/** Default start: tomorrow at 7 PM local time. */
private fun defaultStart(): Long =
    combine(LocalDate.now().plusDays(1), LocalTime.of(19, 0))

private fun localTime(ms: Long): LocalTime = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalTime()

private fun combine(date: LocalDate, time: LocalTime): Long =
    date.atTime(time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
