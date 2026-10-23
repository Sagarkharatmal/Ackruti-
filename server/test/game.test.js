import { test } from 'node:test';
import assert from 'node:assert/strict';
import { Game, GameError, PHASE, ROLE, STATUS, assignRoles, sanitiseSettings } from '../src/game.js';
import { distanceM } from '../src/geo.js';

// Zone centred on India Gate, New Delhi.
const CENTER = { lat: 28.6129, lng: 77.2295 };
const metresNorth = (m) => ({ lat: CENTER.lat + m / 111_320, lng: CENTER.lng });

function setup({ players = ['Asha', 'Bilal', 'Chetan', 'Divya'], settings = {} } = {}) {
  let clock = 1_000_000;
  const sent = [];
  const game = new Game({
    code: 'TEST42',
    now: () => clock,
    send: (id, msg) => sent.push({ id, msg }),
    random: () => 0.42,
    settings: {
      zone: { ...CENTER, radiusM: 300 },
      headStartSec: 60,
      pingIntervalSec: 120,
      durationMin: 10,
      ...settings,
    },
  });
  const ps = players.map((n) => game.addPlayer(n));
  const advance = (ms) => {
    // Tick once per simulated second like the real server.
    for (let i = 0; i < ms; i += 1000) {
      clock += Math.min(1000, ms - i);
      game.tick();
    }
  };
  return { game, ps, sent, advance, now: () => clock };
}

test('haversine distance is sane', () => {
  const d = distanceM(CENTER.lat, CENTER.lng, metresNorth(500).lat, CENTER.lng);
  assert.ok(Math.abs(d - 500) < 1, `got ${d}`);
});

test('settings are clamped to safe ranges', () => {
  const s = sanitiseSettings({ durationMin: 9999, headStartSec: -5, zone: { lat: 200, lng: 0, radiusM: 10 } });
  assert.equal(s.durationMin, 180);
  assert.equal(s.headStartSec, 30);
  assert.equal(s.zone, null, 'invalid zone rejected');
  const ok = sanitiseSettings({ zone: { lat: 10, lng: 10, radiusM: 99999 } });
  assert.equal(ok.zone.radiusM, 1500);
});

test('first player is host, names must be unique, late joins are refused', () => {
  const { game, ps } = setup();
  assert.equal(game.hostId, ps[0].id);
  assert.throws(() => game.addPlayer('asha'), GameError);
  game.start(ps[0].id);
  assert.throws(() => game.addPlayer('Eshan'), /already started/);
});

test('only the host can start and change settings', () => {
  const { game, ps } = setup();
  assert.throws(() => game.start(ps[1].id), /Only the host/);
  assert.throws(() => game.updateSettings(ps[1].id, { durationMin: 20 }), /Only the host/);
  game.updateSettings(ps[0].id, { durationMin: 20 });
  assert.equal(game.settings.durationMin, 20);
  assert.equal(game.settings.zone.radiusM, 300, 'unrelated settings are kept');
});

test('cannot start with the zone enabled but not placed', () => {
  const { game, ps } = setup({ settings: { zone: null } });
  assert.throws(() => game.start(ps[0].id), /play zone/);
  game.updateSettings(ps[0].id, { zoneEnabled: false });
  game.start(ps[0].id);
  assert.equal(game.phase, PHASE.HEADSTART);
});

test('role assignment honours preferences and always has both teams', () => {
  const mk = (prefs) => prefs.map((rolePref, i) => ({ id: `p${i}`, rolePref }));
  let roles = assignRoles(mk(['random', 'random', 'random', 'random']), () => 0.1);
  let values = [...roles.values()];
  assert.equal(values.filter((r) => r === ROLE.SEEKER).length, 1);
  assert.equal(values.filter((r) => r === ROLE.HIDER).length, 3);

  roles = assignRoles(mk(['seeker', 'seeker', 'seeker']), () => 0.1);
  values = [...roles.values()];
  assert.ok(values.includes(ROLE.HIDER), 'someone must hide');

  roles = assignRoles(mk(['hider', 'hider']), () => 0.1);
  values = [...roles.values()];
  assert.ok(values.includes(ROLE.SEEKER), 'someone must seek');

  roles = assignRoles(mk(['seeker', 'hider', 'random', 'random', 'random', 'random', 'random', 'random']), () => 0.3);
  assert.equal(roles.get('p0'), ROLE.SEEKER);
  assert.equal(roles.get('p1'), ROLE.HIDER);
  assert.equal([...roles.values()].filter((r) => r === ROLE.SEEKER).length, 2, 'about one seeker per four players');
});

function startWithSeeker(ctx, seekerIndex = 0) {
  const { game, ps } = ctx;
  ps.forEach((p, i) => game.setRolePref(p.id, i === seekerIndex ? 'seeker' : 'hider'));
  game.start(ps[0].id);
  return { seeker: ps[seekerIndex], hiders: ps.filter((_, i) => i !== seekerIndex) };
}

