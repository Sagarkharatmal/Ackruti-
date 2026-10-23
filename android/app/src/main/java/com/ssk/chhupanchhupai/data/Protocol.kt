package com.ssk.chhupanchhupai.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// Mirrors the JSON the game server sends (server/src/game.js -> viewFor()).
// Every field has a default so older/newer servers don't crash the app.

val GameJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    encodeDefaults = true
    coerceInputValues = true
}

object Phase {
    const val LOBBY = "lobby"
    const val HEADSTART = "headstart"
    const val HUNT = "hunt"
    const val ENDED = "ended"
}

object Role {
    const val SEEKER = "seeker"
    const val HIDER = "hider"
    const val RANDOM = "random"
}

object Status {
    const val ACTIVE = "active"
    const val CAUGHT = "caught"
    const val LEFT = "left"
}

@Serializable
data class Zone(
    val lat: Double,
    val lng: Double,
    val radiusM: Int,
    val currentRadiusM: Int? = null,
)

@Serializable
data class GameSettings(
    val durationMin: Int = 30,
    val headStartSec: Int = 180,
    val pingIntervalSec: Int = 180,
    val pingOnHuntStart: Boolean = true,
    val zoneEnabled: Boolean = true,
    val zone: Zone? = null,
    val shrinkEnabled: Boolean = false,
    val shrinkToPct: Int = 40,
    val outOfZoneGraceSec: Int = 30,
    val dhappaEnabled: Boolean = false,
)

@Serializable
data class DhappaBase(val lat: Double, val lng: Double, val radiusM: Int = 25)

@Serializable
data class Loc(
    val lat: Double,
    val lng: Double,
    val acc: Double? = null,
    val at: Long = 0,
)

@Serializable
data class PlayerInfo(
    val id: String,
    val name: String,
    val connected: Boolean = true,
    val rolePref: String = Role.RANDOM,
    val role: String? = null,
    val status: String = Status.ACTIVE,
    val caughtAt: Long? = null,
    val caughtBy: String? = null,
)

@Serializable
data class TagRequest(
    val requestId: String,
    val fromId: String,
    val fromName: String,
    val expiresAt: Long,
)

@Serializable
data class You(
    val id: String,
    val name: String,
    val role: String? = null,
    val status: String = Status.ACTIVE,
    val rolePref: String = Role.RANDOM,
    val isHost: Boolean = false,
    val spectator: Boolean = false,
    /** Caught with "Dhappa!" on: wait at the base to be freed. */
    val inJail: Boolean = false,
    val loc: Loc? = null,
    val outSince: Long? = null,
    val revealed: Boolean = false,
    val catches: Int = 0,
    val distanceM: Int = 0,
    val pendingTagRequests: List<TagRequest> = emptyList(),
)

@Serializable
data class PingMark(
    val playerId: String,
    val lat: Double? = null,
    val lng: Double? = null,
    val acc: Double? = null,
    val locAt: Long? = null,
    val stale: Boolean = false,
)

@Serializable
data class Ping(
    val seq: Int,
    val at: Long,
    val marks: List<PingMark> = emptyList(),
)

@Serializable
data class LiveLoc(
    val playerId: String,
    val lat: Double,
    val lng: Double,
    val acc: Double? = null,
    val at: Long = 0,
    /** "teammate", "revealed" or "spectate" */
    val kind: String = "spectate",
)

@Serializable
data class GameEvent(
    val at: Long,
    val kind: String,
    val text: String,
    val playerId: String? = null,
)

@Serializable
data class ResultPlayer(
    val id: String,
    val name: String,
    val role: String,
    val status: String = Status.ACTIVE,
    val caughtAt: Long? = null,
    val caughtBy: String? = null,
    val survivedMs: Long? = null,
    val catches: Int = 0,
    val timesCaught: Int = 0,
    val rescues: Int = 0,
    val distanceM: Int = 0,
    val trail: List<List<Double>> = emptyList(),
)

@Serializable
data class Results(
    val winner: String? = null,
    val reason: String = "",
    val endedAt: Long = 0,
    val startedAt: Long? = null,
    val huntStartsAt: Long? = null,
    val pings: Int = 0,
    val players: List<ResultPlayer> = emptyList(),
)

@Serializable
data class GameState(
    val now: Long,
    val code: String,
    val eventId: String? = null,
    val round: Int = 1,
    val phase: String = Phase.LOBBY,
    val hostId: String? = null,
    val settings: GameSettings = GameSettings(),
    val startedAt: Long? = null,
    val huntStartsAt: Long? = null,
    val endsAt: Long? = null,
    val nextPingAt: Long? = null,
    val lastPingAt: Long? = null,
    val pingCount: Int = 0,
    val zone: Zone? = null,
    val dhappaBase: DhappaBase? = null,
    val hidersLeft: Int = 0,
    val players: List<PlayerInfo> = emptyList(),
    val you: You,
    val pings: List<Ping> = emptyList(),
    val live: List<LiveLoc> = emptyList(),
    val events: List<GameEvent> = emptyList(),
    val results: Results? = null,
) {
    fun player(id: String?): PlayerInfo? = players.firstOrNull { it.id == id }
    val isSeeker get() = you.role == Role.SEEKER
    val isActiveHider get() = you.role == Role.HIDER && you.status == Status.ACTIVE
}

@Serializable
data class Welcome(val code: String, val playerId: String, val token: String)

/** A short message from the server that should buzz the phone / show a toast. */
data class Notice(val kind: String, val text: String)
