// Community events: a host announces a "Chhupan Chhupai Night" at a meeting
// point, people RSVP, chat, check in on arrival, and the host runs several
// rounds that feed one leaderboard for the night.

import { createHash, timingSafeEqual } from 'node:crypto';
import { distanceM, isValidLatLng } from './geo.js';
import { makeGameCode, makeId, makeToken, normaliseCode } from './ids.js';
import { GameError, sanitiseSettings } from './game.js';

export const CHECKIN_RADIUS_M = 300;
export const CHECKIN_OPENS_BEFORE_MS = 2 * 60 * 60_000;
const MAX_CHAT = 500;
const MAX_ANNOUNCEMENTS = 20;
const LIST_GRACE_MS = 6 * 60 * 60_000; // keep showing events for 6 h after they start

const hashSecret = (secret) => createHash('sha256').update(String(secret)).digest();

function text(raw, { min = 0, max, field }) {
  const value = String(raw ?? '')
    .replace(/[\u0000-\u0008\u000b-\u001f\u007f]/g, '')
    .trim();
  if (value.length < min) throw new GameError(min === 1 ? `Please enter ${field}.` : `${field} is too short.`);
  if (value.length > max) throw new GameError(`${field} is too long (max ${max} characters).`);
  return value;
}

/**
 * Points for one round:
 *  - hiders: 1 per full minute survived after the head start, +10 if never found
 *  - seekers: 5 per catch, +5 if the seekers won
 *  - everyone: +2 per "Dhappa" rescue of team-mates
 */
export function roundPoints(results) {
  const huntOffset = (results.huntStartsAt ?? results.startedAt ?? 0) - (results.startedAt ?? 0);
  return results.players.map((p) => {
    let points = 0;
    if (p.role === 'hider') {
      points += Math.floor(Math.max(0, (p.survivedMs ?? 0) - huntOffset) / 60_000);
      if (p.status === 'active') points += 10;
    } else if (p.role === 'seeker') {
      points += 5 * (p.catches ?? 0);
      if (results.winner === 'seeker') points += 5;
    }
    points += 2 * (p.rescues ?? 0);
    return { ...p, points };
  });
}

export class EventService {
  /**
   * @param {import('./store.js').MemoryStore} store
   * @param {{ now?: () => number }} [opts]
   */
  constructor(store, { now = Date.now } = {}) {
    this.store = store;
    this.now = now;
  }

  // ------------------------------------------------------------- users

  createUser(rawName) {
    const name = text(rawName, { min: 1, max: 20, field: 'a name' });
    const secret = makeToken();
    const user = { id: makeId(), name, secretHash: hashSecret(secret).toString('hex'), createdAt: this.now() };
    this.store.put('users', user.id, user);
    return { user: publicUser(user), secret };
  }

  /** Returns the user for valid credentials, otherwise throws. */
  auth(userId, secret) {
    const user = userId ? this.store.get('users', String(userId)) : null;
    if (!user || !secret) throw new GameError('Please sign in again.');
    const expected = Buffer.from(user.secretHash, 'hex');
    const given = hashSecret(secret);
    if (expected.length !== given.length || !timingSafeEqual(expected, given)) {
      throw new GameError('Please sign in again.');
    }
    return user;
  }

  /** Like auth() but returns null instead of throwing (for optional sign-in). */
  tryAuth(userId, secret) {
    try {
      return this.auth(userId, secret);
    } catch {
      return null;
    }
  }

  renameUser(user, rawName) {
    user.name = text(rawName, { min: 1, max: 20, field: 'a name' });
    this.store.put('users', user.id, user);
    return publicUser(user);
  }

  // ------------------------------------------------------------- events

