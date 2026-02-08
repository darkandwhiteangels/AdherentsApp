//package com.antechrist.adherentsapp.data.remote
//
//import com.google.firebase.functions.FirebaseFunctions
//import kotlinx.coroutines.tasks.await
//
///**
// * Résultat de l'appel setUserRoleByEmail :
// * - created     : true si le compte Auth a été créé (cas admin/bureau quand il n'existait pas)
// * - deletedAuth : true si le compte Auth a été supprimé (cas member avec deleteAuthIfMember=true)
// */
//data class SetRoleOutcome(
//    val created: Boolean = false,
//    val deletedAuth: Boolean = false
//)
//
//// Utilise la même région que ta base (europe-west1).
//private val functions = FirebaseFunctions.getInstance("europe-west1")
//
//object RolesRemote {
//
//    /**
//     * Appelle la Cloud Function `setUserRoleByEmail`.
//     *
//     * @param email Email de l'adhérent
//     * @param role "super_admin" | "admin" | "bureau" | "member"
//     * @param deleteAuthIfMember Si true et role == "member", supprime le compte Auth s'il existe
//     */
//    suspend fun setRoleByEmail(
//        email: String,
//        role: String,
//        deleteAuthIfMember: Boolean = false
//    ): SetRoleOutcome {
//        val data = hashMapOf<String, Any>(
//            "email" to email.trim().lowercase(),
//            "role" to role,
//            "deleteAuthIfMember" to deleteAuthIfMember
//        )
//
//        val res = functions
//            .getHttpsCallable("setUserRoleByEmail")
//            .call(data)
//            .await()
//
//        @Suppress("UNCHECKED_CAST")
//        val map = res.data as? Map<String, Any?> ?: emptyMap()
//
//        val created = (map["created"] as? Boolean) == true
//        val deletedAuth = (map["deletedAuth"] as? Boolean) == true
//
//        return SetRoleOutcome(created = created, deletedAuth = deletedAuth)
//    }
//}
package com.antechrist.adherentsapp.data.remote

import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await

/**
 * Résultat de l'appel setUserRoleByEmail :
 * - created     : true si le compte Auth a été créé (cas admin/bureau quand il n'existait pas)
 * - deletedAuth : true si le compte Auth a été supprimé (cas member avec deleteAuthIfMember=true)
 */
data class SetRoleOutcome(
    val created: Boolean = false,
    val deletedAuth: Boolean = false
)

// Utilise la même région que ta base (europe-west1).
private val functions = FirebaseFunctions.getInstance("europe-west1")

object RolesRemote {

    /**
     * Appelle la Cloud Function `setUserRoleByEmail`.
     *
     * @param email Email de l'adhérent
     * @param role "super_admin" | "admin" | "bureau" | "parent" | "member"
     * @param deleteAuthIfMember Si true et role == "member", supprime le compte Auth s'il existe
     * @param guardianId ID du guardian (obligatoire si role == "parent")
     */
    suspend fun setRoleByEmail(
        email: String,
        role: String,
        deleteAuthIfMember: Boolean = false,
        guardianId: String? = null
    ): SetRoleOutcome {
        val data = hashMapOf<String, Any>(
            "email" to email.trim().lowercase(),
            "role" to role,
            "deleteAuthIfMember" to deleteAuthIfMember
        )

        // Ajouter guardianId si présent (nécessaire pour role=parent)
        if (guardianId != null) {
            data["guardianId"] = guardianId
        }

        val res = functions
            .getHttpsCallable("setUserRoleByEmail")
            .call(data)
            .await()

        @Suppress("UNCHECKED_CAST")
        val map = res.data as? Map<String, Any?> ?: emptyMap()

        val created = (map["created"] as? Boolean) == true
        val deletedAuth = (map["deletedAuth"] as? Boolean) == true

        return SetRoleOutcome(created = created, deletedAuth = deletedAuth)
    }
}