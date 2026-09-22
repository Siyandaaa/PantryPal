const fetch = require('node-fetch');

/**
 * The Admin SDK can create/manage users but deliberately cannot verify a
 * password or mint a client-usable ID token (that's by design - it's meant
 * to run trusted, server-side). The standard way to do real "log a user in
 * with their password" or "log a user in with a Google credential" from a
 * custom backend is Firebase's Identity Toolkit REST API - it's the exact
 * same API the Firebase client SDKs call under the hood, so this file is
 * effectively a minimal server-side Firebase Auth client.
 */

function baseUrl(service) {
  // If FIREBASE_AUTH_EMULATOR_HOST is set (e.g. "localhost:9099"), route
  // requests to the local Auth Emulator instead of production Google APIs -
  // useful for `npm run dev:emulators` / offline marking environments.
  const emulatorHost = process.env.FIREBASE_AUTH_EMULATOR_HOST;
  if (emulatorHost) {
    return `http://${emulatorHost}/${service}`;
  }
  return `https://${service}`;
}

function webApiKey() {
  const key = process.env.FIREBASE_WEB_API_KEY;
  if (!key) throw new Error('FIREBASE_WEB_API_KEY is not set - see backend/.env.example');
  return key;
}

async function callIdentityToolkit(path, body) {
  const url = `${baseUrl('identitytoolkit.googleapis.com')}/v1/${path}?key=${webApiKey()}`;
  const response = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body)
  });
  const json = await response.json();
  if (!response.ok) {
    const err = new Error(json.error?.message || 'Firebase Identity Toolkit request failed');
    err.statusCode = response.status;
    err.firebaseError = json.error;
    throw err;
  }
  return json;
}

/** Verifies an email/password pair and returns { idToken, refreshToken, localId, email }. */
function signInWithPassword(email, password) {
  return callIdentityToolkit('accounts:signInWithPassword', { email, password, returnSecureToken: true });
}

/**
 * Exchanges a Google ID token (from Android's Credential Manager /
 * GoogleSignInClient, requested with .requestIdToken()) for a Firebase
 * session - creating the Firebase user on first sign-in automatically.
 * Returns { idToken, refreshToken, localId, email, isNewUser }.
 */
function signInWithGoogleIdToken(googleIdToken) {
  return callIdentityToolkit('accounts:signInWithIdp', {
    postBody: `id_token=${googleIdToken}&providerId=google.com`,
    requestUri: 'https://pantrypal.app/oauth-callback', // not actually navigated to; required by the API shape
    returnSecureToken: true
  });
}

/** Exchanges a refresh token for a new short-lived ID token. */
async function refreshIdToken(refreshToken) {
  const url = `${baseUrl('securetoken.googleapis.com')}/v1/token?key=${webApiKey()}`;
  const response = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({ grant_type: 'refresh_token', refresh_token: refreshToken })
  });
  const json = await response.json();
  if (!response.ok) {
    const err = new Error(json.error?.message || 'Refresh token exchange failed');
    err.statusCode = response.status;
    throw err;
  }
  return { idToken: json.id_token, refreshToken: json.refresh_token };
}

/** Triggers Firebase's own "reset your password" email flow - no SMTP setup needed. */
function sendPasswordResetEmail(email) {
  return callIdentityToolkit('accounts:sendOobCode', { requestType: 'PASSWORD_RESET', email });
}

module.exports = { signInWithPassword, signInWithGoogleIdToken, refreshIdToken, sendPasswordResetEmail };
