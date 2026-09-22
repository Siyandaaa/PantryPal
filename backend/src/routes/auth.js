const express = require('express');
const { auth, firestore } = require('../firebaseAdmin');
const identityToolkit = require('../services/identityToolkit');

const router = express.Router();

const USERS_COLLECTION = 'users';
const DEFAULT_PROFILE = {
  theme: 'system',
  dietaryPreferences: [],
  allergies: [],
  nutritionGoals: {}
};

async function getOrCreateProfile(uid, fullName, email) {
  const ref = firestore.collection(USERS_COLLECTION).doc(uid);
  const snapshot = await ref.get();
  if (snapshot.exists) return snapshot.data();
  const profile = { fullName, email, ...DEFAULT_PROFILE, createdAt: Date.now() };
  await ref.set(profile);
  return profile;
}

function toUserDto(uid, profile) {
  return {
    id: uid,
    fullName: profile.fullName,
    email: profile.email,
    theme: profile.theme,
    dietaryPreferences: profile.dietaryPreferences || [],
    allergies: profile.allergies || [],
    nutritionGoals: profile.nutritionGoals || {}
  };
}

function sessionResponse(uid, idToken, refreshToken, profile) {
  return { accessToken: idToken, refreshToken, user: toUserDto(uid, profile) };
}

/**
 * POST /api/v1/auth/register
 * Creates the Firebase Auth user (Admin SDK), creates their Firestore
 * profile document, then immediately signs them in via Identity Toolkit so
 * registration returns a ready-to-use session, just like login does.
 */
router.post('/register', async (req, res) => {
  const { fullName, email, password } = req.body;
  if (!fullName || !email || !password || password.length < 8) {
    return res.status(400).json({ error: 'fullName, email and an 8+ character password are required' });
  }

  try {
    const userRecord = await auth.createUser({ email, password, displayName: fullName });
    const profile = await getOrCreateProfile(userRecord.uid, fullName, email);
    const session = await identityToolkit.signInWithPassword(email, password);
    res.status(201).json(sessionResponse(userRecord.uid, session.idToken, session.refreshToken, profile));
  } catch (err) {
    if (err.code === 'auth/email-already-exists') {
      return res.status(409).json({ error: 'An account with that email already exists' });
    }
    console.error('register failed', err);
    res.status(500).json({ error: 'Could not create account' });
  }
});

/**
 * POST /api/v1/auth/login
 * Verifies the password via Identity Toolkit (the Admin SDK cannot check
 * passwords itself), then returns the caller's Firestore profile alongside
 * the Firebase-issued tokens.
 */
router.post('/login', async (req, res) => {
  const { email, password } = req.body;
  if (!email || !password) {
    return res.status(400).json({ error: 'email and password are required' });
  }
  try {
    const session = await identityToolkit.signInWithPassword(email, password);
    const profile = await getOrCreateProfile(session.localId, session.displayName || email, email);
    res.json(sessionResponse(session.localId, session.idToken, session.refreshToken, profile));
  } catch (err) {
    if (['EMAIL_NOT_FOUND', 'INVALID_PASSWORD', 'INVALID_LOGIN_CREDENTIALS'].includes(err.firebaseError?.message)) {
      return res.status(401).json({ error: 'Invalid email or password' });
    }
    console.error('login failed', err);
    res.status(err.statusCode || 500).json({ error: 'Login failed' });
  }
});

/**
 * POST /api/v1/auth/sso/google
 * Body: { idToken } - the Google ID token obtained on-device from
 * GoogleSignInClient's .requestIdToken(webClientId). Exchanged here for a
 * Firebase session; creates the Firebase user + Firestore profile on the
 * caller's very first Google sign-in.
 */
router.post('/sso/google', async (req, res) => {
  const { idToken: googleIdToken } = req.body;
  if (!googleIdToken) {
    return res.status(400).json({ error: 'idToken (the Google ID token) is required' });
  }
  try {
    const session = await identityToolkit.signInWithGoogleIdToken(googleIdToken);
    const profile = await getOrCreateProfile(session.localId, session.displayName || session.email, session.email);
    res.json(sessionResponse(session.localId, session.idToken, session.refreshToken, profile));
  } catch (err) {
    console.error('Google SSO failed', err);
    res.status(err.statusCode || 502).json({ error: 'Google sign-in failed' });
  }
});

// POST /api/v1/auth/refresh  { refreshToken }
router.post('/refresh', async (req, res) => {
  const { refreshToken } = req.body;
  if (!refreshToken) return res.status(400).json({ error: 'refreshToken is required' });
  try {
    const result = await identityToolkit.refreshIdToken(refreshToken);
    res.json(result);
  } catch (err) {
    res.status(401).json({ error: 'Invalid or expired refresh token' });
  }
});

// POST /api/v1/auth/password-reset/request  { email }
// Firebase sends and hosts the reset flow itself - no SMTP setup required.
router.post('/password-reset/request', async (req, res) => {
  const { email } = req.body;
  if (!email) return res.status(400).json({ error: 'email is required' });
  try {
    await identityToolkit.sendPasswordResetEmail(email);
  } catch (err) {
    // Deliberately don't leak whether the email exists.
    console.warn('password reset request:', err.message);
  }
  res.status(204).send();
});

module.exports = router;
