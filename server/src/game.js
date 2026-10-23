// Authoritative game rules for one Chhupan Chhupai round.
//
// The server is the referee: it decides roles, runs the clocks, takes the
// location "pings", enforces the zone and decides who won. Each player only
// ever receives the slice of state their role is allowed to see (viewFor), so a
// modified client cannot reveal seekers to hiders or hiders to seekers.

import { distanceM, isValidLatLng, simplifyTrail } from './geo.js';
import { makeId, makeToken } from './ids.js';

export const PHASE = Object.freeze({
  LOBBY: 'lobby',
  HEADSTART: 'headstart',
  HUNT: 'hunt',
  ENDED: 'ended',
});

export const ROLE = Object.freeze({ SEEKER: 'seeker', HIDER: 'hider' });
export const PREF = Object.freeze({ SEEKER: 'seeker', HIDER: 'hider', RANDOM: 'random' });
export const STATUS = Object.freeze({ ACTIVE: 'active', CAUGHT: 'caught', LEFT: 'left' });

export const MAX_PLAYERS = 30;
export const MIN_PLAYERS = 2;
const TAG_REQUEST_TTL_MS = 60_000;
const STALE_LOCATION_MS = 90_000;
const MAX_TRAIL_POINTS = 5000;
const MAX_ACCEPTED_ACCURACY_M = 40;
const MIN_TRAIL_STEP_M = 8;
const ZONE_ACCURACY_ALLOWANCE_M = 30;
const MAX_EVENTS = 40;
const PINGS_SHOWN = 5;

export class GameError extends Error {}

const clamp = (value, min, max, fallback) => {
  const n = Number(value);
  if (!Number.isFinite(n)) return fallback;
  return Math.min(max, Math.max(min, n));
};

export const DEFAULT_SETTINGS = Object.freeze({
  durationMin: 30,
  headStartSec: 180,
  pingIntervalSec: 180,
  pingOnHuntStart: true,
  zoneEnabled: true,
  zone: null, // { lat, lng, radiusM }
  shrinkEnabled: false,
  shrinkToPct: 40,
  outOfZoneGraceSec: 30,
});

/** Returns a complete, clamped settings object built from `base` + `patch`. */
export function sanitiseSettings(patch = {}, base = DEFAULT_SETTINGS) {
  const merged = { ...base, ...patch };
  const settings = {
    durationMin: Math.round(clamp(merged.durationMin, 5, 180, base.durationMin)),
    headStartSec: Math.round(clamp(merged.headStartSec, 30, 1800, base.headStartSec)),
    pingIntervalSec: Math.round(clamp(merged.pingIntervalSec, 30, 1800, base.pingIntervalSec)),
    pingOnHuntStart: Boolean(merged.pingOnHuntStart),
    zoneEnabled: Boolean(merged.zoneEnabled),
    zone: null,
    shrinkEnabled: Boolean(merged.shrinkEnabled),
    shrinkToPct: Math.round(clamp(merged.shrinkToPct, 10, 90, base.shrinkToPct)),
    outOfZoneGraceSec: Math.round(clamp(merged.outOfZoneGraceSec, 10, 120, base.outOfZoneGraceSec)),
  };
  const z = merged.zone;
  if (z && isValidLatLng(Number(z.lat), Number(z.lng))) {
    settings.zone = {
      lat: Number(z.lat),
      lng: Number(z.lng),
      radiusM: Math.round(clamp(z.radiusM, 50, 1500, 400)),
    };
  }
  return settings;
}

function cleanName(raw) {
  const name = String(raw ?? '')
    .replace(/[\u0000-\u001f\u007f]/g, '')
    .trim()
    .slice(0, 20);
  if (!name) throw new GameError('Please enter a name.');
  return name;
}

function shuffle(items, random) {
  const a = [...items];
  for (let i = a.length - 1; i > 0; i--) {
    const j = Math.floor(random() * (i + 1));
    [a[i], a[j]] = [a[j], a[i]];
  }
  return a;
}

