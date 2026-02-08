package com.antechrist.adherentsapp.ui.navigation

object Destinations {
    const val Login = "login"
    const val AccessDenied = "access_denied"
    const val List = "adherents_list"
    const val Detail = "adherent_detail/{id}"
    // WhatsApp Parents
    const val WhatsAppParents = "whatsAppParents"
    const val Form = "adherent_form"
    const val ArgId = "id"
    const val Splash = "splash"
    const val ClubConfig = "config/club"
    // Bureau Hub
    const val BureauHome = "bureau/home"

    //Bureau Modules (placeholder pour le moment)
    const val BureauStats = "bureau/stats"
    const val BureauAgCr = "bureau/ag_cr/{seasonKey}"
    fun bureauAgCr(seasonKey: String) = "bureau/ag_cr/$seasonKey"
    // Présences
    const val PresenceTake = "presence/take"
    const val PresenceReports = "presence/reports"

    // Finance / Cotisations
    const val Cotisations = "finance/cotisations"
    const val ArgGuardianId = "guardianId"
    const val CotisationHousehold = "finance/cotisations/{$ArgGuardianId}"
    fun cotisationHousehold(guardianId: String) = "finance/cotisations/$guardianId"

    // Rapport licences FFK
    const val ArgSeasonKey = "seasonKey"
    const val LicencesReport = "finance/licences_report/{$ArgSeasonKey}"
    fun licencesReport(seasonKey: String) = "finance/licences_report/$seasonKey"

    // ✅ Rapport cotisations (snapshot)
    const val CotisationsReport = "finance/cotisations_report/{$ArgSeasonKey}"
    fun cotisationsReport(seasonKey: String) = "finance/cotisations_report/$seasonKey"

    // Fin de saison (clôture)
    const val SeasonClosure = "finance/season_closure/{$ArgSeasonKey}"
    fun seasonClosure(seasonKey: String) = "finance/season_closure/$seasonKey"

    // 🔹 Nouveau : route édition responsable
    const val GuardianList = "guardians"
    const val GuardianEdit = "finance/guardian/edit/{$ArgGuardianId}"
    fun guardianEdit(guardianId: String) = "finance/guardian/edit/$guardianId"

    // Notification et Messagerie
    const val InfoMessages = "info_messages"
    const val NotificationGroups = "notificationGroups"

    // ═══════════════════════════════════════════════════════════════
    // ESPACE PARENT
    // ═══════════════════════════════════════════════════════════════

    // Route d'activation (obsolète si on utilise directement ParentHouseholdHome)
    const val ParentActivation = "parent/activation"

    // Écran principal : sélection des enfants du foyer
    const val ParentHouseholdHome = "parent/household"

    // Profil d'un adhérent (enfant)
    const val ArgAdherentId = "adherentId"
    const val ParentAdherentProfile = "parent/adherent/{$ArgAdherentId}"
    fun parentAdherentProfile(adherentId: String) = "parent/adherent/$adherentId"

    // Cotisations du foyer
    const val ParentPayment = "parent/payment"

    // Messages du club (pour les parents)
    const val ParentMessages = "parent/messages"

    // Profil du parent
    const val ParentProfile = "parent/profile"

    // Ancien ParentHome (peut être supprimé ou redirigé vers ParentHouseholdHome)
    @Deprecated("Use ParentHouseholdHome instead")
    const val ParentHome = "parent/home"
}