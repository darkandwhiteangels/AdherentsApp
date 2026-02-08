package com.antechrist.adherentsapp.domain.model.finance

data class SeasonAdminDoc(
    val seasonKey: String = "",

    // AG
    val agDateMillis: Long? = null,

    // Draft CR (unique)
    val minutesDraftId: String = "",
    val minutesDraftUpdatedAtMillis: Long? = null,
    val minutesDraftUpdatedByUid: String? = null,

    // PDF Draft
    val minutesPdfGeneratedAtMillis: Long? = null,
    val minutesPdfVersion: Int = 0,

    // Approval
    val minutesApprovedAtMillis: Long? = null,
    val minutesApprovedByUid: String? = null,
    val minutesApprovedByRole: String? = null, // PRESIDENT | SECRETARY | SUPERADMIN
)
