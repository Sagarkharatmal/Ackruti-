// Durable storage for things that must outlive a single game: users, events,
// event chat. Every backend keeps a full copy in memory (reads are instant and
// synchronous) and writes changes through to its medium.
//
//   MemoryStore - tests / throwaway servers
//   FileStore   - one JSON file (good for a laptop or a VPS with a disk)
//   PgStore     - PostgreSQL via DATABASE_URL (good for free hosts whose disk
//                 is wiped on restart, e.g. Render free + Neon free Postgres)

import fs from 'node:fs/promises';
import path from 'node:path';

export class MemoryStore {
  constructor() {
    /** @type {Map<string, Map<string, object>>} */
    this.collections = new Map();
  }

  async init() {}

  col(name) {
    let c = this.collections.get(name);
    if (!c) {
      c = new Map();
      this.collections.set(name, c);
    }
    return c;
  }

  get(collection, id) {
    return this.col(collection).get(id) ?? null;
  }

  all(collection) {
    return [...this.col(collection).values()];
  }

  put(collection, id, doc) {
    this.col(collection).set(id, doc);
    this.persist(collection, id, doc);
    return doc;
  }

  delete(collection, id) {
    this.col(collection).delete(id);
    this.persist(collection, id, null);
  }

  // Backends override these.
  persist() {}
  async flush() {}
  async close() {}
}

export class FileStore extends MemoryStore {
  constructor(file, { debounceMs = 500, log = console.log } = {}) {
    super();
    this.file = file;
    this.debounceMs = debounceMs;
    this.log = log;
    this.timer = null;
    this.writing = Promise.resolve();
  }

  async init() {
    try {
      const data = JSON.parse(await fs.readFile(this.file, 'utf8'));
      for (const [name, docs] of Object.entries(data)) {
        const c = this.col(name);
        for (const [id, doc] of Object.entries(docs)) c.set(id, doc);
      }
      this.log(`loaded data from ${this.file}`);
    } catch (err) {
      if (err.code !== 'ENOENT') throw err;
    }
  }

  persist() {
    if (this.timer) return;
    this.timer = setTimeout(() => {
      this.timer = null;
      this.writing = this.writing.then(() => this.writeNow()).catch((err) => this.log('store write failed', err));
    }, this.debounceMs);
  }

  async writeNow() {
    const data = {};
    for (const [name, c] of this.collections) data[name] = Object.fromEntries(c);
    await fs.mkdir(path.dirname(this.file), { recursive: true });
    const tmp = `${this.file}.tmp`;
    await fs.writeFile(tmp, JSON.stringify(data));
    await fs.rename(tmp, this.file);
  }

  async flush() {
    if (this.timer) {
      clearTimeout(this.timer);
      this.timer = null;
      this.writing = this.writing.then(() => this.writeNow());
    }
    await this.writing;
  }

  async close() {
    await this.flush();
  }
}

export class PgStore extends MemoryStore {
  /** @param {{ query: Function, end?: Function }} pool a `pg` Pool (or compatible) */
  constructor(pool, { log = console.log } = {}) {
    super();
    this.pool = pool;
    this.log = log;
    this.pending = Promise.resolve();
  }

  async init() {
    await this.pool.query(
      `CREATE TABLE IF NOT EXISTS cc_docs (
         collection TEXT NOT NULL,
         id TEXT NOT NULL,
         doc JSONB NOT NULL,
         updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
         PRIMARY KEY (collection, id)
       )`,
    );
    const { rows } = await this.pool.query('SELECT collection, id, doc FROM cc_docs');
    for (const row of rows) {
      const doc = typeof row.doc === 'string' ? JSON.parse(row.doc) : row.doc;
      this.col(row.collection).set(row.id, doc);
    }
    this.log(`loaded ${rows.length} documents from PostgreSQL`);
  }

  persist(collection, id, doc) {
    // Writes are queued so they reach the database in order.
    this.pending = this.pending
      .then(() =>
        doc === null
          ? this.pool.query('DELETE FROM cc_docs WHERE collection = $1 AND id = $2', [collection, id])
          : this.pool.query(
              `INSERT INTO cc_docs (collection, id, doc, updated_at) VALUES ($1, $2, $3, NOW())
               ON CONFLICT (collection, id) DO UPDATE SET doc = EXCLUDED.doc, updated_at = NOW()`,
              [collection, id, JSON.stringify(doc)],
            ),
      )
      .catch((err) => this.log('database write failed', err));
  }

  async flush() {
    await this.pending;
  }

  async close() {
    await this.flush();
    await this.pool.end?.();
  }
}

/** Picks a backend from the environment. */
export async function storeFromEnv(env = process.env, log = console.log) {
  let store;
  if (env.DATABASE_URL) {
    const { default: pg } = await import('pg');
    const ssl = /sslmode=disable/.test(env.DATABASE_URL) ? false : { rejectUnauthorized: false };
    store = new PgStore(new pg.Pool({ connectionString: env.DATABASE_URL, ssl, max: 4 }), { log });
  } else {
    store = new FileStore(env.DATA_FILE || path.resolve('data/chhupan.json'), { log });
  }
  await store.init();
  return store;
}
