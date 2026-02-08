import { onCall, HttpsError } from "firebase-functions/v2/https";
import { onDocumentCreated } from "firebase-functions/v2/firestore";
import { logger } from "firebase-functions/v2";
import * as admin from "firebase-admin";

admin.initializeApp();

/* ────────────────────────────────────────────────
   TYPES & HELPERS RÔLES
   ──────────────────────────────────────────────── */

type CanonicalRole =
  | "super_admin"
  | "admin"
  | "bureau"
  | "membre_bureau"
  | "tresorier"
  | "secretaire"
  | "professeur"
  | "juge_grade"
  | "parent"
  | "member";

function normalizeRole(input?: string | null): CanonicalRole | null {
  if (!input) return null;
  const v = input.trim().toLowerCase();

  // alias usuels / sans accents
  if (["superadmin", "super-admin"].includes(v)) return "super_admin";
  if (v === "adherent" || v === "adhérent" || v === "membre") return "member";
  if (v === "tresorier" || v === "trésorier") return "tresorier";
  if (v === "secretaire" || v === "secrétaire") return "secretaire";
  if (v === "juge" || v === "judge" || v === "juge-grade") return "juge_grade";

  // rôles canoniques autorisés
  const allowed: CanonicalRole[] = [
    "super_admin",
    "admin",
    "bureau",
    "membre_bureau",
    "tresorier",
    "secretaire",
    "professeur",
    "juge_grade",
    "parent",
    "member",
  ];
  return allowed.includes(v as CanonicalRole) ? (v as CanonicalRole) : null;
}

/* ────────────────────────────────────────────────
   MUTATION DE RÔLE - AVEC SUPPORT PARENT
   ──────────────────────────────────────────────── */

