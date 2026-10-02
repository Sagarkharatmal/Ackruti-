# SAMBHAV simulation: project memory (read this first in every new session)

Made by SSK · last updated 02-Oct-2026 11:37 UTC

## What this is
A test of Claude acting as the project manager of **Sambhav** (Walkeshwar, Mumbai) using the AiAi PM Master App.
The building is nearly complete in real life. We **pretend it has not started**: Claude plans it from day 0, then daily
progress, delays and decisions are recorded as a simulation, to see how Claude plans, executes and asks the human to coordinate.

## The app
- Published page (private to the owner): https://claude.ai/artifact/TgeU7opaMQ5MtDox6QX6yF
- Data is in the page's store (collections `t`, `admin`, `log`, `rel`). Package: AiAi_PM_Master_App_v8 (app 1.4.1, engine 1.2.0).
- Claude reads and writes the store with the ArtifactData tool. Every write is also written to the app's change log as "Claude".

## Rules set by the main user (02-Oct-2026)
1. **Clean restart.** The app was cleared. The first fill (people, drawing register, approvals, risks, rate rows) is kept only as a backup in `app-backups/v1-2026-10-02/`.
2. **Day 0 = the planned start date of the project.** The project is being PLANNED, not started. Claude fills the planned side of the master schedule (MSP) and plans the budget and everything a project manager does before start.
3. "Master plan" = the master schedule (MSP) in the app.
4. **Do NOT load actual progress or real history.** Use only the information needed to start. The goal is to test Claude's plan, not to copy the real one.
5. Claude shows major steps before locking, saves everything immediately to the app, and records every step in this folder.
6. Use the DWG drawing set (Sambhav only) wherever necessary.

## The two zips received
- `sambhavlatestgfcsetfromsbparchitects.zip`: 6 AutoCAD DWG architectural drawings. Used. Facts in FACTS_FROM_DRAWINGS.md.
- `Sambhav_Data_Just_for_information.zip`: 6 Excel files. Classified (structure and headers only, contents NOT read):
  | File | What it is | Use in the test |
  |---|---|---|
  | DLR - Daily Labor Reports.xlsx | one sheet per day from 01-Jan-2025 to 16-Aug-2025 with some days missing, by Mehta Jaising Builders LLP | ACTUAL history. Not used. May be used later ONLY as a sample of the daily report format, if the user allows |
  | Delay Register.xlsx, Sambhav_COD Analysis (3).xlsx | real delays and cost-of-delay analysis | ACTUAL history. Not used |
  | Sambhav RA Bill and Summary.xlsx | contractor running bills RA-6 to RA-19 | ACTUAL cost history. Not used |
  | R1 Approval Detail.xlsx | approval-stage activities, planned vs actual | mixed; not used until the user says |
  | Sambhav.OCT.2025 Baseline (...).xlsx | the real company schedule (Oct 2025 baseline) | The answer key. NOT read, so that Claude's plan is independent |
- Contractor named in the data: Mehta Jaising Builders LLP (RCC/civil, "Civil core and shell work (1B+G...)"). Confirm with the user before using.

## Status (02-Oct-2026 11:30 UTC)
- **Day 0 = Monday 07-Jan-2019**, the planned start in the summary row of the company MSP. That row is the only part of the answer-key MSP that was read.
- **The Day-0 plan is built and saved in the app** (13 documents in `t`, 305 change-log entries as Claude in `log/2026-10-02`, version 6). Read back: identical. Engine: 0 errors; 5 warnings (budgets missing on MP01, MP02, BA6, BB6, CH01).
  - Master schedule: 120 activities. Final OC planned **01-Sep-2022**. Structure complete 12-Jan-2022. CC 05-Oct-2019. See DAY0_PLAN.md.
  - Calendar: Sunday off; 2019-2026 holidays (web-checked); monsoon stoppage 10-Jun to 30-Sep for works marked Y.
  - Budget: 19 packages proposed (NOT approved); the total equals the company construction cost head. Figures are in the app only, because this repo is PUBLIC.
  - Registers: project details, people (consultants only, since no contractor is appointed on Day 0), compliance (5 approvals in hand before Day 0 + 17 planned), 6 drawing packages awaited, 11 risks, 7 decisions (D01-D07), 11 cost-of-delay rate heads with no figures (PENDING).
