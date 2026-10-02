const SRC='/tmp/claude-0/-home-user-Ackruti-/d1ceb428-4747-5f0b-b9e2-a218c556721a/scratchpad/pkg/AiAi_PM_Master_App_v8/source/';
const fs=require('fs');
const D=require(SRC+'09_Dashboards/Master_App/src/app_data.js');
const E=require(SRC+'10_Engine/engine_core.js');
const S=JSON.parse(fs.readFileSync(SRC+'09_Dashboards/Master_App/src/schemas.json','utf8'));
D.init(S);
const m=D.emptyModel(); const log=[]; let seq=0;
function rid(){ return 'c'+(++seq).toString(36).padStart(3,'0'); }
function chk(key,data){ const sp=D.sheet(key), cols={}; (sp.cols||sp.fields).forEach(c=>cols[c.key]=c);
  Object.keys(data).forEach(k=>{ if(k[0]==='_')return; if(!cols[k]) throw new Error(key+': no column '+k); const p=D.parseCell(cols[k],data[k]); if(p.error) throw new Error(key+'.'+k+': '+p.error); data[k]=p.value; }); return data; }
function kv(key,data,why){ chk(key,data); D.writeKv(m,key,data); Object.keys(data).forEach(f=>log.push({k:'claude',s:key,r:'',f,o:null,n:data[f],w:why})); }
let ord={};
function row(key,data,why,label){ chk(key,data); ord[key]=(ord[key]||0)+1; data._o=ord[key]; data._del=false; D.writeRow(m,key,rid(),data);
  log.push({k:'claude',s:key,r:label||data.id||data.number||data.role||'',f:'(new row)',o:null,n:Object.keys(data).filter(k=>k[0]!=='_'&&data[k]!==null).map(k=>k+': '+data[k]).join('; ').slice(0,400),w:why}); }
const DRG='GFC set from SBP Architects (6 DWG files), read by Claude on 02-Oct-2026';

// ---------------- Project details (facts from the drawings; RERA from public listings, unverified)
kv('project_details.Details',{
  name:'Sambhav', projectCode:'SAMBHAV',
  location:'Dongarsi Road, Walkeshwar (Malabar Hill), Mumbai, Maharashtra',
  authority:'Brihanmumbai Municipal Corporation (BMC / MCGM), D Ward - to confirm',
  buildingType:'Residential high-rise tower with mechanised parking podium',
  floors:'Basement/car pit + lower ground + ground + intermediate + 15 mechanised parking levels + 1st (amenity & refuge) + 2nd-18th residential (8th & 15th refuge) + 19th-20th duplex + terrace + OHT/LMR',
  builtUpArea:'12,008.75 sq m construction BUA (built-up area summary on drawings)',
  currentStage:'NOT CONFIRMED BY SITE. Drawing note on WL/FP/116 (26-Jun-2026): Apply for OC 23/06/2026',
  reraNumber:'P51900025024 (from public listings - verify on MahaRERA)',
  reraDate:'2028-12-30',
  architect:'Suresh Babu & Partners (SBP), 11 S.A. Brelvi Road, Fort, Mumbai 400001. Tel 022-22666010',
  structuralConsultant:'Global Engineering Services (GES), 607 Mayuresh Cosmos, Sector 11, CBD Belapur, Navi Mumbai 400614. Tel 022-67940050, designs@gescpl.com'
}, 'From the GFC drawings (title block, built-up area summary, notes). RERA number and RERA completion date 30-Dec-2028 are from public property listings and are NOT verified on MahaRERA.');

// ---------------- People and authority
const P='Title block of the GFC drawings';
[['Client / promoter','','M/s Helictite Residency Pvt. Ltd.','','','',''],
 ['Architect','Team on title block: S. Babu, Vikrant, Abhijit, Sanika (roles not stated)','Suresh Babu & Partners (SBP)','Architectural drawings, revisions','','022-22666010',''],
 ['Structural consultant','','Global Engineering Services (GES)','Structural drawings and comments','','022-67940050','designs@gescpl.com'],
 ['MEP consultant','','Pankaj Dharkar & Associates','MEP drawings, cut-outs','','022-26837283','mumbaipda@gmail.com'],
 ['Landscape consultant','','Aakar Architect','Landscape design','','022-26129933',''],
 ['Liaison consultant','','Name not shown on the drawings','Approvals with BMC, CFO and other authorities','','','']
].forEach(r=>row('people.People',{role:r[0],name:r[1]||null,organisation:r[2],supplies:r[3]||null,phone:r[5]||null,email:r[6]||null,remarks:r[0]==='Landscape consultant'?'GR. FLR, Satyanarayan Prasad Commercial Centre, Dayaldas Rd., Vile Parle (E), Mumbai 400057. www.aakararchitect.org':(r[0]==='MEP consultant'?'Mumbai office: 302 Trade Avenue, 260 Suren Road, off Gurunanak Petrol Pump, Andheri (E), Mumbai 400093':null)},P,r[0]));
(S.PREFILL['people.People']).forEach(r=>row('people.People',{role:r.role},'Standard roles the app asks for. Names to be given by the main user.',r.role));

