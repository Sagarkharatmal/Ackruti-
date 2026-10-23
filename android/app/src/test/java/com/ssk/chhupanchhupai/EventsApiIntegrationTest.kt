package com.ssk.chhupanchhupai

import com.ssk.chhupanchhupai.data.ApiClient
import com.ssk.chhupanchhupai.data.ApiException
import com.ssk.chhupanchhupai.data.EventInput
import com.ssk.chhupanchhupai.data.GameSettings
import com.ssk.chhupanchhupai.data.Identity
import com.ssk.chhupanchhupai.data.IdentityStore
import com.ssk.chhupanchhupai.data.Place
import com.ssk.chhupanchhupai.data.Sponsor
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Assume.assumeTrue
import org.junit.Test

private class MemIdentity(override val playerName: String, override val serverUrl: String) : IdentityStore {
    override var identity: Identity? = null
}

/** The app's events client against a live server (runs when ITEST_URL is set). */
class EventsApiIntegrationTest {
    private val url = System.getenv("ITEST_URL").orEmpty()

    @Test
    fun hostAndPlayerGoThroughAnEventNight() = runBlocking {
        assumeTrue("set ITEST_URL to run against a live server", url.isNotEmpty())
        val host = ApiClient(MemIdentity("Host Riya", url))
        val player = ApiClient(MemIdentity("Aman", url))
        val place = Place(19.0176, 72.8562, "Shivaji Park gate 5")

        val created = host.createEvent(
            EventInput(
                title = "Shivaji Park Night",
                community = "Dadar Runners",
                startsAt = System.currentTimeMillis() + 30 * 60_000,
                place = place,
                capacity = 20,
                sponsor = Sponsor("Vada Pav Co."),
                rules = GameSettings(headStartSec = 60, zone = null),
            ),
        )
        assertTrue(created.me.isHost)
        assertNotNull(created.rules.zone)

        val nearby = player.listEvents(place.lat, place.lng, 5)
        assertTrue(nearby.any { it.id == created.id && it.sponsor?.name == "Vada Pav Co." })

        val joined = player.rsvp(created.code)
        assertTrue(joined.me.going)
        player.postMessage(created.id, "Pakka aa raha hoon")
        host.announce(created.id, "Meet at gate 5")
        val chat = player.messages(created.id, 0)
        assertTrue(chat.any { it.kind == "announcement" && it.text == "Meet at gate 5" })

        try {
            player.checkIn(created.id, place.lat + 0.05, place.lng, 5f)
            fail("far away check-in must be refused")
        } catch (e: ApiException) {
            assertTrue(e.message!!.contains("meeting point"))
        }
        val checked = player.checkIn(created.id, place.lat, place.lng, 5f)
        assertTrue(checked.me.checkedIn)

        val code = host.startRound(created.id)
        assertEquals(6, code.length)
        assertEquals(code, player.event(created.id).currentRound?.code)
        val mine = player.myEvents()
        assertTrue(mine.attending.any { it.id == created.id })
    }
}
