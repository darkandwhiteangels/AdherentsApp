// Usage: node scripts/set_role.js <UID> <super_admin|admin|bureau|member>
// Prérequis: GOOGLE_APPLICATION_CREDENTIALS pointant vers le JSON du service account

const admin = require('firebase-admin');

if (!process.env.GOOGLE_APPLICATION_CREDENTIALS) {
  console.error('Set GOOGLE_APPLICATION_CREDENTIALS to your service account JSON path');
  process.exit(1);
}

admin.initializeApp();

const uid = process.argv[2];
const role = process.argv[3];

const ALLOWED = new Set(['super_admin', 'admin', 'bureau', 'member']);

if (!uid || !role || !ALLOWED.has(role)) {
  console.error('Usage: node scripts/set_role.js <UID> <super_admin|admin|bureau|member>');
  process.exit(1);
}

(async () => {
  try {
    // ⚠️ setCustomUserClaims REMPLACE l’objet de claims (écrase l’ancien)
    await admin.auth().setCustomUserClaims(uid, { role });

    // Miroir Firestore (utile pour l’UI / debug)
    await admin.firestore().collection('users').doc(uid).set({
      role,
      roleUpdatedAt: admin.firestore.FieldValue.serverTimestamp(),
    }, { merge: true });

    // (optionnel) Forcer l’invalidation des sessions (facultatif)
    // await admin.auth().revokeRefreshTokens(uid);

    console.log(`OK: ${uid} -> role="${role}"`);
    process.exit(0);
  } catch (e) {
    console.error('ERROR:', e);
    process.exit(2);
  }
})();
