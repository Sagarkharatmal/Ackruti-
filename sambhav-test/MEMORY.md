# SAMBHAV simulation: project memory (read this first in every new session)

Made by SSK · last updated 02-Oct-2026

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

## Status
- Day 0 date: **NOT YET GIVEN** (user will give it).
- App contents: empty (cleared). Admin state: phase 3.
- Next: user to give day 0 and the answers listed in NEXT_STEPS.md; then Claude builds calendar, project details, packages, master schedule (draft for approval), budget (pending approval), rates (pending approval).
