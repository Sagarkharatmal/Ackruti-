import { test } from 'node:test';
import assert from 'node:assert/strict';
import WebSocket from 'ws';
import { createServer } from '../src/server.js';

const PLACE = { lat: 12.9716, lng: 77.5946, name: 'Cubbon Park gate' }; // Bengaluru

function wsClient(port) {
  const ws = new WebSocket(`ws://127.0.0.1:${port}/ws`);
  const inbox = [];
  const waiters = [];
  ws.on('message', (raw) => {
    const msg = JSON.parse(raw.toString());
    inbox.push(msg);
    for (const w of [...waiters]) if (w.match(msg)) (waiters.splice(waiters.indexOf(w), 1), w.resolve(msg));
  });
  return {
    ws,
    opened: new Promise((r) => ws.once('open', r)),
    send: (m) => ws.send(JSON.stringify(m)),
    waitFor(match, ms = 4000) {
      const found = inbox.find(match);
      if (found) return Promise.resolve(found);
      return new Promise((resolve, reject) => {
        const t = setTimeout(() => reject(new Error('timeout')), ms);
        waiters.push({ match, resolve: (m) => (clearTimeout(t), resolve(m)) });
      });
    },
  };
}

test('event night: sign up, host, RSVP, chat, check in, run a round, leaderboard', async () => {
  let clock = Date.UTC(2026, 10, 7, 13, 0, 0);
  const server = await createServer({ port: 0, host: '127.0.0.1', now: () => clock, log: () => {} });
  const base = `http://127.0.0.1:${server.port}`;
  const call = async (method, path, { user, body } = {}) => {
    const headers = { 'content-type': 'application/json' };
    if (user) Object.assign(headers, { 'x-user-id': user.user.id, 'x-user-secret': user.secret });
    const res = await fetch(base + path, { method, headers, body: body ? JSON.stringify(body) : undefined });
    return { status: res.status, ...(await res.json()) };
  };
  try {
    const host = await call('POST', '/api/users', { body: { name: 'Riya' } });
    const aman = await call('POST', '/api/users', { body: { name: 'Aman' } });
    const bela = await call('POST', '/api/users', { body: { name: 'Bela' } });
    assert.ok(host.secret && host.user.id);

    assert.equal((await call('POST', '/api/events', { body: { title: 'x' } })).status, 401, 'auth required');
    const created = await call('POST', '/api/events', {
      user: host,
      body: {
        title: 'Cubbon Park Chhupan Chhupai',
        community: 'BLR Night Runners',
        startsAt: clock + 30 * 60_000,
        durationMin: 180,
        place: PLACE,
        capacity: 50,
        sponsor: { name: 'Filter Kaapi Co.' },
        rules: { headStartSec: 30, pingIntervalSec: 60, durationMin: 10 },
      },
    });
    assert.equal(created.status, 200, created.error);
    const ev = created.event;
    assert.equal(ev.me.isHost, true);

    const list = await call('GET', `/api/events?lat=${PLACE.lat}&lng=${PLACE.lng}&radiusKm=10`);
    assert.equal(list.events.length, 1);
    assert.equal(list.events[0].sponsor.name, 'Filter Kaapi Co.');

    assert.equal((await call('GET', `/api/events/${ev.id}/messages`, { user: aman })).status, 400, 'chat needs RSVP');
    await call('POST', `/api/events/${ev.code}/rsvp`, { user: aman });
    await call('POST', `/api/events/${ev.id}/rsvp`, { user: bela });
    const msg = await call('POST', `/api/events/${ev.id}/messages`, { user: aman, body: { text: 'Main aa raha hoon!' } });
    assert.equal(msg.message.text, 'Main aa raha hoon!');
    await call('POST', `/api/events/${ev.id}/announce`, { user: host, body: { text: 'Gate 2, 7pm sharp' } });
    const msgs = await call('GET', `/api/events/${ev.id}/messages?since=0`, { user: bela });
    assert.ok(msgs.messages.some((m) => m.kind === 'announcement'));

    const far = await call('POST', `/api/events/${ev.id}/checkin`, { user: aman, body: { lat: PLACE.lat + 0.02, lng: PLACE.lng } });
    assert.equal(far.status, 400);
    await call('POST', `/api/events/${ev.id}/checkin`, { user: aman, body: { ...PLACE, acc: 8 } });
    await call('POST', `/api/events/${ev.id}/checkin/${bela.user.id}`, { user: host });
    const mine = await call('GET', '/api/events/mine', { user: aman });
    assert.equal(mine.attending.length, 1);
    assert.equal(mine.attending[0].checkedIn, 2);

    assert.equal((await call('POST', `/api/events/${ev.id}/rounds`, { user: aman })).status, 400, 'host only');
    const round = await call('POST', `/api/events/${ev.id}/rounds`, { user: host });
    assert.equal(round.status, 200, round.error);
    assert.equal(round.event.currentRound.code, round.code);
    assert.equal((await call('POST', `/api/events/${ev.id}/rounds`, { user: host })).status, 400, 'one round at a time');

    // Join over WebSocket. Anonymous joins are refused for event rounds.
    const anon = wsClient(server.port);
    await anon.opened;
    anon.send({ t: 'join', code: round.code, name: 'Stranger' });
    assert.match((await anon.waitFor((m) => m.t === 'error')).message, /event/);
    anon.ws.close();

    const clients = {};
    for (const [key, u] of Object.entries({ aman, bela, host })) {
      const c = wsClient(server.port);
      await c.opened;
      c.send({ t: 'join', code: round.code, name: u.user.name, userId: u.user.id, userSecret: u.secret });
      c.welcome = await c.waitFor((m) => m.t === 'welcome');
      clients[key] = c;
    }
    const lobby = await clients.host.waitFor((m) => m.t === 'state' && m.players.length === 3);
    assert.equal(lobby.you.isHost, true, 'event host hosts the round even though they joined last');
    clients.host.send({ t: 'setRole', pref: 'seeker' });
    clients.aman.send({ t: 'setRole', pref: 'hider' });
    clients.bela.send({ t: 'setRole', pref: 'hider' });
    await clients.host.waitFor((m) => m.t === 'state' && m.players.every((p) => p.rolePref !== 'random'));
    clients.host.send({ t: 'start' });
    await clients.host.waitFor((m) => m.t === 'state' && m.phase === 'headstart');
    clock += 31_000;
    await clients.host.waitFor((m) => m.t === 'state' && m.phase === 'hunt');
    clock += 3 * 60_000;
    clients.bela.send({ t: 'caught', byId: clients.host.welcome.playerId });
    await clients.host.waitFor((m) => m.t === 'state' && m.hidersLeft === 1);
    clock += 8 * 60_000; // time runs out -> hiders win
    await clients.host.waitFor((m) => m.t === 'state' && m.phase === 'ended');

    const after = await call('GET', `/api/events/${ev.id}`, { user: bela });
    assert.equal(after.event.currentRound, null);
    assert.equal(after.event.rounds.length, 1);
    assert.equal(after.event.rounds[0].winner, 'hider');
    const board = after.event.leaderboard;
    assert.equal(board[0].name, 'Aman', JSON.stringify(board));
    assert.equal(board[0].points, 10 + 10, 'survived 10 hunt minutes + never found');
    assert.equal(board.find((r) => r.name === 'Riya').points, 5);
    for (const c of Object.values(clients)) c.ws.close();
  } finally {
    await server.close();
  }
});