export const setUserRoleByEmail = onCall(
  { region: "europe-west1" },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "Auth required.");
    }

    const callerRole = (request.auth.token as any)?.role as
      | string
      | undefined;
    if (callerRole !== "super_admin") {
      throw new HttpsError(
        "permission-denied",
        "Only super_admin can set roles."
      );
    }

    const rawEmail = request.data?.email as string | undefined;
    const rawRole = request.data?.role as string | undefined;
    const deleteAuthIfMember = Boolean(request.data?.deleteAuthIfMember);
    const guardianId = request.data?.guardianId as string | undefined;

    const email = rawEmail?.trim().toLowerCase();
    const role = normalizeRole(rawRole);

    if (!email || !role) {
      throw new HttpsError(
        "invalid-argument",
        "email and valid role are required."
      );
    }

    // Validation : si role=parent, guardianId est obligatoire
    if (role === "parent" && !guardianId) {
      throw new HttpsError(
        "invalid-argument",
        "guardianId is required for role=parent"
      );
    }

    // Récupère l'utilisateur s'il existe
    let userRecord: admin.auth.UserRecord | null = null;
    try {
      userRecord = await admin.auth().getUserByEmail(email);
    } catch {
      userRecord = null;
    }

    // Cas "member" : adhérent adulte de base
    // -> pas forcément besoin d'accès app.
    // deleteAuthIfMember=true => on supprime le compte auth si il existe encore
    if (role === "member") {
      if (userRecord) {
        if (deleteAuthIfMember) {
          await admin.auth().deleteUser(userRecord.uid);
          await admin
            .firestore()
            .collection("users")
            .doc(userRecord.uid)
            .set(
              {
                email,
                role: "member",
                roleUpdatedAt: admin.firestore.FieldValue.serverTimestamp(),
              },
              { merge: true }
            );
          return {
            ok: true,
            uid: userRecord.uid,
            email,
            role,
            created: false,
            deletedAuth: true,
          };
        } else {
          await admin.auth().setCustomUserClaims(userRecord.uid, {
            role: "member",
            member: true,
          });
          await admin
            .firestore()
            .collection("users")
            .doc(userRecord.uid)
            .set(
              {
                email,
                role: "member",
                roleUpdatedAt: admin.firestore.FieldValue.serverTimestamp(),
              },
              { merge: true }
            );
          return {
            ok: true,
            uid: userRecord.uid,
            email,
            role,
            created: false,
            deletedAuth: false,
          };
        }
      }
      // pas de userRecord => pas de compte auth créé pour un simple "member"
      return {
        ok: true,
        uid: null,
        email,
        role,
        created: false,
        deletedAuth: false,
        note: "no-auth-user",
      };
    }

    // Rôles non-"member" => on doit (potentiellement) créer un compte
    let created = false;
    if (!userRecord) {
      const tempPassword = Math.random().toString(36).slice(-12) + "Aa1!";
      userRecord = await admin.auth().createUser({
        email,
        password: tempPassword,
        emailVerified: false,
        disabled: false,
      });
      created = true;
      logger.info(
        `[setUserRoleByEmail] Created Auth user ${userRecord.uid} for ${email}`
      );
    }

    // Construire les claims
    const claims: Record<string, any> = { role };
    claims[role] = true; // bool direct

    // héritages
    if (role === "super_admin") {
      claims.superadmin = true;
      claims.super_admin = true;
      claims.admin = true;
      claims.bureau = true;
    }
    if (role === "admin") {
      claims.admin = true;
      claims.bureau = true;
    }
    if (role === "bureau" || role === "membre_bureau") {
      claims.bureau = true;
      if (role === "membre_bureau") claims.membre_bureau = true;
    }
    if (role === "tresorier") {
      claims.tresorier = true;
      claims.treasurer = true;
    }
    if (role === "secretaire") {
      claims.secretaire = true;
      claims.secretary = true;
    }
    if (role === "professeur") {
      claims.professeur = true;
    }
    if (role === "juge_grade") {
      claims.juge_grade = true;
    }
    if (role === "parent") {
      claims.parent = true;
    }

    await admin.auth().setCustomUserClaims(userRecord.uid, claims);
    logger.info(
      `[setUserRoleByEmail] Set custom claims for ${userRecord.uid}: role=${role}`
    );

    // Créer/mettre à jour le doc users
    const userDocData: Record<string, any> = {
      email,
      role,
      roleUpdatedAt: admin.firestore.FieldValue.serverTimestamp(),
    };

    // BLOC PARENT : liaison avec guardian
    if (role === "parent" && guardianId) {
      userDocData.guardianId = guardianId;

      try {
        // Mettre à jour guardian.authUid
        await admin
          .firestore()
          .collection("guardians")
          .doc(guardianId)
          .update({
            authUid: userRecord.uid,
            accessGrantedAt: admin.firestore.FieldValue.serverTimestamp(),
          });

        logger.info(
          `[setUserRoleByEmail] Linked guardian ${guardianId} to authUid ${userRecord.uid}`
        );
    // 🆕 ENVOYER EMAIL AUTO SI COMPTE CRÉÉ
//         if (created) {
//           try {
//             const resetLink = await admin.auth().generatePasswordResetLink(email);
//             logger.info(
//               `[setUserRoleByEmail] Password reset link generated for ${email}: ${resetLink}`
//             );
//           } catch (emailError) {
//             logger.error(
//               `[setUserRoleByEmail] Failed to send reset email to ${email}:`,
//               emailError
//             );
//             // Ne pas bloquer si l'email échoue
//           }
//         }
      } catch (error) {
        logger.error(
          `[setUserRoleByEmail] Failed to update guardian ${guardianId}:`,
          error
        );
        throw new HttpsError(
          "internal",
          `Failed to link guardian: ${error}`
        );
      }
    }

    // Si création, ajouter createdAt
    if (created) {
      userDocData.createdAt = admin.firestore.FieldValue.serverTimestamp();
    }

    await admin
      .firestore()
      .collection("users")
      .doc(userRecord.uid)
      .set(userDocData, { merge: true });

    logger.info(
      `[setUserRoleByEmail] Created/updated users/${userRecord.uid} for ${email}`
    );

    return {
      ok: true,
      uid: userRecord.uid,
      email,
      role,
      created,
      deletedAuth: false,
    };
  }
);

