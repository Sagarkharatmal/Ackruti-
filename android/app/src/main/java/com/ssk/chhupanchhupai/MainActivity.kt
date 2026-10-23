package com.ssk.chhupanchhupai

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssk.chhupanchhupai.data.GameConnection
import com.ssk.chhupanchhupai.data.Phase
import com.ssk.chhupanchhupai.location.GameLocationService
import com.ssk.chhupanchhupai.ui.components.CenteredMessage
import com.ssk.chhupanchhupai.ui.components.rememberGamePermissions
import com.ssk.chhupanchhupai.data.EventDetail
import com.ssk.chhupanchhupai.ui.screens.CreateScreen
import com.ssk.chhupanchhupai.ui.screens.EventDetailScreen
import com.ssk.chhupanchhupai.ui.screens.EventEditScreen
import com.ssk.chhupanchhupai.ui.screens.EventsScreen
import com.ssk.chhupanchhupai.ui.screens.GameScreen
import com.ssk.chhupanchhupai.ui.screens.HomeScreen
import com.ssk.chhupanchhupai.ui.screens.HowToPlayScreen
import com.ssk.chhupanchhupai.ui.screens.JoinScreen
import com.ssk.chhupanchhupai.ui.screens.LobbyScreen
import com.ssk.chhupanchhupai.ui.screens.ResultsScreen
import com.ssk.chhupanchhupai.ui.screens.SettingsScreen
import com.ssk.chhupanchhupai.ui.theme.ChhupanTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) handleIntent(intent)
        setContent {
            ChhupanTheme {
                AppRoot()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent?.getStringExtra(EXTRA_EVENT_ID)?.let { (application as ChhupanApp).openEventRequest.value = it }
    }

    companion object {
        const val EXTRA_EVENT_ID = "eventId"
    }

    override fun onStart() {
        super.onStart()
        // Back from the background (or a fresh start): rejoin the game we were in.
        (application as ChhupanApp).connection.resumeSavedSession()
    }
}

private enum class Menu { HOME, CREATE, JOIN, SETTINGS, HOW_TO, EVENTS, EVENT, EVENT_EDIT, EVENT_NEW }

@Composable
private fun AppRoot() {
    val app = LocalContext.current.applicationContext as ChhupanApp
    val context = LocalContext.current
    val conn by app.connection.state.collectAsStateWithLifecycle()
    var menu by rememberSaveable { mutableStateOf(Menu.HOME) }
    var eventId by rememberSaveable { mutableStateOf<String?>(null) }
    var editingEvent by remember { mutableStateOf<EventDetail?>(null) }
    val openRequest by app.openEventRequest.collectAsStateWithLifecycle()
    var serverUrl by remember { mutableStateOf(app.prefs.serverUrl) }
    val snackbar = remember { SnackbarHostState() }
    val permissions = rememberGamePermissions()

    LaunchedEffect(Unit) {
        launch {
            app.connection.notices.collect {
                snackbar.currentSnackbarData?.dismiss()
                launch { snackbar.showSnackbar(it.text) }
            }
        }
        app.connection.errors.collect {
            snackbar.currentSnackbarData?.dismiss()
            launch { snackbar.showSnackbar(it) }
        }
    }

    // GPS + keep-alive service runs for as long as we hold a seat in a game.
    LaunchedEffect(conn.hasSession, permissions.granted) {
        if (conn.hasSession && permissions.granted) GameLocationService.start(context)
    }

    // A reminder notification asked to open an event.
    LaunchedEffect(openRequest) {
        openRequest?.let {
            eventId = it
            menu = Menu.EVENT
            app.openEventRequest.value = null
        }
    }

    // Leaving an event round takes you back to that event's page.
    val leave = {
        val fromEvent = conn.game?.eventId
        app.connection.leave()
        if (fromEvent != null) {
            eventId = fromEvent
            menu = Menu.EVENT
        } else {
            menu = Menu.HOME
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            val game = conn.game
            when {
                game != null -> when (game.phase) {
                    Phase.LOBBY -> LobbyScreen(game, permissions, onLeave = leave)
                    Phase.HEADSTART, Phase.HUNT -> GameScreen(game, permissions, onLeave = leave)
                    else -> ResultsScreen(game, onLeave = leave)
                }

                conn.hasSession -> CenteredMessage(
                    title = if (conn.link == GameConnection.Link.RECONNECTING) "Reconnecting…" else "Joining your game…",
                    body = "Keep the app open. If you have no signal you'll rejoin automatically when it comes back.",
                ) {
                    CircularProgressIndicator()
                    OutlinedButton(onClick = leave, modifier = Modifier.padding(top = 24.dp)) { Text("Leave game") }
                }

                else -> {
                    val back = {
                        menu = when (menu) {
                            Menu.EVENT, Menu.EVENT_NEW -> Menu.EVENTS
                            Menu.EVENT_EDIT -> Menu.EVENT
                            else -> Menu.HOME
                        }
                    }
                    if (menu != Menu.HOME) BackHandler { back() }
                    when (menu) {
                        Menu.HOME -> HomeScreen(
                            serverMissing = serverUrl.isBlank(),
                            onCreate = { menu = Menu.CREATE },
                            onJoin = { menu = Menu.JOIN },
                            onEvents = { menu = Menu.EVENTS },
                            onHowTo = { menu = Menu.HOW_TO },
                            onSettings = { menu = Menu.SETTINGS },
                        )

                        Menu.CREATE -> CreateScreen(conn.busy, onBack = { menu = Menu.HOME }) { name, settings ->
                            app.prefs.playerName = name
                            app.prefs.lastSettings = settings
                            app.connection.create(app.prefs.serverUrl, name, settings)
                        }

                        Menu.JOIN -> JoinScreen(conn.busy, onBack = { menu = Menu.HOME }) { code, name ->
                            app.prefs.playerName = name
                            app.connection.join(app.prefs.serverUrl, code, name)
                        }

                        Menu.SETTINGS -> SettingsScreen(onBack = {
                            serverUrl = app.prefs.serverUrl
                            menu = Menu.HOME
                        })

                        Menu.HOW_TO -> HowToPlayScreen(onBack = { menu = Menu.HOME })

                        Menu.EVENTS -> EventsScreen(
                            onBack = { menu = Menu.HOME },
                            onOpen = {
                                eventId = it
                                menu = Menu.EVENT
                            },
                            onHost = { menu = Menu.EVENT_NEW },
                        )

                        Menu.EVENT -> {
                            val id = eventId
                            if (id == null) {
                                menu = Menu.EVENTS
                            } else {
                                EventDetailScreen(id, onBack = { back() }, onEdit = {
                                    editingEvent = it
                                    menu = Menu.EVENT_EDIT
                                })
                            }
                        }

                        Menu.EVENT_NEW, Menu.EVENT_EDIT -> EventEditScreen(
                            existing = if (menu == Menu.EVENT_EDIT) editingEvent else null,
                            onBack = { back() },
                            onSaved = {
                                eventId = it
                                menu = Menu.EVENT
                            },
                        )
                    }
                }
            }
        }
    }
}