/**
 * Decides who seeks and who hides. Explicit preferences are honoured where
 * possible; "random" players fill seeker slots up to roughly one seeker per four
 * players. There is always at least one seeker and one hider.
 */
export function assignRoles(players, random = Math.random) {
  const n = players.length;
  if (n < MIN_PLAYERS) throw new GameError(`Need at least ${MIN_PLAYERS} players.`);
  const seekers = shuffle(players.filter((p) => p.rolePref === PREF.SEEKER), random);
  const hiders = shuffle(players.filter((p) => p.rolePref === PREF.HIDER), random);
  const randoms = shuffle(players.filter((p) => p.rolePref !== PREF.SEEKER && p.rolePref !== PREF.HIDER), random);
  const targetSeekers = Math.max(1, Math.round(n / 4));
  while (randoms.length && seekers.length < targetSeekers) seekers.push(randoms.pop());
  hiders.push(...randoms);
  if (seekers.length === 0) seekers.push(hiders.splice(Math.floor(random() * hiders.length), 1)[0]);
  if (hiders.length === 0) hiders.push(seekers.splice(Math.floor(random() * seekers.length), 1)[0]);
  const roles = new Map();
  for (const p of seekers) roles.set(p.id, ROLE.SEEKER);
  for (const p of hiders) roles.set(p.id, ROLE.HIDER);
  return roles;
}

export class Game {
  /**
   * @param {object} opts
   * @param {string} opts.code six-character join code
   * @param {() => number} [opts.now] clock in epoch ms (injectable for tests)
   * @param {(playerId: string, message: object) => void} [opts.send] direct messages
   * @param {() => number} [opts.random]
   */
  constructor({ code, now = Date.now, send = () => {}, random = Math.random, settings = {} }) {
    this.code = code;
    this.now = now;
    this.send = send;
    this.random = random;
    this.createdAt = now();
    this.updatedAt = this.createdAt;
    this.settings = sanitiseSettings(settings);
    this.players = new Map();
    this.hostId = null;
    this.events = [];
    this.tagRequests = new Map();
    this.dirty = true;
    this.resetRound();
  }

  resetRound() {
    this.phase = PHASE.LOBBY;
    this.round = (this.round ?? 0) + 1;
    this.startedAt = null;
    this.huntStartsAt = null;
    this.endsAt = null;
    this.nextPingAt = null;
    this.pings = [];
    this.tagRequests.clear();
    this.results = null;
    this.endedAt = null;
    for (const p of [...this.players.values()]) {
      if (p.status === STATUS.LEFT) {
        this.players.delete(p.id);
        continue;
      }
      p.role = null;
      p.status = STATUS.ACTIVE;
      Object.assign(p, freshRoundStats());
    }
  }

  // ---------------------------------------------------------------- helpers

  touch() {
    this.dirty = true;
    this.updatedAt = this.now();
  }

  log(kind, text, extra = {}) {
    this.events.push({ at: this.now(), kind, text, ...extra });
    if (this.events.length > MAX_EVENTS) this.events.splice(0, this.events.length - MAX_EVENTS);
    this.touch();
  }

  /** Sends a short notice (phones buzz / show a toast) to the given players. */
  notify(playerIds, kind, text, extra = {}) {
    for (const id of playerIds) this.send(id, { t: 'notice', kind, text, ...extra });
  }

  allIds() {
    return [...this.players.values()].filter((p) => p.status !== STATUS.LEFT).map((p) => p.id);
  }

  player(id) {
    const p = this.players.get(id);
    if (!p || p.status === STATUS.LEFT) throw new GameError('You are not in this game.');
    return p;
  }

  requireHost(id) {
    if (id !== this.hostId) throw new GameError('Only the host can do that.');
  }

  presentPlayers() {
    return [...this.players.values()].filter((p) => p.status !== STATUS.LEFT);
  }

