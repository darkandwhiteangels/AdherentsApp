package com.antechrist.adherentsapp.data.firestore

import com.antechrist.adherentsapp.domain.model.Adherent
import com.antechrist.adherentsapp.domain.repository.AdherentsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AdherentsRepositoryImpl @Inject constructor(
    private val remote: AdherentsRemoteDataSource
) : AdherentsRepository {

    override fun streamAll(): Flow<List<Adherent>> = remote.streamAdherents()

    override fun streamForGuardian(guardianId: String): Flow<List<Adherent>> =
        remote.streamAdherentsForGuardian(guardianId)

    override fun streamById(id: String): Flow<Adherent?> = remote.streamById(id)

    override suspend fun add(adherent: Adherent) {
        // ⚠️ volontairement on N'ÉCRIT PAS cotisationPaid ici (écrit par le module cotisations)
        remote.add(adherent.toFirestoreMap())
    }

    override suspend fun update(adherent: Adherent) {
        // ⚠️ idem : pas de cotisationPaid ici, on laisse le module cotisations piloter ce flag
        remote.update(adherent.id, adherent.toFirestoreMap())
    }

    override suspend fun delete(id: String) = remote.delete(id)

    // MODIFIÉ : Ajout de l'implémentation pour la fonction d'archivage
    override suspend fun archiveAdherent(id: String) {
        // Le Repository délègue simplement l'appel au RemoteDataSource
        remote.archive(id)
    }

    override suspend fun existsByEmail(email: String, excludeId: String?): Boolean =
        remote.existsByEmail(email, excludeId)

    /**
     * ⚠️ IMPORTANT :
     * On filtre côté client pour supporter la coexistence :
     *   - nouveau champ "groups": List<String> (clés normalisées "baby","enfant_u14","adult_14p")
     *   - ancien champ "groupe": String (libellé d'affichage)
     */
    override fun streamByGroup(groupe: String): Flow<List<Adherent>> =
        remote.streamAdherents().map { all ->
            val key = normalizeGroupKey(groupe)
            all
                .filter { a ->
                    a.groups?.contains(key) == true  // ✅ On utilise uniquement groups
                }
                .sortedWith(compareBy({ it.nom.lowercase() }, { it.prenom.lowercase() }))
        }

    /** ✅ Utilisé par l’export PDF (inchangé) */
    override suspend fun getMemberNamesByIds(ids: Set<String>): Map<String, Pair<String, String>> {
        if (ids.isEmpty()) return emptyMap()
        return remote.getNamesByIds(ids)
    }

    override suspend fun setAdherentPhoto(adherentId: String, downloadUrl: String, updatedAt: Long) {
        remote.updatePhotoFields(adherentId, downloadUrl, updatedAt)
    }

    // ----------------- helpers -----------------

    private fun normalizeGroupKey(display: String): String = when (display.trim()) {
        "Baby" -> "baby"
        "Enfants < 14 ans" -> "enfant_u14"
        "14+ / Adultes" -> "adult_14p"
        else -> display.lowercase()
            .replace("[^a-z0-9]+".toRegex(), "_")
            .trim('_')
    }
}

/** Mapping domaine -> Firestore (Map) */
private fun Adherent.toFirestoreMap(): Map<String, Any?> = mapOf(
    "nom" to nom,
    "prenom" to prenom,
    "dateNaissance" to dateNaissance,

    // ✅ Liste de 1..2 clés normalisées ; null si absente
    "groups" to groups?.take(2),

    "adresse" to adresse,
    "codePostal" to codePostal,
    "ville" to ville,
    "email" to email,
    "telephone" to telephone,

    "photoUri" to photoUri,
    "photoUpdatedAt" to photoUpdatedAt,

    // Grade / ceinture
    "beltCode" to beltCode,
    "stripeCount" to stripeCount,

    // 🆕 Rôle simple
    "isPractitioner" to isPractitioner,

    // 🆕 Liens vers responsables (acceptés par les règles actuelles)
    "guardianIds" to guardianIds,
    "primaryGuardianId" to primaryGuardianId,
    "householdId" to householdId,

    "attribution" to attribution,

    // Le champ 'isArchived' est déjà dans le toFirestoreMap, ce qui est parfait.
    "isArchived" to isArchived
)
