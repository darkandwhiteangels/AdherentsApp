package com.antechrist.adherentsapp.domain.model

enum class Role {
    SUPER_ADMIN,
    ADMIN,
    BUREAU,
    MEMBRE_BUREAU,   // nouveau
    TRESORIER,
    SECRETAIRE,
    PROFESSEUR,      // nouveau
    JUGE_GRADE,      // nouveau
    PARENT,          // nouveau
    MEMBER
}

fun roleFromString(v: String?): Role = when (v?.trim()?.lowercase()) {
    "super_admin", "superadmin", "super-admin" -> Role.SUPER_ADMIN
    "admin" -> Role.ADMIN
    "bureau" -> Role.BUREAU
    "membre_bureau" -> Role.MEMBRE_BUREAU
    "tresorier", "trésorier" -> Role.TRESORIER
    "secretaire", "secrétaire" -> Role.SECRETAIRE
    "professeur" -> Role.PROFESSEUR
    "juge_grade", "juge", "judge", "juge-grade" -> Role.JUGE_GRADE
    "parent" -> Role.PARENT
    "member", "membre", "adherent", "adhérent" -> Role.MEMBER
    else -> Role.MEMBER
}

fun Role.isStaff(): Boolean = when (this) {
    Role.SUPER_ADMIN,
    Role.ADMIN,
    Role.BUREAU,
    Role.MEMBRE_BUREAU,
    Role.TRESORIER,
    Role.SECRETAIRE,
    Role.PROFESSEUR -> true
    else -> false
}

fun Role.isSuperAdmin(): Boolean = when (this) {
    Role.SUPER_ADMIN -> true
    else -> false
}

fun Role.isAdmin(): Boolean = when (this) {
    Role.ADMIN -> true
    else -> false
}

fun Role.isFinanceManager(): Boolean = when (this) {
    Role.SUPER_ADMIN, Role.ADMIN, Role.TRESORIER -> true
    else -> false
}

fun Role?.canSendCertificates(): Boolean = when (this) {
    Role.SUPER_ADMIN, Role.ADMIN, Role.SECRETAIRE -> true
    else -> false
}

fun Role?.canOpenConfig(): Boolean = when (this) {
    Role.SUPER_ADMIN, Role.ADMIN -> true
    else -> false
}

fun Role?.isAuthorizedForApp(): Boolean =
    this?.isStaff() == true || this == Role.TRESORIER || this == Role.SECRETAIRE