/* ────────────────────────────────────────────────
   RENVOYER EMAIL RÉINITIALISATION PARENT
   ──────────────────────────────────────────────── */

export const resendParentPasswordEmail = onCall(
  { region: "europe-west1" },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "Auth required.");
    }

    const callerRole = (request.auth.token as any)?.role as string | undefined;
    if (callerRole !== "super_admin" && callerRole !== "admin") {
      throw new HttpsError(
        "permission-denied",
        "Only admin can resend emails."
      );
    }

    const guardianId = request.data?.guardianId as string | undefined;
    if (!guardianId) {
      throw new HttpsError("invalid-argument", "guardianId is required.");
    }

    // Récupérer le guardian
    const guardianDoc = await admin
      .firestore()
      .collection("guardians")
      .doc(guardianId)
      .get();

    if (!guardianDoc.exists) {
      throw new HttpsError("not-found", "Guardian not found.");
    }

    const guardian = guardianDoc.data();
    const email = guardian?.email;

    if (!email) {
      throw new HttpsError(
        "failed-precondition",
        "Guardian has no email address."
      );
    }

    // Générer le lien de réinitialisation
    // Firebase Auth envoie automatiquement l'email
    const link = await admin.auth().generatePasswordResetLink(email);

    logger.info(
      `[resendParentPasswordEmail] Password reset link generated for ${email}: ${link}`
    );

    return {
      ok: true,
      email,
      resetLink: link,
      message: "Email de réinitialisation envoyé",
    };
  }
);

/* ────────────────────────────────────────────────
   RÉVOQUER ACCÈS PARENT
   ──────────────────────────────────────────────── */

export const revokeParentAccess = onCall(
  { region: "europe-west1" },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "Auth required.");
    }

    const callerRole = (request.auth.token as any)?.role as string | undefined;
    if (callerRole !== "super_admin" && callerRole !== "admin") {
      throw new HttpsError(
        "permission-denied",
        "Only admin can revoke access."
      );
    }

    const guardianId = request.data?.guardianId as string | undefined;
    if (!guardianId) {
      throw new HttpsError("invalid-argument", "guardianId is required.");
    }

    // Récupérer le guardian
    const guardianDoc = await admin
      .firestore()
      .collection("guardians")
      .doc(guardianId)
      .get();

    if (!guardianDoc.exists) {
      throw new HttpsError("not-found", "Guardian not found.");
    }

    const guardian = guardianDoc.data();
    const authUid = guardian?.authUid;

    if (!authUid) {
      throw new HttpsError(
        "failed-precondition",
        "Guardian has no active account."
      );
    }

    // Supprimer le compte Auth
    try {
      await admin.auth().deleteUser(authUid);
      logger.info(`[revokeParentAccess] Deleted Auth user ${authUid}`);
    } catch (error) {
      logger.warn(
        `[revokeParentAccess] Auth user ${authUid} already deleted:`,
        error
      );
    }

    // Supprimer le doc users
    await admin.firestore().collection("users").doc(authUid).delete();

    // Réinitialiser guardian.authUid
    await admin
      .firestore()
      .collection("guardians")
      .doc(guardianId)
      .update({
        authUid: admin.firestore.FieldValue.delete(),
        accessGrantedAt: admin.firestore.FieldValue.delete(),
        accessRevokedAt: admin.firestore.FieldValue.serverTimestamp(),
      });

    logger.info(
      `[revokeParentAccess] Revoked access for guardian ${guardianId}, deleted user ${authUid}`
    );

    return {
      ok: true,
      guardianId,
      revokedUid: authUid,
      message: "Accès révoqué avec succès",
    };
  }
);

/* ────────────────────────────────────────────────
   PUSH NOTIF SUR NOUVEAU MESSAGE info_messages/*
   ──────────────────────────────────────────────── */

type InfoMessageDoc = {
  title: string;
  body: string;
  audience: "ALL_REGISTERED" | "ADULTS_ONLY" | "GUARDIANS";
  active: boolean;
  createdAt: number | FirebaseFirestore.Timestamp;
  createdByUid: string;
};

