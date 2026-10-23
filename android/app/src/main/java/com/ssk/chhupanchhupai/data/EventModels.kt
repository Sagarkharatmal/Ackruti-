package com.ssk.chhupanchhupai.data

import kotlinx.serialization.Serializable

// Mirrors server/src/events.js (summary() and detail()).

object EventStatus {
    const val UPCOMING = "upcoming"
    const val LIVE = "live"
    const val ENDED = "ended"
    const val CANCELLED = "cancelled"
}

@Serializable
data class Place(val lat: Double, val lng: Double, val name: String = "")

@Serializable
data class Sponsor(val name: String)

@Serializable
data class EventSummary(
    val id: String,
    val code: String? = null,
    val title: String,
    val community: String = "",
    val hostName: String = "",
    val startsAt: Long,
    val endsAt: Long = 0,
    val place: Place,
    val capacity: Int = 0,
    val going: Int = 0,
    val checkedIn: Int = 0,
    val sponsor: Sponsor? = null,
    val visibility: String = "public",
    val status: String = EventStatus.UPCOMING,
    val distanceKm: Double? = null,
)

@Serializable
data class Attendee(
    val userId: String,
    val name: String,
    val isHost: Boolean = false,
    val checkedIn: Boolean = false,
)

@Serializable
data class EventMe(val isHost: Boolean = false, val going: Boolean = false, val checkedIn: Boolean = false)

@Serializable
data class CurrentRound(val code: String, val phase: String? = null, val players: Int = 0, val round: Int = 1)

@Serializable
data class RoundSummary(val number: Int, val endedAt: Long = 0, val winner: String? = null, val players: Int = 0)

@Serializable
data class LeaderRow(
    val userId: String? = null,
    val name: String,
    val points: Int = 0,
    val rounds: Int = 0,
    val catches: Int = 0,
    val timesFound: Int = 0,
    val survivedMs: Long = 0,
)

@Serializable
data class Announcement(val at: Long, val text: String)

@Serializable
data class EventDetail(
    val id: String,
    val code: String,
    val title: String,
    val community: String = "",
    val hostName: String = "",
    val description: String = "",
    val startsAt: Long,
    val endsAt: Long = 0,
    val durationMin: Int = 120,
    val place: Place,
    val capacity: Int = 0,
    val going: Int = 0,
    val checkedIn: Int = 0,
    val sponsor: Sponsor? = null,
    val visibility: String = "public",
    val status: String = EventStatus.UPCOMING,
    val rules: GameSettings = GameSettings(),
    val announcements: List<Announcement> = emptyList(),
    val attendees: List<Attendee> = emptyList(),
    val me: EventMe = EventMe(),
    val currentRound: CurrentRound? = null,
    val rounds: List<RoundSummary> = emptyList(),
    val leaderboard: List<LeaderRow> = emptyList(),
)

@Serializable
data class ChatMessage(
    val id: String,
    val at: Long,
    val userId: String? = null,
    val name: String = "",
    val text: String,
    /** "chat", "announcement" or "system" */
    val kind: String = "chat",
)

/** What the host fills in on the "Host an event" form. */
@Serializable
data class EventInput(
    val title: String,
    val description: String = "",
    val community: String = "",
    val startsAt: Long,
    val durationMin: Int = 120,
    val place: Place,
    val capacity: Int = 30,
    val visibility: String = "public",
    val sponsor: Sponsor? = null,
    val rules: GameSettings = GameSettings(),
)

@Serializable
internal data class EventListResponse(val events: List<EventSummary> = emptyList())

@Serializable
data class MyEvents(val hosting: List<EventSummary> = emptyList(), val attending: List<EventSummary> = emptyList())

@Serializable
internal data class EventResponse(val event: EventDetail)

@Serializable
internal data class MessagesResponse(val messages: List<ChatMessage> = emptyList())

@Serializable
internal data class RoundResponse(val code: String, val event: EventDetail)

@Serializable
internal data class ApiUser(val id: String, val name: String)

@Serializable
internal data class SignUpResponse(val user: ApiUser, val secret: String)

@Serializable
internal data class ErrorResponse(val error: String = "")
