const { auth } = require('../firebaseAdmin');

/**
 * PantryPal's "access token" IS a Firebase ID token - there's no separate
 * JWT scheme layered on top. This keeps the whole auth story to one concept
 * (a Firebase-issued, Firebase-verifiable token) instead of two.
 */
async function requireAuth(req, res, next) {
  const header = req.headers.authorization || '';
  const idToken = header.startsWith('Bearer ') ? header.slice(7) : null;
  if (!idToken) {
    return res.status(401).json({ error: 'Missing bearer token' });
  }
  try {
    const decoded = await auth.verifyIdToken(idToken);
    req.userId = decoded.uid;
    req.userEmail = decoded.email;
    next();
  } catch (err) {
    return res.status(401).json({ error: 'Invalid or expired token' });
  }
}

module.exports = { requireAuth };
