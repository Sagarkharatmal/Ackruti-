# Video Transcript: "A Bengaluru engineer was tired of potholes, so he built an AI that names the contractor responsible"

> **Made by SSK** · Prepared 06 October 2026, 10:21 IST
> Project: Pothole Detection System (Mumbai)

## Source

| Field | Value |
|---|---|
| File | `vidssave.com_A_Bengaluru_engineer_was_tired_of_potholes_so_he_built_an_AI_that_names_the_contractor_responsible._720P.mp4` |
| Duration | 45.07 seconds |
| Format | 720×1280 vertical (9:16), 30 fps, H.264, HE-AAC stereo audio |
| Presenter / channel | `@realkushdesai`, shown as a watermark on every frame. The presenter is Kush Desai, not the engineer. |
| Subject of the video | Gaurav Sen (YouTube `@gkcs`, founder of InterviewReady) |
| Sources cited on screen | X post by @VaibhavSisinty (Aug 15, 2026); CNBC‑TV18; Business Today (Aug 17); OneIndia (Oct 2); "Gaurav Sen, YouTube @gkcs"; "Building Bharat" |

## How this transcript was made, and how far to trust it

1. I extracted the audio track and ran it through **OpenAI Whisper "small" (multilingual, int8)** offline, using `sherpa-onnx`. The usual model hosts (HuggingFace, OpenAI CDN) are blocked from this environment, so I downloaded the model from GitHub.
2. I ran the speech-to-text three times, cutting the audio into 5 s, 9 s and 15 s chunks, and compared the three outputs.
3. I checked each line against the **burned-in on-screen captions**, which I read frame by frame. Where speech-to-text and captions disagreed, I used the captions.

**Machine errors I corrected:** "codecs" → **Codex** (the caption shows *Codex* in italics, with the Codex logo). "Adashkin" → **A dashcam**. "POTROL" → **pothole**. "Officer Ford" / "officer photo" → **officer, photo**. "doesn't fill a single portal" → **pothole**.

**Confidence:** high for all lines. The only point I could not settle is whether the speaker says "officer, photo, location" or "officer, location". The audio is fast there; both readings are marked below.

👉 **You mentioned you could give me the 100% transcript, but nothing came through after "Transcript of video".** Please paste it and I'll diff it against this one.

## Transcript (verbatim, corrected as described above)

| Time | Spoken words | On-screen visual |
|---|---|---|
| 0:00–0:04 | "The pothole you hit on the way to work has someone's **name** on it." | Presenter; photos of water-filled potholes. Label: "Building Bharat" |
| 0:04–0:07 | "And one engineer in Bengaluru built an AI that finds it." | Screenshot of an X post by Vaibhav Sisinty, Aug 15, 2026 (18,708 likes, 550 replies): *"An Indian engineer in Bengaluru got tired of potholes destroying his car. So he built an app with Codex that detects them, finds who is responsible, and files a complaint. Here is how it works. He installed a dashcam with GPS and an accelerometer. The app records everything"* |
| 0:07–0:10 | "Gaurav Sen used **Codex** to turn his car into a pothole detector." | Illustration: road with a "REC" indicator, the Codex logo, a pothole appearing |
| 0:10–0:13 | "A **dashcam** records the road with GPS and a motion sensor." | CNBC‑TV18 footage: a phone in a windshield mount running the **"Pothole Reporter"** app |
| 0:13–0:17 | "AI vision sorts every pothole into small, medium or large." | Illustration labelled "AI vision": three potholes of different sizes |
| 0:17–0:21 | "Then comes the part contractors won't like. A lot of these roads are still under **warranty**," | Business Today, Aug 17 quote: *"This matters because road contracts can include maintenance and defect-liability provisions. In such cases, contractors can be responsible for…"* |
| 0:21–0:24 | "so the contractor who built it should fix it for free." | CNBC‑TV18 footage of a broken road surface |
| 0:24–0:27 | "His app reads **2,900** government contracts" | Illustration of Gaurav Sen's demo: "2,900 government contracts", a list of rows tagged "Tender" |
| 0:27–0:29 | "and finds that one contractor," | One "Tender" row highlighted |
| 0:29–0:32 | "tender number, officer, [photo,] location —" *(low confidence on "photo")* | "Building Bharat" footage: phone showing the app's complaint screen |
| 0:32–0:34 | "in **four** seconds." | Same footage |
| 0:34–0:37 | "One drive to work: **12 potholes, 12 complaint-ready files.**" | Illustration: route line with 4 red pins → "12 complaint-ready files". Business Today quote: *"The demonstration showed how quickly the process could work in practice. During a single drive to work, the system detected 12 potholes and generated 12 complaints ready to be filed."* |
| 0:37–0:40 | "To be fair, it's still in **beta** and it doesn't fill a single pothole." | "Gaurav Sen, YouTube @gkcs": grainy pothole clip |
| 0:40–0:42 | "But officials in Bengaluru and Chennai have already reached out to him." | OneIndia, Oct 2: *"Sen said some civic officials, including commissioners from Bengaluru and Chennai have contacted him. Politicians from Karnataka and other states have also shown interest. However, he said he has yet to see the kind of ground-level implementation he would like."* |
| 0:42–0:45 | "The potholes were never the problem. **Nobody being named was.**" | Presenter, closing line |

## Text read from the app's own screens (CNBC‑TV18 clip, 0:10–0:13)

The phone is small and blurry in the frame, so these readings are **low confidence**. The originals are in `video-evidence/app-screen-dashcam-mode.jpg`.

- App title: **"Pothole Reporter"** *(confident)*
- Subtitle under the title: [unclear]
- A red **"● Stop"** button at the top right of the camera view *(confident)*: the app is recording in a live session
- Bottom overlay: **"1 pothole"** *(confident)*
- Below that: "Scanning (stills) · 78 checked, 4 analyzing" *(medium confidence)*
- Help text: "Mount the phone facing the road. Frames are checked continuously while you move (about every 10 meters). The…" *(low confidence; the rest is [unclear])*

What this suggests, which is my inference and not confirmed: the app **samples still frames about every 10 m of travel** and sends them for AI analysis. It does **not** process every video frame. That fits the news reports that it "forwards data to OpenAI models for analysis".

Complaint screen (0:29–0:34): a dark-mode card with a road photo, an orange/red highlighted heading, and several lines of text. **All of the text is illegible**, so I have not transcribed it.

---
*Made by SSK · 06 Oct 2026, 10:21 IST*
