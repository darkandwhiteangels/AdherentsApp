package com.antechrist.adherentsapp.domain.model

data class Guardian(
    val id: String,
    val nom: String? = null,
    val prenom: String? = null,
    val relation: String? = null,
    val telephone: String? = null,  // digits only
    val email: String? = null,      // lowercase
    val adresse: String? = null,
    val codePostal: String? = null, // digits only (≤5)
    val ville: String? = null,

    // 🔗 Lien optionnel vers le compte Firebase Auth du responsable
    // (Chez toi guardianId != uid, donc ce champ devient la "jonction")
    val authUid: String? = null
)
