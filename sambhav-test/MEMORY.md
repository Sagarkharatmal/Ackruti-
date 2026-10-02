# SAMBHAV simulation: project memory (read this first in every new session)

Made by SSK · last updated 02-Oct-2026 11:30 UTC

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

## Inputs used for the Day-0 plan (from Sambhav_Data_Just_for_information.zip)
- MSP answer key: summary row only (planned start 07-Jan-2019).
- R1 Approval Detail.xlsx: only approvals dated BEFORE Day 0 (demarcation 2014 and 2017, CFO NOC 2014, IOD 26-Dec-2018, layout amalgamation 26-Dec-2018) and the names of approvals needed.
- Sambhav_COD Analysis (3).xlsx: Planned Budget Quarterly sheet (package budgets) and the planned budget total.
- RA Bill file: tender quantities and rates of the civil BOQ only (no executed quantities).
- **Disclosure (honest record):** to find the budget figures and the approval list, Claude's screen also showed (a) the actual approval dates and actual slab dates in R1, (b) the actual delay reasons and the actual completion date in the COD summary, and (c) the company's original planned completion, 30-Jun-2023. These were NOT used to set durations or logic. The risk register uses only location- and type-based foresight. RK09 (Banganga heritage area) overlaps with a real delay reason, so it is marked here as possibly influenced.

## Files kept outside the repo (confidential, in the session scratchpad only)
- The Day-0 builder script with budget figures, the full app backup v2 and the engine summary. Ask the user before pushing them, or make the repo private.