test('head start hides everything from seekers, then the hunt begins with a ping', () => {
  const ctx = setup();
  const { game, advance } = ctx;
  const { seeker, hiders } = startWithSeeker(ctx);
  hiders.forEach((h, i) => game.updateLocation(h.id, { ...metresNorth(50 * (i + 1)), acc: 5 }));
  game.updateLocation(seeker.id, { ...CENTER, acc: 5 });

  let view = game.viewFor(seeker.id);
  assert.equal(view.phase, PHASE.HEADSTART);
  assert.deepEqual(view.pings, []);
  assert.deepEqual(view.live, [], 'no hider positions during head start');

  advance(60_000);
  view = game.viewFor(seeker.id);
  assert.equal(view.phase, PHASE.HUNT);
  assert.equal(view.pings.length, 1, 'ping on hunt start');
  assert.equal(view.pings[0].marks.length, hiders.length);
  assert.ok(view.pings[0].marks.every((m) => m.lat != null));

  advance(119_000);
  assert.equal(game.viewFor(seeker.id).pings.length, 1);
  advance(1_000);
  assert.equal(game.viewFor(seeker.id).pings.length, 2, 'second ping after the interval');
});

test('hiders never see seekers or other hiders', () => {
  const ctx = setup();
  const { game, advance } = ctx;
  const { seeker, hiders } = startWithSeeker(ctx);
  game.updateLocation(seeker.id, { lat: 28.6011111, lng: 77.2177777, acc: 5 });
  game.updateLocation(hiders[1].id, { lat: 28.6122222, lng: 77.2288888, acc: 5 });
  advance(61_000);
  const view = game.viewFor(hiders[0].id);
  assert.deepEqual(view.live, []);
  assert.deepEqual(view.pings, []);
  assert.ok(view.lastPingAt, 'hiders know when they were pinged');
  const json = JSON.stringify(view);
  assert.ok(!json.includes('28.6011111') && !json.includes('77.2177777'), 'seeker position leaked');
  assert.ok(!json.includes('28.6122222'), 'other hider position leaked');
});

test('seekers see each other live', () => {
  const ctx = setup({ players: ['A', 'B', 'C', 'D', 'E'] });
  const { game, ps } = ctx;
  game.setRolePref(ps[0].id, 'seeker');
  game.setRolePref(ps[1].id, 'seeker');
  for (const p of ps.slice(2)) game.setRolePref(p.id, 'hider');
  game.start(ps[0].id);
  game.updateLocation(ps[1].id, { ...CENTER, acc: 5 });
  game.updateLocation(ps[2].id, { ...CENTER, acc: 5 });
  const live = game.viewFor(ps[0].id).live;
  assert.equal(live.length, 1);
  assert.equal(live[0].kind, 'teammate');
});

test('leaving the zone reveals a hider live after the grace period', () => {
  const ctx = setup();
  const { game, advance, sent } = ctx;
  const { seeker, hiders } = startWithSeeker(ctx);
  const runaway = hiders[0];
  advance(61_000);
  game.updateLocation(runaway.id, { ...metresNorth(450), acc: 5 });
  assert.ok(sent.some((s) => s.id === runaway.id && s.msg.kind === 'zone_warning'));
  assert.equal(game.viewFor(seeker.id).live.length, 0, 'not revealed during grace');
  advance(30_000);
  const live = game.viewFor(seeker.id).live;
  assert.equal(live.length, 1);
  assert.equal(live[0].kind, 'revealed');
  assert.equal(game.viewFor(runaway.id).you.revealed, true);

  game.updateLocation(runaway.id, { ...metresNorth(100), acc: 5 });
  assert.equal(game.viewFor(seeker.id).live.length, 0, 'hidden again once back inside');
});

test('GPS accuracy gives a little benefit of the doubt at the edge', () => {
  const ctx = setup();
  const { game, advance } = ctx;
  const { hiders } = startWithSeeker(ctx);
  advance(61_000);
  game.updateLocation(hiders[0].id, { ...metresNorth(320), acc: 25 });
  assert.equal(game.players.get(hiders[0].id).outSince, null);
});

test('shrinking zone gets smaller over the hunt', () => {
  const ctx = setup({ settings: { shrinkEnabled: true, shrinkToPct: 50 } });
  const { game, advance } = ctx;
  startWithSeeker(ctx);
  assert.equal(game.currentRadiusM(), 300);
  advance(60_000);
  assert.equal(game.currentRadiusM(), 300);
  advance(5 * 60_000);
  assert.equal(game.currentRadiusM(), 225);
});

