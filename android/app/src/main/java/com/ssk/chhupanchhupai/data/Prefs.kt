package com.ssk.chhupanchhupai.data

import android.content.Context
import com.ssk.chhupanchhupai.BuildConfig

/** Saved session so the app can rejoin after the phone drops signal or the app restarts. */
data class Session(val serverUrl: String, val code: String, val playerId: String, val token: String)

interface SessionStore {
    var savedSession: Session?
}

class Prefs(context: Context) : SessionStore, IdentityStore {
    private val settings = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val session = context.getSharedPreferences("session", Context.MODE_PRIVATE)
    private val account = context.getSharedPreferences("account", Context.MODE_PRIVATE)

    override var playerName: String
        get() = settings.getString("name", "") ?: ""
        set(value) = settings.edit().putString("name", value.trim()).apply()

    override var serverUrl: String
        get() = settings.getString("serverUrl", null) ?: BuildConfig.DEFAULT_SERVER_URL
        set(value) = settings.edit().putString("serverUrl", normaliseServerUrl(value)).apply()

    /** Server credentials for events. Tied to the server they came from. */
    override var identity: Identity?
        get() {
            val id = account.getString("userId", null) ?: return null
            val secret = account.getString("secret", null) ?: return null
            if (account.getString("server", null) != serverUrl) return null
            return Identity(id, secret)
        }
        set(value) {
            val editor = account.edit().clear()
            if (value != null) {
                editor.putString("userId", value.userId).putString("secret", value.secret).putString("server", serverUrl)
            }
            editor.apply()
        }

    /** Events the player asked to be reminded about: id -> start time. */
    var reminders: Map<String, Long>
        get() = settings.getStringSet("reminders", emptySet()).orEmpty().mapNotNull {
            val (id, at) = it.split('|').takeIf { parts -> parts.size == 2 } ?: return@mapNotNull null
            at.toLongOrNull()?.let { t -> id to t }
        }.toMap()
        set(value) = settings.edit().putStringSet("reminders", value.map { "${it.key}|${it.value}" }.toSet()).apply()

    var reminderTitles: Map<String, String>
        get() = settings.getStringSet("reminderTitles", emptySet()).orEmpty().mapNotNull {
            val i = it.indexOf('|')
            if (i <= 0) null else it.substring(0, i) to it.substring(i + 1)
        }.toMap()
        set(value) = settings.edit().putStringSet("reminderTitles", value.map { "${it.key}|${it.value}" }.toSet()).apply()

    var lastSettings: GameSettings
        get() = settings.getString("lastSettings", null)?.let {
            runCatching { GameJson.decodeFromString(GameSettings.serializer(), it) }.getOrNull()
        } ?: GameSettings()
        set(value) = settings.edit()
            .putString("lastSettings", GameJson.encodeToString(GameSettings.serializer(), value))
            .apply()

    override var savedSession: Session?
        get() {
            val url = session.getString("url", null) ?: return null
            val code = session.getString("code", null) ?: return null
            val id = session.getString("playerId", null) ?: return null
            val token = session.getString("token", null) ?: return null
            return Session(url, code, id, token)
        }
        set(value) {
            val editor = session.edit().clear()
            if (value != null) {
                editor.putString("url", value.serverUrl)
                    .putString("code", value.code)
                    .putString("playerId", value.playerId)
                    .putString("token", value.token)
            }
            editor.apply()
        }

    companion object {
        // A laptop on the same Wi-Fi (e.g. 192.168.1.20:8080) has no TLS.
        private val LOCAL_HOST = Regex("""^(localhost|\d{1,3}(\.\d{1,3}){3})(:\d+)?(/|$)""")

        /**
         * Accepts what people are likely to paste ("my-app.onrender.com",
         * "https://my-app.onrender.com/") and turns it into a WebSocket URL ending in /ws.
         */
        /** "wss://host/ws" -> "https://host" (the REST API lives on the same server). */
        fun httpBaseFromWs(wsUrl: String): String {
            val url = normaliseServerUrl(wsUrl)
            if (url.isEmpty()) return ""
            val scheme = if (url.startsWith("wss://")) "https://" else "http://"
            val hostPart = url.substringAfter("://").substringBefore('/')
            return scheme + hostPart
        }

        fun normaliseServerUrl(raw: String): String {
            var url = raw.trim().trimEnd('/')
            if (url.isEmpty()) return ""
            url = when {
                url.startsWith("https://") -> "wss://" + url.removePrefix("https://")
                url.startsWith("http://") -> "ws://" + url.removePrefix("http://")
                url.startsWith("ws://") || url.startsWith("wss://") -> url
                LOCAL_HOST.containsMatchIn(url) -> "ws://$url"
                else -> "wss://$url"
            }
            if (!url.substringAfter("://").contains('/')) url += "/ws"
            return url
        }
    }
}
