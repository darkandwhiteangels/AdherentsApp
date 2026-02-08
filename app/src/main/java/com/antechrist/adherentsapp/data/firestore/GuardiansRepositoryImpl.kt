package com.antechrist.adherentsapp.data.firestore

import android.util.Log
import com.antechrist.adherentsapp.domain.model.Guardian
import com.antechrist.adherentsapp.domain.repository.GuardiansRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "GuardiansRepo"

@Singleton
class GuardiansRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore
) : GuardiansRepository {

    private val colGuardians by lazy { db.collection("guardians") }
    private val colAdherents by lazy { db.collection("adherents") }

    override suspend fun getById(id: String): Guardian? {
        Log.e("GETBYID", "════════════════════════════════════════════")
        Log.e("GETBYID", "📖 getById()")
        Log.e("GETBYID", "Guardian ID: $id")

        val currentUser = FirebaseAuth.getInstance().currentUser
        Log.e("GETBYID", "Auth UID: ${currentUser?.uid}")
        Log.e("GETBYID", "Auth Email: ${currentUser?.email}")

        return try {
            val snap = colGuardians.document(id).get().await()
            val result = if (snap.exists()) snap.toGuardian() else null
            Log.e(TAG, "✅ Résultat: ${if (result != null) "Guardian trouvé" else "Guardian non trouvé"}")
            Log.e(TAG, "════════════════════════════════════════════")
            result
        } catch (e: Exception) {
            Log.e(TAG, "❌ ERREUR getById", e)
            Log.e(TAG, "Type: ${e.javaClass.simpleName}")
            Log.e(TAG, "Message: ${e.message}")
            Log.e(TAG, "════════════════════════════════════════════")
            throw e
        }
    }

    override fun streamAll(): Flow<List<Guardian>> = callbackFlow {
        val sub = colGuardians
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .limit(500)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    android.util.Log.e("GuardiansRepo", "streamAll error", err)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snap?.documents?.map { it.toGuardian() } ?: emptyList()
                trySend(
                    list.sortedWith(compareBy<Guardian> { (it.nom ?: "").trim() }.thenBy { (it.prenom ?: "").trim() })
                )
            }
        awaitClose { sub.remove() }
    }

    override suspend fun update(guardian: Guardian) {
        require(guardian.id.isNotBlank()) { "Guardian id manquant" }
        colGuardians.document(guardian.id).set(guardian.toMap(), SetOptions.merge()).await()
    }

    override suspend fun findByEmailOrTel(email: String?, telephoneDigits: String?): Guardian? {
        val e = email?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
        val t = telephoneDigits?.filter { it.isDigit() }?.takeIf { it.isNotEmpty() }

        var found: Guardian? = null

        if (e != null) {
            val qs = colGuardians.whereEqualTo("emailLower", e).limit(1).get().await()
            found = qs.documents.firstOrNull()?.toGuardian()
        }
        if (found == null && t != null) {
            val qs = colGuardians.whereEqualTo("telephone", t).limit(1).get().await()
            found = qs.documents.firstOrNull()?.toGuardian()
        }
        return found
    }

    override suspend fun createGuardian(
        nom: String,
        prenom: String,
        email: String?,
        telephoneDigits: String?,
        relation: String?
    ): String {
        val t = telephoneDigits?.filter { it.isDigit() }?.takeIf { it.isNotEmpty() }
        val e = email?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
        require(!e.isNullOrEmpty() || !t.isNullOrEmpty()) {
            "Email ou téléphone requis"
        }

        val doc = colGuardians.document()
        val data = hashMapOf<String, Any?>(
            "nom" to nom,
            "prenom" to prenom,
            "relation" to relation,
            "telephone" to t,
            "email" to e,
            "emailLower" to e,
            "updatedAt" to System.currentTimeMillis(),
            "createdAt" to System.currentTimeMillis()
        )
        doc.set(data, SetOptions.merge()).await()
        return doc.id
    }

    override suspend fun linkGuardianToAdherent(
        adherentId: String,
        guardianId: String,
        setPrimaryIfEmpty: Boolean
    ) {
        val aRef = colAdherents.document(adherentId)
        db.runTransaction { tr ->
            val snap = tr.get(aRef)
            val guardianIds = (snap.get("guardianIds") as? List<*>)?.mapNotNull { it as? String }?.toMutableSet() ?: mutableSetOf()
            val primary = snap.getString("primaryGuardianId")

            guardianIds.add(guardianId)
            val updates = hashMapOf<String, Any?>(
                "guardianIds" to guardianIds.toList(),
                "updatedAt" to System.currentTimeMillis()
            )
            if (setPrimaryIfEmpty && primary.isNullOrBlank()) {
                updates["primaryGuardianId"] = guardianId
            }
            tr.update(aRef, updates as Map<String, Any?>)
            null
        }.await()
    }

    override suspend fun unlinkGuardianFromAdherent(
        adherentId: String,
        guardianId: String
    ) {
        val aRef = colAdherents.document(adherentId)
        db.runTransaction { tr ->
            val snap = tr.get(aRef)
            val guardianIds = (snap.get("guardianIds") as? List<*>)?.mapNotNull { it as? String }?.toMutableList() ?: mutableListOf()
            val primary = snap.getString("primaryGuardianId")

            val newList = guardianIds.filter { it != guardianId }
            val updates = hashMapOf<String, Any?>(
                "guardianIds" to newList,
                "updatedAt" to System.currentTimeMillis()
            )
            if (primary == guardianId) {
                updates["primaryGuardianId"] = newList.firstOrNull()
            }
            tr.update(aRef, updates as Map<String, Any?>)
            null
        }.await()
    }

    override suspend fun setPrimaryGuardian(adherentId: String, guardianId: String) {
        val aRef = colAdherents.document(adherentId)
        db.runTransaction { tr ->
            val snap = tr.get(aRef)
            val guardianIds = (snap.get("guardianIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
            val updates = hashMapOf<String, Any?>(
                "updatedAt" to System.currentTimeMillis(),
                "primaryGuardianId" to guardianId
            )
            if (!guardianIds.contains(guardianId)) {
                tr.update(aRef, "guardianIds", FieldValue.arrayUnion(guardianId))
            }
            tr.update(aRef, updates as Map<String, Any?>)
            null
        }.await()
    }

    override suspend fun getByAuthUid(authUid: String): Guardian? {
        Log.e(TAG, "════════════════════════════════════════════")
        Log.e("TEST2", "🔍 TEST 2: allow get sur guardians (getByAuthUid)")

        val uid = authUid.trim().takeIf { it.isNotEmpty() } ?: return null

        val currentUser = FirebaseAuth.getInstance().currentUser
        Log.e(TAG, "📍 Location: /guardians (query)")
        Log.e(TAG, "🔧 Operation: whereEqualTo + get")
        Log.e(TAG, "✅ Authenticated: ${currentUser != null}")
        Log.e(TAG, "🆔 Auth UID: ${currentUser?.uid}")
        Log.e(TAG, "📧 Auth Email: ${currentUser?.email}")
        Log.e(TAG, "🔎 Query: authUid == $uid")

        return try {
            val qs = colGuardians.whereEqualTo("authUid", uid).limit(1).get().await()
            val result = qs.documents.firstOrNull()?.toGuardian()

            Log.e(TAG, "📊 Result: ${if (result != null) "✅ Guardian trouvé (id=${result.id})" else "⚠️ Aucun guardian trouvé"}")
            Log.e(TAG, "════════════════════════════════════════════")

            result
        } catch (e: Exception) {
            Log.e(TAG, "❌ ERREUR getByAuthUid", e)
            Log.e(TAG, "Type: ${e.javaClass.simpleName}")
            Log.e(TAG, "Message: ${e.message}")
            Log.e(TAG, "════════════════════════════════════════════")
            throw e
        }
    }

    override suspend fun linkAuthUid(guardianId: String, authUid: String) {
        val gid = guardianId.trim()
        val uid = authUid.trim()
        require(gid.isNotEmpty()) { "guardianId manquant" }
        require(uid.isNotEmpty()) { "authUid manquant" }

        val gRef = colGuardians.document(gid)

        db.runTransaction { tr ->
            val snap = tr.get(gRef)
            if (!snap.exists()) {
                throw IllegalStateException("Guardian introuvable: $gid")
            }
            val existing = snap.getString("authUid")?.trim().orEmpty()
            if (existing.isNotEmpty() && existing != uid) {
                throw IllegalStateException("Guardian déjà lié à un autre compte")
            }

            tr.set(
                gRef,
                mapOf(
                    "authUid" to uid,
                    "updatedAt" to System.currentTimeMillis()
                ),
                SetOptions.merge()
            )
            null
        }.await()
    }

    override suspend fun acceptInvitation(inviteId: String, guardianId: String) {
        Log.e(TAG, "════════════════════════════════════════════")
        Log.e(TAG, "🎯 TEST 3: allow update sur guardians (acceptInvitation)")

        val uid = FirebaseAuth.getInstance().currentUser?.uid
            ?: error("Not authenticated")

        val currentUser = FirebaseAuth.getInstance().currentUser
        Log.e(TAG, "📍 Location: /guardians/$guardianId")
        Log.e(TAG, "🔧 Operation: batch.update (via transaction)")
        Log.e(TAG, "✅ Authenticated: ${currentUser != null}")
        Log.e(TAG, "🆔 Auth UID: ${currentUser?.uid}")
        Log.e(TAG, "📧 Auth Email: ${currentUser?.email}")
        Log.e(TAG, "📝 Invitation ID: $inviteId")
        Log.e(TAG, "👤 Guardian ID: $guardianId")

        val db = Firebase.firestore

        try {
            db.runTransaction { tx ->
                val inviteRef = db.collection("guardian_invitations").document(inviteId)
                val guardianRef = db.collection("guardians").document(guardianId)

                val inviteSnap = tx.get(inviteRef)
                if (!inviteSnap.exists()) error("Invitation introuvable")

                val status = inviteSnap.getString("status")
                Log.e(TAG, "📊 Invitation status: $status")

                if (status != "PENDING") {
                    Log.e(TAG, "⚠️ Status n'est pas PENDING, transaction annulée")
                    return@runTransaction
                }

                // Lecture du guardian AVANT update
                val guardianSnap = tx.get(guardianRef)
                Log.e(TAG, "📖 Guardian existe: ${guardianSnap.exists()}")
                if (guardianSnap.exists()) {
                    val existingAuthUid = guardianSnap.getString("authUid")
                    Log.e(TAG, "📊 Data (before): authUid = ${existingAuthUid ?: "null"}")
                }

                // ✅ 1. Invitation acceptée
                tx.update(inviteRef, mapOf(
                    "status" to "ACCEPTED",
                    "acceptedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                    "acceptedByUid" to uid
                ))
                Log.e(TAG, "✅ Invitation updated to ACCEPTED")

                // ✅ 2. Liaison authUid → guardian
                tx.update(guardianRef, mapOf(
                    "authUid" to uid,
                    "activatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                ))
                Log.e(TAG, "✅ Guardian updated with authUid")
                Log.e(TAG, "📊 Data (after): authUid = $uid")
            }.await()

            Log.e(TAG, "🎉 Transaction completed successfully")
            Log.e(TAG, "════════════════════════════════════════════")

        } catch (e: Exception) {
            Log.e(TAG, "❌ ERREUR acceptInvitation", e)
            Log.e(TAG, "Type: ${e.javaClass.simpleName}")
            Log.e(TAG, "Message: ${e.message}")
            Log.e(TAG, "════════════════════════════════════════════")
            throw e
        }
    }
}