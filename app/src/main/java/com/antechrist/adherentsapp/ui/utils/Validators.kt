package com.antechrist.adherentsapp.ui.utils

import android.util.Patterns

/** Email valide (basique, via Android) */
fun isValidEmail(input: String?): Boolean {
    val e = input?.trim().orEmpty()
    return e.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(e).matches()
}

/** Téléphone FR: 10 chiffres (commence souvent par 0) */
fun isValidPhoneFRDigits(digitsOnly: String?): Boolean {
    val d = phoneDigitsOnly(digitsOnly)
    return d.length == 10
}
