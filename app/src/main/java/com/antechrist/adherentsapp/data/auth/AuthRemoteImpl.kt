package com.antechrist.adherentsapp.data.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Implémentation distante de l'authentification via FirebaseAuth.
 * Cette classe implémente l'interface AuthDataSource.
 */
class AuthRemoteImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) : AuthDataSource {

    override suspend fun signIn(email: String, password: String): FirebaseUser {
        val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
        return result.user ?: throw IllegalStateException("FirebaseUser null après signIn")
    }

    override suspend fun signOut() {
        firebaseAuth.signOut()
    }

    override fun currentUser(): FirebaseUser? = firebaseAuth.currentUser

    override suspend fun refreshIdTokenAndGetClaims(): Map<String, Any?> {
        val user = firebaseAuth.currentUser ?: return emptyMap()
        val tokenResult = user.getIdToken(true).await() // force un refresh du token
        return tokenResult.claims
    }
}