package com.ssk.chhupanchhupai.data

import android.content.Context
import com.ssk.chhupanchhupai.BuildConfig

/** Saved session so the app can rejoin after the phone drops signal or the app restarts. */
data class Session(val serverUrl: String, val code: String, val playerId: String, val token: String)

interface SessionStore {
    var savedSession: Session?
}

class Prefs(context: Context) : SessionStore {
    private val settings = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val session = context.getSharedPreferences("session", Context.MODE_PRIVATE)

    var playerName: String
        get() = settings.getString("name", "") ?: ""
        set(value) = settings.edit().putString("name", value.trim()).apply()

    var serverUrl: String
        get() = settings.getString("serverUrl", null) ?: BuildConfig.DEFAULT_SERVER_URL
        set(value) = settings.edit().putString("serverUrl", normaliseServerUrl(value)).apply()

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
