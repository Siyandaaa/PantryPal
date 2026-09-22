const express = require('express');
const { auth, firestore } = require('../firebaseAdmin');
const { requireAuth } = require('../middleware/auth');

const router = express.Router();
router.use(requireAuth);

const USERS_COLLECTION = 'users';

function toUserDto(uid, profile) {
  return {
    id: uid,
    fullName: profile.fullName,
    email: profile.email,
    theme: profile.theme || 'system',
    dietaryPreferences: profile.dietaryPreferences || [],
    allergies: profile.allergies || [],
    nutritionGoals: profile.nutritionGoals || {}
  };
}

// GET /api/v1/users/me
router.get('/me', async (req, res) => {
  const ref = firestore.collection(USERS_COLLECTION).doc(req.userId);
  const snapshot = await ref.get();
  if (!snapshot.exists) return res.status(404).json({ error: 'User profile not found' });
  res.json(toUserDto(req.userId, snapshot.data()));
});

// PATCH /api/v1/users/me - the Settings screen's save endpoint.
router.patch('/me', async (req, res) => {
  const { theme, dietaryPreferences, allergies, nutritionGoals } = req.body;
  const updates = {};
  if (theme !== undefined) updates.theme = theme;
  if (dietaryPreferences !== undefined) updates.dietaryPreferences = dietaryPreferences;
  if (allergies !== undefined) updates.allergies = allergies;
  if (nutritionGoals !== undefined) updates.nutritionGoals = nutritionGoals;

  const ref = firestore.collection(USERS_COLLECTION).doc(req.userId);
  await ref.set(updates, { merge: true });
  const updated = await ref.get();
  res.json(toUserDto(req.userId, updated.data()));
});

// DELETE /api/v1/users/me
router.delete('/me', async (req, res) => {
  await firestore.collection(USERS_COLLECTION).doc(req.userId).delete();
  await auth.deleteUser(req.userId);
  res.status(204).send();
});

module.exports = router;
