# How SCRAM works, and how Chhupan Chhupai rebuilds it

> **Made by SSK** · Written 23 Oct 2026, 20:30 UTC · Events & marketing section added 23 Oct 2026, 21:10 UTC

This document has three parts. First, what SCRAM: Hide and Seek does, based only on public information. Second, how its features map to Chhupan Chhupai. Third, what is still unknown, plus the legal and safety points to keep in mind.

Every claim is marked as one of these:

- **[Fact]**: stated publicly by SCRAM, on its Google Play listing, its website or its Instagram profile.
- **[Inferred]**: my engineering reading of what the app must do to deliver those facts.
- **[Unknown]**: not answerable from public information.

---

## 1. Method: clean-room, not decompiled

- I **did not** download, decompile or copy SCRAM's APK, code, artwork or text. There are two reasons.
  - **Legal:** the APK's code, logo and artwork are copyrighted, and the name "SCRAM" is likely a trademark. Copying any of them would expose you to takedowns or legal claims.
  - **Practical:** this build environment cannot reach the Play Store anyway.
- What I used instead:
  - your Instagram screenshot
  - the Google Play listing for `com.scram.app`
  - the official site, scramhideandseek.com

  Those pages could not be fetched directly from this environment, so their content came through web-search summaries of them. Exact wording and default values may therefore differ slightly from the live pages.
- **Game rules and mechanics are generally not protected by copyright. The specific code, art, name and text are.** Chhupan Chhupai re-implements the *idea* (real-life hide and seek, refereed by phones) with its own code, name, icon, colours and wording. This is general information, not legal advice.

Sources:

- Google Play listing: <https://play.google.com/store/apps/details?id=com.scram.app>
- Official site: <https://scramhideandseek.com/>
- Instagram `@scramhideandseek`: your screenshot, taken 23 Oct 2026

---

## 2. What SCRAM is

### 2.1 Positioning (from your screenshot)

| Item | Value | Type |
|---|---|---|
| Name | "SCRAM: Hide and Seek", handle `scramhideandseek`, verified badge | Fact |
| Tagline | "Hide-and-seek for the whole neighbourhood. Runners hide. Hunters chase. The phone referees." | Fact |
| Platforms | "Join the waitlist for iPhone and download Android" | Fact |
| Audience size | 33.1K followers, 16 posts; one reel at about 1M views, others about 14.5K and 3.5K | Fact (as of the screenshot) |
| Marketing | Night-time videos of a ring of phones with red screens, the text "this is manhunt.", a collab with `@bucketlistbrandd` | Fact |
| Post-game screen | One reel shows two phone screens with route maps and stats such as "Caught after 35:26" and "1.6 km" | Fact (seen in the screenshot thumbnail; the small text is partly unreadable, marked low-confidence) |

### 2.2 Game rules

| # | Rule | Type |
|---|---|---|
| 1 | Two roles: **Runners** (hide) and **Hunters** (chase). Players pick **Hunter, Runner or Random**. | Fact |
| 2 | One friend **hosts**, sets the rules and shares a **6-character game code**. | Fact |
| 3 | The host **drags the map to place a play zone**, from "a few blocks" up to **3 km across**. The zone can be **turned off** to play anywhere. | Fact |
| 4 | The host sets the **round length**, the **head start** and the **ping frequency**. | Fact |
| 5 | **Head start:** runners go and hide inside the zone while hunters wait. | Fact |
| 6 | **Pings:** every few minutes, hunters see **each runner's last known location**. **Runners never see hunters.** | Fact |
| 7 | **Tagging:** hunters tag runners **in person**. The **runner confirms it in the app** and is out, but **can keep watching**. | Fact |
| 8 | **Winning:** runners still free when time runs out win. | Fact |
| 9 | **Zone penalty:** leaving the zone starts a **30-second** countdown, after which hunters **see you live until you return**. | Fact |
| 10 | The zone can optionally **shrink over time**. | Fact |
| 11 | A game needs **at least 3 players**. | Fact |
| 12 | Location is shared **only during a round** and **only with players in the same game**. | Fact |

### 2.3 What the app must be doing underneath

| Need | Most likely solution | Type |
|---|---|---|
| Several phones see one shared game in real time | A **cloud backend** with real-time messaging (WebSocket, Firebase or similar) | Inferred |
| Runners never see hunters | The **server filters what each phone receives**. A client-side filter would be trivial to cheat. | Inferred |
| Location keeps flowing while the phone is in a pocket | An Android **foreground service** of type `location` with a persistent notification | Inferred (Android requires this) |
| Map for placing the zone and showing pings | A map SDK (Google Maps, Mapbox or OpenStreetMap) | Inferred; which one is Unknown |
| "The phone referees" | The server decides the clock, pings, zone breaches, tags and the winner | Inferred |
| Route maps and stats after the game | The server keeps each player's location trail for the round | Inferred from the reel thumbnail |
| Reliable alerts with the screen off | Vibration and notifications for pings, tags and zone warnings | Inferred |