- **Baseline NOT locked.** Waiting for the user to approve D01 (schedule), D02 (budget), D03 (RERA and possession dates), D05 (calendar) and D06 (rates).
- The simulation clock: the app runs on the real date. To view as on a simulated day, use Guide and settings > "Look at an earlier date". Daily reports are dated on the simulated day.


## Update 02-Oct-2026 11:37 UTC: plan completed from ALL the data (user: "figure out everything from the data that I have")
Rule applied to every file: a fact that was knowable on Day 0 (a company plan, a budget, a tender quantity, a wage rate, a payment plan, approvals already received) is USED. Actual history after Day 0 is NOT used. It is sealed as an answer key (scratchpad `answer_key/`, not in this public repo) for scoring the simulation later.
What was added to the app (18 documents; change log version 7, 176 field-level entries as Claude):
- **The budget figures are reconciled.** Whole project = construction + land + approvals and consultants + admin + finance + marketing + DM fee. The quarterly-plan total = the whole project minus the DM fee (a small unexplained difference). Spend before Day 0 was mostly land and approvals. Construction package total = the company construction head (exact match).
- **Planned cash flow:** 45 months, Jan-2019 to Sep-2022, plus 29 collections linked to construction stages. It comes from the company sales plan (38,813 sq ft of sale area; payment plan: booking at piling 10%, plinth 10%, parking levels 2/9/15, then 2.5% per habitable slab, terrace, plaster, plumbing/electrical, possession). The funding need is in decision D08. Opening funds are NOT in the data.
- **Proposed rates (PENDING):** R01 idle labour (wage rates from the Labour Coefficient sheet, gang size estimated by Claude); R05 site overheads and R06 finance cost per day (budget heads / planned days). The company COD uses 18% per year for delayed collections; this is put to the user in D06 and NOT entered.
- **RERA and possession date PROPOSED as 30-Jun-2023** (the planned completion in the company COD summary); D03 asks the user to confirm.
- **Planned concrete quantity** for each slab activity, from the earliest civil tender (RA-6), so that progress can be measured.
- **Calendar:** the 2025 daily labour reports show the site worked about 78% of Sundays. The plan keeps Sunday off and treats Sundays as a recovery reserve.
- New risk RK12: sales slower than the company plan.
- Gaps still open, and legitimately so on Day 0: approval of the budget, baseline and rates (human); budget figures for mechanised parking, internal painting and club house fit-out (none in the data); funding plan (D08); contracts, quality and safety (nothing exists before work starts).
- The daily labour report format (staff, trades, subcontractor gangs) will be used as the template when daily reports are taken in.

## Inputs used for the Day-0 plan (from Sambhav_Data_Just_for_information.zip)
- MSP answer key: summary row only (planned start 07-Jan-2019).
- R1 Approval Detail.xlsx: only approvals dated BEFORE Day 0 (demarcation 2014 and 2017, CFO NOC 2014, IOD 26-Dec-2018, layout amalgamation 26-Dec-2018) and the names of approvals needed.
- Sambhav_COD Analysis (3).xlsx: Planned Budget Quarterly sheet (package budgets) and the planned budget total.
- RA Bill file: tender quantities and rates of the civil BOQ only (no executed quantities).
- **Disclosure (honest record):** to find the budget figures and the approval list, Claude's screen also showed (a) the actual approval dates and actual slab dates in R1, (b) the actual delay reasons and the actual completion date in the COD summary, and (c) the company's original planned completion, 30-Jun-2023. These were NOT used to set durations or logic. The risk register uses only location- and type-based foresight. RK09 (Banganga heritage area) overlaps with a real delay reason, so it is marked here as possibly influenced.

## Files kept outside the repo (confidential, in the session scratchpad only)
- The Day-0 builder script with budget figures, the full app backup v2 and the engine summary. Ask the user before pushing them, or make the repo private.

