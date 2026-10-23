<img src="docs/icon.png" width="120" alt="Chhupan Chhupai icon" align="right">

# Chhupan Chhupai 🙈

> **Made by SSK** · Last updated 23 Oct 2026

**Chhupan Chhupai** is real-life hide-and-seek for the whole mohalla. **Hiders hide. Seekers hunt. The phone is the referee.**

It is an Android app plus a small game server. There are two ways to play:

1. **Quick game:** friends join with a 6-character code.
2. **Chhupan Chhupai Nights (events):** a host publishes an event at a meeting point. People nearby find it, RSVP, chat and get a reminder. They check in when they arrive, and the host runs several rounds that feed one **leaderboard** for the night.

In every round:

- **Head start.** Hiders get a head start to hide inside a play zone on the map.
- **Pings.** Every few minutes, seekers see where each hider *was* (a "ping").
- **Zone rule.** Hiders who leave the zone become visible live.
- **Tagging.** A tag only counts when the hider confirms it on their phone.
- **Winning.** Survivors win when the clock runs out.
- **Dhappa! (optional).** Caught hiders wait at the base, and a free hider who reaches it frees them all.
- **Role reveal.** All phones show "?" and then flip to SEEKER or HIDER together when a round starts.
- **Share card.** After each round you can share a story card with your route and stats.

It is inspired by the SCRAM: Hide and Seek app. It was rebuilt from scratch with its own code, name and artwork, using only public information about how that game plays. Nothing was copied or decompiled. [docs/ANALYSIS.md](docs/ANALYSIS.md) has the full feature analysis, the design choices and the risks.

---

## What's in this repository

| Folder | What it is |
|---|---|
| `android/` | The Android app (Kotlin, Jetpack Compose, OpenStreetMap maps). |
| `server/` | The game server (Node.js + WebSocket). Every phone in a game connects to it. |
| `docs/` | `ANALYSIS.md` (how SCRAM works and how this rebuilds it) and `PROTOCOL.md` (the messages between app and server). |
| `.github/workflows/` | Automatic builds. Every push builds the APK and runs all tests on GitHub. |
| `render.yaml` | One-click setup for hosting the server for free on Render. |

---

## Getting it running

You need two things: **(A)** the server running somewhere on the internet, and **(B)** the app on everyone's phone.

### A. Put the game server online (free, about 10 minutes, one time)

All players in a game must connect to the **same** server. The simplest free option is **Render**:

1. Go to <https://render.com> and sign up with your GitHub account.
2. In the Render dashboard click **New → Blueprint**.
3. Choose this repository (`Sagarkharatmal/Ackruti-`) and the branch that contains this code (`main` once it is merged).
4. Render reads `render.yaml` and proposes a free web service called `chhupan-chhupai-server`. Click **Apply** / **Deploy**.
5. Wait until it says **Live**. Copy its address, e.g. `https://chhupan-chhupai-server.onrender.com`.
6. Check it works: open `https://<your-address>/health` in a browser. You should see `{"ok":true,...}`.

About the free plan: the server **sleeps after 15 minutes with no players** and takes about **1 minute to wake up** for the first game after that. While a game is running, the phones keep it awake. Render's free-plan limits change from time to time, so check their pricing page.

**Make events permanent (strongly recommended, free, about 5 minutes).** Render's free disk is wiped whenever the server restarts or sleeps. That's fine for quick games, but events, chat and leaderboards would be lost. Use a free PostgreSQL database instead:

1. Sign up at <https://neon.com> (free plan) and create a project.
2. Copy its **connection string**. It looks like `postgresql://user:password@ep-xxx.neon.tech/neondb?sslmode=require`.
3. In Render, open your service → **Environment** → set `DATABASE_URL` to that string → **Save** (Render redeploys).
4. The server creates its table automatically. Its log shows `loaded N documents from PostgreSQL`.

Free plans and limits change; check Neon's pricing page. Render's own free Postgres is deleted after 30 days, so it's not a good fit here.

**Alternative: test at home without the internet**

- Install Node.js 20 or newer from <https://nodejs.org> on a laptop.
- Then run:

  ```bash
  cd server
  npm install
  npm start            # listens on port 8080
  ```

- Connect every phone to the **same Wi-Fi** as the laptop.
- Find the laptop's IP address (e.g. `192.168.1.20`) and use `192.168.1.20:8080` as the server address in the app.

### B. Get the app (APK) onto phones

The app is built automatically by GitHub every time code is pushed.

1. Open the repository on GitHub → **Actions** tab → the latest green **Chhupan Chhupai** run.
2. Scroll to **Artifacts** and download **ChhupanChhupai-apk-N**. It is a zip; inside is `ChhupanChhupai-N.apk`. Downloading artifacts requires being signed in to GitHub.
   - **For friends without a GitHub account:** push a tag like `v1.0.0`. The workflow then publishes a public **GitHub Release** with the APK attached, and anyone can download it from the **Releases** page.
3. Send the APK to each phone (WhatsApp as a *document*, Google Drive, USB…).
4. On the phone, tap the APK. Android will ask to **allow installs from this source**: allow it, then install.
5. Open **Chhupan Chhupai → Settings**, paste the server address from step A, tap **Save & test**. You should see *"Connected!"*.

Optional: to skip step 5 for everyone, go to GitHub → **Settings → Secrets and variables → Actions → Variables** and add a variable `CHHUPAN_SERVER_URL` with your server address. Future builds will have it built in.

### C. Play a quick game