  validateEvent(input, existing = null) {
    const now = this.now();
    const out = {};
    const has = (k) => input[k] !== undefined;
    if (!existing || has('title')) out.title = text(input.title, { min: 3, max: 60, field: 'Title' });
    if (!existing || has('description')) out.description = text(input.description ?? '', { max: 1000, field: 'Description' });
    if (!existing || has('community')) out.community = text(input.community ?? '', { max: 40, field: 'Community name' });
    if (!existing || has('startsAt')) {
      const startsAt = Number(input.startsAt);
      if (!Number.isFinite(startsAt)) throw new GameError('Pick a start date and time.');
      if (startsAt < now - 60 * 60_000) throw new GameError('The start time is in the past.');
      if (startsAt > now + 366 * 24 * 60 * 60_000) throw new GameError('Events can be at most a year ahead.');
      out.startsAt = Math.round(startsAt);
    }
    if (!existing || has('durationMin')) {
      const d = Math.round(Number(input.durationMin ?? 120));
      if (!(d >= 30 && d <= 12 * 60)) throw new GameError('Event length must be between 30 minutes and 12 hours.');
      out.durationMin = d;
    }
    if (!existing || has('place')) {
      const p = input.place ?? {};
      const lat = Number(p.lat);
      const lng = Number(p.lng);
      if (!isValidLatLng(lat, lng)) throw new GameError('Pick the meeting point on the map.');
      out.place = { lat, lng, name: text(p.name ?? '', { min: 1, max: 80, field: 'a meeting point name' }) };
    }
    if (!existing || has('capacity')) {
      const c = Math.round(Number(input.capacity ?? 30));
      if (!(c >= 2 && c <= 500)) throw new GameError('Capacity must be between 2 and 500.');
      if (existing && c < existing.attendees.length) throw new GameError('More people have already RSVPed than that.');
      out.capacity = c;
    }
    if (!existing || has('visibility')) {
      out.visibility = input.visibility === 'private' ? 'private' : 'public';
    }
    if (!existing || has('sponsor')) {
      const s = input.sponsor;
      out.sponsor = s && String(s.name ?? '').trim() ? { name: text(s.name, { max: 40, field: 'Sponsor name' }) } : null;
    }
    if (!existing || has('rules')) {
      out.rules = sanitiseSettings(input.rules ?? {});
    }
    return out;
  }

  createEvent(user, input) {
    const fields = this.validateEvent(input);
    const event = {
      id: makeId(),
      code: this.freshCode(),
      hostId: user.id,
      createdAt: this.now(),
      cancelled: false,
      attendees: [{ userId: user.id, rsvpAt: this.now(), checkedInAt: null }],
      announcements: [],
      rounds: [],
      currentGameCode: null,
      ...fields,
    };
    if (event.rules.zoneEnabled && !event.rules.zone) {
      event.rules.zone = { lat: event.place.lat, lng: event.place.lng, radiusM: 400 };
    }
    this.store.put('events', event.id, event);
    this.store.put('chat', event.id, { eventId: event.id, messages: [] });
    this.systemMessage(event, `${user.name} created this event.`);
    return event;
  }

  freshCode() {
    const taken = new Set(this.store.all('events').map((e) => e.code));
    let code;
    do code = makeGameCode();
    while (taken.has(code));
    return code;
  }

  find(idOrCode) {
    const key = String(idOrCode ?? '');
    const byId = this.store.get('events', key);
    if (byId) return byId;
    const code = normaliseCode(key);
    const event = code.length === 6 ? this.store.all('events').find((e) => e.code === code) : null;
    if (!event) throw new GameError('Event not found.');
    return event;
  }

  requireHost(event, user) {
    if (event.hostId !== user.id) throw new GameError('Only the host can do that.');
  }

  endsAt(event) {
    return event.startsAt + event.durationMin * 60_000;
  }

  isOver(event) {
    return this.now() > this.endsAt(event);
  }

  updateEvent(user, idOrCode, input) {
    const event = this.find(idOrCode);
    this.requireHost(event, user);
    if (event.cancelled) throw new GameError('This event was cancelled.');
    Object.assign(event, this.validateEvent(input, event));
    this.save(event);
    this.systemMessage(event, 'The host updated the event details.');
    return event;
  }

  cancelEvent(user, idOrCode) {
    const event = this.find(idOrCode);
    this.requireHost(event, user);
    event.cancelled = true;
    this.save(event);
    this.systemMessage(event, 'The host cancelled this event.');
    return event;
  }

  /** Upcoming public events, soonest first, optionally near a point. */
  listPublic({ lat, lng, radiusKm } = {}) {
    const now = this.now();
    const near = isValidLatLng(Number(lat), Number(lng));
    return this.store
      .all('events')
      .filter((e) => e.visibility === 'public' && !e.cancelled && e.startsAt > now - LIST_GRACE_MS && !this.isOver(e))
      .map((e) => ({ e, km: near ? distanceM(Number(lat), Number(lng), e.place.lat, e.place.lng) / 1000 : null }))
      .filter(({ km }) => km == null || !Number.isFinite(Number(radiusKm)) || km <= Number(radiusKm))
      .sort((a, b) => a.e.startsAt - b.e.startsAt)
      .slice(0, 100)
      .map(({ e, km }) => this.summary(e, km));
  }

  mine(user) {
    const all = this.store.all('events').filter((e) => e.attendees.some((a) => a.userId === user.id));
    const sorted = all.sort((a, b) => b.startsAt - a.startsAt).slice(0, 100);
    return {
      hosting: sorted.filter((e) => e.hostId === user.id).map((e) => this.summary(e)),
      attending: sorted.filter((e) => e.hostId !== user.id).map((e) => this.summary(e)),
    };
  }

