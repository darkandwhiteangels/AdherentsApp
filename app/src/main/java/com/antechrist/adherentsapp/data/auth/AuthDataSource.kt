package com.antechrist.adherentsapp.data.auth

import com.google.firebase.auth.FirebaseUser

interface AuthDataSource {
    suspend fun signIn(email: String, password: String): FirebaseUser
    suspend fun signOut()
    fun currentUser(): FirebaseUser?
    /** Force un refresh du token pour relire les custom claims (role). */
    suspend fun refreshIdTokenAndGetClaims(): Map<String, Any?>
}
