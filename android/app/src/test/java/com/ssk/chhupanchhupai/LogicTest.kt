package com.ssk.chhupanchhupai

import com.ssk.chhupanchhupai.data.GameJson
import com.ssk.chhupanchhupai.data.GameState
import com.ssk.chhupanchhupai.data.Prefs
import com.ssk.chhupanchhupai.ui.formatClock
import com.ssk.chhupanchhupai.ui.formatDistance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LogicTest {
    @Test
    fun serverUrlsAreNormalised() {
        assertEquals("wss://my-game.onrender.com/ws", Prefs.normaliseServerUrl("my-game.onrender.com"))
        assertEquals("wss://my-game.onrender.com/ws", Prefs.normaliseServerUrl("https://my-game.onrender.com/"))
        assertEquals("ws://192.168.1.20:8080/ws", Prefs.normaliseServerUrl("192.168.1.20:8080"))
        assertEquals("ws://localhost:8080/ws", Prefs.normaliseServerUrl("http://localhost:8080"))
        assertEquals("wss://x.example/custom", Prefs.normaliseServerUrl("wss://x.example/custom"))
        assertEquals("", Prefs.normaliseServerUrl("  "))
    }

    @Test
    fun restBaseIsDerivedFromTheGameServerAddress() {
        assertEquals("https://my-game.onrender.com", Prefs.httpBaseFromWs("my-game.onrender.com"))
        assertEquals("http://192.168.1.20:8080", Prefs.httpBaseFromWs("ws://192.168.1.20:8080/ws"))
        assertEquals("", Prefs.httpBaseFromWs(""))
    }

    @Test
    fun clockAndDistanceFormatting() {
        assertEquals("00:00", formatClock(-5000))
        assertEquals("02:05", formatClock(125_000))
        assertEquals("1:02:34", formatClock(3_754_000))
        assertEquals("850 m", formatDistance(850))
        assertEquals("1.2 km", formatDistance(1234))
    }

    @Test
    fun parsesServerStateAndIgnoresUnknownFields() {
        val json = """
            {"t":"state","now":1000,"code":"ABC234","phase":"hunt","hostId":"h",
             "settings":{"durationMin":20,"zone":{"lat":19.0,"lng":72.8,"radiusM":400},"futureSetting":true},
             "zone":{"lat":19.0,"lng":72.8,"radiusM":400,"currentRadiusM":350},
             "players":[{"id":"h","name":"Host","role":"seeker","status":"active"}],
             "you":{"id":"h","name":"Host","role":"seeker","isHost":true,"loc":null},
             "pings":[{"seq":1,"at":900,"marks":[{"playerId":"x","lat":null,"lng":null,"stale":true}]}],
             "live":[],"events":[],"results":null,"brandNewField":[1,2,3]}
        """.trimIndent()
        val state = GameJson.decodeFromString(GameState.serializer(), json)
        assertEquals("ABC234", state.code)
        assertEquals(350, state.zone?.currentRadiusM)
        assertEquals(20, state.settings.durationMin)
        assertEquals(true, state.isSeeker)
        assertNull(state.pings[0].marks[0].lat)
    }
}
