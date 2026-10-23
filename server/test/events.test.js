import { test } from 'node:test';
import assert from 'node:assert/strict';
import { EventService, roundPoints } from '../src/events.js';
import { MemoryStore } from '../src/store.js';

const PLACE = { lat: 18.9220, lng: 72.8347, name: 'Gateway of India' };
const HOUR = 60 * 60_000;

function setup() {
  let clock = Date.UTC(2026, 10, 1, 12, 0, 0);
  const events = new EventService(new MemoryStore(), { now: () => clock });
  const host = events.createUser('Riya').user;
  const users = ['Aman', 'Bela', 'Chirag'].map((n) => events.createUser(n).user);
  const event = events.createEvent(host, {
    title: 'Chhupan Chhupai Night',
    description: 'Bring water!',
    community: 'Colaba Run Club',
    startsAt: clock + 24 * HOUR,
    durationMin: 120,
    place: PLACE,
    capacity: 3,
    sponsor: { name: 'Chai Point' },
  });
  return { events, host, users, event, tick: (ms) => (clock += ms), now: () => clock };
}

test('users get a secret that is required to authenticate', () => {
  const events = new EventService(new MemoryStore());
  const { user, secret } = events.createUser('Kabir');
  assert.equal(events.auth(user.id, secret).name, 'Kabir');
  assert.throws(() => events.auth(user.id, 'wrong'), /sign in/);
  assert.equal(events.tryAuth('nobody', 'x'), null);
  assert.throws(() => events.createUser('   '), /name/);
});

test('creating an event validates input and makes the host the first attendee', () => {
  const { events, host, event, now } = setup();
  assert.equal(event.attendees.length, 1);
  assert.equal(event.hostId, host.id);
  assert.match(event.code, /^[A-Z2-9]{6}$/);
  assert.ok(event.rules.zone, 'zone defaults to the meeting point');
  assert.equal(event.rules.zone.lat, PLACE.lat);
  assert.throws(() => events.createEvent(host, { title: 'x', startsAt: now() + HOUR, place: PLACE }), /Title/);
  assert.throws(() => events.createEvent(host, { title: 'Past', startsAt: now() - 2 * HOUR, place: PLACE }), /past/);
  assert.throws(() => events.createEvent(host, { title: 'No place', startsAt: now() + HOUR, place: { lat: 999 } }), /meeting point/);
});

test('discovery lists upcoming public events, nearest filter and soonest first', () => {
  const { events, host, now } = setup();
  events.createEvent(host, { title: 'Private one', startsAt: now() + HOUR, place: PLACE, visibility: 'private' });
  events.createEvent(host, { title: 'Pune night', startsAt: now() + 2 * HOUR, place: { lat: 18.52, lng: 73.85, name: 'FC Road' } });
  const all = events.listPublic();
  assert.deepEqual(all.map((e) => e.title), ['Pune night', 'Chhupan Chhupai Night']);
  const near = events.listPublic({ lat: 18.93, lng: 72.83, radiusKm: 20 });
  assert.deepEqual(near.map((e) => e.title), ['Chhupan Chhupai Night']);
  assert.ok(near[0].distanceKm < 2);
});

test('RSVP respects capacity; the host cannot leave; cancelled events refuse RSVPs', () => {
  const { events, host, users, event } = setup();
  events.rsvp(users[0], event.id);
  events.rsvp(users[0], event.code); // idempotent, by code too
  events.rsvp(users[1], event.id);
  assert.throws(() => events.rsvp(users[2], event.id), /full/);
  events.cancelRsvp(users[1], event.id);
  events.rsvp(users[2], event.id);
  assert.equal(event.attendees.length, 3);
  assert.throws(() => events.cancelRsvp(host, event.id), /host/);
  events.cancelEvent(host, event.id);
  assert.throws(() => events.rsvp(users[1], event.id), /cancelled/);
});

test('check-in needs to be near the meeting point and within the time window', () => {
  const { events, host, users, event, tick } = setup();
  events.rsvp(users[0], event.id);
  assert.throws(() => events.checkIn(users[0], event.id, PLACE), /opens 2 hours/);
  tick(23 * HOUR);
  assert.throws(() => events.checkIn(users[0], event.id, { lat: PLACE.lat + 0.01, lng: PLACE.lng }), /m from the meeting point/);
  events.checkIn(users[0], event.id, { lat: PLACE.lat + 0.001, lng: PLACE.lng, acc: 10 });
  const d = events.detail(event, users[0]);
  assert.equal(d.me.checkedIn, true);
  assert.equal(d.checkedIn, 1);
  events.rsvp(users[1], event.id);
  assert.throws(() => events.hostCheckIn(users[0], event.id, users[1].id), /Only the host/);
  events.hostCheckIn(host, event.id, users[1].id);
  assert.equal(events.detail(event, host).checkedIn, 2);
  tick(4 * HOUR);
  assert.throws(() => events.checkIn(users[1], event.id, PLACE), /finished/);
});

test('chat is for attendees; announcements are host-only; polling with since works', () => {
  const { events, host, users, event } = setup();
  assert.throws(() => events.postMessage(users[0], event.id, 'hi'), /RSVP/);
  events.rsvp(users[0], event.id);
  const first = events.postMessage(users[0], event.id, 'Kitne baje milna hai?');
  events.announce(host, event.id, 'Meet at the big gate at 7!');
  assert.throws(() => events.announce(users[0], event.id, 'I am the host now'), /Only the host/);
  const all = events.messages(users[0], event.id);
  assert.ok(all.some((m) => m.kind === 'system'));
  const since = events.messages(users[0], event.id, first.at);
  assert.deepEqual(since.map((m) => m.kind), ['announcement']);
  assert.equal(events.detail(event, host).announcements.length, 1);
});

test('round points and the event leaderboard', () => {
  const { events, host, users, event } = setup();
  const results = {
    winner: 'hider',
    startedAt: 0,
    huntStartsAt: 60_000,
    endedAt: 20 * 60_000,
    players: [
      { userId: host.id, name: 'Riya', role: 'seeker', status: 'active', catches: 1 },
      { userId: users[0].id, name: 'Aman', role: 'hider', status: 'active', survivedMs: 20 * 60_000, rescues: 1 },
      { userId: users[1].id, name: 'Bela', role: 'hider', status: 'caught', survivedMs: 6 * 60_000 + 30_000, catches: 0 },
    ],
  };
  const scored = roundPoints(results);
  assert.deepEqual(scored.map((p) => p.points), [5, 19 + 10 + 2, 5]);
  events.recordRound(event.id, 'ABC234', results);
  events.recordRound(event.id, 'DEF567', { ...results, winner: 'seeker' });
  const board = events.detail(event, host).leaderboard;
  assert.equal(board[0].name, 'Aman');
  assert.equal(board[0].points, 62);
  assert.equal(board.find((r) => r.name === 'Riya').points, 15);
  assert.equal(board.find((r) => r.name === 'Bela').timesFound, 2);
});
