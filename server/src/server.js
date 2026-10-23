// HTTP + WebSocket entry point. Holds every running game in memory.
//
//   GET /         -> plain-text banner (handy to check a deployment is up)
//   GET /health   -> {"ok":true,...}
//   WS  /ws       -> game protocol (JSON messages, see docs/PROTOCOL.md)

import http from 'node:http';
import { pathToFileURL } from 'node:url';
import { WebSocketServer } from 'ws';
import { Game, GameError, PHASE } from './game.js';
import { makeGameCode, normaliseCode } from './ids.js';

const TICK_MS = 1000;
const HEARTBEAT_MS = 25_000;
const MAX_MESSAGE_BYTES = 16 * 1024;
const MAX_MESSAGES_PER_10S = 120;
const LOBBY_IDLE_MS = 45 * 60_000;
const ENDED_KEEP_MS = 60 * 60_000;
const MAX_GAME_AGE_MS = 8 * 60 * 60_000;
const MAX_GAMES = 2000;

export function createServer({ port = 8080, host = '0.0.0.0', now = Date.now, log = console.log } = {}) {
  /** @type {Map<string, Game>} */
  const games = new Map();
  /** playerId -> ws */
  const sockets = new Map();

  const send = (ws, msg) => {
    if (ws && ws.readyState === ws.OPEN) ws.send(JSON.stringify(msg));
  };
  const sendToPlayer = (playerId, msg) => send(sockets.get(playerId), msg);

  const broadcastState = (game) => {
    for (const p of game.players.values()) {
      const ws = sockets.get(p.id);
      if (ws && ws.session?.code === game.code) send(ws, game.viewFor(p.id));
    }
    game.dirty = false;
    game.lastBroadcast = now();
  };

  const newGame = (settings) => {
    if (games.size >= MAX_GAMES) throw new GameError('The server is busy, please try again later.');
    let code;
    do code = makeGameCode();
    while (games.has(code));
    const game = new Game({ code, now, send: sendToPlayer, settings });
    games.set(code, game);
    return game;
  };

  const findGame = (rawCode) => {
    const game = games.get(normaliseCode(rawCode));
    if (!game) throw new GameError('No game found with that code.');
    return game;
  };

  const bind = (ws, game, player) => {
    const previous = sockets.get(player.id);
    if (previous && previous !== ws) {
      previous.session = null;
      send(previous, { t: 'error', message: 'You connected from another device.' });
      previous.close(4000, 'replaced');
    }
    ws.session = { code: game.code, playerId: player.id };
    sockets.set(player.id, ws);
    game.setConnected(player.id, true);
    send(ws, { t: 'welcome', code: game.code, playerId: player.id, token: player.token });
    broadcastState(game);
  };

  const handlers = {
    create(ws, msg) {
      const game = newGame(msg.settings);
      const player = game.addPlayer(msg.name);
      log(`game ${game.code} created by ${player.name}`);
      bind(ws, game, player);
    },
    join(ws, msg) {
      const game = findGame(msg.code);
      const player = game.addPlayer(msg.name);
      bind(ws, game, player);
    },
    resume(ws, msg) {
      const game = findGame(msg.code);
      const player = game.authenticate(String(msg.playerId ?? ''), String(msg.token ?? ''));
      bind(ws, game, player);
    },
  };

  // Messages that need an existing session: [game, playerId, msg] => void
  const sessionHandlers = {
    setRole: (g, id, m) => g.setRolePref(id, m.pref),
    settings: (g, id, m) => g.updateSettings(id, m.settings),
    kick: (g, id, m) => {
      g.kick(id, m.playerId);
      const kicked = sockets.get(m.playerId);
      if (kicked) kicked.session = null;
      sockets.delete(m.playerId);
    },
    start: (g, id) => g.start(id),
    loc: (g, id, m) => g.updateLocation(id, m),
    tag: (g, id, m) => g.requestTag(id, m.playerId),
    tagReply: (g, id, m) => g.respondTag(id, m.requestId, Boolean(m.accept)),
    caught: (g, id, m) => g.reportCaught(id, m.byId),
    end: (g, id) => g.endEarly(id),
    rematch: (g, id) => g.rematch(id),
    leave: (g, id, m, ws) => {
      g.removePlayer(id);
      sockets.delete(id);
      ws.session = null;
      send(ws, { t: 'left' });
    },
  };

  const handleMessage = (ws, raw) => {
    if (raw.length > MAX_MESSAGE_BYTES) throw new GameError('Message too large.');
    let msg;
    try {
      msg = JSON.parse(raw.toString('utf8'));
    } catch {
      throw new GameError('Bad message.');
    }
    if (!msg || typeof msg.t !== 'string') throw new GameError('Bad message.');
    if (msg.t === 'hello') return send(ws, { t: 'hello', now: now() });
    if (handlers[msg.t]) {
      if (ws.session) throw new GameError('Leave your current game first.');
      return handlers[msg.t](ws, msg);
    }
    const handler = sessionHandlers[msg.t];
    if (!handler) throw new GameError(`Unknown message "${msg.t}".`);
    const session = ws.session;
    const game = session && games.get(session.code);
    if (!game) throw new GameError('You are not in a game.');
    handler(game, session.playerId, msg, ws);
    if (msg.t !== 'loc') broadcastState(game);
    if (game.presentPlayers().length === 0) games.delete(game.code);
  };

  const httpServer = http.createServer((req, res) => {
    if (req.url === '/health') {
      res.writeHead(200, { 'content-type': 'application/json' });
      res.end(JSON.stringify({ ok: true, games: games.size, players: sockets.size, time: now() }));
      return;
    }
    res.writeHead(200, { 'content-type': 'text/plain; charset=utf-8' });
    res.end('Chhupan Chhupai game server is running. Connect the app to ws(s)://<this-host>/ws\n');
  });

  const wss = new WebSocketServer({ server: httpServer, maxPayload: MAX_MESSAGE_BYTES });

  wss.on('connection', (ws) => {
    ws.isAlive = true;
    ws.session = null;
    ws.rate = { windowStart: now(), count: 0 };
    ws.on('pong', () => {
      ws.isAlive = true;
    });
    ws.on('message', (raw) => {
      const t = now();
      if (t - ws.rate.windowStart > 10_000) ws.rate = { windowStart: t, count: 0 };
      if (++ws.rate.count > MAX_MESSAGES_PER_10S) {
        send(ws, { t: 'error', message: 'Slow down.' });
        return;
      }
      try {
        handleMessage(ws, raw);
      } catch (err) {
        if (err instanceof GameError) send(ws, { t: 'error', message: err.message });
        else {
          log('unexpected error', err);
          send(ws, { t: 'error', message: 'Something went wrong on the server.' });
        }
      }
    });
    ws.on('close', () => {
      const session = ws.session;
      if (!session) return;
      if (sockets.get(session.playerId) === ws) sockets.delete(session.playerId);
      const game = games.get(session.code);
      if (game) game.setConnected(session.playerId, false);
    });
  });

  const tick = setInterval(() => {
    const t = now();
    for (const [code, game] of games) {
      game.tick();
      const connected = [...game.players.values()].some((p) => p.connected);
      const idle = t - game.updatedAt;
      const expired =
        t - game.createdAt > MAX_GAME_AGE_MS ||
        (game.phase === PHASE.ENDED && idle > ENDED_KEEP_MS) ||
        (!connected && idle > LOBBY_IDLE_MS);
      if (expired) {
        games.delete(code);
        continue;
      }
      if (game.dirty || t - (game.lastBroadcast ?? 0) >= 5000) broadcastState(game);
    }
  }, TICK_MS);

  const heartbeat = setInterval(() => {
    for (const ws of wss.clients) {
      if (!ws.isAlive) {
        ws.terminate();
        continue;
      }
      ws.isAlive = false;
      ws.ping();
    }
  }, HEARTBEAT_MS);

  return new Promise((resolve) => {
    httpServer.listen(port, host, () => {
      const address = httpServer.address();
      log(`Chhupan Chhupai server listening on ${address.address}:${address.port}`);
      resolve({
        port: address.port,
        games,
        close: () =>
          new Promise((done) => {
            clearInterval(tick);
            clearInterval(heartbeat);
            for (const ws of wss.clients) ws.terminate();
            wss.close();
            httpServer.close(() => done());
          }),
      });
    });
  });
}

if (import.meta.url === pathToFileURL(process.argv[1] ?? '').href) {
  createServer({ port: Number(process.env.PORT) || 8080 });
}