  activeHiders() {
    return this.presentPlayers().filter((p) => p.role === ROLE.HIDER && p.status === STATUS.ACTIVE);
  }

  activeSeekers() {
    return this.presentPlayers().filter((p) => p.role === ROLE.SEEKER);
  }

  inRound() {
    return this.phase === PHASE.HEADSTART || this.phase === PHASE.HUNT;
  }

  /** Current zone radius, taking the optional shrink into account. */
  currentRadiusM(at = this.now()) {
    const zone = this.settings.zone;
    if (!this.settings.zoneEnabled || !zone) return null;
    if (!this.settings.shrinkEnabled || (this.phase !== PHASE.HUNT && this.phase !== PHASE.ENDED)) {
      return zone.radiusM;
    }
    const total = this.endsAt - this.huntStartsAt;
    const until = Math.min(at, this.endedAt ?? this.endsAt);
    const progress = Math.min(1, Math.max(0, (until - this.huntStartsAt) / total));
    const minRadius = (zone.radiusM * this.settings.shrinkToPct) / 100;
    return Math.round(zone.radiusM - (zone.radiusM - minRadius) * progress);
  }

  isOutsideZone(loc, at = this.now()) {
    const radius = this.currentRadiusM(at);
    if (radius == null || !loc) return false;
    const zone = this.settings.zone;
    const d = distanceM(zone.lat, zone.lng, loc.lat, loc.lng);
    const allowance = Math.min(loc.acc ?? 0, ZONE_ACCURACY_ALLOWANCE_M);
    return d - allowance > radius;
  }

  // ---------------------------------------------------------------- lobby

  addPlayer(rawName) {
    if (this.phase !== PHASE.LOBBY) throw new GameError('This game has already started.');
    if (this.presentPlayers().length >= MAX_PLAYERS) throw new GameError('This game is full.');
    const name = cleanName(rawName);
    if (this.presentPlayers().some((p) => p.name.toLowerCase() === name.toLowerCase())) {
      throw new GameError('Someone in this game already has that name.');
    }
    const player = {
      id: makeId(),
      token: makeToken(),
      name,
      rolePref: PREF.RANDOM,
      role: null,
      status: STATUS.ACTIVE,
      connected: true,
      joinedAt: this.now(),
      ...freshRoundStats(),
    };
    this.players.set(player.id, player);
    if (!this.hostId) this.hostId = player.id;
    this.log('join', `${name} joined`);
    return player;
  }

  authenticate(playerId, token) {
    const p = this.players.get(playerId);
    if (!p || p.token !== token || p.status === STATUS.LEFT) {
      throw new GameError('Could not rejoin that game.');
    }
    return p;
  }

  setConnected(playerId, connected) {
    const p = this.players.get(playerId);
    if (!p || p.connected === connected) return;
    p.connected = connected;
    this.touch();
  }

  setRolePref(playerId, pref) {
    const p = this.player(playerId);
    if (this.phase !== PHASE.LOBBY) throw new GameError('Roles are locked once the game starts.');
    if (!Object.values(PREF).includes(pref)) throw new GameError('Unknown role.');
    p.rolePref = pref;
    this.touch();
  }

  updateSettings(playerId, patch) {
    this.requireHost(playerId);
    if (this.phase !== PHASE.LOBBY) throw new GameError('Settings are locked once the game starts.');
    this.settings = sanitiseSettings(patch ?? {}, this.settings);
    this.touch();
  }

  kick(playerId, targetId) {
    this.requireHost(playerId);
    if (this.phase !== PHASE.LOBBY) throw new GameError('You can only remove players in the lobby.');
    if (targetId === playerId) throw new GameError('You cannot remove yourself.');
    const target = this.player(targetId);
    this.send(target.id, { t: 'kicked', text: 'The host removed you from the game.' });
    this.removePlayer(target.id, `${target.name} was removed by the host`);
  }

