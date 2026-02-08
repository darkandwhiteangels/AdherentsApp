//package com.antechrist.adherentsapp.data.auth
//
//import android.util.Log
//import com.antechrist.adherentsapp.domain.repository.AuthRepository
//import com.antechrist.adherentsapp.domain.repository.AuthStatus
//import javax.inject.Inject
//
///**
// * Implémentation du AuthRepository qui orchestre
// * les appels à AuthDataSource et interprète les claims.
// */
//class AuthRepositoryImpl @Inject constructor(
//    private val ds: AuthDataSource
//) : AuthRepository {
//
//    override suspend fun signIn(email: String, password: String): AuthStatus {
//        ds.signIn(email, password)
//        return refreshStatus()
//    }
//
//    override suspend fun signOut() {
//        ds.signOut()
//    }
//
//    override suspend fun refreshStatus(): AuthStatus {
//        val user = ds.currentUser()
//        if (user == null) {
//            return AuthStatus(isAuthenticated = false, isAdmin = false, email = null)
//        }
//
//        val claims = ds.refreshIdTokenAndGetClaims()
//        Log.d("AuthRepo", "claims=$claims")
//        val role = (claims["role"] as? String)?.lowercase()
//        val isAdmin = when (role) {
//            "super_admin", "admin", "bureau" -> true
//            else -> false
//        }
//
//        return AuthStatus(
//            isAuthenticated = true,
//            isAdmin = isAdmin,
//            email = user.email
//        )
//    }
//
//    override fun currentEmailOrNull(): String? = ds.currentUser()?.email
//}
package com.antechrist.adherentsapp.data.auth

import android.util.Log
import com.antechrist.adherentsapp.domain.model.Role
import com.antechrist.adherentsapp.domain.repository.AuthRepository
import com.antechrist.adherentsapp.domain.repository.AuthStatus
import javax.inject.Inject

/**
 * Implémentation du AuthRepository qui orchestre
 * les appels à AuthDataSource et interprète les claims.
 */
class AuthRepositoryImpl @Inject constructor(
    private val ds: AuthDataSource
) : AuthRepository {

    override suspend fun signIn(email: String, password: String): AuthStatus {
        ds.signIn(email, password)
        return refreshStatus()
    }

    override suspend fun signOut() {
        ds.signOut()
    }

    override suspend fun refreshStatus(): AuthStatus {
        val user = ds.currentUser()
        if (user == null) {
            return AuthStatus(
                isAuthenticated = false,
                isAdmin = false,
                role = null,
                email = null
            )
        }

        val claims = ds.refreshIdTokenAndGetClaims()
        Log.d("AuthRepo", "claims=$claims")

        val roleString = (claims["role"] as? String)?.lowercase()
        val role = mapClaimToRole(roleString)

        // isAdmin conservé pour compatibilité
        val isAdmin = when (role) {
            Role.SUPER_ADMIN, Role.ADMIN, Role.BUREAU -> true
            else -> false
        }

        return AuthStatus(
            isAuthenticated = true,
            isAdmin = isAdmin,
            role = role,
            email = user.email
        )
    }

    override fun currentEmailOrNull(): String? = ds.currentUser()?.email

    /**
     * Mappe le string claim vers l'enum Role.
     * Cohérent avec RoleViewModel.
     */
    private fun mapClaimToRole(claim: String?): Role? {
        if (claim.isNullOrBlank()) return null

        return when (claim.trim().lowercase()) {
            "member", "membre", "adherent", "adhérent" -> Role.MEMBER
            "parent" -> Role.PARENT
            "bureau" -> Role.BUREAU
            "membre_bureau" -> Role.MEMBRE_BUREAU
            "admin" -> Role.ADMIN
            "super_admin", "superadmin", "super-admin" -> Role.SUPER_ADMIN
            "tresorier", "trésorier" -> Role.TRESORIER
            "secretaire", "secrétaire" -> Role.SECRETAIRE
            "professeur" -> Role.PROFESSEUR
            "juge_grade", "juge", "jury" -> Role.JUGE_GRADE
            else -> null
        }
    }
}