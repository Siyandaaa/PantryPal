const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

// A syntactically valid but entirely fake service account key, so
// firebase-admin's initializeApp() succeeds locally without ever making a
// real network call to Google - which lets these tests run in any CI
// environment, including one with no Firebase project configured.
process.env.FIREBASE_SERVICE_ACCOUNT_JSON = fs.readFileSync(
  path.join(__dirname, 'fixtures.fakeServiceAccount.json'),
  'utf8'
);
process.env.FIREBASE_WEB_API_KEY = 'test-fake-web-api-key';

const request = require('supertest');
const app = require('../src/index');

test('GET /health returns ok without touching Firebase', async () => {
  const res = await request(app).get('/health');
  assert.equal(res.status, 200);
  assert.equal(res.body.status, 'ok');
});

test('POST /api/v1/auth/register rejects missing fields before calling Firebase', async () => {
  const res = await request(app).post('/api/v1/auth/register').send({ email: 'sam@example.com' });
  assert.equal(res.status, 400);
  assert.match(res.body.error, /required/i);
});

test('POST /api/v1/auth/register rejects a short password', async () => {
  const res = await request(app)
    .post('/api/v1/auth/register')
    .send({ fullName: 'Sam', email: 'sam@example.com', password: 'short' });
  assert.equal(res.status, 400);
});

test('POST /api/v1/auth/login rejects a missing password', async () => {
  const res = await request(app).post('/api/v1/auth/login').send({ email: 'sam@example.com' });
  assert.equal(res.status, 400);
});

test('POST /api/v1/auth/sso/google rejects a missing idToken', async () => {
  const res = await request(app).post('/api/v1/auth/sso/google').send({});
  assert.equal(res.status, 400);
});

test('POST /api/v1/auth/refresh rejects a missing refreshToken', async () => {
  const res = await request(app).post('/api/v1/auth/refresh').send({});
  assert.equal(res.status, 400);
});

test('GET /api/v1/users/me requires a bearer token', async () => {
  const res = await request(app).get('/api/v1/users/me');
  assert.equal(res.status, 401);
});

test('unknown routes return 404 as JSON', async () => {
  const res = await request(app).get('/api/v1/does-not-exist');
  assert.equal(res.status, 404);
});
