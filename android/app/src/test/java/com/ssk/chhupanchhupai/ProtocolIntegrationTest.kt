package com.ssk.chhupanchhupai

import com.ssk.chhupanchhupai.data.GameConnection
import com.ssk.chhupanchhupai.data.GameSettings
import com.ssk.chhupanchhupai.data.GameState
import com.ssk.chhupanchhupai.data.Phase
import com.ssk.chhupanchhupai.data.Role
import com.ssk.chhupanchhupai.data.Session
import com.ssk.chhupanchhupai.data.SessionStore
import com.ssk.chhupanchhupai.data.Zone
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

private class MemStore : SessionStore {
    override var savedSession: Session? = null
}

/**
 * Plays a whole round with the real app networking code against a running game
 * server. Runs only when ITEST_URL is set (CI starts the Node server first):
 *   ITEST_URL=ws://127.0.0.1:8123/ws ./gradlew testDebugUnitTest
 */
class ProtocolIntegrationTest {
    private val url = System.getenv("ITEST_URL").orEmpty()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private suspend fun GameConnection.awaitGame(timeoutMs: Long = 10_000, cond: (GameState) -> Boolean): GameState =
        withTimeout(timeoutMs) { state.first { s -> s.game?.let(cond) == true }.game!! }

    @Test
    fun kotlinClientPlaysAFullRoundAgainstTheNodeServer() = runBlocking {
        assumeTrue("set ITEST_URL to run against a live server", url.isNotEmpty())
        val errors = mutableListOf<String>()
        val host = GameConnection(MemStore(), scope)
        val aStore = MemStore()
        val a = GameConnection(aStore, scope)
        val b = GameConnection(MemStore(), scope)
        for (c in listOf(host, a, b)) scope.launch { c.errors.collect { errors += it } }

        assertNull(host.testServer(url))
        host.create(url, "Host", GameSettings(zone = Zone(19.076, 72.8777, 500), headStartSec = 30, pingIntervalSec = 30, durationMin = 5))
        val lobby = host.awaitGame { it.phase == Phase.LOBBY }
        assertEquals(6, lobby.code.length)
        assertTrue(lobby.you.isHost)

        a.join(url, lobby.code.lowercase(), "Asha")
        b.join(url, lobby.code, "Bunty")
        a.awaitGame { true }
        b.awaitGame { true }
        host.setRolePreference(Role.SEEKER)
        a.setRolePreference(Role.HIDER)
        b.setRolePreference(Role.HIDER)
        host.awaitGame { g -> g.players.size == 3 && g.players.none { it.rolePref == Role.RANDOM } }

        host.start()
        host.awaitGame { it.phase == Phase.HEADSTART }
        host.sendLocation(19.076, 72.8777, 5f)
        a.sendLocation(19.077, 72.8777, 5f)
        b.sendLocation(19.075, 72.8777, 5f)

        val hunt = host.awaitGame(45_000) { it.phase == Phase.HUNT && it.pings.isNotEmpty() }
        assertEquals(2, hunt.pings.last().marks.size)
        val aHunt = a.awaitGame { it.phase == Phase.HUNT }
        assertTrue("hider must not see live players", aHunt.live.isEmpty())
        assertTrue("hider must not see pings", aHunt.pings.isEmpty())
        assertTrue(a.serverNow() > 0)

        // App restart: a fresh connection with the saved session rejoins the same seat.
        val a2 = GameConnection(aStore, scope)
        a2.resumeSavedSession()
        val resumed = a2.awaitGame { it.phase == Phase.HUNT }
        assertEquals(aHunt.you.id, resumed.you.id)

        host.tag(resumed.you.id)
        val withRequest = a2.awaitGame { it.you.pendingTagRequests.isNotEmpty() }
        a2.replyToTag(withRequest.you.pendingTagRequests.first().requestId, true)
        val spectating = a2.awaitGame { it.you.spectator }
        assertTrue(spectating.live.isNotEmpty())

        b.reportCaught(null)
        val ended = host.awaitGame { it.phase == Phase.ENDED }
        assertEquals(Role.SEEKER, ended.results?.winner)
        assertEquals(3, ended.results?.players?.size)

        host.rematch()
        host.awaitGame { it.phase == Phase.LOBBY && it.round == 2 }
        b.leave()
        host.awaitGame { it.phase == Phase.LOBBY && it.players.size == 2 }
        assertFalse(b.state.value.hasSession)
        println("errors seen: $errors")
        scope.cancel()
    }
}
