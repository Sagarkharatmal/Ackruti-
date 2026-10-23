package com.ssk.chhupanchhupai.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.math.min

/**
 * One WebSocket to the game server, shared by the whole app (UI + location
 * service). Reconnects automatically while the player is in a game and rejoins
 * with the saved token, so walking through a dead zone doesn't kick you out.
 */
class GameConnection(private val prefs: SessionStore, private val scope: CoroutineScope) {

    enum class Link { IDLE, CONNECTING, ONLINE, RECONNECTING }

    data class State(
        val link: Link = Link.IDLE,
        val game: GameState? = null,
        /** True while we hold a seat in a game (even if temporarily offline). */
        val hasSession: Boolean = false,
        /** True while a create/join request is waiting for the server. */
        val busy: Boolean = false,
    )

    private val _state = MutableStateFlow(State(hasSession = prefs.savedSession != null))
    val state: StateFlow<State> = _state.asStateFlow()

    private val _notices = MutableSharedFlow<Notice>(extraBufferCapacity = 64)
    val notices: SharedFlow<Notice> = _notices.asSharedFlow()

    private val _errors = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val errors: SharedFlow<String> = _errors.asSharedFlow()

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    private val lock = Any()
    private var socket: WebSocket? = null
    private var socketUrl: String? = null
    private var generation = 0
    private var pending: JsonObject? = null
    private var session: Session? = prefs.savedSession
    private var resuming = false
    private var reconnectJob: Job? = null
    private var attempts = 0

    @Volatile
    private var clockOffsetMs = 0L

    /** Current time on the server's clock (all game timestamps use it). */
    fun serverNow(): Long = System.currentTimeMillis() + clockOffsetMs

    /** Rejoins the game saved from last time, if there is one. */
    fun resumeSavedSession() {
        val s = synchronized(lock) { session } ?: return
        if (_state.value.link == Link.IDLE) open(s.serverUrl)
    }

    fun create(serverUrl: String, name: String, settings: GameSettings) {
        request(serverUrl, buildJsonObject {
            put("t", "create")
            put("name", name)
            put("settings", GameJson.encodeToJsonElement(GameSettings.serializer(), settings))
        })
    }

    fun join(serverUrl: String, code: String, name: String) {
        request(serverUrl, buildJsonObject {
            put("t", "join")
            put("code", code)
            put("name", name)
        })
    }

    fun setRolePreference(pref: String) = send(buildJsonObject { put("t", "setRole"); put("pref", pref) })

    fun updateSettings(settings: GameSettings) = send(buildJsonObject {
        put("t", "settings")
        put("settings", GameJson.encodeToJsonElement(GameSettings.serializer(), settings))
    })

    fun kick(playerId: String) = send(buildJsonObject { put("t", "kick"); put("playerId", playerId) })
    fun start() = send(buildJsonObject { put("t", "start") })
    fun tag(playerId: String) = send(buildJsonObject { put("t", "tag"); put("playerId", playerId) })
    fun endRound() = send(buildJsonObject { put("t", "end") })
    fun rematch() = send(buildJsonObject { put("t", "rematch") })

    fun replyToTag(requestId: String, accept: Boolean) = send(buildJsonObject {
        put("t", "tagReply")
        put("requestId", requestId)
        put("accept", accept)
    })

    fun reportCaught(byId: String?) = send(buildJsonObject {
        put("t", "caught")
        if (byId != null) put("byId", byId)
    })

    fun sendLocation(lat: Double, lng: Double, accuracyM: Float?) {
        if (_state.value.link != Link.ONLINE || _state.value.game == null) return
        send(buildJsonObject {
            put("t", "loc")
            put("lat", lat)
            put("lng", lng)
            if (accuracyM != null) put("acc", accuracyM.toDouble())
        })
    }

    /** Leaves the game for good (a hider leaving counts as out). */
    fun leave() {
        send(buildJsonObject { put("t", "leave") })
        endSession()
    }

