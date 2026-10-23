import { test } from 'node:test';
import assert from 'node:assert/strict';
import WebSocket from 'ws';
import { createServer } from '../src/server.js';

const CENTER = { lat: 19.076, lng: 72.8777 }; // Mumbai

/** Minimal test client that records every message and can wait for one. */
function client(port) {
  const ws = new WebSocket(`ws://127.0.0.1:${port}/ws`);
  const inbox = [];
  const waiters = [];
  ws.on('message', (raw) => {
    const msg = JSON.parse(raw.toString());
    inbox.push(msg);
    for (const w of [...waiters]) {
      if (w.match(msg)) {
        waiters.splice(waiters.indexOf(w), 1);
        w.resolve(msg);
      }
    }
  });
  const opened = new Promise((r) => ws.once('open', r));
  return {
    ws,
    inbox,
    opened,
    send: (msg) => ws.send(JSON.stringify(msg)),
    waitFor(match, timeoutMs = 4000) {
      const found = inbox.find(match);
      if (found) return Promise.resolve(found);
      return new Promise((resolve, reject) => {
        const timer = setTimeout(() => reject(new Error('timed out waiting for message')), timeoutMs);
        waiters.push({ match, resolve: (m) => (clearTimeout(timer), resolve(m)) });
      });
    },
    lastState: () => [...inbox].reverse().find((m) => m.t === 'state'),
  };
}

test('full lobby → round → results flow over WebSocket', async () => {
  let clock = 5_000_000;
  const server = await createServer({ port: 0, host: '127.0.0.1', now: () => clock, log: () => {} });
  try {
    const host = client(server.port);
    await host.opened;
    host.send({ t: 'create', name: 'Host', settings: { zone: { ...CENTER, radiusM: 500 }, headStartSec: 30 } });
    const welcome = await host.waitFor((m) => m.t === 'welcome');
    assert.match(welcome.code, /^[A-Z2-9]{6}$/);

    const bad = client(server.port);
    await bad.opened;
    bad.send({ t: 'join', code: 'ZZZZZZ', name: 'Nobody' });
    assert.match((await bad.waitFor((m) => m.t === 'error')).message, /No game/);
    bad.ws.close();

    const a = client(server.port);
    const b = client(server.port);
    await Promise.all([a.opened, b.opened]);
    a.send({ t: 'join', code: welcome.code.toLowerCase(), name: 'Anika' });
    b.send({ t: 'join', code: welcome.code, name: 'Bunty' });
    const aw = await a.waitFor((m) => m.t === 'welcome');
    await b.waitFor((m) => m.t === 'welcome');

    host.send({ t: 'setRole', pref: 'seeker' });
    a.send({ t: 'setRole', pref: 'hider' });
    b.send({ t: 'setRole', pref: 'hider' });
    a.send({ t: 'start' });
    assert.match((await a.waitFor((m) => m.t === 'error')).message, /Only the host/);

    await host.waitFor((m) => m.t === 'state' && m.players.length === 3 && m.players.every((p) => p.rolePref !== 'random'));
    host.send({ t: 'start' });
    await host.waitFor((m) => m.t === 'state' && m.phase === 'headstart');

    host.send({ t: 'loc', ...CENTER, acc: 5 });
    a.send({ t: 'loc', lat: CENTER.lat + 0.001, lng: CENTER.lng, acc: 5 });
    b.send({ t: 'loc', lat: CENTER.lat - 0.001, lng: CENTER.lng, acc: 5 });

    clock += 31_000; // head start over; the 1s tick should start the hunt and ping
    const hunt = await host.waitFor((m) => m.t === 'state' && m.phase === 'hunt' && m.pings.length === 1);
    assert.equal(hunt.pings[0].marks.length, 2);
    const hiderView = await a.waitFor((m) => m.t === 'state' && m.phase === 'hunt');
    assert.deepEqual(hiderView.live, []);
    assert.deepEqual(hiderView.pings, []);

    // Reconnect with the saved token after a dropped connection.
    a.ws.close();
    await host.waitFor((m) => m.t === 'state' && m.players.find((p) => p.id === aw.playerId)?.connected === false);
    const a2 = client(server.port);
    await a2.opened;
    a2.send({ t: 'resume', code: aw.code, playerId: aw.playerId, token: aw.token });
    await a2.waitFor((m) => m.t === 'welcome');

    host.send({ t: 'tag', playerId: aw.playerId });
    const req = await a2.waitFor((m) => m.t === 'tagRequest');
    assert.equal(req.fromName, 'Host');
    a2.send({ t: 'tagReply', requestId: req.requestId, accept: true });
    await a2.waitFor((m) => m.t === 'state' && m.you.spectator === true);

    b.send({ t: 'caught' });
    const end = await host.waitFor((m) => m.t === 'state' && m.phase === 'ended');
    assert.equal(end.results.winner, 'seeker');
    assert.equal(end.results.players.length, 3);

    host.send({ t: 'rematch' });
    await host.waitFor((m) => m.t === 'state' && m.phase === 'lobby' && m.round === 2);

    const health = await fetch(`http://127.0.0.1:${server.port}/health`).then((r) => r.json());
    assert.equal(health.ok, true);
    assert.equal(health.games, 1);
    for (const c of [host, a2, b]) c.ws.close();
  } finally {
    await server.close();
  }
});
