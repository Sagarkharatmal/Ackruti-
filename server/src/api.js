// REST API for accounts-lite users and community events. JSON in, JSON out.
//
// Auth: the app creates a user once (POST /api/users) and keeps the returned
// id + secret; every other call sends them as X-User-Id / X-User-Secret.

import { GameError } from './game.js';

const MAX_BODY_BYTES = 32 * 1024;

class HttpError extends Error {
  constructor(status, message) {
    super(message);
    this.status = status;
  }
}

/** Fixed-window rate limiter: at most `limit` hits per key per window. */
export function rateLimiter(limit, windowMs, now = Date.now) {
  const hits = new Map();
  return (key) => {
    const t = now();
    const entry = hits.get(key);
    if (!entry || t - entry.start > windowMs) {
      hits.set(key, { start: t, count: 1 });
      if (hits.size > 10_000) hits.clear();
      return true;
    }
    entry.count += 1;
    return entry.count <= limit;
  };
}

function readJson(req) {
  return new Promise((resolve, reject) => {
    let size = 0;
    const chunks = [];
    req.on('data', (chunk) => {
      size += chunk.length;
      if (size > MAX_BODY_BYTES) {
        reject(new HttpError(413, 'Request too large.'));
        req.destroy();
        return;
      }
      chunks.push(chunk);
    });
    req.on('end', () => {
      if (chunks.length === 0) return resolve({});
      try {
        resolve(JSON.parse(Buffer.concat(chunks).toString('utf8')));
      } catch {
        reject(new HttpError(400, 'Bad JSON.'));
      }
    });
    req.on('error', reject);
  });
}

function clientIp(req) {
  const forwarded = req.headers['x-forwarded-for'];
  if (typeof forwarded === 'string' && forwarded) return forwarded.split(',')[0].trim();
  return req.socket.remoteAddress ?? 'unknown';
}

/**
 * @param {object} deps
 * @param {import('./events.js').EventService} deps.events
 * @param {(user: object, event: object, settings: object) => string} deps.startRound creates a game, returns its code
 * @param {(code: string) => object|null} deps.roundInfo live info about a game
 */
export function createApi({ events, startRound, roundInfo, now = Date.now, log = console.log }) {
  const signupLimit = rateLimiter(30, 60 * 60_000, now);
  const createLimit = rateLimiter(20, 60 * 60_000, now);
  const chatLimit = rateLimiter(30, 60_000, now);

  const routes = [];
  const route = (method, pattern, handler, { auth = true } = {}) => {
    const keys = [];
    const regex = new RegExp(
      '^' + pattern.replace(/:(\w+)/g, (_, k) => (keys.push(k), '([^/]+)')) + '/?$',
    );
    routes.push({ method, regex, keys, handler, auth });
  };

  const detail = (event, user) => {
    const info = event.currentGameCode ? roundInfo(event.currentGameCode) : null;
    return events.detail(event, user, info);
  };

  route('POST', '/api/users', ({ body, ip }) => {
    if (!signupLimit(ip)) throw new HttpError(429, 'Too many sign-ups from this network. Try again later.');
    const { user, secret } = events.createUser(body.name);
    return { user, secret };
  }, { auth: false });

  route('GET', '/api/users/me', ({ user }) => ({ user: { id: user.id, name: user.name } }));
  route('PATCH', '/api/users/me', ({ user, body }) => ({ user: events.renameUser(user, body.name) }));

  route('GET', '/api/events', ({ query }) => ({
    events: events.listPublic({ lat: query.get('lat'), lng: query.get('lng'), radiusKm: query.get('radiusKm') }),
  }), { auth: false });

  route('GET', '/api/events/mine', ({ user }) => events.mine(user));

  route('POST', '/api/events', ({ user, body }) => {
    if (!createLimit(user.id)) throw new HttpError(429, 'You have created a lot of events. Try again later.');
    return { event: detail(events.createEvent(user, body), user) };
  });

  route('GET', '/api/events/:id', ({ user, params }) => ({ event: detail(events.find(params.id), user) }), { auth: 'optional' });

  route('PATCH', '/api/events/:id', ({ user, params, body }) => ({ event: detail(events.updateEvent(user, params.id, body), user) }));
  route('DELETE', '/api/events/:id', ({ user, params }) => ({ event: detail(events.cancelEvent(user, params.id), user) }));
  route('POST', '/api/events/:id/rsvp', ({ user, params }) => ({ event: detail(events.rsvp(user, params.id), user) }));
  route('DELETE', '/api/events/:id/rsvp', ({ user, params }) => ({ event: detail(events.cancelRsvp(user, params.id), user) }));
  route('POST', '/api/events/:id/checkin', ({ user, params, body }) => ({ event: detail(events.checkIn(user, params.id, body), user) }));
  route('POST', '/api/events/:id/checkin/:userId', ({ user, params }) => ({
    event: detail(events.hostCheckIn(user, params.id, params.userId), user),
  }));

  route('GET', '/api/events/:id/messages', ({ user, params, query }) => ({
    messages: events.messages(user, params.id, query.get('since')),
  }));
  route('POST', '/api/events/:id/messages', ({ user, params, body }) => {
    if (!chatLimit(user.id)) throw new HttpError(429, 'Slow down a little!');
    return { message: events.postMessage(user, params.id, body.text) };
  });
  route('POST', '/api/events/:id/announce', ({ user, params, body }) => ({ message: events.announce(user, params.id, body.text) }));

  route('POST', '/api/events/:id/rounds', ({ user, params, body }) => {
    const event = events.find(params.id);
    events.requireHost(event, user);
    if (event.cancelled) throw new GameError('This event was cancelled.');
    if (event.currentGameCode && roundInfo(event.currentGameCode)) {
      throw new GameError('A round is already open. Finish it first.');
    }
    const code = startRound(user, event, { ...event.rules, ...(body.settings ?? {}) });
    events.attachRound(user, event.id, code);
    return { code, event: detail(event, user) };
  });

  /** Returns true if it handled the request. */
  return async function handle(req, res) {
    const url = new URL(req.url, 'http://localhost');
    if (!url.pathname.startsWith('/api/')) return false;
    const reply = (status, data) => {
      res.writeHead(status, { 'content-type': 'application/json; charset=utf-8', 'cache-control': 'no-store' });
      res.end(JSON.stringify(data));
    };
    try {
      const match = routes
        .filter((r) => r.method === req.method)
        .map((r) => ({ r, m: r.regex.exec(url.pathname) }))
        .find(({ m }) => m);
      if (!match) throw new HttpError(404, 'Not found.');
      const params = Object.fromEntries(match.r.keys.map((k, i) => [k, decodeURIComponent(match.m[i + 1])]));
      const userId = req.headers['x-user-id'];
      const secret = req.headers['x-user-secret'];
      let user = null;
      if (match.r.auth === true) {
        try {
          user = events.auth(userId, secret);
        } catch (err) {
          throw new HttpError(401, err.message);
        }
      } else if (match.r.auth === 'optional') {
        user = events.tryAuth(userId, secret);
      }
      const body = req.method === 'GET' || req.method === 'DELETE' ? {} : await readJson(req);
      const result = await match.r.handler({ user, params, body, query: url.searchParams, ip: clientIp(req) });
      reply(200, result);
    } catch (err) {
      if (err instanceof HttpError) reply(err.status, { error: err.message });
      else if (err instanceof GameError) reply(err.message === 'Event not found.' ? 404 : 400, { error: err.message });
      else {
        log('api error', err);
        reply(500, { error: 'Something went wrong on the server.' });
      }
    }
    return true;
  };
}