  /** Permanently removes a player (they chose to leave or were kicked). */
  removePlayer(playerId, message) {
    const p = this.players.get(playerId);
    if (!p || p.status === STATUS.LEFT) return;
    if (this.phase === PHASE.LOBBY) {
      this.players.delete(playerId);
    } else {
      p.status = STATUS.LEFT;
      p.connected = false;
      if (p.role === ROLE.HIDER && p.endedAt == null) p.endedAt = this.now();
    }
    for (const [id, req] of this.tagRequests) {
      if (req.fromId === playerId || req.toId === playerId) this.tagRequests.delete(id);
    }
    this.log('leave', message ?? `${p.name} left`);
    if (this.hostId === playerId) {
      const next = this.presentPlayers().sort((a, b) => a.joinedAt - b.joinedAt)[0];
      this.hostId = next ? next.id : null;
      if (next) this.log('host', `${next.name} is now the host`);
    }
    if (this.inRound()) this.checkForWinner();
  }

  // ---------------------------------------------------------------- round

  start(playerId) {
    this.requireHost(playerId);
    if (this.phase !== PHASE.LOBBY) throw new GameError('The game is already running.');
    const players = this.presentPlayers();
    if (players.length < MIN_PLAYERS) throw new GameError(`Need at least ${MIN_PLAYERS} players to start.`);
    if (this.settings.zoneEnabled && !this.settings.zone) {
      throw new GameError('Set the play zone on the map first (or switch the zone off).');
    }
    const roles = assignRoles(players, this.random);
    for (const p of players) {
      p.role = roles.get(p.id);
      p.status = STATUS.ACTIVE;
      Object.assign(p, freshRoundStats());
    }
    const now = this.now();
    this.phase = PHASE.HEADSTART;
    this.startedAt = now;
    this.huntStartsAt = now + this.settings.headStartSec * 1000;
    this.endsAt = this.huntStartsAt + this.settings.durationMin * 60_000;
    this.nextPingAt = this.settings.pingOnHuntStart
      ? this.huntStartsAt
      : this.huntStartsAt + this.settings.pingIntervalSec * 1000;
    this.pings = [];
    this.results = null;
    const seekers = players.filter((p) => p.role === ROLE.SEEKER);
    const hiders = players.filter((p) => p.role === ROLE.HIDER);
    this.log('start', `Round started: ${seekers.length} seeker(s), ${hiders.length} hider(s). Hiders, go hide!`);
    this.notify(hiders.map((p) => p.id), 'role', 'You are a HIDER. Go hide!');
    this.notify(seekers.map((p) => p.id), 'role', 'You are a SEEKER. Wait for the head start to finish.');
  }

  updateLocation(playerId, raw) {
    const p = this.player(playerId);
    const lat = Number(raw?.lat);
    const lng = Number(raw?.lng);
    if (!isValidLatLng(lat, lng)) throw new GameError('Invalid location.');
    const acc = clamp(raw?.acc, 0, 10_000, 50);
    const at = this.now();
    p.loc = { lat, lng, acc, at };
    if (this.inRound()) {
      this.recordTrail(p, p.loc);
      this.updateZoneStatus(p, at);
    }
    this.touch();
  }

  recordTrail(p, loc) {
    if (loc.acc > MAX_ACCEPTED_ACCURACY_M) return;
    if (p.role === ROLE.HIDER && p.status !== STATUS.ACTIVE) return;
    const last = p.trail[p.trail.length - 1];
    if (!last) {
      p.trail.push([loc.lat, loc.lng, loc.at]);
      return;
    }
    const step = distanceM(last[0], last[1], loc.lat, loc.lng);
    if (step < MIN_TRAIL_STEP_M) return;
    p.distanceM += step;
    if (p.trail.length < MAX_TRAIL_POINTS) p.trail.push([loc.lat, loc.lng, loc.at]);
  }