// ---------------- Drawing register
const dr=[]; const d=(no,title,rev,issued,status,remarks)=>dr.push({number:no,title,discipline:'architecture',revision:rev,status,issuedOn:issued,remarks});
d('WL/FP/101','Fire tank level plan','R55','2024-07-29','GFC','File 20251110 All level Parking plans');
d('WL/FP/102','Car pit level plan','R55','2024-07-29','GFC','File 20251110 All level Parking plans');
d('WL/FP/103','Entrance lobby plan (ground)','R55','2024-07-29','GFC','Also re-issued in file 20260528 Ground floor plan: refer it only for fire meter room');
d('WL/FP/104','DG set & double height entrance lobby plan','R55','2024-07-29','GFC','File 20251110');
const pl=['1st','2nd','3rd','4th','5th','6th','7th to 9th','10th','11th to 13th','14th','15th'];
pl.forEach((p,i)=>d('WL/FP/'+(105+i),'Parking '+p+' level plan','R55','2024-07-29','GFC','File 20251110 All level Parking plans'));
const res=[['116','1st level floor plan (club house, refuge, society office)'],['117','2nd level floor plan'],['118','3rd level floor plan'],['119','4th & 6th level floor plan'],['120','5th level floor plan'],['121','7th level floor plan'],['122','8th level floor plan (refuge)'],['123','9th, 11th, 13th & 17th level floor plan'],['124','10th, 12th, 16th & 18th level floor plan'],['125','14th level floor plan'],['126','15th level floor plan (refuge)']];
res.forEach(r=>d('WL/FP/'+r[0],r[1],'R68','2026-09-03','GFC','File 20260902 1st to 18th Floor Plans. R68: revised as per society office'+(['124','125','126'].includes(r[0])?'. DRAWING NUMBER CLASH: same number used on a sheet in the 19th-LMR file':'')));
d('WL/FP/124','19th & 20th level (duplex) floor plans','R65','2026-02-10','GFC','File 20260210. DRAWING NUMBER CLASH with 10th/12th/16th/18th plan');
d('WL/FP/125','Terrace & upper terrace level floor plans','R64','2025-10-06','GFC','File 20260210. DRAWING NUMBER CLASH with 14th level plan');
d('WL/FP/126','LMR bottom & top level floor plans','R64','2025-10-06','GFC','File 20260210. DRAWING NUMBER CLASH with 15th level plan');
d('WL/D/GI/922','Ground infrastructure','R05','2026-08-20','GFC','File 20260608 Ground Floor Infrastructure Detail');
d('WL/D/GI/923','Ground infrastructure','R05','2026-08-20','GFC','File 20260608');
d('WL/D/GI/925','Ground infrastructure - reference levels','R05','2026-08-20','GFC','File 20260608');
d('WL/D/GI/926','Ground infrastructure - gate details',null,'2026-06-08','GFC','File 20260608. No revision shown');
[['WL/SD/501','Staircase ST-01 detail'],['WL/SD/502','Staircase ST-02 detail'],['WL-D-TD-501','Toilet 01 detail'],['WL-D-TD-502','Toilet 02 detail'],['WL-D-TD-503','Toilet 03 detail'],['WL-D-TD-504','Toilet 04 detail'],['WL-D-TD-505','Toilet 05 detail'],['WL-D-TD-506','Toilet 06 detail'],['WL-D-TD-507','Toilet 07 detail'],['WL-D-TD-508','Toilet 08 detail'],['NW-D-KD-510','Kitchen 01 detail']]
 .forEach(r=>dr.push({number:r[0],title:r[1],discipline:'architecture',status:'awaited',remarks:'Referred to on the plans but not in the set received. NW- prefix on the kitchen detail differs from WL-: check it is the right project'}));
dr.forEach(x=>{ Object.keys(x).forEach(k=>{if(x[k]===null||x[k]===undefined)delete x[k];}); row('drawings.Drawings',x,DRG,x.number); });

