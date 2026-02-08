package com.antechrist.adherentsapp.ui.screens.guardian

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antechrist.adherentsapp.domain.model.Guardian
import com.antechrist.adherentsapp.domain.usecase.GetGuardianByIdUseCase
import com.antechrist.adherentsapp.domain.usecase.UpdateGuardianUseCase
import com.antechrist.adherentsapp.ui.utils.isValidEmail
import com.antechrist.adherentsapp.ui.utils.isValidPhoneFRDigits
import com.antechrist.adherentsapp.ui.utils.normalizeEmailLower
import com.antechrist.adherentsapp.ui.utils.phoneDigitsOnly
import com.antechrist.adherentsapp.ui.utils.toUcFirstCityFR
import com.antechrist.adherentsapp.ui.utils.toUcFirstNameFR
import com.antechrist.adherentsapp.ui.utils.toUcFirstRelationFR
import com.antechrist.adherentsapp.ui.utils.toUpperNameFR
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class GuardianEditViewModel @Inject constructor(
    private val getGuardianById: GetGuardianByIdUseCase,
    private val updateGuardian: UpdateGuardianUseCase
) : ViewModel() {

    data class UiState(
        val loading: Boolean = true,
        val saving: Boolean = false,
        val error: String? = null,
        val success: Boolean = false,
        val guardianId: String = "",
        val lastName: String = "",
        val firstName: String = "",
        val relation: String = "",
        val phoneDigits: String = "",
        val email: String = "",
        val address: String = "",
        val postalCode: String = "",
        val city: String = "",
        val eLast: String? = null,
        val eFirst: String? = null,
        val eRelation: String? = null,
        val ePhone: String? = null,
        val eEmail: String? = null,
        val ePostal: String? = null,
        val eCity: String? = null
    )

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    fun load(guardianId: String) {
        _ui.update { UiState(guardianId = guardianId, loading = true) }
        viewModelScope.launch {
            try {
                val g = getGuardianById(guardianId)
                if (g == null) {
                    _ui.update { it.copy(loading = false, error = "Responsable introuvable.") }
                } else {
                    _ui.update {
                        it.copy(
                            loading = false,
                            error = null,
                            lastName = g.nom.orEmpty(),
                            firstName = g.prenom.orEmpty(),
                            relation = g.relation.orEmpty(),
                            phoneDigits = phoneDigitsOnly(g.telephone).take(10),
                            email = g.email.orEmpty(),
                            address = g.adresse.orEmpty(),
                            postalCode = g.codePostal.orEmpty().filter { c -> c.isDigit() }.take(5),
                            city = g.ville.orEmpty()
                        )
                    }
                }
            } catch (e: Exception) {
                _ui.update { it.copy(loading = false, error = e.message ?: "Erreur chargement") }
            }
        }
    }

    fun onLastName(v: String) { _ui.update { it.copy(lastName = v) } }
    fun onFirstName(v: String) { _ui.update { it.copy(firstName = v) } }
    fun onRelation(v: String) { _ui.update { it.copy(relation = v) } }
    fun onPhone(v: String) { _ui.update { it.copy(phoneDigits = phoneDigitsOnly(v).take(10)) } }
    fun onEmail(v: String) { _ui.update { it.copy(email = v.trim().lowercase()) } }
    fun onAddress(v: String) { _ui.update { it.copy(address = v) } }
    fun onPostalCode(v: String) { _ui.update { it.copy(postalCode = v.filter { it.isDigit() }.take(5)) } }
    fun onCity(v: String) { _ui.update { it.copy(city = v) } }
    fun consumeSuccess() { if (_ui.value.success) _ui.update { it.copy(success = false) } }

    fun save() {
        val s = _ui.value
        val normLast = toUpperNameFR(s.lastName).orEmpty()
        val normFirst = toUcFirstNameFR(s.firstName).orEmpty()
        val normRelation = toUcFirstRelationFR(s.relation).orEmpty()
        val normEmail = normalizeEmailLower(s.email).orEmpty()
        val normCity = toUcFirstCityFR(s.city).orEmpty()
        val phone = phoneDigitsOnly(s.phoneDigits)
        val postal = s.postalCode.filter { it.isDigit() }

        val eLast = if (normLast.isBlank()) "Nom requis" else null
        val eFirst = if (normFirst.isBlank()) "Prénom requis" else null
        val eRel = if (normRelation.isBlank()) "Relation requise" else null
        val ePhone = if (!isValidPhoneFRDigits(phone)) "Téléphone invalide (10 chiffres)" else null
        val eMail = if (!isValidEmail(normEmail)) "Email invalide" else null
        val ePostal = if (postal.isNotEmpty() && postal.length != 5) "Code postal invalide" else null

        val hasError = listOf(eLast, eFirst, eRel, ePhone, eMail, ePostal).any { it != null }
        if (hasError) {
            _ui.update { it.copy(eLast = eLast, eFirst = eFirst, eRelation = eRel, ePhone = ePhone, eEmail = eMail, ePostal = ePostal) }
            return
        }

        _ui.update { it.copy(saving = true, error = null, eLast = null, eFirst = null, eRelation = null, ePhone = null, eEmail = null, ePostal = null) }

        viewModelScope.launch {
            try {
                val updated = Guardian(
                    id = s.guardianId,
                    nom = normLast,
                    prenom = normFirst,
                    relation = normRelation,
                    telephone = phone,
                    email = normEmail,
                    adresse = s.address.trim(),
                    codePostal = postal,
                    ville = normCity
                )
                updateGuardian(updated)
                _ui.update { it.copy(saving = false, success = true) }
            } catch (e: Exception) {
                _ui.update { it.copy(saving = false, error = e.message ?: "Erreur sauvegarde") }
            }
        }
    }
}
