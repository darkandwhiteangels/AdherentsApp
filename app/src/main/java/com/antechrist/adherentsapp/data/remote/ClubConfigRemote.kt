package com.antechrist.adherentsapp.data.remote

import android.net.Uri
import com.antechrist.adherentsapp.domain.model.admin.ClubConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.google.firebase.storage.ktx.storageMetadata
import kotlinx.coroutines.tasks.await

object ClubConfigRemote {
    private val db = Firebase.firestore
    private val storage = Firebase.storage

    private fun docRef() = db.collection("club_config").document("main")

    suspend fun get(): ClubConfig? {
        val snap = docRef().get().await()
        if (!snap.exists()) return null
        val d = snap.data ?: return null
        return ClubConfig(
            id = "main",
            clubName = d["clubName"] as? String ?: "",
            clubCity = d["clubCity"] as? String ?: "",
            presidentName = d["presidentName"] as? String ?: "",
            secretaryName = d["secretaryName"] as? String ?: "",
            logoUrl = d["logoUrl"] as? String,
            presidentSignatureUrl = d["presidentSignatureUrl"] as? String,
            secretarySignatureUrl = d["secretarySignatureUrl"] as? String,
            updatedAt = (d["updatedAt"] as? Number)?.toLong() ?: 0L,
            updatedBy = d["updatedBy"] as? String
        )
    }

    suspend fun upsert(cfg: ClubConfig) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val data = hashMapOf(
            "clubName" to cfg.clubName,
            "clubCity" to cfg.clubCity,
            "presidentName" to cfg.presidentName,
            "secretaryName" to cfg.secretaryName,
            "logoUrl" to cfg.logoUrl,
            "presidentSignatureUrl" to cfg.presidentSignatureUrl,
            "secretarySignatureUrl" to cfg.secretarySignatureUrl,
            "updatedAt" to System.currentTimeMillis(),
            "updatedBy" to uid
        )
        docRef().set(data).await()
    }

    /**
     * Upload du logo depuis une Uri (sélecteur de fichiers).
     * Ne met PAS à jour Firestore — on renvoie l’URL, l’UI sauvegarde ensuite via upsert().
     */
    suspend fun uploadLogo(fileUri: Uri): String {
        val path = "club_assets/logos/logo_main.png"
        val ref = storage.reference.child(path)
        // (Optionnel) metadata contentType
        val md = storageMetadata { contentType = "image/png" }
        ref.putFile(fileUri, md).await()
        return ref.downloadUrl.await().toString()
    }

    /**
     * Upload de la signature depuis une Uri (fallback "Importer").
     * Ne met PAS à jour Firestore — on renvoie l’URL, l’UI sauvegarde ensuite via upsert().
     */
    suspend fun uploadSignature(fileUri: Uri): String {
        val path = "club_assets/signatures/president_main.png"
        val ref = storage.getReference(path)
        val md = storageMetadata { contentType = "image/png" }
        ref.putFile(fileUri, md).await()
        return ref.downloadUrl.await().toString()
    }

    /**
     * Upload de la signature du secrétaire depuis une Uri (fallback "Importer").
     * Ne met PAS à jour Firestore — on renvoie l’URL, l’UI sauvegarde ensuite via upsert().
     */
    suspend fun uploadSecretarySignature(fileUri: Uri): String {
        val path = "club_assets/signatures/secretary_main.png"
        val ref = storage.getReference(path)
        val md = storageMetadata { contentType = "image/png" }
        ref.putFile(fileUri, md).await()
        return ref.downloadUrl.await().toString()
    }

    /**
     * Upload de la signature depuis des octets (PNG) — utilisé par la capture manuscrite.
     * Ne met PAS à jour Firestore — on renvoie l’URL, l’UI sauvegarde ensuite via upsert().
     */
    suspend fun uploadSignatureBytes(
        bytes: ByteArray,
        contentType: String = "image/png"
    ): String {
        val path = "club_assets/signatures/president_main.png"
        val ref = storage.getReference(path)
        val md = storageMetadata { this.contentType = contentType }
        ref.putBytes(bytes, md).await()
        return ref.downloadUrl.await().toString()
    }

    /**
     * Upload de la signature du secrétaire depuis des octets (PNG) — utilisé par la capture manuscrite.
     * Ne met PAS à jour Firestore — on renvoie l’URL, l’UI sauvegarde ensuite via upsert().
     */
    suspend fun uploadSecretarySignatureBytes(
        bytes: ByteArray,
        contentType: String = "image/png"
    ): String {
        val path = "club_assets/signatures/secretary_main.png"
        val ref = storage.getReference(path)
        val md = storageMetadata { this.contentType = contentType }
        ref.putBytes(bytes, md).await()
        return ref.downloadUrl.await().toString()
    }
}
