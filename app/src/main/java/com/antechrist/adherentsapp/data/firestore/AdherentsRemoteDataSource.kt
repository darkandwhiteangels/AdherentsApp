package com.antechrist.adherentsapp.data.firestore

import android.util.Log
import com.antechrist.adherentsapp.domain.model.Adherent
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AdherentsRemoteDataSource @Inject constructor(
    private val db: FirebaseFirestore
) {
    private val col get() = db.collection("adherents")

    // -------------------- Helpers --------------------

    private fun toFirestoreMap(a: Adherent): Map<String, Any?> = mapOf(
        "nom" to a.nom,
        "prenom" to a.prenom,
        "dateNaissance" to a.dateNaissance,
        "groups" to (a.groups?.take(2)),
        "adresse" to a.adresse,
        "codePostal" to a.codePostal,
        "ville" to a.ville,
        "email" to a.email,
        "telephone" to a.telephone,
        "photoUri" to a.photoUri,
        "photoUpdatedAt" to a.photoUpdatedAt,
        "beltCode" to a.beltCode,
        "stripeCount" to a.stripeCount.coerceIn(0, 3),
        "isPractitioner" to a.isPractitioner,
        "guardianIds" to a.guardianIds,
        "primaryGuardianId" to a.primaryGuardianId,
        "householdId" to a.householdId,
        "consents" to buildMap<String, Any?> {
            if (a.consentEvacuation  != null) put("accidentEvac",     a.consentEvacuation)
            if (a.consentImageSocial != null) put("photoSocial",      a.consentImageSocial)
            if (a.consentImageOfficial!= null)put("photoOfficial",    a.consentImageOfficial)
            if (a.consentRI          != null) put("riAccepted",       a.consentRI)
            if (a.consentsUpdatedAt  != null) put("riAcceptedAt",     a.consentsUpdatedAt)
        }.ifEmpty { null },
        "attribution" to a.attribution,
        // MODIFIÉ : Ajout du champ 'isArchived' dans le mapping
        "isArchived" to a.isArchived
    )

    private fun mapDocToAdherent(doc: DocumentSnapshot): Adherent? {
        if (!doc.exists()) return null

        val groupsList: List<String>? =
            (doc.get("groups") as? List<*>)?.mapNotNull { it as? String }?.take(2)

        val guardianIds: List<String> =
            (doc.get("guardianIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
        val primaryGuardianId: String? = doc.getString("primaryGuardianId")

        val cotisationPaid: Boolean? = when (val v = doc.get("cotisationPaid")) {
            is Boolean -> v
            else -> null
        }

        val consents = (doc.get("consents") as? Map<*, *>)
        val consentEvacuation  = (consents?.get("accidentEvac")  as? Boolean) ?: doc.getBoolean("consentEvacuation")
        val consentImageSocial = (consents?.get("photoSocial")   as? Boolean) ?: doc.getBoolean("consentImageSocial")
        val consentImageOfficial= (consents?.get("photoOfficial") as? Boolean) ?: doc.getBoolean("consentImageOfficial")
        val consentRI          = (consents?.get("riAccepted")    as? Boolean) ?: doc.getBoolean("consentRI")
        val consentsUpdatedAt  = (consents?.get("riAcceptedAt")  as? Number)?.toLong() ?: doc.getLong("consentsUpdatedAt")

        val isPract: Boolean = when (val v = doc.get("isPractitioner")) {
            is Boolean -> v
            else -> true
        }

        val attribution: String? = doc.getString("attribution")

        // MODIFIÉ : Lecture du champ 'isArchived' depuis Firestore
        val isArchived: Boolean = doc.getBoolean("isArchived") ?: false

        return Adherent(
            id = doc.id,
            nom = (doc.getString("nom") ?: "").trim(),
            prenom = (doc.getString("prenom") ?: "").trim(),
            dateNaissance = doc.getString("dateNaissance"),
            groups = groupsList,
            adresse = doc.getString("adresse"),
            codePostal = doc.getString("codePostal"),
            ville = doc.getString("ville"),
            email = doc.getString("email"),
            telephone = doc.getString("telephone"),
            photoUri = doc.getString("photoUri"),
            photoUpdatedAt = doc.getLong("photoUpdatedAt"),
            beltCode = doc.getString("beltCode"),
            stripeCount = (doc.get("stripeCount") as? Number)?.toInt() ?: 0,
            isPractitioner = isPract,
            guardianIds = guardianIds,
            primaryGuardianId = primaryGuardianId,
            householdId = doc.getString("householdId"),
            cotisationPaid = cotisationPaid,
            consentEvacuation = consentEvacuation,
            consentImageSocial = consentImageSocial,
            consentImageOfficial = consentImageOfficial,
            consentRI = consentRI,
            consentsUpdatedAt = consentsUpdatedAt,
            attribution = attribution,
            // MODIFIÉ : Assignation du champ lu
            isArchived = isArchived
        )
    }

    // -------------------- Streams --------------------

    fun streamAdherents(): Flow<List<Adherent>> = callbackFlow {
        // MODIFIÉ : Ajout du filtre pour exclure les archivés et tri composite
        val query = col
            .whereEqualTo("isArchived", false)
            .orderBy("nom", Query.Direction.ASCENDING)

        val sub = query.addSnapshotListener { snap, err ->
            if (err != null) {
                val fe = err as? com.google.firebase.firestore.FirebaseFirestoreException
                Log.e(
                    "AdherentsRemote",
                    "❌ streamAdherents ERROR | code=${fe?.code} | msg=${err.message}",
                    err
                )

                if (err.message?.contains("requires an index") == true) {
                    Log.e(
                        "FIRESTORE_INDEX",
                        "Index manquant pour (isArchived ASC, nom ASC)"
                    )
                }

                trySend(emptyList())
                return@addSnapshotListener
            }
            val list = snap?.documents?.mapNotNull { mapDocToAdherent(it) } ?: emptyList()
            trySend(list)
        }
        awaitClose { sub.remove() }
    }

//    fun streamAdherentsForGuardian(guardianId: String): Flow<List<Adherent>> = callbackFlow {
//        if (guardianId.isBlank()) {
//            trySend(emptyList())
//            close()
//            return@callbackFlow
//        }
//
//        // ✅ Query filtrée : uniquement les enfants liés à ce guardian
//        val query = col
//            .whereEqualTo("isArchived", false)
//            .whereArrayContains("guardianIds", guardianId)
//            //.orderBy("nom", Query.Direction.ASCENDING)
//
//        val sub = query.addSnapshotListener { snap, err ->
//            if (err != null) {
//                Log.e("AdherentsRemote", "streamAdherentsForGuardian PERMISSION/ERROR", err)
//                if (err.message?.contains("requires an index") == true) {
//                    android.util.Log.e(
//                        "FIRESTORE_INDEX",
//                        "Index manquant pour (isArchived=false + guardianIds array-contains + orderBy nom). Suivez le lien dans l'erreur pour le créer."
//                    )
//                }
//                trySend(emptyList())
//                return@addSnapshotListener
//            }
//
//            val list = snap?.documents?.mapNotNull { mapDocToAdherent(it) } ?: emptyList()
//            trySend(list)
//        }
//        awaitClose { sub.remove() }
//    }
    fun streamAdherentsForGuardian(guardianId: String): Flow<List<Adherent>> = callbackFlow {
        if (guardianId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        // ✅ Query MINIMALE pour tester
        val query = col.whereArrayContains("guardianIds", guardianId)

        val sub = query.addSnapshotListener { snap, err ->
            if (err != null) {
                Log.e("AdherentsRemote", "❌ ERROR: ${err.message}", err)
                // NE PAS trySend(emptyList()) => ça écrase le state
                return@addSnapshotListener
            }

            val list = snap?.documents?.mapNotNull { mapDocToAdherent(it) } ?: emptyList()
            Log.d("AdherentsRemote", "✅ RESULT: ${list.size} adherents")
            trySend(list)
        }
        awaitClose { sub.remove() }
    }

    fun streamById(id: String): Flow<Adherent?> = callbackFlow {
        val sub = col.document(id).addSnapshotListener { snap, err ->
            if (err != null) { close(err); return@addSnapshotListener }
            trySend(snap?.let { mapDocToAdherent(it) })
        }
        awaitClose { sub.remove() }
    }

    // -------------------- Writes --------------------

    suspend fun add(adherent: Adherent): String {
        val doc = col.document()
        val data = toFirestoreMap(adherent.copy(id = doc.id))
        doc.set(data, SetOptions.merge()).await()
        return doc.id
    }

    suspend fun add(adherentMap: Map<String, Any?>) {
        val doc = col.document()
        doc.set(adherentMap - "cotisationPaid", SetOptions.merge()).await()
    }

    suspend fun update(a: Adherent) {
        require(a.id.isNotBlank()) { "id vide pour update" }
        col.document(a.id).set(toFirestoreMap(a), SetOptions.merge()).await()
    }

    suspend fun update(id: String, adherentMap: Map<String, Any?>) {
        col.document(id).set(adherentMap - "cotisationPaid", SetOptions.merge()).await()
    }

    suspend fun patchCotisationPaid(adherentId: String, paid: Boolean) {
        col.document(adherentId).set(
            mapOf(
                "cotisationPaid" to paid,
                "updatedAt" to System.currentTimeMillis()
            ),
            SetOptions.merge()
        ).await()
    }

    suspend fun patchConsents(
        adherentId: String,
        consentEvacuation: Boolean?,
        consentImageSocial: Boolean?,
        consentImageOfficial: Boolean?,
        consentRI: Boolean?
    ) {
        val map = buildMap<String, Any?> {
            if (consentEvacuation  != null) put("accidentEvac",  consentEvacuation)
            if (consentImageSocial != null) put("photoSocial",   consentImageSocial)
            if (consentImageOfficial!= null)put("photoOfficial", consentImageOfficial)
            if (consentRI          != null) put("riAccepted",    consentRI)
            put("riAcceptedAt", System.currentTimeMillis())
        }
        col.document(adherentId).set(mapOf("consents" to map), SetOptions.merge()).await()
    }

    suspend fun delete(id: String) {
        col.document(id).delete().await()
    }

    // MODIFIÉ : La fonction d'archivage est maintenant correcte
    suspend fun archive(id: String) {
        col.document(id).update("isArchived", true).await()
    }

    // -------------------- Queries auxiliaires --------------------

    suspend fun existsByEmail(email: String, excludeId: String? = null): Boolean {
        if (email.isBlank()) return false
        val snap: QuerySnapshot = col.whereEqualTo("email", email).get().await()
        val docs = snap.documents
        return when {
            docs.isEmpty() -> false
            excludeId == null -> true
            else -> docs.any { it.id != excludeId }
        }
    }

    fun streamByGroup(groupe: String): Flow<List<Adherent>> = callbackFlow {
        val ref = col.whereEqualTo("groupe", groupe)
        val reg = ref.addSnapshotListener { snap, err ->
            if (err != null) { trySend(emptyList()); return@addSnapshotListener }
            val list = snap?.documents?.mapNotNull { mapDocToAdherent(it) }.orEmpty()
            trySend(list)
        }
        awaitClose { reg.remove() }
    }

    suspend fun getNamesByIds(ids: Set<String>): Map<String, Pair<String, String>> {
        if (ids.isEmpty()) return emptyMap()
        val out = HashMap<String, Pair<String, String>>(ids.size)
        for (chunk in ids.chunked(10)) {
            val snap = col.whereIn(FieldPath.documentId(), chunk).get().await()
            for (doc in snap.documents) {
                val nom = (doc.getString("nom") ?: "").trim()
                val prenom = (doc.getString("prenom") ?: "").trim()
                out[doc.id] = nom to prenom
            }
        }
        return out
    }

    suspend fun updatePhotoFields(adherentId: String, downloadUrl: String, updatedAt: Long) {
        val doc = col.document(adherentId)
        val data = hashMapOf<String, Any?>(
            "photoUri" to downloadUrl,
            "photoUpdatedAt" to updatedAt
        )
        doc.set(data, SetOptions.merge()).await()
    }

}
