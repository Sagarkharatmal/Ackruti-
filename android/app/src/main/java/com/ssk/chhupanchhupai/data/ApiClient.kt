package com.ssk.chhupanchhupai.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class ApiException(message: String, val status: Int = 0) : Exception(message)

/** Saved "accounts-lite" identity: the server gives us an id + secret once. */
data class Identity(val userId: String, val secret: String)

interface IdentityStore {
    var identity: Identity?
    val playerName: String
    val serverUrl: String
}

/**
 * REST client for community events (server/src/api.js). Signs up silently the
 * first time it's needed and again if the server forgot us (e.g. its database
 * was reset).
 */
class ApiClient(private val store: IdentityStore) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val jsonType = "application/json; charset=utf-8".toMediaType()

    private fun base(): String {
        val url = Prefs.httpBaseFromWs(store.serverUrl)
        if (url.isEmpty()) throw ApiException("Set the game server address in Settings first.")
        return url
    }

    suspend fun listEvents(lat: Double?, lng: Double?, radiusKm: Int?): List<EventSummary> {
        val q = buildList {
            if (lat != null && lng != null) {
                add("lat=$lat")
                add("lng=$lng")
            }
            if (radiusKm != null) add("radiusKm=$radiusKm")
        }.joinToString("&")
        return call("GET", "/api/events" + if (q.isEmpty()) "" else "?$q", null, EventListResponse.serializer(), auth = false).events
    }

    suspend fun myEvents(): MyEvents = call("GET", "/api/events/mine", null, MyEvents.serializer())

    suspend fun event(idOrCode: String): EventDetail =
        call("GET", "/api/events/${enc(idOrCode)}", null, EventResponse.serializer()).event

    suspend fun createEvent(input: EventInput): EventDetail =
        call("POST", "/api/events", GameJson.encodeToJsonElement(EventInput.serializer(), input) as JsonObject, EventResponse.serializer()).event

    suspend fun updateEvent(id: String, input: EventInput): EventDetail =
        call("PATCH", "/api/events/${enc(id)}", GameJson.encodeToJsonElement(EventInput.serializer(), input) as JsonObject, EventResponse.serializer()).event

    suspend fun cancelEvent(id: String): EventDetail = call("DELETE", "/api/events/${enc(id)}", null, EventResponse.serializer()).event
    suspend fun rsvp(id: String): EventDetail = call("POST", "/api/events/${enc(id)}/rsvp", null, EventResponse.serializer()).event
    suspend fun cancelRsvp(id: String): EventDetail = call("DELETE", "/api/events/${enc(id)}/rsvp", null, EventResponse.serializer()).event

    suspend fun checkIn(id: String, lat: Double, lng: Double, acc: Float?): EventDetail =
        call("POST", "/api/events/${enc(id)}/checkin", buildJsonObject {
            put("lat", lat)
            put("lng", lng)
            if (acc != null) put("acc", acc.toDouble())
        }, EventResponse.serializer()).event

    suspend fun hostCheckIn(id: String, userId: String): EventDetail =
        call("POST", "/api/events/${enc(id)}/checkin/${enc(userId)}", null, EventResponse.serializer()).event

    suspend fun messages(id: String, since: Long): List<ChatMessage> =
        call("GET", "/api/events/${enc(id)}/messages?since=$since", null, MessagesResponse.serializer()).messages

    suspend fun postMessage(id: String, text: String) {
        call<Unit>("POST", "/api/events/${enc(id)}/messages", buildJsonObject { put("text", text) }, null)
    }

    suspend fun announce(id: String, text: String) {
        call<Unit>("POST", "/api/events/${enc(id)}/announce", buildJsonObject { put("text", text) }, null)
    }

    /** Opens a new round for the event. Returns the game code to join. */
    suspend fun startRound(id: String): String =
        call("POST", "/api/events/${enc(id)}/rounds", buildJsonObject {}, RoundResponse.serializer()).code

    /** Makes sure we have server credentials; returns them. */
    suspend fun ensureIdentity(): Identity {
        store.identity?.let { return it }
        val name = store.playerName.ifBlank { throw ApiException("Please set your name first.") }
        val res = call("POST", "/api/users", buildJsonObject { put("name", name) }, SignUpResponse.serializer(), auth = false)
        return Identity(res.user.id, res.secret).also { store.identity = it }
    }

    /** Tells the server our current display name. */
    suspend fun syncName() {
        if (store.identity == null || store.playerName.isBlank()) return
        call<Unit>("PATCH", "/api/users/me", buildJsonObject { put("name", store.playerName) }, null)
    }

    // ------------------------------------------------------------------

    private suspend fun <T> call(
        method: String,
        path: String,
        body: JsonObject?,
        serializer: KSerializer<T>?,
        auth: Boolean = true,
        retried: Boolean = false,
    ): T = withContext(Dispatchers.IO) {
        val identity = if (auth) ensureIdentity() else store.identity
        val builder = Request.Builder().url(base() + path)
        if (identity != null) {
            builder.header("X-User-Id", identity.userId).header("X-User-Secret", identity.secret)
        }
        val requestBody = when {
            body != null -> body.toString().toRequestBody(jsonType)
            method == "POST" || method == "PATCH" -> "{}".toRequestBody(jsonType)
            else -> null
        }
        builder.method(method, requestBody)
        val response = try {
            client.newCall(builder.build()).execute()
        } catch (e: IOException) {
            throw ApiException("Can't reach the server. Check your internet connection.")
        } catch (e: IllegalArgumentException) {
            throw ApiException("The server address in Settings is not valid.")
        }
        response.use { res ->
            val text = res.body?.string().orEmpty()
            if (res.code == 401 && auth && !retried) {
                // The server doesn't know us any more: sign up again once.
                store.identity = null
                return@withContext call(method, path, body, serializer, auth, retried = true)
            }
            if (!res.isSuccessful) {
                val message = runCatching { GameJson.decodeFromString(ErrorResponse.serializer(), text).error }
                    .getOrNull()?.takeIf { it.isNotBlank() } ?: "Server error (${res.code})."
                throw ApiException(message, res.code)
            }
            @Suppress("UNCHECKED_CAST")
            if (serializer == null) Unit as T else GameJson.decodeFromString(serializer, text)
        }
    }

    private fun enc(s: String) = java.net.URLEncoder.encode(s, "UTF-8")
}