// ---------------- Approvals and compliance calendar
row('compliance.Compliance',{id:'AP01',item:'MahaRERA registration of Sambhav - project completion date',type:'RERA registration',party:'MahaRERA',reference:'P51900025024',dueDate:'2028-12-30',status:'OPEN',breach:'N',source:'Public property listings - NOT verified on MahaRERA',notes:'Verify number, completion date and any extension on the MahaRERA certificate'},'Public listings, unverified');
row('compliance.Compliance',{id:'AP02',item:'Occupancy Certificate (OC) for the building',type:'OC clearance',party:'BMC (MCGM) - building proposal department',status:'OPEN',breach:'N',source:'Note on drawing WL/FP/116 dated 26-Jun-2026',notes:'Drawing note: As per approved dwg only except additional passage A; Apply for OC 23/06/2026. Confirm whether applied, the file number and the date'},'From the drawing note');
row('compliance.Compliance',{id:'AP03',item:'Final fire NOC from Chief Fire Officer (needed for OC)',type:'OC clearance',party:'Mumbai Fire Brigade (CFO)',status:'OPEN',breach:'N',source:'CFO requirements are referred to in revision R51 of the drawings',notes:'Status, file number and date to be confirmed'},'Standard OC clearance for a high-rise; status unknown');
row('compliance.Compliance',{id:'AP04',item:'Lift and mechanised car parking inspection / licence (needed before occupation)',type:'OC clearance',party:'Lift inspection authority, Govt. of Maharashtra - to confirm',status:'OPEN',breach:'N',source:'2 passenger lifts, fire lift and mechanised parking shown on the drawings',notes:'Confirm the authority, status and dates'},'Standard OC clearance; status unknown');

// ---------------- Risk register (from notes printed on the drawings)
const RS='Note printed on the GFC drawings';
row('risks.Risks',{id:'RK01',date:'2026-10-02',risk:'Building height only assumed acceptable to the client and BMC (architect note 1 on every plan: "We are assuming building height is okay for client & municipal authority")',category:'approvals',warningSign:'Any query on height during OC scrutiny',actionNow:'Get written confirmation that the as-built height matches the approved plans before the OC inspection',status:'OPEN'},RS);
row('risks.Risks',{id:'RK02',date:'2026-10-02',risk:'Amenity area raised from 2% to 4% and slabs extended; the architect asked for comments (1st floor plan)',category:'approvals',warningSign:'Extended slabs not shown in the latest approved (amended) plans',actionNow:'Confirm the extension is covered by the approved plans before the OC inspection',status:'OPEN'},RS);
row('risks.Risks',{id:'RK03',date:'2026-10-02',risk:'OC note: the building matches the approved drawing except the additional passage marked A on the 1st floor',category:'approvals',warningSign:'BMC objects to passage A at OC scrutiny',actionNow:'Confirm passage A is approved or regularised; keep the approval letter ready',status:'OPEN'},RS);
row('risks.Risks',{id:'RK04',date:'2026-10-02',risk:'Same drawing numbers used for different sheets (WL/FP/124, 125, 126): the wrong sheet can be used on site',category:'design',warningSign:'Site refers to a drawing by number only',actionNow:'Ask SBP to renumber; until then quote number AND title',status:'OPEN'},'Found by Claude when reading the drawing set');

// ---------------- Decisions waiting
row('decisions.Decisions',{id:'D01',date:'2026-10-02',decision:'Confirm the stage of the project today: is the OC applied for or received, and what work is still open on site?',options:'Needed before the master schedule can be built. Near OC: a short schedule of remaining work and clearances. Still building: a full floor-by-floor schedule.',forChairman:'N',status:'OPEN'},'Needed to build the schedule');

// ---------------- Cost of delay rates: standard rows, no figures, PENDING
S.PREFILL['rates.Rates'].forEach(r=>row('rates.Rates',D.clone(r),'Standard cost heads with no figures. Rates are filled and approved by the main user; Claude never invents a rate.',r.id));

// ---------------- check with the engine
const inp=D.buildInputs(m,{today:'2026-10-02'}); const res2=E.run(inp);
console.log('engine errors:',res2.validation.errors.length, JSON.stringify(res2.validation.errors.slice(0,5)));
console.log('warnings:',res2.validation.warnings.length);
console.log('gaps:',res2.gaps.map(g=>g.item).join(' | '));
// ---------------- store documents
const stamp=Date.now(), writes=[], idx={};
Object.keys(m.docs).forEach(id=>{ idx[id.replace(/\./g,'__')]=stamp; });
writes.push({op:'set',collection:'t',doc_id:'_index',data:{idx}});
Object.keys(m.docs).forEach(id=>writes.push({op:'set',collection:'t',doc_id:id,data:m.docs[id]}));
fs.writeFileSync('writes.json',JSON.stringify(writes));
const e={}; log.forEach((x,i)=>{ x.t=stamp+i; x.un='Claude'; e['ec'+stamp.toString(36)+i.toString(36)]=x; });
fs.writeFileSync('log.json',JSON.stringify(e));
console.log('docs',Object.keys(m.docs).map(k=>k+'('+(m.docs[k].rows?Object.keys(m.docs[k].rows).length:Object.keys(m.docs[k].kv).length)+')').join(', '),' bytes',JSON.stringify(writes).length,' log entries',log.length);
