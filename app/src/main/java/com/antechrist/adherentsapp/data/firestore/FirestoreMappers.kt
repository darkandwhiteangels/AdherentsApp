package com.antechrist.adherentsapp.data.firestore

import com.antechrist.adherentsapp.data.model.AdherentDto
import com.antechrist.adherentsapp.domain.model.Adherent
import com.google.firebase.firestore.DocumentSnapshot

fun DocumentSnapshot.toAdherent(): Adherent? {
    val dto = this.toObject(AdherentDto::class.java) ?: return null
    return Adherent(
        id = this.id,
        nom = dto.nom.orEmpty(),
        prenom = dto.prenom.orEmpty(),
        dateNaissance = dto.dateNaissance,          // ← garde le nullable
        groups = dto.groups,
        adresse = dto.adresse,
        codePostal = dto.codePostal,
        ville = dto.ville,
        email = dto.email,
        telephone = dto.telephone,
        photoUri = dto.photoUri,
        photoUpdatedAt = dto.photoUpdatedAt,
        // 🆕 liens parents
        guardianIds = dto.guardianIds ?: emptyList(),
        primaryGuardianId = dto.primaryGuardianId,
        householdId = dto.householdId,
        // liste des ceintures
        beltCode = dto.beltCode,
        stripeCount = dto.stripeCount ?: 0,
        // 🆕 flag rôle simple
        isPractitioner = dto.isPractitioner ?: true,
        // 🆕 attribution
        attribution = dto.attribution,
        // 🆕 consents map -> déballage (tolère null)
        consentEvacuation = (dto.consents?.get("accidentEvac") as? Boolean)
            ?: (dto.consents?.get("minorAccidentEvac") as? Boolean),
        consentImageSocial = (dto.consents?.get("photoSocial") as? Boolean)
            ?: (dto.consents?.get("minorPhotoSocial") as? Boolean),
        consentImageOfficial = (dto.consents?.get("photoOfficial") as? Boolean)
            ?: (dto.consents?.get("minorPhotoOfficial") as? Boolean),
        consentRI = (dto.consents?.get("riAccepted") as? Boolean)
            ?: (dto.consents?.get("gRiAccepted") as? Boolean),
        consentsUpdatedAt  = (dto.consents?.get("riAcceptedAt") as? Number)?.toLong()
    )
}

fun Adherent.toDto(): AdherentDto = AdherentDto(
    nom = nom,
    prenom = prenom,
    dateNaissance = dateNaissance,
    groups = groups?.take(2),
    adresse = adresse,
    codePostal = codePostal,
    ville = ville,
    email = email,
    telephone = telephone,
    photoUri = photoUri,
    photoUpdatedAt = photoUpdatedAt,
    // 🆕 n’écrit guardianIds que si non vide (sinon laisse absent/null)
    guardianIds = guardianIds?.ifEmpty { null },
    primaryGuardianId = primaryGuardianId,
    householdId = householdId,
    // Ceintures (liste)
    beltCode = beltCode,
    stripeCount = stripeCount,
    isPractitioner = isPractitioner,
    attribution = attribution,
    consents = buildMap<String, Any?> {
        if (consentEvacuation  != null) put("accidentEvac",     consentEvacuation)
        if (consentImageSocial != null) put("photoSocial",      consentImageSocial)
        if (consentImageOfficial!= null)put("photoOfficial",    consentImageOfficial)
        if (consentRI          != null) put("riAccepted",       consentRI)
        if (consentsUpdatedAt  != null) put("riAcceptedAt",     consentsUpdatedAt)
    }.ifEmpty { null }

)