1. **Host:** tap **Create a game**. Drag the map so the circle covers your play area, set the round length, head start and ping timer, then tap **Create game**.
2. **Everyone else:** tap **Join with a code** and enter the code shown in the host's lobby (use the share button).
3. Each player picks **Hider**, **Seeker** or **Random**.
4. The host taps **Start**. Allow location ("While using the app") and notifications when asked.
5. **Hiders** run and hide during the head start. **Seekers** see an "Eyes closed!" screen.
6. During the hunt:
   - Seekers get pings and tap **Tag** on a hider they caught.
   - The hider confirms.
   - Hiders can also tap **I've been caught**.
7. At the end, everyone sees the **results map** with each player's route, time survived and distance. The host can tap **Play again**.

### D. Host a Chhupan Chhupai Night (event)

1. **Home → Events near you → Host an event.**
2. **Step 1: drag the map** so the crosshair is on the meeting point, give it a name (e.g. "Shivaji Park, Gate 5") and pick the zone size.
3. **Step 2: fill in the details.**
   - name, club or college, description
   - sponsor (optional)
   - date and time, length
   - max people
   - public (anyone nearby can find it) or private (code only)
   - the round rules
4. Tap **Publish**, then use the **share** icon to invite people on WhatsApp or Instagram.
5. Players: **Events near you** → open it → **I'm in!** They join the chat and get a reminder.
6. On the day, players tap **I'm here — check in** within 300 m of the meeting point. You can also check people in from the **People** tab.
7. Tap **Start round 1**. Everyone who RSVPed sees **Join round 1 now**. Huddle up, hold the phones together, and start the round in the lobby.
8. After each round the results go on the **Ranking** tab. Start the next round when everyone's back.

Tips:

- On Xiaomi, Oppo, Vivo, Realme, OnePlus and Samsung phones, set the app's **battery usage to "Unrestricted"** so Android doesn't stop it mid-game.
- 3 or more players is best; the app balances teams at about one seeker for every four players.
- Agree on safety rules: stay off roads and private property, keep an eye on younger kids, and pick a meeting point for the end of the round.

---

## For developers

### Server

```bash
cd server
npm install
npm test                 # 34 tests: rules, Dhappa, visibility filtering, events, storage, full WebSocket + HTTP flows
TEST_DATABASE_URL=postgres://user:pass@localhost/db npm test   # also tests the PostgreSQL store
npm start                # PORT env var, default 8080
npm run simulate         # bots play a ~1 minute round against ws://localhost:8080/ws
node scripts/simulate.js wss://your-server.onrender.com/ws   # check a deployment
```

### Android app

- Open the `android/` folder in **Android Studio** (Ladybug or newer). It needs JDK 17 and Android SDK 35.
- Or build from the command line:

  ```bash
  cd android
  ./gradlew assembleDebug        # app/build/outputs/apk/debug/app-debug.apk
  ./gradlew testDebugUnitTest    # unit tests
  ITEST_URL=ws://127.0.0.1:8080/ws ./gradlew testDebugUnitTest   # + end-to-end test against a running server
  ```

- The optional built-in server address is read from Gradle property `-Pchhupan.serverUrl=...` or the env var `CHHUPAN_SERVER_URL`.

| Piece | Choice |
|---|---|
| Language / UI | Kotlin 2.0, Jetpack Compose, Material 3 |
| Map | osmdroid 6.1 with OpenStreetMap tiles. Switch to a commercial tile provider before a large public launch; see ANALYSIS §5. |
| GPS | Google Play services Fused Location in a `location` foreground service |
| Network | OkHttp WebSocket + kotlinx.serialization |
| Min / target Android | 8.0 (API 26) / 15 (API 35) |

### Release signing (recommended before sharing widely)

Without signing secrets, the "release" APK is signed with a throw-away debug key. The problem is that each CI build uses a different key, so **phones must uninstall the old version before installing a new one**. To get proper in-place updates:

1. Create a keystore once and keep it safe. If you lose it you can never update the app again.

   ```bash
   keytool -genkeypair -v -keystore chhupan-release.jks -alias chhupan -keyalg RSA -keysize 2048 -validity 10000
   base64 -w0 chhupan-release.jks > keystore.b64
   ```

2. In GitHub → **Settings → Secrets and variables → Actions → Secrets**, add:
   - `CHHUPAN_KEYSTORE_BASE64`: the contents of `keystore.b64`
   - `CHHUPAN_KEYSTORE_PASSWORD`
   - `CHHUPAN_KEY_ALIAS`: e.g. `chhupan`
   - `CHHUPAN_KEY_PASSWORD`
3. **Never commit the keystore**: this repository is public.

### Server settings (environment variables)

| Variable | Meaning |
|---|---|
| `PORT` | Port to listen on (Render sets this). Default 8080. |
| `DATABASE_URL` | PostgreSQL connection string. When set, users, events, chat and leaderboards are stored there. |
| `DATA_FILE` | Without `DATABASE_URL`, the JSON file used for storage (default `data/chhupan.json`). |

### Privacy

- The app sends your location to the game server **only while you are in a game**.
- The server shows it to other players **only as the rules allow**: hiders are never shown live unless they leave the zone or the round is over.
- Games live only in the server's memory and are deleted after the game ends (or after 8 hours at most).
- **Events** store your chosen display name, RSVPs, check-in times, chat messages and round results. They do **not** store your location trail. The only location used is the one-off check-in position, which is compared with the meeting point and not saved.
- There are no email or phone accounts: the app gets a random ID and secret from the server. No ads, no analytics.

---

*Made by SSK · 23 Oct 2026*