### 2.4 Unknowns

These cannot be known without SCRAM's private data or code:

- backend technology and hosting
- exact default values (round length, ping interval)
- maximum number of players
- business model (ads, in-app purchases, subscriptions)
- anti-cheat beyond role filtering, such as detecting faked GPS
- whether accounts or logins exist
- data retention policy

---

## 3. SCRAM features mapped to Chhupan Chhupai

| SCRAM | Chhupan Chhupai | Where in the code |
|---|---|---|
| Runner / Hunter / Random | **Hider / Seeker / Random**. Teams are balanced automatically at about one seeker per four players, and there is always at least one of each. | `server/src/game.js` → `assignRoles` |
| Host plus 6-character code | Host plus 6-character code. The alphabet leaves out the look-alikes 0/O and 1/I/L, so codes are easy to read out on the street. Share button included. | `server/src/ids.js`, `LobbyScreen.kt` |
| Drag map to place zone, up to 3 km | Drag the map so the crosshair marks the centre; zone 100 m to **3 km across**; can be switched off. | `RulesEditor.kt` → `ZonePicker` |
| Round length, head start, ping timer | Round length 5–120 min (server accepts up to 180), head start 30 s–15 min, ping every 30 s–15 min, optional ping as soon as the hunt starts. | `RulesForm`, `sanitiseSettings` |
| Head start | Hiders see a countdown and a "Go hide!" screen. Seekers get a full-screen "Eyes closed!" cover, and the server sends them no hider data at all. | `GameScreen.kt` → `SeekerWaitCover`, `Game.viewFor` |
| Pings of last known location | Server snapshots every active hider at each ping. Seekers see the newest ping labelled with name and age, plus older pings faded. Hiders get a buzz: "you were just pinged, move!" | `Game.takePing`, `buildOverlays` |
| Runners never see hunters | Enforced **on the server**: hiders receive no other player's location. A unit test checks the JSON for leaks. | `viewFor`, `game.test.js` |
| Zone penalty: 30 s, then live | Grace period adjustable from 10 to 120 s (default 30). After it, the hider is shown **live** to seekers until back inside. GPS accuracy gets up to 30 m benefit of the doubt. | `updateZoneStatus` |
| Shrinking zone | Linear shrink during the hunt down to 10–90 % of the original radius (default 40 %). | `currentRadiusM` |
| In-person tag, runner confirms | Two ways. **(a)** The seeker taps *Tag*, and the hider must answer *Yes, I'm caught* or *No* within 60 s. **(b)** The hider taps *I've been caught* and picks who caught them. | `requestTag`, `respondTag`, `reportCaught` |
| Caught runner keeps watching | Caught hiders become **spectators** and see everyone live. | `viewFor` (spectator) |
| Survivors win at time-up | Same rule. Seekers win if everyone is caught. Hiders also win if every seeker leaves. The host can end the round early (nobody wins). | `checkForWinner`, `finish` |
| Route maps and stats | Results screen shows a map of everyone's route and, for each player: time survived or "caught by X after mm:ss", distance moved, catches. | `ResultsScreen.kt`, `Game.finish` |
| Location only during a round, only with that game | Location is sent only while you are in a game, only to that game's server, and only as the rules allow. Nothing is stored on disk; games are deleted from memory when finished. | `GameLocationService.kt`, `server.js` cleanup |
| *(new)* | **Seekers see each other live**, which helps them coordinate. | `viewFor` (teammate) |
| *(new)* | **Play again** keeps the same group and code. | `Game.rematch` |
| *(new)* | **Auto-rejoin** after losing signal or restarting the app (saved session token). | `GameConnection.kt` |
| *(new)* | **Self-hosted server**: no Google or Firebase account needed, and you own the data. | `server/`, `render.yaml` |

---

## 4. Architecture

```
 Phone (Android app)                         Game server (Node.js)
 ───────────────────                         ─────────────────────
 Compose UI  ◄── StateFlow ──┐               WebSocket /ws
                             │               ┌──────────────────────────┐
 GameConnection (OkHttp WS) ◄┼── JSON ─────► │ Game (one per code)       │
   • create / join / resume  │               │  • lobby, roles           │
   • auto-reconnect          │               │  • clocks: head start,    │
 GameLocationService ────────┘ loc every 2-4s│    pings, end of round    │
   • foreground "location"                    │  • zone / shrink / reveal │
   • fused GPS, high accuracy                 │  • tags, winner, results  │
 Alerts (vibrate + notifications)            │  • viewFor(player): only  │
 osmdroid map (OpenStreetMap tiles)          │    what that role may see │
                                             └──────────────────────────┘
```

