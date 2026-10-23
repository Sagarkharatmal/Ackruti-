package com.ssk.chhupanchhupai.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssk.chhupanchhupai.BuildConfig
import com.ssk.chhupanchhupai.ChhupanApp
import com.ssk.chhupanchhupai.R
import com.ssk.chhupanchhupai.data.GameSettings
import com.ssk.chhupanchhupai.data.Prefs
import com.ssk.chhupanchhupai.ui.components.AskOnce
import com.ssk.chhupanchhupai.ui.components.BigButton
import com.ssk.chhupanchhupai.ui.components.ScreenScaffold
import com.ssk.chhupanchhupai.ui.components.SectionCard
import com.ssk.chhupanchhupai.ui.components.rememberGamePermissions
import com.ssk.chhupanchhupai.ui.theme.HiderTeal
import com.ssk.chhupanchhupai.ui.theme.Marigold
import com.ssk.chhupanchhupai.ui.theme.Saffron
import com.ssk.chhupanchhupai.ui.theme.SeekerRed
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    serverMissing: Boolean,
    onCreate: () -> Unit,
    onJoin: () -> Unit,
    onHowTo: () -> Unit,
    onSettings: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(32.dp))
        AppLogo(Modifier.size(120.dp))
        Spacer(Modifier.height(20.dp))
        Text("Chhupan", fontSize = 44.sp, fontWeight = FontWeight.Black, color = Saffron, lineHeight = 46.sp)
        Text("Chhupai", fontSize = 44.sp, fontWeight = FontWeight.Black, color = Marigold, lineHeight = 46.sp)
        Spacer(Modifier.height(12.dp))
        Text(
            "Real-life hide-and-seek for the whole mohalla.\nHiders hide. Seekers hunt. The phone is the referee.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(36.dp))
        if (serverMissing) {
            SectionCard {
                Text("One-time setup needed", style = MaterialTheme.typography.titleMedium, color = Marigold)
                Text(
                    "Add your game server address in Settings before creating or joining a game.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                TextButton(onClick = onSettings) { Text("Open Settings") }
            }
            Spacer(Modifier.height(16.dp))
        }
        BigButton("Create a game", onCreate, color = Saffron)
        Spacer(Modifier.height(12.dp))
        BigButton("Join with a code", onJoin, color = HiderTeal)
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onHowTo) { Text("How to play") }
            OutlinedButton(onClick = onSettings) { Text("Settings") }
        }
        Spacer(Modifier.height(32.dp))
        Text(
            "Made by SSK · v${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun NameField(name: String, onName: (String) -> Unit) {
    OutlinedTextField(
        value = name,
        onValueChange = { onName(it.take(20)) },
        label = { Text("Your name") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
fun JoinScreen(busy: Boolean, onBack: () -> Unit, onJoin: (code: String, name: String) -> Unit) {
    val app = LocalContext.current.applicationContext as ChhupanApp
    var name by rememberSaveable { mutableStateOf(app.prefs.playerName) }
    var code by rememberSaveable { mutableStateOf("") }
    val permissions = rememberGamePermissions()
    AskOnce(permissions)
    ScreenScaffold(
        title = "Join a game",
        onBack = onBack,
        bottomBar = {
            BigButton(
                if (busy) "Joining…" else "Join game",
                onClick = { onJoin(code, name.trim()) },
                enabled = !busy && code.length == 6 && name.isNotBlank(),
                color = HiderTeal,
            )
        },
    ) {
        Text(
            "Ask the host for the 6-character code shown in their lobby.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        NameField(name) { name = it }
        OutlinedTextField(
            value = code,
            onValueChange = { raw -> code = raw.uppercase().filter { it.isLetterOrDigit() }.take(6) },
            label = { Text("Game code") },
            singleLine = true,
            textStyle = MaterialTheme.typography.headlineMedium.copy(letterSpacing = 6.sp),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                keyboardType = KeyboardType.Ascii,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        if (busy) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
    }
}

@Composable
fun CreateScreen(busy: Boolean, onBack: () -> Unit, onCreate: (name: String, settings: GameSettings) -> Unit) {
    val app = LocalContext.current.applicationContext as ChhupanApp
    var name by rememberSaveable { mutableStateOf(app.prefs.playerName) }
    // Start from the last rules used, but place the zone fresh wherever the host is now.
    var settings by remember { mutableStateOf(app.prefs.lastSettings.copy(zone = null)) }
    val permissions = rememberGamePermissions()
    AskOnce(permissions)
    val zoneReady = !settings.zoneEnabled || settings.zone != null
    // The map is kept out of the scrolling part so dragging it never scrolls the page.
    ScreenScaffold(
        title = "Create a game",
        onBack = onBack,
        scroll = false,
        bottomBar = {
            BigButton(
                if (busy) "Creating…" else "Create game",
                onClick = { onCreate(name.trim(), settings) },
                enabled = !busy && name.isNotBlank() && zoneReady,
            )
        },
    ) {
        NameField(name) { name = it }
        if (settings.zoneEnabled) {
            ZonePicker(settings, { settings = it }, permissions.granted)
            if (!permissions.granted) {
                TextButton(onClick = permissions.request) { Text("Allow location to centre the map on you") }
            }
        }
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RulesForm(settings) { settings = it }
            Text(
                "Tip: 3 or more players makes the best game. About one seeker for every four players.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as ChhupanApp
    val scope = rememberCoroutineScope()
    var name by rememberSaveable { mutableStateOf(app.prefs.playerName) }
    var url by rememberSaveable { mutableStateOf(app.prefs.serverUrl) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var testing by remember { mutableStateOf(false) }
    ScreenScaffold(
        title = "Settings",
        onBack = {
            app.prefs.playerName = name
            app.prefs.serverUrl = url
            onBack()
        },
    ) {
        SectionCard {
            Text("You", style = MaterialTheme.typography.titleMedium)
            NameField(name) {
                name = it
                app.prefs.playerName = it
            }
        }
        SectionCard {
            Text("Game server", style = MaterialTheme.typography.titleMedium)
            Text(
                "Everyone in a game must use the same server. Paste the address of your deployed server, " +
                    "e.g. my-chhupan.onrender.com, or a laptop on the same Wi-Fi, e.g. 192.168.1.20:8080.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = url,
                onValueChange = {
                    url = it.trim()
                    testResult = null
                },
                label = { Text("Server address") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth(),
            )
            val normalised = Prefs.normaliseServerUrl(url)
            if (normalised.isNotEmpty() && normalised != url) {
                Text("Will connect to: $normalised", style = MaterialTheme.typography.bodySmall)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    enabled = !testing && normalised.isNotEmpty(),
                    onClick = {
                        app.prefs.serverUrl = url
                        testing = true
                        testResult = null
                        scope.launch {
                            testResult = app.connection.testServer(normalised) ?: "OK"
                            testing = false
                        }
                    },
                ) { Text("Save & test") }
                if (testing) CircularProgressIndicator(Modifier.size(24.dp))
            }
            testResult?.let {
                Text(
                    if (it == "OK") "Connected! The server is working." else it,
                    color = if (it == "OK") HiderTeal else SeekerRed,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        SectionCard {
            Text("About", style = MaterialTheme.typography.titleMedium)
            Text("Chhupan Chhupai v${BuildConfig.VERSION_NAME} · Made by SSK", style = MaterialTheme.typography.bodyMedium)
            Text(
                "Your location is only shared while you are in a game, only with players in that game, and " +
                    "only as the rules allow (hiders are never shown live unless they leave the zone). " +
                    "Map data © OpenStreetMap contributors.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun HowToPlayScreen(onBack: () -> Unit) {
    ScreenScaffold(title = "How to play", onBack = onBack) {
        Rule("1. Host & join", "One friend creates a game, drags the map to set the play zone and picks the round length, head start and ping timer. Everyone else joins with the 6-character code and picks Hider, Seeker or Random.")
        Rule("2. Head start", "When the host starts, hiders run and hide inside the zone. Seekers wait — no peeking, the app hides everything from them until the head start ends.")
        Rule("3. Pings", "Every few minutes a ping shows seekers where each hider was at that moment. Hiders feel a buzz when they've been pinged — time to move!")
        Rule("4. Stay in the zone", "Step outside the zone and you get a short warning. Stay out past the grace period and seekers see you live until you come back in.")
        Rule("5. Tag!", "Seekers catch hiders by tagging them in person. The seeker taps Tag and the hider confirms on their phone (or a hider taps \"I've been caught\"). Caught hiders become spectators and can watch everyone.")
        Rule("6. Winning", "Seekers win if they catch everyone. Hiders who are still free when the clock runs out win the round. Then hit Play again!")
        Rule("Stay safe", "Play in places you know, stay off roads and private property, keep an eye on younger players, and agree on a meeting point for the end of the round.")
    }
}

@Composable
private fun Rule(title: String, body: String) {
    SectionCard {
        Text(title, style = MaterialTheme.typography.titleMedium, color = Saffron)
        Text(body, style = MaterialTheme.typography.bodyMedium)
    }
}

/** The app mark: the peeking kid on a saffron disc. */
@Composable
fun AppLogo(modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.Box(
        modifier
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(Saffron),
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
