// scripts/set_admin_claim.js
// Usage: node set_admin_claim.js <UID> <true|false>
// Prérequis: GOOGLE_APPLICATION_CREDENTIALS vers une clé service account

const admin = require('firebase-admin');

if (!process.env.GOOGLE_APPLICATION_CREDENTIALS) {
  console.error('Set GOOGLE_APPLICATION_CREDENTIALS to your service account JSON path');
  process.exit(1);
}

admin.initializeApp();

const uid = process.argv[2];
const makeAdmin = process.argv[3] === 'true';

if (!uid) {
  console.error('Usage: node set_admin_claim.js <UID> <true|false>');
  process.exit(1);
}

(async () => {
  try {
    const claims = makeAdmin ? { role: 'admin' } : {};
    await admin.auth().setCustomUserClaims(uid, claims);
    console.log(`Claims updated for ${uid}:`, claims);
    process.exit(0);
  } catch (e) {
    console.error(e);
    process.exit(2);
  }
})();