  updateZoneStatus(p, at) {
    if (p.role !== ROLE.HIDER || p.status !== STATUS.ACTIVE) {
      p.outSince = null;
      p.revealed = false;
      return;
    }
    const outside = this.isOutsideZone(p.loc, at);
    if (!outside) {
      if (p.outSince != null) {
        if (p.revealed) this.log('zone', `${p.name} is back inside the zone`);
        p.outSince = null;
        p.revealed = false;
        this.send(p.id, { t: 'notice', kind: 'zone_ok', text: 'You are back inside the zone.' });
      }
      return;
    }
    if (p.outSince == null) {
      p.outSince = at;
      const grace = this.settings.outOfZoneGraceSec;
      this.send(p.id, {
        t: 'notice',
        kind: 'zone_warning',
        text: `You left the zone! Get back within ${grace}s or seekers will see you live.`,
      });
    }
    const graceOver = at - p.outSince >= this.settings.outOfZoneGraceSec * 1000;
    if (this.phase === PHASE.HUNT && graceOver && !p.revealed) {
      p.revealed = true;
      this.log('revealed', `${p.name} is outside the zone and now visible live!`, { playerId: p.id });
      this.send(p.id, { t: 'notice', kind: 'revealed', text: 'Seekers can see you live until you return to the zone!' });
      this.notify(this.activeSeekers().map((s) => s.id), 'revealed', `${p.name} left the zone — tracking live!`);
    }
  }

  takePing(at) {
    const seq = this.pings.length + 1;
    const marks = this.activeHiders().map((h) => {
      if (!h.loc) return { playerId: h.id, lat: null, lng: null, acc: null, locAt: null, stale: true };
      return {
        playerId: h.id,
        lat: h.loc.lat,
        lng: h.loc.lng,
        acc: h.loc.acc,
        locAt: h.loc.at,
        stale: at - h.loc.at > STALE_LOCATION_MS,
      };
    });
    this.pings.push({ seq, at, marks });
    this.log('ping', `Ping #${seq}! Seekers can see where every hider was.`);
    this.notify(this.activeSeekers().map((s) => s.id), 'ping', `Ping #${seq}: hider locations updated.`);
    this.notify(this.activeHiders().map((h) => h.id), 'pinged', `Ping #${seq}: seekers just saw where you are. Move!`);
  }

  requestTag(seekerId, hiderId) {
    const seeker = this.player(seekerId);
    if (this.phase !== PHASE.HUNT) throw new GameError('You can only tag during the hunt.');
    if (seeker.role !== ROLE.SEEKER) throw new GameError('Only seekers can tag.');
    const hider = this.player(hiderId);
    if (hider.role !== ROLE.HIDER || hider.status !== STATUS.ACTIVE) throw new GameError('That player is not hiding.');
    for (const [id, req] of this.tagRequests) {
      if (req.fromId === seekerId && req.toId === hiderId) this.tagRequests.delete(id);
    }
    const at = this.now();
    const req = { id: makeId(), fromId: seekerId, toId: hiderId, at, expiresAt: at + TAG_REQUEST_TTL_MS };
    this.tagRequests.set(req.id, req);
    this.send(hider.id, {
      t: 'tagRequest',
      requestId: req.id,
      fromId: seeker.id,
      fromName: seeker.name,
      expiresAt: req.expiresAt,
    });
    this.touch();
    return req;
  }

  respondTag(hiderId, requestId, accept) {
    const req = this.tagRequests.get(requestId);
    if (!req || req.toId !== hiderId) throw new GameError('That tag request has expired.');
    this.tagRequests.delete(requestId);
    const seeker = this.players.get(req.fromId);
    const hider = this.player(hiderId);
    if (accept) {
      this.markCaught(hider, seeker);
    } else {
      this.send(req.fromId, { t: 'notice', kind: 'tag_denied', text: `${hider.name} says you didn't tag them.` });
      this.touch();
    }
  }

