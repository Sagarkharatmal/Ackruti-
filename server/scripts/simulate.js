// Plays a short bot game against a running server so you can check a
// deployment works end to end without walking around with three phones.
//
//   node scripts/simulate.js                      # against ws://localhost:8080/ws
//   node scripts/simulate.js wss://your-app.onrender.com/ws
//
// Takes about 70 seconds: 30 s head start, a ping, a tag, then the end screen.

import WebSocket from 'ws';

const url = process.argv[2] ?? 'ws://localhost:8080/ws';
const CENTER = { lat: 18.5204, lng: 73.8567 }; // Pune
const log = (who, text) => console.log(`${new Date().toISOString().slice(11, 19)}  ${who.padEnd(7)} ${text}`);

function bot(name) {
  const ws = new WebSocket(url);
  const self = { name, ws, state: null, welcome: null, handlers: [] };
  self.ready = new Promise((resolve, reject) => {
    ws.once('open', resolve);
    ws.once('error', reject);
  });
  self.send = (msg) => ws.send(JSON.stringify(msg));
  self.next = (match, timeoutMs = 60_000) =>
    new Promise((resolve, reject) => {
      const timer = setTimeout(() => reject(new Error(`${name}: timed out`)), timeoutMs);
      self.handlers.push((m) => {
        if (!match(m)) return false;
        clearTimeout(timer);
        resolve(m);
        return true;
      });
    });
  ws.on('message', (raw) => {
    const msg = JSON.parse(raw.toString());
    if (msg.t === 'state') self.state = msg;
    if (msg.t === 'welcome') self.welcome = msg;
    if (msg.t === 'notice' || msg.t === 'error' || msg.t === 'tagRequest') {
      log(name, `<- ${msg.t}: ${msg.text ?? msg.message ?? `tag request from ${msg.fromName}`}`);
    }
    self.handlers = self.handlers.filter((h) => !h(msg));
  });
  return self;
}

const walk = (b, dLat, dLng) => {
  let step = 0;
  return setInterval(() => {
    step++;
    b.send({ t: 'loc', lat: CENTER.lat + dLat * step, lng: CENTER.lng + dLng * step, acc: 6 });
  }, 2000);
};

async function main() {
  log('sim', `connecting to ${url}`);
  const seeker = bot('Seeker');
  const hiders = [bot('Hider1'), bot('Hider2')];
  await Promise.all([seeker.ready, ...hiders.map((h) => h.ready)]);

  seeker.send({
    t: 'create',
    name: 'Seeker',
    settings: { zone: { ...CENTER, radiusM: 400 }, headStartSec: 30, pingIntervalSec: 30, durationMin: 5 },
  });
  const { code } = await seeker.next((m) => m.t === 'welcome');
  log('sim', `game code ${code}`);
  for (const h of hiders) {
    h.send({ t: 'join', code, name: h.name });
    await h.next((m) => m.t === 'welcome');
    h.send({ t: 'setRole', pref: 'hider' });
  }
  seeker.send({ t: 'setRole', pref: 'seeker' });
  await seeker.next((m) => m.t === 'state' && m.players.length === 3 && m.players.filter((p) => p.rolePref === 'hider').length === 2);
  seeker.send({ t: 'start' });
  await seeker.next((m) => m.t === 'state' && m.phase === 'headstart');
  log('sim', 'head start running (30 s)…');

  const timers = [walk(seeker, 0.00002, 0), walk(hiders[0], 0.00005, 0.00003), walk(hiders[1], -0.00004, 0.00001)];

  const hunt = await seeker.next((m) => m.t === 'state' && m.phase === 'hunt' && m.pings.length > 0, 45_000);
  log('sim', `hunt started; seeker sees ping with ${hunt.pings[0].marks.length} hiders`);
  if (hiders[0].state.live.length || hiders[0].state.pings.length) throw new Error('hider can see secret data!');

  const target = hiders[0];
  seeker.send({ t: 'tag', playerId: target.welcome.playerId });
  const req = await target.next((m) => m.t === 'tagRequest');
  target.send({ t: 'tagReply', requestId: req.requestId, accept: true });
  await target.next((m) => m.t === 'state' && m.you.spectator);
  log('sim', `${target.name} is now spectating and sees ${target.state.live.length} live players`);

  hiders[1].send({ t: 'caught' });
  const end = await seeker.next((m) => m.t === 'state' && m.phase === 'ended');
  log('sim', `game over — winner: ${end.results.winner} (${end.results.reason})`);
  for (const p of end.results.players) {
    log('sim', `  ${p.name.padEnd(7)} ${p.role.padEnd(6)} ${p.distanceM} m  catches=${p.catches}`);
  }
  timers.forEach(clearInterval);
  for (const b of [seeker, ...hiders]) b.ws.close();
  log('sim', 'SUCCESS');
}

main().catch((err) => {
  console.error('SIMULATION FAILED:', err.message);
  process.exit(1);
});
