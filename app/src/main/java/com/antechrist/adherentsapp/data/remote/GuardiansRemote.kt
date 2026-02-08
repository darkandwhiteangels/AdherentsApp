package com.antechrist.adherentsapp.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

data class GuardianFound(
    val id: String,
    val nom: String,
    val prenom: String,
    val relation: String?,
    val email: String?,
    val telephone: String?
)

object GuardiansRemote {
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    /** Cherche un guardian par email (prioritaire) ou téléphone (fallback). */
    suspend fun findByEmailOrTel(email: String?, telephone: String?): GuardianFound? {
        val col = db.collection("guardians")

        // 1) Email exact (lowercase conseillé côté stockage)
        val e = email?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
        if (e != null) {
            val q = col.whereEqualTo("email", e).limit(1).get().await()
            q.documents.firstOrNull()?.let { d ->
                return GuardianFound(
                    id = d.id,
                    nom = d.getString("nom").orEmpty(),
                    prenom = d.getString("prenom").orEmpty(),
                    relation = d.getString("relation"),
                    email = d.getString("email"),
                    telephone = d.getString("telephone")
                )
            }
        }

        // 2) Téléphone exact (pas de normalisation forte ici pour rester simple)
        val t = telephone?.trim()?.takeIf { it.isNotEmpty() }
        if (t != null) {
            val q = col.whereEqualTo("telephone", t).limit(1).get().await()
            q.documents.firstOrNull()?.let { d ->
                return GuardianFound(
                    id = d.id,
                    nom = d.getString("nom").orEmpty(),
                    prenom = d.getString("prenom").orEmpty(),
                    relation = d.getString("relation"),
                    email = d.getString("email"),
                    telephone = d.getString("telephone")
                )
            }
        }

        return null
    }

    /** Crée un guardian minimal (au moins nom/prenom + email OU téléphone) et renvoie son id. */
    suspend fun createGuardian(
        nom: String,
        prenom: String,
        email: String?,
        telephone: String?,
        relation: String?
    ): String {
        val data = hashMapOf<String, Any?>(
            "nom" to nom.trim(),
            "prenom" to prenom.trim(),
            "email" to email?.trim()?.lowercase()?.ifBlank { null },
            "telephone" to telephone?.trim()?.ifBlank { null },
            "relation" to relation?.trim()?.ifBlank { null }
        )

        val doc = db.collection("guardians").add(data).await()
        return doc.id
    }

    /**
     * Lie un guardian à un adhérent (transaction) :
     * - ajoute guardianId à guardianIds (max 10, pas de doublon)
     * - si primary est vide et setPrimaryIfEmpty=true, le définit.
     */
    suspend fun linkGuardianToAdherent(
        adherentId: String,
        guardianId: String,
        setPrimaryIfEmpty: Boolean = true
    ) {
        val adherentRef = db.collection("adherents").document(adherentId)

        db.runTransaction { tx ->
            val snap = tx.get(adherentRef)
            val currentList = (snap.get("guardianIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
            val already = currentList.contains(guardianId)
            val newList = if (already) currentList else currentList + guardianId
            if (newList.size > 10) throw IllegalStateException("Trop de responsables (max 10)")

            val updates = hashMapOf<String, Any?>("guardianIds" to newList)
            val currentPrimary = snap.getString("primaryGuardianId")
            if (setPrimaryIfEmpty && (currentPrimary.isNullOrBlank())) {
                updates["primaryGuardianId"] = guardianId
            }

            tx.update(adherentRef, updates as Map<String, Any?>)
            null
        }.await()
    }

    suspend fun setPrimaryGuardian(
        adherentId: String,
        guardianId: String
    ) {
        val ref = db.collection("adherents").document(adherentId)
        db.runTransaction { tx ->
            val snap = tx.get(ref)
            val list = (snap.get("guardianIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
            require(list.contains(guardianId)) { "Guardian non lié" }
            tx.update(ref, mapOf("primaryGuardianId" to guardianId))
            null
        }.await()
    }

    suspend fun unlinkGuardianFromAdherent(
        adherentId: String,
        guardianId: String
    ) {
        val ref = db.collection("adherents").document(adherentId)
        db.runTransaction { tx ->
            val snap = tx.get(ref)
            val current = (snap.get("guardianIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
            if (!current.contains(guardianId)) return@runTransaction null

            val newList = current.filterNot { it == guardianId }
            val currentPrimary = snap.getString("primaryGuardianId")

            val updates = mutableMapOf<String, Any?>(
                "guardianIds" to newList
            )
            if (currentPrimary == guardianId) {
                // si on enlève le principal → remettre le 1er restant ou null
                updates["primaryGuardianId"] = newList.firstOrNull()
            }
            tx.update(ref, updates)
            null
        }.await()
    }
}
