import { Dwg_File_Type, LibreDwg } from '@mlightcad/libredwg-web';
import fs from 'fs'; import path from 'path';
const dir = process.argv[2], out = process.argv[3];
fs.mkdirSync(out, { recursive: true });
const lib = await LibreDwg.create('./node_modules/@mlightcad/libredwg-web/wasm/');
const only = process.argv[4];
for (const f of fs.readdirSync(dir).filter(x => x.toLowerCase().endsWith('.dwg') && (!only || x === only)).sort()) {
  const base = f.replace(/\.dwg$/i, '');
  const t0 = Date.now();
  try {
    const buf = fs.readFileSync(path.join(dir, f));
    const dwg = lib.dwg_read_data(buf, Dwg_File_Type.DWG);
    if (!dwg) { console.log('FAIL read', f); continue; }
    console.log('read ok', f); const db = lib.convert(dwg); console.log('convert ok');
    const ents = db.entities || [];
    const byType = {}; ents.forEach(e => byType[e.type] = (byType[e.type] || 0) + 1);
    const layers = {}; ents.forEach(e => layers[e.layer] = (layers[e.layer] || 0) + 1);
    const texts = [];
    ents.forEach(e => { if (e.type === 'TEXT' || e.type === 'MTEXT' || e.type === 'ATTRIB') { const s = (e.text ?? e.value ?? '').toString(); if (s.trim()) texts.push({ t: s, layer: e.layer, x: Math.round((e.startPoint || e.insertionPoint || {}).x || 0), y: Math.round((e.startPoint || e.insertionPoint || {}).y || 0), h: e.textHeight || e.height }); } });
    // texts inside blocks (title blocks often live there)
    const btexts = [];
    (db.tables?.BLOCK_RECORD?.entries || []).forEach(b => (b.entities || []).forEach(e => { if (e.type === 'TEXT' || e.type === 'MTEXT') { const s = (e.text ?? '').toString(); if (s.trim()) btexts.push({ block: b.name, t: s }); } }));
    const dims = ents.filter(e => e.type === 'DIMENSION').map(e => ({ m: e.measurement, txt: e.text, layer: e.layer }));
    console.log('json...'); fs.writeFileSync(path.join(out, base + '.json'), JSON.stringify({ file: f, header: { units: db.header?.INSUNITS, extmin: db.header?.EXTMIN, extmax: db.header?.EXTMAX, acadver: db.header?.ACADVER }, byType, layers, texts, btexts, dims }, null, 1));
    try { const svg = lib.dwg_to_svg(db); fs.writeFileSync(path.join(out, base + '.svg'), svg); } catch (e) { console.log('svg fail', f, e.message); }
    lib.dwg_free(dwg);
    console.log('OK', f, 'entities', ents.length, 'texts', texts.length, 'blockTexts', btexts.length, 'dims', dims.length, (Date.now() - t0) + 'ms');
  } catch (e) { console.log('ERR', f, e.message); }
}
