import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import pg from 'pg';
import { FileStore, PgStore } from '../src/store.js';

test('FileStore survives a restart', async () => {
  const dir = await fs.mkdtemp(path.join(os.tmpdir(), 'cc-store-'));
  const file = path.join(dir, 'data.json');
  const a = new FileStore(file, { debounceMs: 5, log: () => {} });
  await a.init();
  a.put('events', 'e1', { id: 'e1', title: 'Night game' });
  a.put('users', 'u1', { id: 'u1', name: 'Asha' });
  a.delete('users', 'u1');
  await a.close();
  const b = new FileStore(file, { log: () => {} });
  await b.init();
  assert.equal(b.get('events', 'e1').title, 'Night game');
  assert.equal(b.get('users', 'u1'), null);
});

// Runs against a real PostgreSQL when TEST_DATABASE_URL is set (CI starts one).
test('PgStore writes through to PostgreSQL and reloads', { skip: !process.env.TEST_DATABASE_URL }, async () => {
  const url = process.env.TEST_DATABASE_URL;
  const reset = new pg.Pool({ connectionString: url });
  await reset.query('DROP TABLE IF EXISTS cc_docs');
  await reset.end();
  const a = new PgStore(new pg.Pool({ connectionString: url }), { log: () => {} });
  await a.init();
  a.put('events', 'e1', { id: 'e1', title: 'First' });
  a.put('events', 'e1', { id: 'e1', title: 'Updated', nested: { list: [1, 2] } });
  a.put('events', 'e2', { id: 'e2', title: 'Gone' });
  a.delete('events', 'e2');
  await a.close();
  const b = new PgStore(new pg.Pool({ connectionString: url }), { log: () => {} });
  await b.init();
  assert.equal(b.get('events', 'e1').title, 'Updated');
  assert.deepEqual(b.get('events', 'e1').nested.list, [1, 2]);
  assert.equal(b.get('events', 'e2'), null);
  assert.equal(b.all('events').length, 1);
  await b.close();
});
