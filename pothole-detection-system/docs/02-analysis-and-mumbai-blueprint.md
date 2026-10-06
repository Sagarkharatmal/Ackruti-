# Pothole Detection System (Mumbai): Analysis of the Bengaluru App and Blueprint for a Better One

> **Made by SSK** · Prepared 06 October 2026, 10:21 IST · Status: **DRAFT v0.1, for your review before any code is written**

Every statement below carries one of these labels:
- ✅ **Verified**: confirmed from the video itself or from a cited news or official source.
- 🔶 **Inferred**: my reasoning from the evidence. Likely, but not confirmed.
- 💭 **Proposal / speculation**: my design idea for the Mumbai app.

---

## Part A: What the Bengaluru app ("Pothole Reporter" by Gaurav Sen) actually does

### A1. The pipeline, step by step

```
 ┌──────────┐   ┌─────────────┐   ┌──────────────┐   ┌───────────────────┐   ┌────────────────┐
 │ 1.CAPTURE│──▶│ 2.DETECT    │──▶│ 3.CLASSIFY   │──▶│ 4.ATTRIBUTE       │──▶│ 5.COMPLAINT    │
 │ dashcam/ │   │ AI vision   │   │ small/medium/│   │ GPS point → road  │   │ photo+location │
 │ phone +  │   │ on frames   │   │ large; not a │   │ stretch → tender  │   │ +tender no.    │
 │ GPS +    │   │ ~every 10 m │   │ speed-breaker│   │ (of ~2,900) →     │   │ +contractor    │
 │ accel.   │   │             │   │              │   │ contractor+officer│   │ +officer, ≈4 s │
 └──────────┘   └─────────────┘   └──────────────┘   └───────────────────┘   └────────────────┘
```