    /** Checks that a server URL answers. Returns null on success or an error message. */
    suspend fun testServer(url: String): String? {
        val request = try {
            Request.Builder().url(url).build()
        } catch (e: IllegalArgumentException) {
            return "That doesn't look like a valid address."
        }
        val result = withTimeoutOrNull(10_000) {
            suspendCancellableCoroutine<String> { cont ->
                val ws = client.newWebSocket(request, object : WebSocketListener() {
                    override fun onOpen(webSocket: WebSocket, response: Response) {
                        webSocket.send("""{"t":"hello"}""")
                    }

                    override fun onMessage(webSocket: WebSocket, text: String) {
                        webSocket.close(1000, null)
                        // An empty string means success.
                        if (cont.isActive) cont.resume(if (text.contains("\"hello\"")) "" else "Unexpected reply.")
                    }

                    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                        if (cont.isActive) cont.resume("Could not connect: ${t.message ?: t.javaClass.simpleName}")
                    }
                })
                cont.invokeOnCancellation { ws.cancel() }
            }
        } ?: return "The server did not answer within 10 seconds."
        return result.ifEmpty { null }
    }

    // ------------------------------------------------------------ internals

    private fun request(serverUrl: String, message: JsonObject) {
        if (serverUrl.isBlank()) {
            _errors.tryEmit("Set the game server address in Settings first.")
            return
        }
        synchronized(lock) {
            if (session != null) return
            pending = message
        }
        _state.update { it.copy(busy = true) }
        val online = _state.value.link == Link.ONLINE && socketUrl == serverUrl
        if (online) flushPending() else open(serverUrl)
    }

    private fun send(message: JsonObject): Boolean = synchronized(lock) { socket?.send(message.toString()) ?: false }

    private fun open(url: String) {
        val request = try {
            Request.Builder().url(url).build()
        } catch (e: IllegalArgumentException) {
            fail("The server address \"$url\" is not valid. Fix it in Settings.")
            return
        }
        synchronized(lock) {
            socket?.cancel()
            generation++
            socketUrl = url
            val gen = generation
            _state.update { it.copy(link = if (session != null && attempts > 0) Link.RECONNECTING else Link.CONNECTING) }
            socket = client.newWebSocket(request, Listener(gen))
        }
    }

    private fun flushPending() {
        val message = synchronized(lock) { pending.also { pending = null } } ?: return
        if (!send(message)) fail("Lost connection to the server. Please try again.")
    }

    private fun fail(message: String) {
        synchronized(lock) { pending = null }
        _state.update { it.copy(busy = false) }
        _errors.tryEmit(message)
    }

    private fun endSession() {
        synchronized(lock) {
            session = null
            pending = null
            resuming = false
            attempts = 0
            reconnectJob?.cancel()
            reconnectJob = null
            generation++
            socket?.close(1000, "bye")
            socket = null
            socketUrl = null
        }
        prefs.savedSession = null
        _state.value = State()
    }

    private fun scheduleReconnect() {
        val s = synchronized(lock) { session } ?: return
        reconnectJob?.cancel()
        val wait = min(15_000L, 1_000L shl min(attempts, 4))
        attempts++
        _state.update { it.copy(link = Link.RECONNECTING) }
        reconnectJob = scope.launch {
            delay(wait)
            open(s.serverUrl)
        }
    }

    private fun handle(text: String) {
        val obj = runCatching { GameJson.parseToJsonElement(text).jsonObject }.getOrNull() ?: return
        when (obj["t"]?.jsonPrimitive?.contentOrNull) {
            "welcome" -> {
                val w = GameJson.decodeFromJsonElement(Welcome.serializer(), obj)
                val s = Session(socketUrl ?: return, w.code, w.playerId, w.token)
                synchronized(lock) {
                    session = s
                    resuming = false
                }
                prefs.savedSession = s
                _state.update { it.copy(hasSession = true, busy = false) }
            }

            "state" -> {
                val game = runCatching { GameJson.decodeFromJsonElement(GameState.serializer(), obj) }.getOrNull() ?: return
                clockOffsetMs = game.now - System.currentTimeMillis()
                if (synchronized(lock) { session } == null) return
                _state.update { it.copy(game = game) }
            }

            "notice" -> {
                val kind = obj["kind"]?.jsonPrimitive?.contentOrNull ?: "info"
                val message = obj["text"]?.jsonPrimitive?.contentOrNull ?: return
                _notices.tryEmit(Notice(kind, message))
            }

            "tagRequest" -> {
                val from = obj["fromName"]?.jsonPrimitive?.contentOrNull ?: "A seeker"
                _notices.tryEmit(Notice("tag_request", "$from says they tagged you!"))
            }

            "kicked" -> {
                endSession()
                _errors.tryEmit(obj["text"]?.jsonPrimitive?.contentOrNull ?: "You were removed from the game.")
            }

            "left" -> endSession()

            "error" -> {
                val message = obj["message"]?.jsonPrimitive?.contentOrNull ?: "Something went wrong."
                val wasResuming = synchronized(lock) { resuming }
                when {
                    wasResuming -> {
                        endSession()
                        _errors.tryEmit("That game has finished or no longer exists.")
                    }

                    synchronized(lock) { session } == null -> {
                        fail(message)
                        synchronized(lock) {
                            generation++
                            socket?.close(1000, null)
                            socket = null
                            socketUrl = null
                        }
                        _state.update { it.copy(link = Link.IDLE) }
                    }

                    else -> _errors.tryEmit(message)
                }
            }
        }
    }

    private inner class Listener(private val gen: Int) : WebSocketListener() {
        private fun current() = synchronized(lock) { gen == generation }

        override fun onOpen(webSocket: WebSocket, response: Response) {
            if (!current()) return
            attempts = 0
            _state.update { it.copy(link = Link.ONLINE) }
            val s = synchronized(lock) { session }
            if (s != null) {
                synchronized(lock) { resuming = true }
                webSocket.send(buildJsonObject {
                    put("t", "resume")
                    put("code", s.code)
                    put("playerId", s.playerId)
                    put("token", s.token)
                }.toString())
            } else {
                flushPending()
            }
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            if (current()) handle(text)
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            webSocket.close(code, null)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            if (!current()) return
            if (code == 4000) {
                endSession()
                _errors.tryEmit("You joined this game from another device.")
            } else {
                dropped()
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            if (current()) dropped()
        }

        private fun dropped() {
            synchronized(lock) {
                socket = null
                resuming = false
            }
            if (synchronized(lock) { session } != null) {
                scheduleReconnect()
            } else {
                _state.update { it.copy(link = Link.IDLE) }
                if (synchronized(lock) { pending } != null || _state.value.busy) {
                    fail("Could not reach the game server. Check your internet and the server address in Settings.")
                }
            }
        }
    }
}