// Règle de ciblage notif
// STAFF (= roles club encadrants) reçoit toujours tout.
// PARENT reçoit GUARDIANS + ALL_REGISTERED.
// MEMBER (adulte pratiquant autonome) reçoit ADULTS_ONLY + ALL_REGISTERED.
function userShouldReceiveMessage(
  audience: string,
  userRole: string
): boolean {
  const staffRoles = [
    "super_admin",
    "admin",
    "professeur",
    "bureau",
    "membre_bureau",
    "tresorier",
    "secretaire",
  ];

  if (staffRoles.includes(userRole)) {
    return true;
  }

  if (userRole === "parent") {
    if (audience === "GUARDIANS") return true;
    if (audience === "ALL_REGISTERED") return true;
    return false;
  }

  if (userRole === "member") {
    if (audience === "ADULTS_ONLY") return true;
    if (audience === "ALL_REGISTERED") return true;
    return false;
  }

  return false;
}

// Va chercher les tokens FCM de tous les uids qui doivent recevoir
async function getTargetTokensForAudience(
  audience: string
): Promise<string[]> {
  const db = admin.firestore();

  // Récupère tous les users
  const usersSnap = await db.collection("users").get();

  const targetUids: string[] = [];

  usersSnap.forEach((doc) => {
    const data = doc.data() || {};
    const role: string = (data.role || "").toString();
    const uid = doc.id;
    if (userShouldReceiveMessage(audience, role)) {
      targetUids.push(uid);
    }
  });

  logger.debug(
    `[onInfoMessageCreated] audience=${audience} -> targetUids=${targetUids.join(
      ","
    )}`
  );

  if (targetUids.length === 0) {
    return [];
  }

  // Récupère les tokens FCM enregistrés sous users/{uid}/fcmTokens/*
  const allTokens: string[] = [];

  for (const uid of targetUids) {
    const tokenSnap = await db
      .collection("users")
      .doc(uid)
      .collection("fcmTokens")
      .get();

    tokenSnap.forEach((tDoc) => {
      const tData = tDoc.data();
      const token = tData.token;
      if (token && typeof token === "string") {
        allTokens.push(token);
      }
    });
  }

  const deduped = Array.from(new Set(allTokens));
  logger.debug(
    `[onInfoMessageCreated] collected ${deduped.length} tokens for audience=${audience}`
  );

  return deduped;
}

// Déclenchée à chaque création de doc dans info_messages
export const onInfoMessageCreated = onDocumentCreated(
  {
    region: "europe-west1",
    document: "info_messages/{msgId}",
    memory: "512MiB",
    timeoutSeconds: 60,
    minInstances: 0,
    maxInstances: 10,
  },
  async (event) => {
    const snap = event.data;
    if (!snap) {
      logger.warn("[onInfoMessageCreated] no snapshot data");
      return;
    }

    const msgData = snap.data() as InfoMessageDoc;
    logger.debug("[onInfoMessageCreated] new message:", msgData);

    // Si le message est déjà inactif, ne pas spammer
    if (msgData.active !== true) {
      logger.debug("[onInfoMessageCreated] message not active -> skip");
      return;
    }

    const audience = msgData.audience;
    const title = msgData.title ?? "Information club";
    const body = msgData.body ?? "";

    // 1. Récupérer les tokens FCM
    const tokens = await getTargetTokensForAudience(audience);

    if (!tokens.length) {
      logger.debug("[onInfoMessageCreated] no tokens to notify, done.");
      return;
    }

    // 2. Préparer le payload FCM
    const messagePayload: admin.messaging.MulticastMessage = {
      tokens,
      notification: {
        title,
        body,
      },
      data: {
        type: "INFO_MESSAGE",
        openDestination: "info_messages",
        audience: audience,
        msgTitle: title,
        msgBody: body,
      },
      android: {
        priority: "high",
      },
    };

    // 3. Envoyer
    const res = await admin.messaging().sendEachForMulticast(messagePayload);
    logger.info(
      `[onInfoMessageCreated] sent push: success=${res.successCount}, fail=${res.failureCount}`
    );
  }
);