| # | Step | What it does | Evidence |
|---|---|---|---|
| 1 | Capture | A dashcam or phone in a car mount records the road. GPS and an accelerometer (motion sensor) log alongside the video. | ✅ Video 0:10, X post, CNBC‑TV18 footage |
| 1a | Sampling | Still frames are taken "about every 10 meters". The screen shows "78 checked, 4 analyzing". | 🔶 Read from a blurry phone screen; low confidence |
| 2 | Detect | AI vision models find potholes in the frames. The app was built with OpenAI Codex, and frames are sent to OpenAI models for analysis. | ✅ [ThePrint](https://theprint.in/tech/pothole-reporter-how-bengaluru-techie-turned-yrs-of-frustration-over-broken-roads-into-ai-led-action/3025029/), [Analytics Insight](https://www.analyticsinsight.net/news/bengaluru-engineer-builds-ai-tool-to-spot-potholes-track-responsible-contractors) |
| 3 | Classify | Each pothole is labelled small, medium or large, and potholes are told apart from speed breakers. | ✅ Video 0:13; news reports |
| 4 | Attribute | The pothole's GPS location is matched against about **2,900 government road contracts** to find the **tender number, contractor and supervising officer**. | ✅ Video 0:24; [Business Today](https://www.businesstoday.in/technology/artificial-intelligence/story/bengaluru-engineer-builds-ai-system-that-detects-potholes-and-prepares-complaints-549579-2026-08-17) |
| 5 | Complaint | A "complaint-ready file" is produced in about **4 seconds**, containing the photo, exact location, tender number and responsible officer. | ✅ Video 0:29–0:34 |
| 6 | Filing | The complaint is "ready to be filed". It is **not** clear that the app submits it to the city automatically. | 🔶 The wording "ready to be filed" suggests a person still files it |

### A2. Results he showed

- One drive to work: **12 potholes detected → 12 complaint files** ✅
- Still **in beta**; "doesn't fill a single pothole" ✅
- Commissioners from Bengaluru and Chennai and some politicians contacted him, but there is "no ground-level implementation yet" ✅ (OneIndia, Oct 2)
- Self-funded, "completely free for everyone" ✅ ([Analytics Insight](https://www.analyticsinsight.net/news/bengaluru-engineer-builds-ai-tool-to-spot-potholes-track-responsible-contractors))
- **I found no public source code or GitHub link.** ✅ (searched; none found)

### A3. Roles in his system

| Role | What they do | Evidence |
|---|---|---|
| **Driver / citizen-reporter** | Mounts the phone, drives, presses Start/Stop | ✅ |
| **AI engine** | Detects, classifies, attributes | ✅ |
| **Contract database** | About 2,900 tenders mapped to road stretches | ✅ |
| **Contractor** (named, passive) | Expected to fix potholes free under warranty | ✅ |
| **Officer** (named, passive) | Supervises the stretch; receives the complaint | ✅ |
| **Civic body / government** | Not integrated. Only "interested". | ✅ |

There is **no** role for a reviewer, verifier, the public, journalists, or the contractor responding back. 🔶 Nothing in any coverage mentions one.

### A4. The core idea in one line

> **Most of the value is not in detecting potholes. It is in naming who is responsible.** The video's last line says this directly: *"The potholes were never the problem. Nobody being named was."*
> Pothole detection by AI is a solved, commodity problem. Linking a GPS point to a contract, a warranty period and a person is the new part.

---

## Part B: Gaps and weaknesses in the Bengaluru app, which the Mumbai app should fix

| # | Gap | Why it matters | Label |
|---|---|---|---|
| 1 | **Every frame goes to a cloud LLM (OpenAI).** | Cost per km grows with usage. It needs connectivity and is slow. City-scale use would be expensive. | 🔶 |
| 2 | **No proof the warranty still applies.** Finding the tender is not the same as knowing the road is still inside its defect liability period (DLP). | A complaint against a contractor whose DLP has expired is wrong, and could be defamatory. | 🔶 |
| 3 | **Only one city body's contracts are covered.** | Mumbai roads belong to **about 15 agencies** (BMC, MMRDA, MSRDC, PWD, MbPT, …) ✅ [LiveLaw](https://livelaw.in/news-updates/bombay-high-court-bmc-potholes-bad-road-conditions-211159). Matching only BMC tenders would blame the wrong body. | ✅/🔶 |
| 4 | **Nothing closes the loop.** Complaints are "ready to be filed" but not tracked. | Accountability needs public tracking: filed → acknowledged → fixed → re-broken. | 🔶 |
| 5 | **Single reporter, no cross-checking.** | One phone's GPS can be off by 5–15 m. Without corroboration from several reports there will be false positives and wrongly named contractors. | 🔶 |
| 6 | **Privacy.** Dashcam frames capture faces and number plates. | India's **DPDP Act 2023** applies to personal data. Faces and plates must be blurred before anything is stored or published. | 💭 Needs a legal check |
| 7 | **No severity or risk model** beyond small/medium/large. | Depth, water-logging, a busy junction, a school zone or a two-wheeler route matter more than width alone. | 💭 |
| 8 | **No repair verification.** | BMC already claims fixes. Nobody independently checks that the fix happened, or that it lasted. | 💭 |

---

## Part C: Why Mumbai is the right city for this

| Fact | Source |
|---|---|
| BMC has its **own** pothole apps: **"Pothole QuickFix"** (geo-watermarked photo complaints) and **MyBMC / "MyBMC MARG"**. | ✅ [Google Play: Pothole QuickFix](https://play.google.com/store/apps/details?id=com.bmc.potholequickfix&hl=en) · [MyBMC](https://play.google.com/store/apps/details?id=in.cdac.gov.mgov.mcgm&hl=en_IN) |
| BMC has directed that pothole complaints be **resolved within 24 hours**. | ✅ [Marksmen Daily](https://marksmendaily.com/politics/bmc-orders-24-hour-resolution-of-pothole-complaints/) |
| **Roads still under DLP must be repaired by the contractor free of cost**, with penal action if they don't. | ✅ (as above) |
| **Proposed** Comprehensive Trenching & Reinstatement Policy 2026: **DLP of 10 years for cement-concrete roads, 5 years for non-concrete roads**. | ✅ [Free Press Journal](https://www.freepressjournal.in/mumbai/mumbai-bmc-proposes-new-trenching-policy-with-5x-penalty-for-illegal-digging-10-year-road-defect-liability). ⚠️ It is a *proposal*. Whether it has been adopted, and what DLP each individual contract carries, must be checked contract by contract. |
| A BMC official cited a **3-year DLP** for pothole-repair work (rapid-hardening concrete / reactive asphalt). | ✅ [FPJ](https://www.freepressjournal.in/mumbai/mumbai-finally-bmc-decides-rapid-hardening-core-reactive-asphalt-to-fill-potholes) |
| **Concretisation project:** about ₹17,700 crore, **2,179 roads / 715 km**, about 80% done; Phase I due end-2026, Phase II May 2027. | ✅ [ThePrint](https://theprint.in/india/governance/3-years-rs-17000-crore-later-mumbais-ambitious-concrete-roads-project-is-80-complete/2950617/) |
| **Bombay High Court (Oct 2025, suo motu):** ₹6 lakh compensation for each pothole or open-manhole death, and ₹50,000–₹2.5 lakh for injuries. The amount is **recoverable from the officers, engineers or contractors responsible**. | ✅ [LiveLaw](https://www.livelaw.in/amp/high-court/bombay-high-court/bombay-maharashtra-roads-pothole-deaths-civic-body-contractor-compensation-306827), [Moneylife](https://www.moneylife.in/article/rs6-lakh-compensation-for-pothole-deaths-bombay-hc-slams-civic-apathy-by-bmc-pwd-msrdc/78577.html) |
| BMC tenders are published at **portal.mcgm.gov.in/tenders** (PDFs named like `ETH_<number>_<date>.pdf`) and on **mahatenders.gov.in**. | ✅ [example tender PDF](https://portal.mcgm.gov.in/irj/go/km/docs/documents/Tenders/ETH/ETH_8000089458_260525.pdf) |

🔶 **What this adds up to.** Mumbai is about to have **roughly 715 km of brand-new concrete roads under long warranties**, with named contractors per ward and package. A High Court order already makes officers and contractors financially liable. An *independent* tool that proves "this pothole is on package X, contractor Y, still under DLP until date Z" has real legal and political force in Mumbai, arguably more than in Bengaluru.

🔶 **Positioning.** BMC's own apps take complaints. They do **not** publicly name the contractor, show the DLP status, or let citizens audit whether repairs were done. The Mumbai app should **complement** these apps, for example by filing *into* them, and be the independent accountability layer on top.

---

## Part D: Blueprint for the Mumbai app 💭

Working name: **Pothole Detection System (Mumbai)**. You asked for this name; a public brand name can come later.

### D1. User roles

| Role | Can do |
|---|---|
| **Citizen (walk/drive)** | Live dashcam mode, single-photo report, voice report in Marathi/Hindi/English, track their own complaints |
| **Fleet partner** (taxis, auto unions, delivery fleets, BEST buses) | Passive, always-on scanning. Covers the city daily for no extra effort. |
| **Verifier / volunteer moderator** | Confirms AI detections and approves attribution before any public naming |
| **Journalist / researcher / RTI activist** | Read-only analytics, contractor scorecards, data export, auto-drafted RTI applications |
| **Ward officer / engineer (BMC/MMRDA/…)** | Sees complaints for their jurisdiction, marks "repaired" with a photo, disputes attribution |
| **Contractor** | Right of reply: views complaints on their packages, contests them with evidence, logs repairs |
| **Legal aid / NGO** | Builds case files for High Court compensation claims (accidents, injuries) |
| **Admin** | Data quality, crawler health, abuse handling |

### D2. Features

**Tier 1: MVP (must-have)**
1. **Detection on the phone**: a small vision model (YOLO-class) runs on the device, so it works offline, costs nothing per frame, and keeps raw video private. The cloud is used only for confirmation.
2. **Sensor fusion**: an accelerometer jolt and a visual pothole at the same spot give higher confidence.
3. **Severity score (0–100)**: size, estimated depth, water-logging, traffic class of the road, nearby schools or hospitals, two-wheeler risk.
4. **Auto-blurring of faces and number plates** before anything is uploaded.
5. **Multi-agency jurisdiction engine**: GPS → road segment → **owning agency** (BMC ward / MMRDA / MSRDC / PWD / MbPT / Railways). This is the right order: find the agency *before* looking for a contractor.
6. **Tender intelligence crawler**: covered separately in D3.
7. **DLP check**: the tender's completion date plus its DLP clause give "under warranty until DD-MM-YYYY", or "warranty expired, so BMC's own maintenance budget applies".
8. **Complaint pack**: PDF/JSON with photo, GPS, timestamp, severity, agency, ward, tender no., contractor, DLP status, the legal basis (DLP clause and the HC order), and a request for action.
9. **Filing into existing channels**: BMC 1916 / MyBMC / Pothole QuickFix, email to the ward engineer, X/Twitter tagging. Each channel needs to be checked for what it technically allows.
10. **Public status tracker**: Reported → Verified → Filed → Acknowledged → Repaired → Re-verified, with a **24-hour SLA clock** against BMC's own promise.

**Tier 2: Accountability and intelligence**
11. **Contractor scorecard**: potholes per km under DLP, repair speed, repeat failures, past blacklisting and penalties (from news and official records).
12. **Officer and ward leaderboard**: SLA compliance.
13. **Repair verification**: later drives check whether the pothole is actually fixed, and whether it reopened within N days.
14. **Clustering and de-duplication**: many reports merge into one pothole with a confidence score.
15. **RTI auto-drafter**: when tender data is missing, drafts an RTI application under the RTI Act 2005 asking for the contract, the DLP clause and the measurement book.
16. **Accident and claim helper**: links a pothole to accident reports and pre-fills the claim under the Bombay HC compensation order. Needs legal review.

**Tier 3: Futuristic**
17. **Predictive failure model**: rainfall forecast + road age + material + past potholes predicts which stretches will fail *before* the monsoon.
18. **City "Road Health Twin"**: a live map of every road segment's condition, warranty and spend.
19. **Spend-vs-quality analytics**: ₹ spent per km against observed defects, to flag likely low-quality work for audit.
20. **Contract-document AI**: an LLM reads tender PDFs (Marathi + English) and extracts road names, chainages, ward, package, contractor, value, dates and DLP clause into structured data.
21. **Satellite and street imagery**: periodic change detection where licensing allows.
22. **Open data and API** for researchers and media; an open-source core for trust.

### D3. The tender crawler: "be the best at web crawling"

Sources, in priority order:
1. **BMC portal**: `portal.mcgm.gov.in/tenders`, ETH tender PDFs, work orders, standing-committee approvals.
2. **mahatenders.gov.in** (Maharashtra e-procurement, NIC GePNIC): tenders and **awards of contract (AoC)**.
3. **CPPP** (`eprocure.gov.in`): central tenders.
4. **MMRDA, MSRDC and PWD Maharashtra** portals.
5. **BMC standing-committee minutes and budget documents**: which contractor got which package.
6. **News archives**: blacklisting, penalties, scams. These are corroborating only, never the primary source.
7. **RTI replies** published by activists.

Pipeline: `crawl → store the raw PDF/HTML with a hash and timestamp (evidence chain) → OCR (Marathi + English) → LLM extraction → human spot-check → geocode road names/chainages to map segments → link to a contractor entity (de-duplicate name variants) → versioned database`.

**Rules the crawler will follow:** respect robots.txt and terms of use, rate-limit politely, never bypass logins or CAPTCHAs, and keep a source URL and archived copy for every fact. This is also what makes the data defensible if a contractor challenges it.

⚠️ **Verified constraint:** the government portals (portal.mcgm.gov.in, mahatenders.gov.in, eprocure.gov.in) are **not reachable from this cloud environment**; my connection tests returned no response. I can write the crawler here, but it has to be **tested on your machine or an Indian-region server**.

### D4. Proposed architecture

```
 Mobile app (React Native or Flutter)          Web dashboard (Next.js)
  ├─ on-device detector (TFLite/ONNX)            ├─ public map + tracker
  ├─ face/plate blur                             ├─ contractor/officer scorecards
  └─ offline queue ──────────┐                   └─ verifier / officer / contractor portals
                             ▼                              ▲
              API (Python FastAPI) ─── PostgreSQL + PostGIS (roads, tenders, potholes)
                │            │
                │            └─ object storage (blurred images, archived tender PDFs)
                ├─ Jurisdiction engine (OSM roads + agency/ward boundaries)
                ├─ Tender crawler + OCR + LLM extraction (scheduled workers)
                └─ Complaint generator (PDF) + filing connectors
```
🔶 These are well-established, mostly free and open tools. The final stack choice is yours; see the questions below.

---

## Part E: Risks, said plainly

| Risk | Severity | Mitigation |
|---|---|---|
| **Defamation**: naming a contractor or officer wrongly | 🔴 High | Name someone publicly **only** when the agency is verified, the tender is verified, DLP is active, and confidence is above a threshold or a human has verified it. Phrase it as "per tender X, this stretch is the responsibility of…". Give contractors a right of reply. **Get an Indian lawyer's review before launch.** |
| **Privacy (DPDP Act 2023)** | 🔴 High | Blur on the device, keep minimal personal data, have a consent flow and a retention policy. Needs a legal check. |
| **Wrong road-to-contract mapping** (tenders describe roads in text, not GPS) | 🟠 Med-High | Geocoding is the hardest technical problem. Show confidence, verify by hand for the first wards, and use RTI to get precise package maps. |
| **Crawling legality / portal blocking** | 🟠 Medium | Public data only, polite crawling, archive the evidence, no circumvention. |
| **Distracted driving** | 🟠 Medium | Hands-free only, start automatically when a drive starts; no screen interaction while moving. |
| **Gaming and abuse** (fake reports, rival contractors) | 🟡 Medium | Multi-report corroboration, device reputation, GPS and timestamp integrity checks. |
| **Being dismissed by BMC** | 🟡 Medium | File into BMC's own channels and treat them as partners rather than opponents. Gaurav Sen's experience shows officials show interest but rarely implement. |

---

## Part F: Questions I need answered before building

See the chat message; they are repeated here for the record:
1. **Repo:** keep this as a folder inside `Ackruti-` (current setup), or create a **separate GitHub repo** named `pothole-detection-system`?
2. **Scope of the first build:** (a) working MVP prototype (web app + detection + mock tender data), (b) start with the **tender crawler and contract database**, or (c) full detailed specification only?
3. **Platform:** Android-first mobile app, a mobile-friendly web app (PWA, fastest to ship), or both?
4. **Public naming:** should contractors and officers be named **publicly**, or only inside complaint files sent to the authorities, at least at launch?
5. **Budget and hosting:** will cloud AI APIs and servers be paid for, or should everything aim for free and open-source?
6. **Team:** are you building this alone, or with developers? What is your own technical comfort? This decides how much I explain each step.
7. **Transcript:** please paste the "100% transcript" you mentioned so I can verify mine.

---

## Sources
- [Free Press Journal: Gaurav Sen's AI tool](https://www.freepressjournal.in/tech/an-app-that-detects-potholes-bengaluru-engineer-gaurav-sen-builds-ai-tool-that-also-identifies-contractors-responsible-for-fixing-them)
- [Business Today, Aug 17 2026](https://www.businesstoday.in/technology/artificial-intelligence/story/bengaluru-engineer-builds-ai-system-that-detects-potholes-and-prepares-complaints-549579-2026-08-17)
- [ThePrint: "Pothole Reporter"](https://theprint.in/tech/pothole-reporter-how-bengaluru-techie-turned-yrs-of-frustration-over-broken-roads-into-ai-led-action/3025029/)
- [Analytics Insight](https://www.analyticsinsight.net/news/bengaluru-engineer-builds-ai-tool-to-spot-potholes-track-responsible-contractors)
- [BMC Pothole QuickFix (Google Play)](https://play.google.com/store/apps/details?id=com.bmc.potholequickfix&hl=en) · [MyBMC](https://play.google.com/store/apps/details?id=in.cdac.gov.mgov.mcgm&hl=en_IN)
- [BMC 24-hour pothole resolution directive](https://marksmendaily.com/politics/bmc-orders-24-hour-resolution-of-pothole-complaints/)
- [FPJ: Trenching Policy 2026, 10-year DLP](https://www.freepressjournal.in/mumbai/mumbai-bmc-proposes-new-trenching-policy-with-5x-penalty-for-illegal-digging-10-year-road-defect-liability)
- [FPJ: 3-year DLP on pothole repair tech](https://www.freepressjournal.in/mumbai/mumbai-finally-bmc-decides-rapid-hardening-core-reactive-asphalt-to-fill-potholes)
- [ThePrint: concrete roads project at 80%](https://theprint.in/india/governance/3-years-rs-17000-crore-later-mumbais-ambitious-concrete-roads-project-is-80-complete/2950617/)
- [LiveLaw: Bombay HC pothole compensation](https://www.livelaw.in/amp/high-court/bombay-high-court/bombay-maharashtra-roads-pothole-deaths-civic-body-contractor-compensation-306827) · [Moneylife](https://www.moneylife.in/article/rs6-lakh-compensation-for-pothole-deaths-bombay-hc-slams-civic-apathy-by-bmc-pwd-msrdc/78577.html)
- [LiveLaw: multiple agencies own Mumbai roads](https://livelaw.in/news-updates/bombay-high-court-bmc-potholes-bad-road-conditions-211159)
- [BMC tender PDF example](https://portal.mcgm.gov.in/irj/go/km/docs/documents/Tenders/ETH/ETH_8000089458_260525.pdf)

**What I could not verify:** I could not open the Business Today and FPJ articles directly (blocked from this environment), so their content comes from search-result extracts. I also could not confirm whether the 2026 trenching policy (10-year DLP) has been formally adopted, or the exact DLP in individual concretisation contracts.

---
*Made by SSK · 06 Oct 2026, 10:21 IST*
