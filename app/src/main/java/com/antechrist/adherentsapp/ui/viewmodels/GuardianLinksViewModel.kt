package com.antechrist.adherentsapp.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.antechrist.adherentsapp.domain.model.Guardian
import com.antechrist.adherentsapp.domain.usecase.CreateGuardianUseCase
import com.antechrist.adherentsapp.domain.usecase.FindGuardianByEmailOrTelUseCase
import com.antechrist.adherentsapp.domain.usecase.LinkGuardianToAdherentUseCase
import com.antechrist.adherentsapp.domain.usecase.SetPrimaryGuardianUseCase
import com.antechrist.adherentsapp.domain.usecase.UnlinkGuardianFromAdherentUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel

@HiltViewModel
class GuardianLinksViewModel @Inject constructor(
    private val findGuardian: FindGuardianByEmailOrTelUseCase,
    private val createGuardian: CreateGuardianUseCase,
    private val linkGuardian: LinkGuardianToAdherentUseCase,
    private val unlinkGuardian: UnlinkGuardianFromAdherentUseCase,
    private val setPrimary: SetPrimaryGuardianUseCase
) : ViewModel() {

    suspend fun findByEmailOrTel(email: String?, telDigits: String?): Guardian? =
        withContext(Dispatchers.IO) { findGuardian(email, telDigits) }

    suspend fun create(
        nom: String,
        prenom: String,
        email: String?,
        telDigits: String?,
        relation: String?
    ): String = withContext(Dispatchers.IO) { createGuardian(nom, prenom, email, telDigits, relation) }

    suspend fun link(adherentId: String, guardianId: String, setPrimaryIfEmpty: Boolean) =
        withContext(Dispatchers.IO) { linkGuardian(adherentId, guardianId, setPrimaryIfEmpty) }

    suspend fun unlink(adherentId: String, guardianId: String) =
        withContext(Dispatchers.IO) { unlinkGuardian(adherentId, guardianId) }

    suspend fun makePrimary(adherentId: String, guardianId: String) =
        withContext(Dispatchers.IO) { setPrimary(adherentId, guardianId) }
}