  rsvp(user, idOrCode) {
    const event = this.find(idOrCode);
    if (event.cancelled) throw new GameError('This event was cancelled.');
    if (this.isOver(event)) throw new GameError('This event has finished.');
    if (event.attendees.some((a) => a.userId === user.id)) return event;
    if (event.attendees.length >= event.capacity) throw new GameError('Sorry, this event is full.');
    event.attendees.push({ userId: user.id, rsvpAt: this.now(), checkedInAt: null });
    this.save(event);
    this.systemMessage(event, `${user.name} is coming!`);
    return event;
  }

  cancelRsvp(user, idOrCode) {
    const event = this.find(idOrCode);
    if (event.hostId === user.id) throw new GameError('The host cannot leave their own event. Cancel it instead.');
    const before = event.attendees.length;
    event.attendees = event.attendees.filter((a) => a.userId !== user.id);
    if (event.attendees.length !== before) {
      this.save(event);
      this.systemMessage(event, `${user.name} can't make it any more.`);
    }
    return event;
  }

  /** Self check-in: must be at the meeting point, from 2 h before the start until the end. */
  checkIn(user, idOrCode, loc) {
    const event = this.find(idOrCode);
    if (event.cancelled) throw new GameError('This event was cancelled.');
    const now = this.now();
    if (now < event.startsAt - CHECKIN_OPENS_BEFORE_MS) throw new GameError('Check-in opens 2 hours before the start.');
    if (this.isOver(event)) throw new GameError('This event has finished.');
    const lat = Number(loc?.lat);
    const lng = Number(loc?.lng);
    if (!isValidLatLng(lat, lng)) throw new GameError('We need your location to check you in.');
    const d = distanceM(lat, lng, event.place.lat, event.place.lng);
    const allowance = Math.min(Number(loc?.acc) || 0, 100);
    if (d - allowance > CHECKIN_RADIUS_M) {
      throw new GameError(`You're ${Math.round(d)} m from the meeting point. Check in when you get there (within ${CHECKIN_RADIUS_M} m).`);
    }
    let attendee = event.attendees.find((a) => a.userId === user.id);
    if (!attendee) {
      this.rsvp(user, event.id);
      attendee = event.attendees.find((a) => a.userId === user.id);
    }
    if (!attendee.checkedInAt) {
      attendee.checkedInAt = now;
      this.save(event);
      this.systemMessage(event, `${user.name} has arrived 📍`);
    }
    return event;
  }

  /** The host checks someone in by hand (no GPS, e.g. a phone without signal). */
  hostCheckIn(host, idOrCode, userId) {
    const event = this.find(idOrCode);
    this.requireHost(event, host);
    const attendee = event.attendees.find((a) => a.userId === userId);
    if (!attendee) throw new GameError('That person has not RSVPed.');
    if (!attendee.checkedInAt) {
      attendee.checkedInAt = this.now();
      this.save(event);
      this.systemMessage(event, `${this.userName(userId)} has arrived 📍`);
    }
    return event;
  }

  // ------------------------------------------------------------- chat

  requireMember(event, user) {
    if (!event.attendees.some((a) => a.userId === user.id)) {
      throw new GameError('RSVP to this event to see and send messages.');
    }
  }

  postMessage(user, idOrCode, rawText) {
    const event = this.find(idOrCode);
    this.requireMember(event, user);
    const body = text(rawText, { min: 1, max: 500, field: 'a message' });
    return this.appendMessage(event, { userId: user.id, name: user.name, text: body, kind: 'chat' });
  }

  announce(user, idOrCode, rawText) {
    const event = this.find(idOrCode);
    this.requireHost(event, user);
    const body = text(rawText, { min: 1, max: 300, field: 'an announcement' });
    event.announcements.push({ at: this.now(), text: body });
    if (event.announcements.length > MAX_ANNOUNCEMENTS) event.announcements.shift();
    this.save(event);
    return this.appendMessage(event, { userId: user.id, name: user.name, text: body, kind: 'announcement' });
  }

  systemMessage(event, body) {
    return this.appendMessage(event, { userId: null, name: 'Chhupan Chhupai', text: body, kind: 'system' });
  }

  appendMessage(event, msg) {
    const chat = this.store.get('chat', event.id) ?? { eventId: event.id, messages: [] };
    const last = chat.messages[chat.messages.length - 1];
    // Strictly increasing timestamps make "messages since" polling exact.
    const at = Math.max(this.now(), (last?.at ?? 0) + 1);
    const message = { id: makeId(), at, ...msg };
    chat.messages.push(message);
    if (chat.messages.length > MAX_CHAT) chat.messages.splice(0, chat.messages.length - MAX_CHAT);
    this.store.put('chat', event.id, chat);
    return message;
  }

  messages(user, idOrCode, since = 0) {
    const event = this.find(idOrCode);
    this.requireMember(event, user);
    const chat = this.store.get('chat', event.id);
    return (chat?.messages ?? []).filter((m) => m.at > Number(since || 0));
  }

