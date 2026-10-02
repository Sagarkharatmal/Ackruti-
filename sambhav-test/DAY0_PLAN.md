# Sambhav: Day-0 master schedule (MSP) - DRAFT for approval

**Made by SSK · 02-Oct-2026 11:26 UTC · prepared by Claude acting as project manager**
Simulation date (Day 0): **Monday 07-Jan-2019**. Status: **DRAFT, NOT LOCKED.** Saved in the app (collection `t`, 13 documents) and checked by the app engine: 0 errors.

## Key planned dates
| Milestone | Planned date |
|---|---|
| Day 0, planning starts (IOD already received 26-Dec-2018) | 07-Jan-2019 |
| Commencement Certificate (CC) in hand, site starts | 05-Oct-2019 |
| Plinth complete | 04-Jun-2020 |
| Finishes start (2nd floor) | 28-Apr-2021 |
| Structure complete (LMR top slab) | 12-Jan-2022 |
| Services complete (incl. lift licence) | 21-Jun-2022 |
| OC clearances in hand | 26-Jul-2022 |
| **Final OC** | **01-Sep-2022** |

Critical path: approvals (CRZ is the longest) → CC → site set-up → piling → excavation → raft → plinth → plinth check and further CC → 15 parking slabs → club house slab → 19 residential slabs → terrace, OHT, LMR → lifts → lift licence → OC.

## Main assumptions (each one can be changed before locking)
1. Sunday weekly off. Holidays from the Maharashtra government list, checked by web search (Ganesh Chaturthi 2025 has a conflicting source and needs checking).
2. Monsoon stoppage 10-Jun to 30-Sep each year **only** for piling, excavation, works below the raft, terrace waterproofing, external painting and ground development. RCC slabs and indoor work continue (risk RK11).
3. Slab cycle: parking levels 12 working days (P1: 15), typical residential floors 10, refuge and duplex floors 12, club house (transfer) 25.
4. Approval durations are Claude's estimates. The liaison consultant must confirm them (risk RK01).
5. Further CC for full height is applied for when parking level 8 is cast, so it is in hand before the club house slab.
6. External works are planned to finish before the 2022 monsoon. The first draft let them run into the monsoon, which pushed the OC to 14-Dec-2022. Changing that logic saved 3.5 months.

## Budget
The package budgets are in the app (cost register). They are **not** in this public repo because they come from the client's confidential Excel files. The total matches the company's construction cost head. Three packages have no figure: mechanised parking, internal painting and club house fit-out.

