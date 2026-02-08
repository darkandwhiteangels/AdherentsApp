// reset_trial.js
const admin = require('firebase-admin');

admin.initializeApp({
  credential: admin.credential.applicationDefault(),
});

const db = admin.firestore();
const BATCH_SIZE = 500;

async function resetAdherentsTrial() {
  const snap = await db.collection('adherents').get();
  let batch = db.batch();
  let count = 0;

  for (const doc of snap.docs) {
    batch.set(doc.ref, { trialCompleted: false, trialPresentCount: 0 }, { merge: true });
    count++;
    if (count % BATCH_SIZE === 0) {
      await batch.commit();
      batch = db.batch();
      console.log(`Commit ${count}…`);
    }
  }
  if (count % BATCH_SIZE !== 0) {
    await batch.commit();
  }
  console.log(`OK — ${count} adhérents remis à zéro.`);
}

resetAdherentsTrial().catch((e) => {
  console.error('Erreur reset trial:', e);
  process.exit(1);
});