test('tag requests must be confirmed by the hider', () => {
  const ctx = setup();
  const { game, advance, sent } = ctx;
  const { seeker, hiders } = startWithSeeker(ctx);
  assert.throws(() => game.requestTag(seeker.id, hiders[0].id), /during the hunt/);
  advance(61_000);
  assert.throws(() => game.requestTag(hiders[1].id, hiders[0].id), /Only seekers/);

  const req = game.requestTag(seeker.id, hiders[0].id);
  assert.ok(sent.some((s) => s.id === hiders[0].id && s.msg.t === 'tagRequest'));
  assert.equal(game.viewFor(hiders[0].id).you.pendingTagRequests.length, 1);
  game.respondTag(hiders[0].id, req.id, false);
  assert.equal(game.players.get(hiders[0].id).status, STATUS.ACTIVE);
  assert.ok(sent.some((s) => s.id === seeker.id && s.msg.kind === 'tag_denied'));

  const req2 = game.requestTag(seeker.id, hiders[0].id);
  game.respondTag(hiders[0].id, req2.id, true);
  const caught = game.players.get(hiders[0].id);
  assert.equal(caught.status, STATUS.CAUGHT);
  assert.equal(caught.caughtBy, seeker.id);
  assert.equal(game.players.get(seeker.id).catches, 1);
  assert.throws(() => game.respondTag(hiders[0].id, req2.id, true), /expired/);
});

test('tag requests expire', () => {
  const ctx = setup();
  const { game, advance } = ctx;
  const { seeker, hiders } = startWithSeeker(ctx);
  advance(61_000);
  const req = game.requestTag(seeker.id, hiders[0].id);
  advance(61_000);
  assert.throws(() => game.respondTag(hiders[0].id, req.id, true), /expired/);
});

test('caught hiders become spectators who see everyone', () => {
  const ctx = setup();
  const { game, advance } = ctx;
  const { hiders } = startWithSeeker(ctx);
  for (const p of ctx.ps) game.updateLocation(p.id, { ...CENTER, acc: 5 });
  advance(61_000);
  game.reportCaught(hiders[0].id);
  const view = game.viewFor(hiders[0].id);
  assert.equal(view.you.spectator, true);
  assert.equal(view.live.length, 3);
  assert.ok(view.live.every((l) => l.kind === 'spectate'));
});

test('seekers win when every hider is caught', () => {
  const ctx = setup();
  const { game, advance } = ctx;
  const { seeker, hiders } = startWithSeeker(ctx);
  advance(61_000);
  for (const h of hiders) game.reportCaught(h.id, seeker.id);
  assert.equal(game.phase, PHASE.ENDED);
  assert.equal(game.results.winner, ROLE.SEEKER);
  assert.equal(game.results.players.find((p) => p.id === seeker.id).catches, 3);
});

test('hiders win when time runs out; results include distance and trails', () => {
  const ctx = setup();
  const { game, advance } = ctx;
  const { hiders } = startWithSeeker(ctx);
  for (let i = 0; i <= 10; i++) game.updateLocation(hiders[0].id, { ...metresNorth(i * 20), acc: 5 });
  advance(60_000 + 10 * 60_000);
  assert.equal(game.phase, PHASE.ENDED);
  assert.equal(game.results.winner, ROLE.HIDER);
  const r = game.results.players.find((p) => p.id === hiders[0].id);
  assert.ok(Math.abs(r.distanceM - 200) < 2, `distance ${r.distanceM}`);
  assert.equal(r.trail.length, 11);
  assert.equal(r.survivedMs, 11 * 60_000);
});

test('the game ends for the hiders if every seeker leaves', () => {
  const ctx = setup();
  const { game } = ctx;
  const { seeker } = startWithSeeker(ctx);
  game.removePlayer(seeker.id);
  assert.equal(game.phase, PHASE.ENDED);
  assert.equal(game.results.winner, ROLE.HIDER);
});

test('host leaving hands over hosting', () => {
  const { game, ps } = setup();
  game.removePlayer(ps[0].id);
  assert.equal(game.hostId, ps[1].id);
  assert.equal(game.players.has(ps[0].id), false, 'lobby leavers are removed entirely');
});

test('rematch returns everyone to the lobby with fresh stats', () => {
  const ctx = setup();
  const { game, advance, ps } = ctx;
  const { seeker, hiders } = startWithSeeker(ctx);
  advance(61_000);
  game.removePlayer(hiders[2].id);
  for (const h of hiders.slice(0, 2)) game.reportCaught(h.id, seeker.id);
  assert.equal(game.phase, PHASE.ENDED);
  game.rematch(ps[0].id);
  assert.equal(game.phase, PHASE.LOBBY);
  assert.equal(game.players.size, 3, 'players who left are dropped');
  for (const p of game.players.values()) {
    assert.equal(p.status, STATUS.ACTIVE);
    assert.equal(p.role, null);
    assert.equal(p.catches, 0);
  }
});

test('auth tokens are required to resume', () => {
  const { game, ps } = setup();
  assert.throws(() => game.authenticate(ps[0].id, 'nope'), GameError);
  assert.equal(game.authenticate(ps[0].id, ps[0].token), ps[0]);
});