- **What is stored where?** Live games are kept in memory only. Users, events, chat and leaderboards go in a durable store: a JSON file by default, or **PostgreSQL** when `DATABASE_URL` is set. The free hosting disk is wiped on restart, so a free Neon Postgres database is recommended (README → Events storage).
- **Why WebSocket and Node instead of Firebase?** A server you can run yourself:
  - is fully testable here: 22 automated tests plus a bot simulation;
  - keeps all secret game state on the server, so a hacked app cannot see seekers;
  - needs no Google Cloud account or billing;
  - can be deployed for free.

  The trade-off is that games live in memory: if the server restarts mid-round, that round is lost.
- **Why OpenStreetMap (osmdroid) instead of Google Maps?** Google Maps needs an API key with billing enabled, while OSM is free. The trade-off is that OSM's public tile servers allow only light use (see section 5).

---

## 5. Risks, limits and things to verify before a public launch

1. **Map tiles.** The OpenStreetMap Foundation's [tile usage policy](https://operations.osmfoundation.org/policies/tiles/) forbids **heavy** use without permission and requires an app-specific User-Agent. The app sets `com.ssk.chhupanchhupai/<version>`. That is fine for friends and family. **Before a public Play Store launch, switch to a commercial tile provider** (MapTiler, Stadia, Thunderforest and others, usually free up to a quota) or to Google Maps.
2. **Free server sleeping.** On Render's free plan the server goes to sleep after 15 minutes with no HTTP request or WebSocket message, and takes about a minute to wake. Since Feb 2026 WebSocket messages keep it awake ([Render changelog](https://render.com/changelog/free-web-services-now-remain-active-while-receiving-websocket-messages)). During a game, phones send location every few seconds, so it stays awake. The first game after a quiet period has a ~1-minute delay. Free-plan hour limits change; check Render's pricing page.
3. **Phone battery and background limits.** High-accuracy GPS every 2–4 s uses battery: plan for a noticeable drain over a 30–60 minute round. Some brands (Xiaomi, Oppo, Vivo, Realme, OnePlus, Samsung with "deep sleep") kill background apps aggressively. Players should set battery usage for the app to "Unrestricted".
4. **GPS accuracy.** GPS can be off by 5–30 m outdoors, and much worse indoors or between tall buildings. The game gives up to 30 m benefit of the doubt at the zone edge. Tags are confirmed by humans, not by GPS distance.
5. **Cheating.** Location spoofing apps can fake GPS. Android's "mock location" flag could be detected later. The honour system for tag confirmation matches SCRAM's design.
6. **Google Play publishing.** For a Play Store release you will need:
   - a privacy policy URL (the app handles precise location);
   - the Data safety form;
   - a declaration for the `FOREGROUND_SERVICE_LOCATION` foreground-service type, which may require a short demo video.

   These requirements change; check the current Play Console policy pages when you publish.
7. **Name and trademark.** "Chhupan Chhupai" is a common Hindi name for the game, so it is descriptive and may be hard to register as a trademark. Before investing in branding, search the Indian trademark registry (IP India) and the Play Store for existing apps with that name.
8. **Physical safety.** Running around neighbourhoods at night carries real risks: traffic, private property, strangers, younger children. The "How to play" screen includes safety guidance. Keep it, and consider an in-app safety reminder before each round.

---

## 6. Marketing and events: what SCRAM promotes, and what we built from it

### 6.1 Who is behind SCRAM

| Finding | Type |
|---|---|
| SCRAM: Hide and Seek is published by **Moveera Pty Ltd**, with a contact address in Mount Coolum, Queensland, Australia. The app is free with in-app purchases. AppBrain reports 10,000+ downloads. | Fact (Play listing, as summarised by search) |
| Moveera also publishes **"Moveera: Fitness Events"**, a local-events app for run clubs, yoga and pickleball in South East Queensland. Its features:<br>• discover and book events<br>• save events to your week<br>• reminders<br>• see who is attending<br>• follow communities and hosts<br>• chat before the day<br>• tap-to-check-in on the morning<br>• an automatic group chat for everyone who registers<br>• free listing for hosts<br>• sponsor showcasing<br>Its site claims 1,226 "movers", 43 communities and 434 events hosted; these figures may be outdated. | Fact (moveera.com.au, Play listing) |
| SCRAM grew out of an **events-and-community company**, not a games studio. The "everyone comes to one place" feel of the reels comes from that background: run-club style meet-ups where a crowd plays together. | Inferred |
| The Play listing says games are **private, joined by code, and deleted after seven days**. | Fact |
| The site says the play area can be a **street, park, campus or campsite**. | Fact |
| The site now lists **iPhone and Android** (the earlier Instagram bio said "waitlist for iPhone"). | Fact (wording changed over time) |

### 6.2 What their marketing shows