  /** The hider says they were caught (the honest in-person tag). */
  reportCaught(hiderId, byId) {
    const hider = this.player(hiderId);
    if (!this.inRound()) throw new GameError('The round is not running.');
    if (hider.role !== ROLE.HIDER || hider.status !== STATUS.ACTIVE) throw new GameError('You are not hiding.');
    const by = byId ? this.players.get(byId) : null;
    this.markCaught(hider, by && by.role === ROLE.SEEKER ? by : null);
  }

  markCaught(hider, seeker) {
    if (hider.status !== STATUS.ACTIVE) return;
    hider.status = STATUS.CAUGHT;
    hider.caughtAt = this.now();
    hider.caughtBy = seeker?.id ?? null;
    hider.endedAt = hider.caughtAt;
    hider.outSince = null;
    hider.revealed = false;
    if (seeker) seeker.catches += 1;
    for (const [id, req] of this.tagRequests) if (req.toId === hider.id) this.tagRequests.delete(id);
    const text = seeker ? `${hider.name} was caught by ${seeker.name}!` : `${hider.name} was caught!`;
    this.log('caught', text, { playerId: hider.id });
    this.notify(this.allIds(), 'caught', text, { playerId: hider.id });
    this.checkForWinner();
  }

  checkForWinner() {
    if (!this.inRound()) return;
    if (this.activeHiders().length === 0) {
      this.finish(ROLE.SEEKER, 'All hiders were caught.');
    } else if (this.activeSeekers().length === 0) {
      this.finish(ROLE.HIDER, 'All seekers left the game.');
    }
  }

  endEarly(playerId) {
    this.requireHost(playerId);
    if (!this.inRound()) throw new GameError('The round is not running.');
    this.finish(null, 'The host ended the round.');
  }

  rematch(playerId) {
    this.requireHost(playerId);
    if (this.phase !== PHASE.ENDED) throw new GameError('Finish this round first.');
    this.resetRound();
    this.log('lobby', 'Back in the lobby for another round!');
  }

  finish(winner, reason) {
    const at = this.now();
    this.phase = PHASE.ENDED;
    this.endedAt = at;
    this.tagRequests.clear();
    for (const p of this.presentPlayers()) {
      if (p.role === ROLE.HIDER && p.endedAt == null) p.endedAt = at;
      p.outSince = null;
      p.revealed = false;
    }
    const hidersWon = winner === ROLE.HIDER;
    this.results = {
      winner,
      reason,
      endedAt: at,
      startedAt: this.startedAt,
      huntStartsAt: this.huntStartsAt,
      pings: this.pings.length,
      players: [...this.players.values()]
        .filter((p) => p.role)
        .map((p) => ({
          id: p.id,
          name: p.name,
          role: p.role,
          status: p.status,
          caughtAt: p.caughtAt,
          caughtBy: p.caughtBy,
          survivedMs: p.role === ROLE.HIDER ? (p.endedAt ?? at) - this.startedAt : null,
          catches: p.catches,
          distanceM: Math.round(p.distanceM),
          trail: simplifyTrail(p.trail.map(([lat, lng]) => [lat, lng])),
        })),
    };
    let text;
    if (winner === ROLE.SEEKER) text = `Seekers win! ${reason}`;
    else if (hidersWon) text = `Hiders win! ${reason}`;
    else text = `Round over. ${reason}`;
    this.log('end', text);
    this.notify(this.allIds(), 'game_over', text);
  }

