package com.antechrist.adherentsapp.data.firestore

import com.antechrist.adherentsapp.domain.model.Guardian
import com.google.firebase.firestore.DocumentSnapshot

fun DocumentSnapshot.toGuardian(): Guardian {
    val m = data ?: emptyMap<String, Any?>()
    return Guardian(
        id = id,
        nom = m["nom"] as? String,
        prenom = m["prenom"] as? String,
        relation = m["relation"] as? String,
        telephone = m["telephone"] as? String,
        email = m["email"] as? String,
        adresse = m["adresse"] as? String,
        codePostal = m["codePostal"] as? String,
        ville = m["ville"] as? String,

        // refactor guardian
        authUid = m["authUid"] as? String
    )
}

fun Guardian.toMap(): Map<String, Any?> = mapOf(
    "nom" to nom,
    "prenom" to prenom,
    "relation" to relation,
    "telephone" to telephone,
    "email" to email,
    "adresse" to adresse,
    "codePostal" to codePostal,
    "ville" to ville,
    "authUid" to authUid,        // <-  Refactor guardian
    "updatedAt" to System.currentTimeMillis()
)