## Full activity list (120 rows)
| ID | Activity | WD | Start | Finish | Predecessors | Monsoon |
|---|---|---|---|---|---|---|
| M000 | Project start - Day 0 (planning starts; IOD in hand 26-Dec-2018) | 0 | 07-Jan-2019 | 07-Jan-2019 | - | N |
| AP01 | Comply IOD conditions, compile and submit NOC applications | 20 | 07-Jan-2019 | 30-Jan-2019 | M000 FS | N |
| AP02 | Chief Fire Officer NOC (revised, high-rise) | 40 | 31-Jan-2019 | 18-Mar-2019 | AP01 FS | N |
| PR01 | Mechanised car parking: system selection, design inputs and order | 100 | 07-Jan-2019 | 06-May-2019 | M000 FS | N |
| AP03 | Parking NOC (mechanised parking layout) | 35 | 31-Jan-2019 | 12-Mar-2019 | AP01 FS; PR01 SS+20 | N |
| AP04 | Concessions (amended plans with premium) | 60 | 19-Mar-2019 | 29-May-2019 | AP02 FS; AP03 FS | N |
| AP05 | Solid Waste Management (C&D waste) NOC | 30 | 31-Jan-2019 | 06-Mar-2019 | AP01 FS | N |
| AP06 | MHADA (MBRRB) NOC for redevelopment | 90 | 07-Jan-2019 | 23-Apr-2019 | M000 FS | N |
| AP07 | CRZ clearance (MCZMA) | 120 | 31-Jan-2019 | 21-Jun-2019 | AP01 FS | N |
| AP08 | Tree Authority NOC | 45 | 31-Jan-2019 | 25-Mar-2019 | AP01 FS | N |
| AP09 | Intimation of Approval (IOA) / approval of amended plans | 30 | 22-Jun-2019 | 26-Jul-2019 | AP04 FS; AP05 FS; AP06 FS; AP07 FS | N |
| AP10 | MHADA NOC for CC | 30 | 27-Jul-2019 | 31-Aug-2019 | AP09 FS | N |
| AP11 | Commencement Certificate (CC) up to plinth, incl. premium payment | 25 | 03-Sep-2019 | 01-Oct-2019 | AP10 FS; AP08 FS | N |
| AP12 | MahaRERA registration | 20 | 03-Oct-2019 | 26-Oct-2019 | AP11 FS | N |
| AP13 | Work start notice to MCGM | 3 | 03-Oct-2019 | 05-Oct-2019 | AP11 FS | N |
| M010 | CC in hand - site work can start | 0 | 05-Oct-2019 | 05-Oct-2019 | AP13 FS | N |
| DS01 | Soil investigation (geotechnical report) | 25 | 07-Jan-2019 | 05-Feb-2019 | M000 FS | N |
| DS02 | Piling and foundation design, GFC drawings | 40 | 06-Feb-2019 | 25-Mar-2019 | DS01 FS | N |
| DS03 | GFC drawings: basement, plinth and parking podium | 60 | 01-Mar-2019 | 11-May-2019 | DS02 SS+20 | N |
| DS04 | GFC drawings: club house, residential floors, terrace, LMR | 90 | 13-May-2019 | 26-Aug-2019 | DS03 FS | N |
| DS05 | MEP, fire and facade design freeze | 90 | 06-Apr-2019 | 20-Jul-2019 | DS03 SS+30 | N |
| PR02 | Piling tender and award | 30 | 26-Mar-2019 | 29-Apr-2019 | DS02 FS | N |
| PR03 | Civil core and shell tender and award | 45 | 06-Apr-2019 | 29-May-2019 | DS03 SS+30 | N |
| PR04 | Lifts tender and order (passenger, fire, service) | 40 | 17-Jun-2019 | 01-Aug-2019 | DS04 SS+30 | N |
| PR05 | Windows and facade tender and order | 45 | 22-Jul-2019 | 13-Sep-2019 | DS05 FS | N |
| PR06 | MEP packages tender and award (fire fighting, FA/PA, PHE, electrical) | 45 | 22-Jul-2019 | 13-Sep-2019 | DS05 FS | N |
| SS01 | Site set-up: hoarding, site office, temporary power and water | 20 | 07-Oct-2019 | 31-Oct-2019 | M010 FS | N |
| PL01 | Piling rig mobilisation | 10 | 01-Nov-2019 | 12-Nov-2019 | SS01 FS; PR02 FS | N |
| PL02 | Piling (approx. 40 piles) | 45 | 13-Nov-2019 | 04-Jan-2020 | PL01 FS | Y |
| CV01 | Civil contractor mobilisation | 20 | 06-Dec-2019 | 30-Dec-2019 | PR03 FS; PL02 SS+20 | N |
| EX01 | Excavation and shoring for 1 basement | 30 | 06-Jan-2020 | 08-Feb-2020 | PL02 FS; CV01 FS | Y |
| EX02 | Retaining wall | 25 | 17-Jan-2020 | 14-Feb-2020 | EX01 SS+10 | Y |
| EX03 | Pile breaking and pile treatment | 15 | 10-Feb-2020 | 26-Feb-2020 | EX01 FS | N |
| EX04 | Soling, PCC and box waterproofing below raft | 20 | 27-Feb-2020 | 21-Mar-2020 | EX03 FS; EX02 FS | Y |
| RF01 | Raft reinforcement and shuttering | 25 | 23-Mar-2020 | 20-Apr-2020 | EX04 FS | N |
| RF02 | Raft casting (approx. 1,080 cum) | 3 | 21-Apr-2020 | 23-Apr-2020 | RF01 FS | N |
| TC01 | Tower crane and safety screen installation | 20 | 24-Apr-2020 | 18-May-2020 | RF02 FS | N |
| RC00 | Basement walls, ground, intermediate and plinth slabs | 35 | 24-Apr-2020 | 04-Jun-2020 | RF02 FS | N |
| M020 | Plinth complete | 0 | 04-Jun-2020 | 04-Jun-2020 | RC00 FS | N |
| AP14 | Plinth check and further CC above plinth | 25 | 05-Jun-2020 | 03-Jul-2020 | M020 FS | N |
| RCP01 | Parking level 1 slab (columns, walls, slab) | 15 | 04-Jul-2020 | 21-Jul-2020 | AP14 FS; TC01 FS | N |
| RCP02 | Parking level 2 slab (columns, walls, slab) | 12 | 22-Jul-2020 | 04-Aug-2020 | RCP01 FS | N |
| RCP03 | Parking level 3 slab (columns, walls, slab) | 12 | 05-Aug-2020 | 19-Aug-2020 | RCP02 FS | N |
| RCP04 | Parking level 4 slab (columns, walls, slab) | 12 | 20-Aug-2020 | 03-Sep-2020 | RCP03 FS | N |
| RCP05 | Parking level 5 slab (columns, walls, slab) | 12 | 04-Sep-2020 | 17-Sep-2020 | RCP04 FS | N |
| RCP06 | Parking level 6 slab (columns, walls, slab) | 12 | 18-Sep-2020 | 01-Oct-2020 | RCP05 FS | N |
| RCP07 | Parking level 7 slab (columns, walls, slab) | 12 | 03-Oct-2020 | 16-Oct-2020 | RCP06 FS | N |
| RCP08 | Parking level 8 slab (columns, walls, slab) | 12 | 17-Oct-2020 | 30-Oct-2020 | RCP07 FS | N |
| RCP09 | Parking level 9 slab (columns, walls, slab) | 12 | 31-Oct-2020 | 13-Nov-2020 | RCP08 FS | N |
| RCP10 | Parking level 10 slab (columns, walls, slab) | 12 | 17-Nov-2020 | 30-Nov-2020 | RCP09 FS | N |
| RCP11 | Parking level 11 slab (columns, walls, slab) | 12 | 01-Dec-2020 | 14-Dec-2020 | RCP10 FS | N |
| RCP12 | Parking level 12 slab (columns, walls, slab) | 12 | 15-Dec-2020 | 29-Dec-2020 | RCP11 FS | N |
| RCP13 | Parking level 13 slab (columns, walls, slab) | 12 | 30-Dec-2020 | 12-Jan-2021 | RCP12 FS | N |
| RCP14 | Parking level 14 slab (columns, walls, slab) | 12 | 13-Jan-2021 | 27-Jan-2021 | RCP13 FS | N |
| RCP15 | Parking level 15 slab (columns, walls, slab) | 12 | 28-Jan-2021 | 10-Feb-2021 | RCP14 FS | N |
| AP15 | Further CC for full height (amended plans incl. residential floors) | 60 | 31-Oct-2020 | 12-Jan-2021 | RCP08 FS; DS04 FS | N |
| RC16 | Club house level (16th) slab incl. transfer | 25 | 11-Feb-2021 | 11-Mar-2021 | RCP15 FS; AP15 FS | N |
| RR02 | 2nd residential floor slab - slab 17 | 10 | 12-Mar-2021 | 23-Mar-2021 | RC16 FS | N |
| RR03 | 3rd residential floor slab - slab 18 | 10 | 24-Mar-2021 | 05-Apr-2021 | RR02 FS | N |
| RR04 | 4th residential floor slab - slab 19 | 10 | 06-Apr-2021 | 16-Apr-2021 | RR03 FS | N |
| RR05 | 5th residential floor slab - slab 20 | 10 | 17-Apr-2021 | 28-Apr-2021 | RR04 FS | N |
| RR06 | 6th residential floor slab - slab 21 | 10 | 29-Apr-2021 | 11-May-2021 | RR05 FS | N |
| RR07 | 7th residential floor slab - slab 22 | 10 | 12-May-2021 | 22-May-2021 | RR06 FS | N |
| RR08 | 8th residential floor slab (refuge) - slab 23 | 12 | 24-May-2021 | 05-Jun-2021 | RR07 FS | N |
| RR09 | 9th residential floor slab - slab 24 | 10 | 07-Jun-2021 | 17-Jun-2021 | RR08 FS | N |
| RR10 | 10th residential floor slab - slab 25 | 10 | 18-Jun-2021 | 29-Jun-2021 | RR09 FS | N |
| RR11 | 11th residential floor slab - slab 26 | 10 | 30-Jun-2021 | 10-Jul-2021 | RR10 FS | N |
| RR12 | 12th residential floor slab - slab 27 | 10 | 12-Jul-2021 | 22-Jul-2021 | RR11 FS | N |
| RR13 | 13th residential floor slab - slab 28 | 10 | 23-Jul-2021 | 03-Aug-2021 | RR12 FS | N |
| RR14 | 14th residential floor slab - slab 29 | 10 | 04-Aug-2021 | 14-Aug-2021 | RR13 FS | N |
| RR15 | 15th residential floor slab (refuge) - slab 30 | 12 | 16-Aug-2021 | 28-Aug-2021 | RR14 FS | N |
| RR16 | 16th residential floor slab - slab 31 | 10 | 30-Aug-2021 | 09-Sep-2021 | RR15 FS | N |
| RR17 | 17th residential floor slab - slab 32 | 10 | 11-Sep-2021 | 22-Sep-2021 | RR16 FS | N |
| RR18 | 18th residential floor slab - slab 33 | 10 | 23-Sep-2021 | 05-Oct-2021 | RR17 FS | N |
| RR19 | 19th residential floor slab (duplex) - slab 34 | 12 | 06-Oct-2021 | 20-Oct-2021 | RR18 FS | N |
| RR20 | 20th residential floor slab (duplex) - slab 35 | 12 | 21-Oct-2021 | 03-Nov-2021 | RR19 FS | N |
| RT01 | Terrace slab | 15 | 06-Nov-2021 | 23-Nov-2021 | RR20 FS | N |
| RT02 | OHT and upper terrace slab | 15 | 24-Nov-2021 | 10-Dec-2021 | RT01 FS | N |
| RT03 | LMR bottom slab | 12 | 11-Dec-2021 | 24-Dec-2021 | RT02 FS | N |
| RT04 | LMR top slab and canopy | 15 | 27-Dec-2021 | 12-Jan-2022 | RT03 FS | N |
| M030 | Structure complete | 0 | 12-Jan-2022 | 12-Jan-2022 | RT04 FS | N |
| FP01 | Parking levels: fire, electrical and drainage services | 90 | 11-Feb-2021 | 28-May-2021 | RCP15 FS | N |
| FP02 | Parking levels: anti-carbonation paint and finishing | 60 | 18-Mar-2021 | 28-May-2021 | FP01 SS+30 | N |
| MP01 | Mechanised car parking installation P1-P15 | 156 | 12-Mar-2021 | 13-Sep-2021 | RCP15 FS; PR01 FS; RC16 FS | N |
| MP02 | Mechanised car parking testing and commissioning | 20 | 14-Sep-2021 | 07-Oct-2021 | MP01 FS | N |
| M040 | Finishes start (2nd floor) | 0 | 28-Apr-2021 | 28-Apr-2021 | RR05 FS | N |
| BA1 | Block work (AAC) 2nd to 10th | 60 | 26-May-2021 | 03-Aug-2021 | M040 FS; RR13 FF | N |
| BA2 | Concealed electrical and plumbing works 2nd to 10th | 60 | 07-Jun-2021 | 14-Aug-2021 | BA1 SS+10; BA1 FF+10 | N |
| BA3 | Gypsum and internal plaster 2nd to 10th | 70 | 24-Jun-2021 | 14-Sep-2021 | BA2 SS+15; BA1 FF+15 | N |
| BA4 | Toilet waterproofing, tiling and flooring 2nd to 10th | 80 | 17-Jul-2021 | 21-Oct-2021 | BA3 SS+20 | N |
| BA5 | Door frames and wooden fire-rated doors 2nd to 10th | 40 | 21-Aug-2021 | 08-Oct-2021 | BA4 SS+30 | N |
| BA6 | Putty and internal painting 2nd to 10th | 70 | 21-Aug-2021 | 16-Nov-2021 | BA4 SS+30; BA4 FF+15 | N |
| BB1 | Block work (AAC) 11th to 20th (incl. duplex) | 60 | 22-Sep-2021 | 04-Dec-2021 | BA1 FS; RT01 FF+10 | N |
| BB2 | Concealed electrical and plumbing works 11th to 20th (incl. duplex) | 60 | 05-Oct-2021 | 16-Dec-2021 | BB1 SS+10; BB1 FF+10 | N |
| BB3 | Gypsum and internal plaster 11th to 20th (incl. duplex) | 70 | 23-Oct-2021 | 15-Jan-2022 | BB2 SS+15; BB1 FF+15 | N |
| BB4 | Toilet waterproofing, tiling and flooring 11th to 20th (incl. duplex) | 80 | 18-Nov-2021 | 21-Feb-2022 | BB3 SS+20 | N |
| BB5 | Door frames and wooden fire-rated doors 11th to 20th (incl. duplex) | 40 | 23-Dec-2021 | 09-Feb-2022 | BB4 SS+30 | N |
| BB6 | Putty and internal painting 11th to 20th (incl. duplex) | 70 | 23-Dec-2021 | 16-Mar-2022 | BB4 SS+30; BB4 FF+15 | N |
| DR01 | MS fire-rated doors (staircases, lift lobbies, shafts) | 60 | 18-Nov-2021 | 28-Jan-2022 | BB3 SS+20 | N |
| CH01 | Club house and amenity floor fit-out | 60 | 15-Sep-2021 | 27-Nov-2021 | BA3 FS; RC16 FS | N |
| FC01 | Windows, facade and Reckli panels | 200 | 30-Jun-2021 | 25-Feb-2022 | RR10 FS; PR05 FS | N |
| FC02 | External painting | 40 | 25-Feb-2022 | 13-Apr-2022 | FC01 SS+120; RT04 FS | Y |
| SV01 | Fire fighting system (risers, sprinklers, pumps, tanks) | 220 | 03-Jul-2021 | 25-Mar-2022 | RR02 FS; PR06 FS; RT04 FF+60 | N |
| SV02 | Fire alarm and public address | 120 | 20-Oct-2021 | 12-Mar-2022 | SV01 SS+90 | N |
| SV03 | PHE: drainage, water supply, pumps | 200 | 27-Jul-2021 | 25-Mar-2022 | RR02 FS; PR06 FS; RT04 FF+60 | N |
| SV04 | Electrical: risers, LT panels, substation, DG | 200 | 19-Aug-2021 | 18-Apr-2022 | RR05 FS; PR06 FS; RT04 FF+80 | N |
| SV05 | Passenger and fire lifts installation | 120 | 13-Jan-2022 | 03-Jun-2022 | RT04 FS; PR04 FS | N |
| SV06 | Service / other lift installation | 100 | 13-Jan-2022 | 11-May-2022 | RT04 FS; PR04 FS | N |
| SV07 | Lift inspection and licence | 15 | 04-Jun-2022 | 21-Jun-2022 | SV05 FS; SV06 FS | N |
| SV08 | Testing and commissioning of all services | 30 | 19-Apr-2022 | 23-May-2022 | SV01 FS; SV02 FS; SV03 FS; SV04 FS; MP02 FS; FP01 FS | N |
| M050 | Services complete | 0 | 21-Jun-2022 | 21-Jun-2022 | SV08 FS; SV07 FS | N |
| EW01 | Terrace waterproofing | 30 | 13-Jan-2022 | 17-Feb-2022 | RT04 FS | Y |
| EW02 | Plinth protection and ground development (paving, landscape, gates) | 80 | 25-Feb-2022 | 30-May-2022 | FC01 SS+120 | Y |
| EW03 | Entrance lobby, railings (SS and MS) | 60 | 08-Sep-2021 | 22-Nov-2021 | FC01 SS+60 | N |
| OC01 | Final fire NOC (CFO inspection) | 30 | 24-May-2022 | 27-Jun-2022 | SV08 FS | N |
| OC02 | Completion documents, as-built drawings and OC application | 15 | 22-Jun-2022 | 08-Jul-2022 | M050 FS; M030 FS; BA5 FS; BA6 FS; BB5 FS; BB6 FS; DR01 FS; CH01 FS; FP02 FS; FC02 FS; EW01 FS; EW02 FS; EW03 FS; AP12 FS | N |
| OC03 | Other OC clearances (SWM, drainage, water connection, tree plantation) | 30 | 22-Jun-2022 | 26-Jul-2022 | OC02 SS | N |
| M060 | OC clearances in hand | 0 | 26-Jul-2022 | 26-Jul-2022 | OC01 FS; OC03 FS; SV07 FS | N |
| OC04 | MCGM OC scrutiny and site inspection | 30 | 27-Jul-2022 | 01-Sep-2022 | M060 FS | N |
| M070 | Final Occupancy Certificate (OC) | 0 | 01-Sep-2022 | 01-Sep-2022 | OC04 FS | N |

_Made by SSK · 02-Oct-2026 11:26 UTC_
