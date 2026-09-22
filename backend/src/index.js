require('dotenv').config();
const express = require('express');
const cors = require('cors');
const morgan = require('morgan');
const rateLimit = require('express-rate-limit');

const authRoutes = require('./routes/auth');
const userRoutes = require('./routes/users');

const app = express();

app.use(cors());
app.use(express.json());
app.use(morgan(process.env.NODE_ENV === 'production' ? 'combined' : 'dev'));

// Auth endpoints get their own tighter limit to blunt credential-stuffing /
// brute-force attempts, independent of Firebase's own project-level quotas.
app.use('/api/v1/auth', rateLimit({ windowMs: 60_000, max: 20 }));
app.use(rateLimit({ windowMs: 60_000, max: 300 }));

app.get('/health', (_req, res) => res.json({ status: 'ok', service: 'pantrypal-auth-api', time: new Date().toISOString() }));

// PantryPal's ONE required custom API: authentication, backed by Firebase.
// (Per the POE brief: "You do not need to create or use an API for every
// feature of your application. The API requirement is limited to the
// authentication component." Every other feature in the Android app reads
// and writes its own local Room database directly, or calls Spoonacular
// directly for recipe data - see the root README for the full picture.)
app.use('/api/v1/auth', authRoutes);
app.use('/api/v1/users', userRoutes);

app.use((req, res) => res.status(404).json({ error: `No route for ${req.method} ${req.path}` }));

app.use((err, _req, res, _next) => {
  console.error('Unhandled error:', err);
  res.status(err.statusCode || 500).json({ error: err.message || 'Internal server error' });
});

const PORT = process.env.PORT || 3000;
if (require.main === module) {
  app.listen(PORT, () => console.log(`PantryPal auth API listening on port ${PORT}`));
}

module.exports = app;