These are taken from your Instagram screenshot and the SCRAM TikTok account as summarised by search.

| What the content shows | What it tells us | Type |
|---|---|---|
| A ring of 15–20 phones, all glowing red, held together in the dark (≈1M views) | **Big-group, in-person events** are the hero moment. The phones-together shot is the signature visual. | Fact (visual) / Inferred (meaning) |
| Phones held together showing orange **"?"** marks, captioned **"this is manhunt."** | A **role-reveal ritual** before the round starts: everyone finds out together who hunts. | Fact (visual) / Inferred |
| A reel with two phone screens showing **route maps** and **"Caught after 35:26 · 1.6 km"** | **Shareable post-game stats** turn every game into content. | Fact (low-confidence on exact numbers) |
| A collab with `@bucketlistbrandd` | **Brand / creator partnerships** around events. I found no public details about this partner. | Fact (handle) / Unknown (what it is) |
| TikTok captions such as "ready for manhunt?", #manhunt #scram | They position the game as **"manhunt"**, an established real-life game, to ride existing search interest. | Fact |

### 6.3 Competitor landscape (late 2026)

According to a comparison page at manhuntgame.com (provisional ratings; the author says they haven't played every app):

- **HideZone:** 2–100+ players, ads, Pro tier.
- **Hunting Game:** free up to 4 players, Pro up to 20, power-ups.
- **On The Run:** city-wide games, leaving the area pings your position.
- **Manhunt: Hide & Seek:** Pro adds zone alerts and location history.
- **SLAP:** sign-up required, paid power-ups.
- **Hide Game:** up to 10 players free, 50 with Pro, replays.
- **None of the apps it checked listed the "jail & jailbreak" mechanic.**

### 6.4 What Chhupan Chhupai now has because of this

| Inspiration | Chhupan Chhupai feature |
|---|---|
| Moveera's event discovery and booking | **Chhupan Chhupai Nights**: hosts publish an event (name, club or college, meeting point on a map, date and time, length, capacity, sponsor, public or private). Players see **events near them** (within 50 km, soonest first) or open a private one by code. |
| "See who's attending", follow communities | Event page shows the host, community, going/capacity, the **attendee list** and who has arrived. "My events" lists what you host and attend. |
| "Chat before the day" / automatic group chat | **Group chat** for everyone who RSVPs, plus **pinned host announcements** ("Meet at gate 5"). |
| Reminders | **Reminder notification** 1 hour before (10 minutes if booked late). It is re-armed after the phone restarts. |
| "Check in with a tap" | **GPS-verified check-in** within 300 m of the meeting point, from 2 hours before the start. The host can also check people in by hand. |
| Crowd events in the reels | The host starts **rounds from the event** and players join with one tap. Rounds hold **up to 100 players**, and the server sends updates every 2 s in big games to keep data use reasonable. |
| Results that become content | Each round's points go on the **event leaderboard** (🥇🥈🥉). **Share my round card** makes a 1080×1920 story image with your route, headline stat ("SURVIVED 24:10" or "3 CATCHES"), event name and "Presented by" sponsor. |
| The "?" phones-together ritual | **Synchronised role reveal**: when a round starts, every phone pulses a big "?" for 2.5 s, then all flip at the same moment (server clock) to a full-screen SEEKER (red) or HIDER (teal). |
| Sponsor showcasing | **Sponsor** shown on event cards, the event page, the invite text and every share card. |
| The gap: nobody lists jail/jailbreak | **"Dhappa!"**, the classic Indian Chhupan Chhupai rescue, as an optional rule:<br>• caught hiders wait at the **base** in the zone's centre;<br>• a free hider who reaches it frees everyone;<br>• there is a 2-minute cooldown, and jailed hiders can't spectate, so they can't spy on seekers. |

**Points rules** (shown in the app):

- **Hiders:** 1 point per full minute survived after the head start, and +10 if never found.
- **Seekers:** 5 points per catch, and +5 if the seekers win.
- **Dhappa rescues:** +2 per team-mate freed.

### 6.5 Still not built (honest gaps)

- Following hosts or communities.
- Push notifications from the server, e.g. "a new event near you". That needs Firebase Cloud Messaging and therefore a Google project; the app currently polls while it is open.
- Payments or ticketing.
- Photo uploads.
- An organiser web dashboard.
- Moderation and reporting tools for public chat. **Needed before a public launch.**

---

## 7. Ideas for later versions

- Hindi / Marathi / Gujarati UI translations
- Chor-Police theme pack
- Sounds for pings
- Deep link `chhupan://event/CODE` and a QR code at the meeting point for one-scan check-in
- Follow communities
- Push notifications (needs Firebase Cloud Messaging)
- Season leaderboards across events
- Mock-location detection
- iOS app: the server and protocol already work for any client

---

*Made by SSK · 23 Oct 2026*