  /** Advances clocks. Call roughly once a second. */
  tick() {
    const at = this.now();
    for (const [id, req] of this.tagRequests) {
      if (req.expiresAt <= at) {
        this.tagRequests.delete(id);
        this.touch();
      }
    }
    if (this.phase === PHASE.HEADSTART && at >= this.huntStartsAt) {
      this.phase = PHASE.HUNT;
      this.log('hunt', 'Head start over — seekers, go find them!');
      this.notify(this.allIds(), 'hunt', 'Head start over! The hunt is on.');
    }
    if (this.phase === PHASE.HUNT) {
      // Catch up at most one ping per tick even if the server was paused.
      if (at >= this.nextPingAt) {
        this.takePing(at);
        this.nextPingAt += this.settings.pingIntervalSec * 1000;
        if (this.nextPingAt <= at) this.nextPingAt = at + this.settings.pingIntervalSec * 1000;
      }
      for (const h of this.activeHiders()) if (h.loc) this.updateZoneStatus(h, at);
      if (at >= this.endsAt) {
        const left = this.activeHiders().length;
        this.finish(ROLE.HIDER, `Time is up — ${left} hider(s) were never found.`);
      }
    }
  }

  // ---------------------------------------------------------------- views

  /** The state one player is allowed to see. */
  viewFor(playerId) {
    const me = this.players.get(playerId);
    if (!me) return null;
    const at = this.now();
    const ended = this.phase === PHASE.ENDED;
    const spectator = me.role === ROLE.HIDER && me.status !== STATUS.ACTIVE;
    const seeker = me.role === ROLE.SEEKER;
    const seesEverything = ended || spectator;

    let pings = [];
    let live = [];
    if (this.inRound() || ended) {
      if (seesEverything || (seeker && this.phase !== PHASE.HEADSTART)) {
        pings = this.pings.slice(-PINGS_SHOWN);
      }
      for (const p of this.presentPlayers()) {
        if (p.id === me.id || !p.loc) continue;
        let kind = null;
        if (seesEverything) kind = 'spectate';
        else if (seeker && p.role === ROLE.SEEKER) kind = 'teammate';
        else if (seeker && p.role === ROLE.HIDER && p.revealed) kind = 'revealed';
        if (kind) live.push({ playerId: p.id, lat: p.loc.lat, lng: p.loc.lng, acc: p.loc.acc, at: p.loc.at, kind });
      }
    }

    const lastPing = this.pings[this.pings.length - 1];
    return {
      t: 'state',
      now: at,
      code: this.code,
      round: this.round,
      phase: this.phase,
      hostId: this.hostId,
      settings: this.settings,
      startedAt: this.startedAt,
      huntStartsAt: this.huntStartsAt,
      endsAt: this.endsAt,
      nextPingAt: this.inRound() ? this.nextPingAt : null,
      lastPingAt: lastPing?.at ?? null,
      pingCount: this.pings.length,
      zone:
        this.settings.zoneEnabled && this.settings.zone
          ? { ...this.settings.zone, currentRadiusM: this.currentRadiusM(at) }
          : null,
      hidersLeft: this.activeHiders().length,
      players: this.presentPlayers()
        .sort((a, b) => a.joinedAt - b.joinedAt)
        .map((p) => ({
          id: p.id,
          name: p.name,
          connected: p.connected,
          rolePref: p.rolePref,
          role: p.role,
          status: p.status,
          caughtAt: p.caughtAt,
          caughtBy: p.caughtBy,
        })),
      you: {
        id: me.id,
        name: me.name,
        role: me.role,
        status: me.status,
        rolePref: me.rolePref,
        isHost: me.id === this.hostId,
        spectator,
        loc: me.loc ?? null,
        outSince: me.outSince,
        revealed: me.revealed,
        catches: me.catches,
        distanceM: Math.round(me.distanceM),
        pendingTagRequests: [...this.tagRequests.values()]
          .filter((r) => r.toId === me.id)
          .map((r) => ({
            requestId: r.id,
            fromId: r.fromId,
            fromName: this.players.get(r.fromId)?.name ?? '?',
            expiresAt: r.expiresAt,
          })),
      },
      pings,
      live,
      events: this.events.slice(-15),
      results: ended ? this.results : null,
    };
  }
}

function freshRoundStats() {
  return {
    loc: null,
    trail: [],
    distanceM: 0,
    caughtAt: null,
    caughtBy: null,
    endedAt: null,
    catches: 0,
    outSince: null,
    revealed: false,
  };
}
