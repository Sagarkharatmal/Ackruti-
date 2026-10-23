# Chhupan Chhupai game protocol

> **Made by SSK** · 23 Oct 2026

Games run over **one WebSocket per phone**, at `ws(s)://<host>/ws`. Every message is a JSON object with a `t` (type) field. The server is the only source of truth. Each player's `state` contains **only what their role may see**.

## Messages the phone sends

| `t` | Fields | Who / when | Notes |
|---|---|---|---|
| `hello` | — | anyone | Server replies `{t:"hello", now}`. Used by "Save & test". |
| `create` | `name`, `settings`, optional `userId` + `userSecret` | not in a game | Creates a game; you become its host. |
| `join` | `code`, `name`, optional `userId` + `userSecret` | not in a game | Lobby only. Names must be unique. **Event rounds require** `userId` + `userSecret` (for the leaderboard). |
| `resume` | `code`, `playerId`, `token` | after reconnecting | Rejoins your seat (works mid-round). |
| `setRole` | `pref`: `hider` \| `seeker` \| `random` | lobby | |
| `settings` | `settings` (partial is fine) | host, lobby | Values are clamped to safe ranges. |
| `kick` | `playerId` | host, lobby | |
| `start` | — | host, lobby | Needs ≥ 2 players, and a placed zone if the zone is on. |
| `loc` | `lat`, `lng`, `acc` (m) | in a game | Sent every 2–4 s by the location service. |
| `tag` | `playerId` | seeker, hunt | Asks that hider to confirm (expires after 60 s). |
| `tagReply` | `requestId`, `accept` | the tagged hider | |
| `caught` | `byId` (optional) | active hider | The hider reports being caught themselves. |
| `end` | — | host, during a round | Ends the round with no winner. |
| `rematch` | — | host, after a round | Back to the lobby with the same group. |
| `leave` | — | anyone | In a round, a hider who leaves counts as out. |

## Messages the server sends

| `t` | Fields | Meaning |
|---|---|---|
| `welcome` | `code`, `playerId`, `token` | You're in. The app saves these three for `resume`. |
| `state` | see below | Sent on every change, and at least every 5 s. |
| `notice` | `kind`, `text` | Something happened worth a buzz or a toast. Kinds: `role`, `hunt`, `ping`, `pinged`, `zone_warning`, `zone_ok`, `revealed`, `caught`, `tag_denied`, `game_over`. |
| `tagRequest` | `requestId`, `fromId`, `fromName`, `expiresAt` | A seeker says they tagged you. |
| `kicked` / `left` | — | You are no longer in the game. |
| `error` | `message` | A human-readable reason. |

### `state`

The fields of a `state` message:

- **Clock:** `now` is the server time in ms. The app uses it to sync its countdowns.
- **Game:** `code`, `round`, `phase` (`lobby` → `headstart` → `hunt` → `ended`), `hostId`, `settings`.
- **Timings:** `startedAt`, `huntStartsAt`, `endsAt`, `nextPingAt`, `lastPingAt`, `pingCount`.
- **Zone:** `zone` is `{lat, lng, radiusM, currentRadiusM}` or `null`.
- **Players:** `hidersLeft`, plus `players[]` with `{id, name, connected, rolePref, role, status, caughtAt, caughtBy}`.
- **You:** `you` is `{id, role, status, isHost, spectator, loc, outSince, revealed, catches, distanceM, pendingTagRequests[]}`.
- **Pings:** `pings[]` holds the last 5 pings: `{seq, at, marks[{playerId, lat, lng, acc, locAt, stale}]}`. Seekers get them during the hunt; spectators and everyone at the end get them too. Hiders get an empty list.
- **Live locations:** `live[]` is `{playerId, lat, lng, acc, at, kind}`. `kind` is `teammate` (another seeker), `revealed` (a hider outside the zone after the grace period) or `spectate` (everything, for caught players and after the round).
- **Events:** `events[]` is the last 15 public events.
- **Results:** `results` is set once the round has ended: `{winner, reason, players[{name, role, status, survivedMs, catches, distanceM, trail[[lat,lng]…]}]}`.

## Events REST API (`/api/...`)

- **Format:** JSON in, JSON out.
- **Errors:** failures return `{ "error": "message" }` with HTTP 400, 401, 404, 413 or 429.
- **Auth:** every call except sign-up and listing needs the headers `X-User-Id` and `X-User-Secret`.

| Method & path | Body / query | Notes |
|---|---|---|
| `POST /api/users` | `{name}` | Returns `{user:{id,name}, secret}`. Limited to 30 per hour per IP. |
| `GET/PATCH /api/users/me` | `{name}` | Read or rename yourself. |
| `GET /api/events` | `?lat&lng&radiusKm` | Public upcoming/live events, soonest first, with `distanceKm`. No auth. |
| `GET /api/events/mine` | — | `{hosting[], attending[]}` |
| `POST /api/events` | `{title, description, community, startsAt, durationMin, place:{lat,lng,name}, capacity, visibility, sponsor:{name}, rules}` | The host is the first attendee. The zone defaults to the meeting point. |
| `GET /api/events/:idOrCode` | — | Full page: attendees, `me`, announcements, `currentRound`, rounds, leaderboard. |
| `PATCH` / `DELETE /api/events/:id` | partial fields | Host only. `DELETE` cancels the event. |
| `POST` / `DELETE /api/events/:id/rsvp` | — | Respects capacity. The host can't un-RSVP. |
| `POST /api/events/:id/checkin` | `{lat,lng,acc}` | Within 300 m of the meeting point, from 2 h before the start until the end. |
| `POST /api/events/:id/checkin/:userId` | — | The host checks someone in by hand. |
| `GET /api/events/:id/messages?since=` | — | Attendees only. Polling-friendly: timestamps are strictly increasing. |
| `POST /api/events/:id/messages` | `{text}` | Max 500 characters; 30 per minute. |
| `POST /api/events/:id/announce` | `{text}` | Host only. Pinned on the event page and posted to the chat. |
| `POST /api/events/:id/rounds` | `{settings?}` | Host only, one open round at a time. Returns `{code}`. When that game ends, its results become points on the leaderboard. |

### Points per round
- **Hiders:** +1 per full minute survived after the head start, +10 if never found.
- **Seekers:** +5 per catch, +5 if the seekers win.
- **Anyone:** +2 per hider freed with Dhappa.
