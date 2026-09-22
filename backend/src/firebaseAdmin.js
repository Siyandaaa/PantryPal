const admin = require('firebase-admin');

/**
 * Per the brief: "go to Firebase Console -> Project Settings -> Service
 * Accounts -> Firebase Admin SDK ... select Node.js" - that page gives you
 * a JSON key file. Two ways to supply it here, either works:
 *
 *   1. Set GOOGLE_APPLICATION_CREDENTIALS to the path of the downloaded
 *      serviceAccountKey.json (recommended for local dev - keep the file
 *      out of git, it's already covered by .gitignore).
 *   2. Paste the *entire* JSON file's contents into the FIREBASE_SERVICE_ACCOUNT_JSON
 *      environment variable (recommended for cloud hosting, e.g. an Azure
 *      App Service / Render / Railway "secret" env var, where you can't
 *      easily mount a file).
 */
function loadServiceAccount() {
  if (process.env.FIREBASE_SERVICE_ACCOUNT_JSON) {
    return JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT_JSON);
  }
  if (process.env.GOOGLE_APPLICATION_CREDENTIALS) {
    // admin.credential.applicationDefault() reads this env var itself.
    return null;
  }
  throw new Error(
    'No Firebase credentials found. Set FIREBASE_SERVICE_ACCOUNT_JSON or GOOGLE_APPLICATION_CREDENTIALS - see backend/.env.example.'
  );
}

const serviceAccount = loadServiceAccount();

admin.initializeApp({
  credential: serviceAccount ? admin.credential.cert(serviceAccount) : admin.credential.applicationDefault(),
  projectId: process.env.FIREBASE_PROJECT_ID || serviceAccount?.project_id
});

// Respects FIRESTORE_EMULATOR_HOST / FIREBASE_AUTH_EMULATOR_HOST automatically
// if set, so `npm run dev:emulators` can point this whole file at the local
// Firebase Emulator Suite for testing without touching a real project.
const auth = admin.auth();
const firestore = admin.firestore();

module.exports = { admin, auth, firestore };
