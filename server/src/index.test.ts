import { test, describe, before, after } from 'node:test';
import assert from 'node:assert/strict';
import { buildApp } from './index.js';
import { FastifyInstance } from 'fastify';

describe('Dosezy Server API Contract & Endpoints', () => {
  let app: FastifyInstance;

  before(async () => {
    app = await buildApp();
    await app.ready();
  });

  after(async () => {
    await app.close();
  });

  test('GET /health returns healthy status and version', async () => {
    const res = await app.inject({
      method: 'GET',
      url: '/health',
    });

    assert.equal(res.statusCode, 200);
    const body = JSON.parse(res.payload);
    assert.equal(body.status, 'ok');
    assert.equal(body.version, '1.0.0');
    assert.equal(body.service, 'dosezy-server');
  });

  test('GET /v1/caregiver/patients returns seeded patient summary', async () => {
    const res = await app.inject({
      method: 'GET',
      url: '/v1/caregiver/patients',
    });

    assert.equal(res.statusCode, 200);
    const body = JSON.parse(res.payload);
    assert.ok(Array.isArray(body));
    assert.ok(body.length > 0);
    assert.equal(body[0].userId, 'demo-patient-001');
    assert.equal(body[0].fullName, 'Eleanor Vance');
    assert.ok(typeof body[0].adherenceRate === 'number');
  });

  test('POST /v1/caregiver/pair generates a 6-character code for patient', async () => {
    const res = await app.inject({
      method: 'POST',
      url: '/v1/caregiver/pair',
      payload: {
        role: 'PATIENT',
        patientId: 'demo-patient-001',
      },
    });

    assert.equal(res.statusCode, 200);
    const body = JSON.parse(res.payload);
    assert.equal(typeof body.pairingCode, 'string');
    assert.equal(body.pairingCode.length, 6);
    assert.equal(body.status, 'PENDING');
  });

  test('POST /v1/sync performs delta-sync roundtrip', async () => {
    const now = new Date().toISOString();
    const res = await app.inject({
      method: 'POST',
      url: '/v1/sync',
      payload: {
        syncTimestamp: now,
        users: [],
        medicines: [],
        schedules: [],
      },
    });

    assert.equal(res.statusCode, 200);
    const body = JSON.parse(res.payload);
    assert.ok(body.syncTimestamp);
    assert.ok(Array.isArray(body.users));
    assert.ok(Array.isArray(body.medicines));
    assert.ok(Array.isArray(body.schedules));
  });

  test('POST /v1/schedules/:id/take marks dose as taken', async () => {
    const res = await app.inject({
      method: 'POST',
      url: '/v1/schedules/sched-2/take',
      payload: {
        takenTime: new Date().toISOString(),
        doseNotes: 'Test take from unit test',
      },
    });

    assert.equal(res.statusCode, 200);
    const body = JSON.parse(res.payload);
    assert.equal(body.status, 'TAKEN');
    assert.equal(body.doseNotes, 'Test take from unit test');
  });
});