  // ------------------------------------------------------------- rounds

  /** Called by the server after it created a game for this event. */
  attachRound(user, idOrCode, gameCode) {
    const event = this.find(idOrCode);
    this.requireHost(event, user);
    event.currentGameCode = gameCode;
    this.save(event);
    this.systemMessage(event, `Round ${event.rounds.length + 1} is open! Tap "Join round" — code ${gameCode}.`);
    return event;
  }

  /** Called when a game linked to this event finishes. */
  recordRound(eventId, gameCode, results) {
    const event = this.store.get('events', eventId);
    if (!event) return null;
    const scored = roundPoints(results);
    event.rounds.push({
      gameCode,
      endedAt: results.endedAt,
      winner: results.winner,
      reason: results.reason,
      players: scored.map((p) => ({
        userId: p.userId ?? null,
        name: p.name,
        role: p.role,
        status: p.status,
        points: p.points,
        catches: p.catches ?? 0,
        survivedMs: p.survivedMs,
        distanceM: p.distanceM,
      })),
    });
    if (event.currentGameCode === gameCode) event.currentGameCode = null;
    this.save(event);
    const best = [...scored].sort((a, b) => b.points - a.points)[0];
    const winnerText = results.winner === 'seeker' ? 'Seekers won' : results.winner === 'hider' ? 'Hiders won' : 'Round ended';
    this.systemMessage(event, `Round ${event.rounds.length}: ${winnerText}.${best ? ` Top scorer: ${best.name} (${best.points} pts).` : ''}`);
    return event;
  }

  roundEnded(eventId, gameCode) {
    const event = this.store.get('events', eventId);
    if (event && event.currentGameCode === gameCode) {
      event.currentGameCode = null;
      this.save(event);
    }
  }

  leaderboard(event) {
    const rows = new Map();
    for (const round of event.rounds) {
      for (const p of round.players) {
        const key = p.userId ?? `name:${p.name}`;
        const row = rows.get(key) ?? { userId: p.userId, name: p.name, points: 0, rounds: 0, catches: 0, timesFound: 0, survivedMs: 0 };
        row.name = p.name;
        row.points += p.points;
        row.rounds += 1;
        row.catches += p.catches ?? 0;
        if (p.role === 'hider' && p.status === 'caught') row.timesFound += 1;
        if (p.role === 'hider') row.survivedMs += p.survivedMs ?? 0;
        rows.set(key, row);
      }
    }
    return [...rows.values()].sort((a, b) => b.points - a.points || b.catches - a.catches || a.name.localeCompare(b.name));
  }

  // ------------------------------------------------------------- views

  userName(userId) {
    return this.store.get('users', userId)?.name ?? 'Someone';
  }

  status(event) {
    if (event.cancelled) return 'cancelled';
    const now = this.now();
    if (now > this.endsAt(event)) return 'ended';
    if (now >= event.startsAt) return 'live';
    return 'upcoming';
  }

  summary(event, distanceKm = null) {
    return {
      id: event.id,
      code: event.visibility === 'public' ? event.code : undefined,
      title: event.title,
      community: event.community,
      hostName: this.userName(event.hostId),
      startsAt: event.startsAt,
      endsAt: this.endsAt(event),
      place: event.place,
      capacity: event.capacity,
      going: event.attendees.length,
      checkedIn: event.attendees.filter((a) => a.checkedInAt).length,
      sponsor: event.sponsor,
      visibility: event.visibility,
      status: this.status(event),
      distanceKm: distanceKm == null ? null : Math.round(distanceKm * 10) / 10,
    };
  }

  /** Full event page. `user` may be null (not signed in). */
  detail(event, user, liveRound = null) {
    const me = user ? event.attendees.find((a) => a.userId === user.id) : null;
    const isHost = Boolean(user && user.id === event.hostId);
    return {
      ...this.summary(event),
      code: event.code,
      description: event.description,
      durationMin: event.durationMin,
      rules: event.rules,
      announcements: event.announcements,
      attendees: event.attendees.map((a) => ({
        userId: a.userId,
        name: this.userName(a.userId),
        isHost: a.userId === event.hostId,
        checkedIn: Boolean(a.checkedInAt),
      })),
      me: {
        isHost,
        going: Boolean(me),
        checkedIn: Boolean(me?.checkedInAt),
      },
      currentRound: event.currentGameCode ? { code: event.currentGameCode, ...(liveRound ?? {}) } : null,
      rounds: event.rounds.map((r, i) => ({ number: i + 1, endedAt: r.endedAt, winner: r.winner, players: r.players.length })),
      leaderboard: this.leaderboard(event),
    };
  }

  save(event) {
    this.store.put('events', event.id, event);
  }
}

function publicUser(user) {
  return { id: user.id, name: user.name };
}
