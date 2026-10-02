const SRC='/tmp/claude-0/-home-user-Ackruti-/d1ceb428-4747-5f0b-b9e2-a218c556721a/scratchpad/pkg/AiAi_PM_Master_App_v8/source/';
const fs=require('fs'),D=require(SRC+'09_Dashboards/Master_App/src/app_data.js'),E=require(SRC+'10_Engine/engine_core.js');
D.init(JSON.parse(fs.readFileSync(SRC+'09_Dashboards/Master_App/src/schemas.json','utf8')));
const m=D.emptyModel(); let same=0,diff=0;
fs.readdirSync('readback/t').filter(f=>f!=='_index.json').forEach(f=>{ const id=f.slice(0,-5); const got=JSON.parse(fs.readFileSync('readback/t/'+f)); const want=JSON.parse(fs.readFileSync('docs/'+f));
  const c=o=>JSON.stringify(o,(k,v)=>v&&typeof v==='object'&&!Array.isArray(v)?Object.keys(v).sort().reduce((a,x)=>(a[x]=v[x],a),{}):v); c(got)===c(want)?same++:diff++; D.applyDoc(m,id,got); });
const r=E.run(D.buildInputs(m,{today:'2019-01-07',dataDate:'2019-01-06'}));
console.log('identical docs',same,'different',diff,'| engine errors',r.validation.errors.length,'| milestones',r.schedule.milestones.map(x=>x.tag+' '+x.baselineDate+' '+x.status).join('; '));
