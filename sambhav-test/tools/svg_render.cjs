const { chromium } = require('/opt/node-tools/node_modules/playwright');
const fs = require('fs');
(async () => {
  const [svgPath, outPath, W, vb] = process.argv.slice(2);
  let svg = fs.readFileSync(svgPath, 'utf8');
  if (vb) svg = svg.replace(/viewBox="[^"]*"/, 'viewBox="' + vb + '"');
  const m = /viewBox="([-\d.e]+) ([-\d.e]+) ([-\d.e]+) ([-\d.e]+)"/.exec(svg);
  const w = +W, h = Math.round(w * (+m[4]) / (+m[3]));
  svg = svg.replace(/<svg([^>]*?)\swidth="[^"]*"/, '<svg$1').replace(/<svg([^>]*?)\sheight="[^"]*"/, '<svg$1').replace('<svg', `<svg width="${w}" height="${h}" style="background:#fff"`);
  const b = await chromium.launch({ executablePath: '/opt/pw-browsers/chromium-1194/chrome-linux/chrome' });
  const p = await b.newPage({ viewport: { width: w, height: Math.min(h, 16000) } });
  await p.setContent('<html><head><style>svg *{fill:none!important;stroke:#222!important;stroke-width:1px!important;vector-effect:non-scaling-stroke}svg text,svg tspan{fill:#000!important;stroke:none!important}</style></head><body style="margin:0;background:#fff">' + svg + '</body></html>', { timeout: 120000 });
  await p.screenshot({ path: outPath, fullPage: true });
  await b.close(); console.log('ok', outPath, w, h);
})().catch(e => { console.error(e.message); process.exit(1); });
