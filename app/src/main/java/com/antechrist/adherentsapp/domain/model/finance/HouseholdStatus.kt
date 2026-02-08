package com.antechrist.adherentsapp.domain.model.finance

enum class HouseholdStatus {
    A_REGLER,
    SOLDE,
    EN_RETARD,
    ANNULE;

    fun label(): String = when (this) {
        A_REGLER -> "Réglé"
        SOLDE -> "Soldé"
        EN_RETARD -> "Retard"
        ANNULE -> "Annulé"
    }
}
