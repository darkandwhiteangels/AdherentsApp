package com.antechrist.adherentsapp.ui.screens.form

import android.util.Patterns
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antechrist.adherentsapp.core.AppDefaults.DEFAULT_AVATAR_URL
import com.antechrist.adherentsapp.domain.model.Adherent
import com.antechrist.adherentsapp.domain.repository.AdherentsRepository
import com.antechrist.adherentsapp.domain.usecase.AddAdherentUseCase
import com.antechrist.adherentsapp.domain.usecase.GetAdherentsStreamUseCase
import com.antechrist.adherentsapp.domain.usecase.UpdateAdherentUseCase
import com.antechrist.adherentsapp.domain.usecase.UploadAdherentPhotoForForm
import com.antechrist.adherentsapp.ui.navigation.Destinations
import com.antechrist.adherentsapp.ui.utils.toUcFirstCityFR
import com.antechrist.adherentsapp.ui.utils.toUcFirstNameFR
import com.antechrist.adherentsapp.ui.utils.toUpperNameFR
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

data class FormUiState(
    val id: String? = null,

    // Type d’adhérent (Pratiquant = true / Bureau = false)
    val isPractitioner: Boolean = true,
    val attribution: String? = null,
    // Champs visibles au départ (obligatoires)
    val nom: String = "",
    val prenom: String = "",
    val dateNaissance: String = "",  // JJ/MM/AAAA
    // Optionnels
    val beltCode: String? = null,
    val stripeCount: Int = 0,
    val photoUri: String? = null,
    val photoUpdatedAt: Long? = null,
    // Déduction
    val isMinor: Boolean = false,
    // Champs MAJEUR
    val telephone: String = "",
    val email: String = "",
    val adresse: String = "",
    val codePostal: String = "",
    val ville: String = "",
    // Autorisations ADULTE (si pratiquant)
    val adultConsentAccidentEvac: Boolean = false,
    val adultConsentPhotoSocial: Boolean = false,
    val adultConsentPhotoOfficial: Boolean = false,
    val riAccepted: Boolean = false,
    // Sous-formulaire MINEUR (si pratiquant & mineur)
    val gNom: String = "",
    val gPrenom: String = "",
    val gTelephone: String = "",
    val gEmail: String = "",
    val gRelation: String = "",
    val gAdresse: String = "",
    val gCodePostal: String = "",
    val gVille: String = "",
    // Autorisations pour le mineur
    val minorConsentAccidentEvac: Boolean = false,
    val minorConsentPhotoSocial: Boolean = false,
    val minorConsentPhotoOfficial: Boolean = false,
    val gRiAccepted: Boolean = false,
    // Lookup & sélection guardian
    val matchedGuardianId: String? = null,     // id trouvé par lookup (email/tel)
    val matchedGuardianLabel: String? = null,  // petite info (ex: "Responsable existant chargé")
    val loading: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false,
    val selectedGroupKeys: Set<String> = emptySet()
)

@HiltViewModel
class AdherentFormViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val getStream: GetAdherentsStreamUseCase,
    private val addUseCase: AddAdherentUseCase,
    private val updateUseCase: UpdateAdherentUseCase,
    private val repo: AdherentsRepository,
    private val uploadPhotoForForm: UploadAdherentPhotoForForm,
    private val db: FirebaseFirestore
) : ViewModel() {

    private val _ui = MutableStateFlow(FormUiState())
    val ui: StateFlow<FormUiState> = _ui

    private val editId: String? = savedState.get<String>(Destinations.ArgId)

    /** Catégories normalisées à écrire (1 élément minimum pour pratiquant) */
    private val pendingGroups: MutableSet<String> = linkedSetOf()

    // Debounce pour lookup guardian pendant la saisie
    private var guardianLookupJob: Job? = null

    init {
        if (!editId.isNullOrBlank()) {
            viewModelScope.launch {
                try {
                    _ui.update { it.copy(loading = true, id = editId) }
                    val list = getStream().first()
                    val a = list.firstOrNull { it.id == editId }
                    if (a != null) {
                        // Préremplissage multi : groups (1..2) sinon fallback unique depuis groupe
                        pendingGroups.clear()
                        if (a.groups?.isNotEmpty() == true) {
                            pendingGroups.addAll(a.groups.take(2))
                        }

                        val consents = readConsentsMap(a.id)
                        var gData: Map<String, Any?> = emptyMap()
                        val gid = a.primaryGuardianId ?: a.guardianIds?.firstOrNull()
                        if (!gid.isNullOrBlank()) gData = readGuardianMap(gid)

                        _ui.update {
                            it.copy(
                                id = a.id,
                                isPractitioner = a.isPractitioner,
                                attribution = a.attribution,
                                nom = a.nom,
                                prenom = a.prenom,
                                dateNaissance = a.dateNaissance.orEmpty(),
                                beltCode = a.beltCode,
                                stripeCount = a.stripeCount ?: 0,
                                photoUri = a.photoUri,
                                photoUpdatedAt = a.photoUpdatedAt,
                                isMinor = isMinorFrom(a.dateNaissance),

                                telephone = a.telephone.orEmpty(),
                                email = a.email.orEmpty(),
                                adresse = a.adresse.orEmpty(),
                                codePostal = a.codePostal.orEmpty(),
                                ville = a.ville.orEmpty(),

                                adultConsentAccidentEvac = consents["accidentEvac"] as? Boolean ?: false,
                                adultConsentPhotoSocial = consents["photoSocial"] as? Boolean ?: false,
                                adultConsentPhotoOfficial = consents["photoOfficial"] as? Boolean ?: false,
                                riAccepted = consents["riAccepted"] as? Boolean ?: false,

                                minorConsentAccidentEvac = consents["minorAccidentEvac"] as? Boolean ?: false,
                                minorConsentPhotoSocial = consents["minorPhotoSocial"] as? Boolean ?: false,
                                minorConsentPhotoOfficial = consents["minorPhotoOfficial"] as? Boolean ?: false,
                                gRiAccepted = consents["gRiAccepted"] as? Boolean ?: false,

                                gNom = (gData["nom"] as? String).orEmpty(),
                                gPrenom = (gData["prenom"] as? String).orEmpty(),
                                gTelephone = (gData["telephone"] as? String).orEmpty(),
                                gEmail = (gData["email"] as? String).orEmpty(),
                                gRelation = (gData["relation"] as? String).orEmpty(),
                                gAdresse = (gData["adresse"] as? String).orEmpty(),
                                gCodePostal = (gData["codePostal"] as? String).orEmpty(),
                                gVille = (gData["ville"] as? String).orEmpty(),

                                loading = false,
                                error = null,
                                selectedGroupKeys = pendingGroups.toSet(),
                                matchedGuardianId = gid,
                                matchedGuardianLabel = if (!gid.isNullOrBlank()) "Responsable existant chargé" else null
                            )
                        }
                    } else {
                        _ui.update { it.copy(loading = false, error = "Adhérent introuvable") }
                    }
                } catch (e: Exception) {
                    _ui.update { it.copy(loading = false, error = e.message ?: "Erreur de chargement") }
                }
            }
        }
    }

    // ===== Setters =====
    fun onChangeNom(v: String) = _ui.update { it.copy(nom = v, error = null) }

    fun onChangePrenom(v: String) = _ui.update { it.copy(prenom = v, error = null) }

    fun onChangeDateNaissance(v: String) { _ui.update { it.copy(dateNaissance = v, isMinor = isMinorFrom(v)) } }

    fun onToggleGroup(key: String, checked: Boolean) {
        if (checked) {
            if (pendingGroups.size < 2 || pendingGroups.contains(key)) {
                pendingGroups.add(key)
            }
        } else {
            pendingGroups.remove(key)
        }
        _ui.update { it.copy(selectedGroupKeys = pendingGroups.toSet()) }
    }

    fun onToggleIsPractitioner(b: Boolean) = _ui.update { it.copy(isPractitioner = b) }
    fun onChangeAttribution(v: String?) = _ui.update { it.copy(attribution = v) }

    fun onChangeBeltCode(code: String?) = _ui.update { it.copy(beltCode = code) }
    fun onChangeStripeCount(n: Int) = _ui.update { it.copy(stripeCount = n.coerceIn(0, 3)) }

    fun onChangeTelephone(v: String) = _ui.update { it.copy(telephone = v) }
    fun onChangeEmail(v: String) = _ui.update { it.copy(email = v) }
    fun onChangeAdresse(v: String) = _ui.update { it.copy(adresse = v) }
    fun onChangeCodePostal(v: String) = _ui.update { it.copy(codePostal = v) }
    fun onChangeVille(v: String) = _ui.update { it.copy(ville = toUcFirstCityFR(v) ?: "") }

    fun onToggleAdultAccidentEvac(b: Boolean) = _ui.update { it.copy(adultConsentAccidentEvac = b) }
    fun onToggleAdultPhotoSocial(b: Boolean) = _ui.update { it.copy(adultConsentPhotoSocial = b) }
    fun onToggleAdultPhotoOfficial(b: Boolean) = _ui.update { it.copy(adultConsentPhotoOfficial = b) }
    fun onToggleRiAccepted(b: Boolean) = _ui.update { it.copy(riAccepted = b) }

    fun onChangeGNom(v: String) = _ui.update { it.copy(gNom = v) }
    fun onChangeGPrenom(v: String) = _ui.update { it.copy(gPrenom = v) }

    fun onChangeGTelephone(v: String) {
        _ui.update { it.copy(gTelephone = v) }
        scheduleGuardianLookup()
    }

    fun onChangeGEmail(v: String) {
        _ui.update { it.copy(gEmail = v) }
        scheduleGuardianLookup()
    }

    fun onChangeGRelation(rel: String) = _ui.update { it.copy(gRelation = rel) }
    fun onChangeGAdresse(v: String) = _ui.update { it.copy(gAdresse = v) }
    fun onChangeGCodePostal(v: String) = _ui.update { it.copy(gCodePostal = v) }
    fun onChangeGVille(v: String) = _ui.update { it.copy(gVille = toUcFirstCityFR(v) ?: "") }

    fun onToggleMinorAccidentEvac(b: Boolean) = _ui.update { it.copy(minorConsentAccidentEvac = b) }
    fun onToggleMinorPhotoSocial(b: Boolean) = _ui.update { it.copy(minorConsentPhotoSocial = b) }
    fun onToggleMinorPhotoOfficial(b: Boolean) = _ui.update { it.copy(minorConsentPhotoOfficial = b) }
    fun onToggleGuardianRiAccepted(b: Boolean) = _ui.update { it.copy(gRiAccepted = b) }

    fun onPhotoSelected(localUriString: String?) {
        if (localUriString.isNullOrBlank()) return
        viewModelScope.launch {
            try {
                val httpsUrl = uploadPhotoForForm(editId, localUriString.toUri())
                _ui.update { it.copy(photoUri = httpsUrl, photoUpdatedAt = System.currentTimeMillis()) }
            } catch (e: Exception) {
                _ui.update { it.copy(error = e.message ?: "Échec de l’upload photo") }
            }
        }
    }

    fun save() {
        val s = _ui.value

        // ===== Validations de base =====
        if (s.nom.isBlank() || s.prenom.isBlank()) { fail("Nom et prénom sont obligatoires"); return }
        if (s.dateNaissance.isBlank()) { fail("La date de naissance est obligatoire"); return }

        // Catégorie obligatoire UNIQUEMENT si pratiquant
        val finalGroups = when {
            s.isPractitioner -> {
                val sel = pendingGroups.toList().take(2)
                if (sel.isEmpty()) {
                    fail("La catégorie est obligatoire")
                    return
                }
                sel
            }
            else -> null
        }

        if (s.isPractitioner) {
            if (s.isMinor) {
                if (s.gNom.isBlank() || s.gPrenom.isBlank()) { fail("Nom et prénom du responsable sont obligatoires"); return }
                if (s.gTelephone.isBlank() || s.gEmail.isBlank()) { fail("Téléphone et Email du responsable sont obligatoires"); return }
                if (!Patterns.EMAIL_ADDRESS.matcher(s.gEmail).matches()) { fail("Email du responsable invalide"); return }
                if (s.gRelation.isBlank()) { fail("Le lien de parenté est obligatoire"); return }
                if (!s.gRiAccepted) { fail("Le responsable doit accepter le Règlement Intérieur"); return }
            } else {
                if (s.telephone.isBlank() || s.email.isBlank()) { fail("Téléphone et Email sont obligatoires"); return }
                if (!Patterns.EMAIL_ADDRESS.matcher(s.email).matches()) { fail("Email invalide"); return }
                if (!s.riAccepted) { fail("Vous devez accepter le Règlement Intérieur"); return }
            }
        } else {
            if (s.telephone.isBlank() || s.email.isBlank()) { fail("Téléphone et Email sont obligatoires"); return }
            if (!Patterns.EMAIL_ADDRESS.matcher(s.email).matches()) { fail("Email invalide"); return }
        }

        viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null) }
            try {
                if (((!s.isPractitioner) || (!s.isMinor)) && s.email.isNotBlank()) {
                    val dup = repo.existsByEmail(s.email.trim(), excludeId = s.id)
                    if (dup) { _ui.update { it.copy(loading = false, error = "Un adhérent utilise déjà cet email") }; return@launch }
                }

                val nomUpper = toUpperNameFR(s.nom) ?: ""
                val prenomUc = toUcFirstNameFR(s.prenom) ?: ""
                val emailLower = s.email.trim().lowercase(Locale.ROOT)

                val uiPhoto = s.photoUri?.takeIf { it.isNotBlank() }
                val finalPhotoUri = if (s.id == null && uiPhoto.isNullOrBlank()) DEFAULT_AVATAR_URL else uiPhoto
                val finalPhotoUpdatedAt = if (s.id == null && uiPhoto.isNullOrBlank()) null else s.photoUpdatedAt

                val nowTs = FieldValue.serverTimestamp()
                val nowMs = System.currentTimeMillis()

                if (!s.id.isNullOrBlank()) {
                    // ===== EDIT =====

                    // Lire l'existant
                    val snap = db.collection("adherents").document(s.id).get().await()
                    val existingGuardianIds: List<String> =
                        (snap.get("guardianIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                    val existingPrimaryGuardianId: String? = snap.getString("primaryGuardianId")
                    val existingHouseholdId: String? = snap.getString("householdId")

                    // Choisir (ou upsert) le guardian cible si mineur pratiquant
                    var targetGuardianId: String? = existingPrimaryGuardianId
                    if (s.isPractitioner && s.isMinor) {
                        targetGuardianId = upsertGuardianFromForm(s, nowTs) // évite doublons
                    } else if (!s.matchedGuardianId.isNullOrBlank()) {
                        // Si l'utilisateur a déclenché un match pendant l'édition, honorons-le
                        targetGuardianId = s.matchedGuardianId
                    }

                    // Fusionner la liste des guardians (dédupliquée)
                    val mergedGuardianIds: List<String> = buildList {
                        addAll(existingGuardianIds)
                        if (!targetGuardianId.isNullOrBlank()) add(targetGuardianId)
                    }.distinct()

                    // Finaliser les liens principaux
                    val finalPrimaryGuardianId = targetGuardianId ?: existingPrimaryGuardianId
                    val finalHouseholdId = existingHouseholdId ?: finalPrimaryGuardianId

                    val adherent = Adherent(
                        id = s.id,
                        nom = nomUpper,
                        prenom = prenomUc,
                        dateNaissance = s.dateNaissance.trim(),
                        groups = finalGroups,
                        // Coordonnées : pour mineur pratiquant on masque ; sinon optionnelles (peuvent rester vides)
                        adresse = if (s.isPractitioner && s.isMinor) null else s.adresse.trim(),
                        codePostal = if (s.isPractitioner && s.isMinor) null else s.codePostal.trim(),
                        ville = if (s.isPractitioner && s.isMinor) null else s.ville.trim(),
                        email = if (s.isPractitioner && s.isMinor) null else emailLower,
                        telephone = if (s.isPractitioner && s.isMinor) null else s.telephone.trim(),
                        photoUri = finalPhotoUri,
                        photoUpdatedAt = finalPhotoUpdatedAt,
                        beltCode = s.beltCode,
                        stripeCount = s.stripeCount.coerceIn(0, 3),
                        isPractitioner = s.isPractitioner,

                        guardianIds = mergedGuardianIds,
                        primaryGuardianId = finalPrimaryGuardianId,
                        householdId = finalHouseholdId,

                        attribution = s.attribution
                    )
                    updateUseCase(adherent)

                    if (s.isPractitioner) {
                        val acceptedByGuardian = if (s.isMinor) readPrimaryGuardianIdFor(s.id) else null
                        val patchConsents = consentsMapFor(s, acceptedByGuardianId = acceptedByGuardian).toMutableMap()
                        if (patchConsents["riAcceptedAt"] == null) patchConsents["riAcceptedAt"] = nowMs

                        db.collection("adherents").document(s.id)
                            .set(mapOf("consents" to patchConsents, "updatedAt" to nowTs), SetOptions.merge())
                            .await()
                    } else {
                        db.collection("adherents").document(s.id)
                            .set(mapOf("updatedAt" to nowTs), SetOptions.merge())
                            .await()
                    }
                } else {
                    // ===== CREATE =====
                    var guardianId: String? = null
                    if (s.isPractitioner && s.isMinor) {
                        // Upsert du responsable (évite doublons par email/tel)
                        guardianId = upsertGuardianFromForm(s, nowTs)
                    } else if (!s.matchedGuardianId.isNullOrBlank()) {
                        guardianId = s.matchedGuardianId
                    }

                    val aRef = db.collection("adherents").document()
                    val consentsForCreate =
                        if (s.isPractitioner)
                            consentsMapFor(s, acceptedByGuardianId = guardianId).toMutableMap().apply {
                                if (this["riAcceptedAt"] == null) this["riAcceptedAt"] = nowMs
                            }
                        else emptyMap<String, Any?>()

                    val aData = hashMapOf<String, Any?>(
                        "nom" to nomUpper,
                        "prenom" to prenomUc,
                        "dateNaissance" to s.dateNaissance.trim(),
                        "groups" to finalGroups, // null si non pratiquant
                        // Coordonnées : optionnelles (sauf mineur pratiquant où l’on masque)
                        "adresse" to if (s.isPractitioner && s.isMinor) null else s.adresse.trim().ifBlank { null },
                        "codePostal" to if (s.isPractitioner && s.isMinor) null else s.codePostal.trim().ifBlank { null },
                        "ville" to if (s.isPractitioner && s.isMinor) null else toUcFirstCityFR(s.ville),
                        "email" to if (s.isPractitioner && s.isMinor) null else emailLower.ifBlank { null },
                        "telephone" to if (s.isPractitioner && s.isMinor) null else s.telephone.trim().ifBlank { null },

                        "photoUri" to (finalPhotoUri ?: DEFAULT_AVATAR_URL),
                        "photoUpdatedAt" to finalPhotoUpdatedAt,
                        "beltCode" to s.beltCode,
                        "stripeCount" to s.stripeCount.coerceIn(0, 3),

                        "isPractitioner" to s.isPractitioner,
                        "attribution" to s.attribution,

                        "cotisationPaid" to false,
                        "isArchived" to false,

                        "primaryGuardianId" to guardianId,
                        "guardianIds" to (guardianId?.let { listOf(it) } ?: emptyList<String>()),
                        "householdId" to guardianId,

                        "updatedAt" to nowTs,

                        "consents" to consentsForCreate
                    )
                    aRef.set(aData).await()
                }

                _ui.update { it.copy(loading = false, saved = true) }
            } catch (e: Exception) {
                _ui.update { it.copy(loading = false, error = e.message ?: "Erreur lors de l'enregistrement") }
            }
        }
    }

    // ===== Helpers =====

    private fun fail(msg: String) { _ui.update { it.copy(error = msg) } }

    private fun isMinorFrom(dob: String?): Boolean {
        val age = computeAgeYears(dob)
        return age != null && age < 18
    }

    private fun computeAgeYears(dob: String?): Int? {
        if (dob.isNullOrBlank()) return null
        return try {
            val fmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val birth = fmt.parse(dob) ?: return null
            val calBirth = Calendar.getInstance().apply { time = birth }
            val calNow = Calendar.getInstance()
            var years = calNow.get(Calendar.YEAR) - calBirth.get(Calendar.YEAR)
            val beforeBirthday = calNow.get(Calendar.DAY_OF_YEAR) < calBirth.get(Calendar.DAY_OF_YEAR)
            if (beforeBirthday) years -= 1
            years
        } catch (_: Exception) { null }
    }

    private fun groupKeyOf(display: String): String = when (display.trim()) {
        "Baby"             -> "baby"
        "Enfants < 14 ans" -> "enfant_u14"
        "14+ / Adultes"    -> "adult_14p"
        else -> display.lowercase(Locale.ROOT)
            .replace("[^a-z0-9]+".toRegex(), "_")
            .trim('_')
    }

    // ----- Consents helpers -----

    private suspend fun readConsentsMap(adherentId: String): Map<String, Any?> {
        return try {
            val snap = db.collection("adherents").document(adherentId).get().await()
            (snap.get("consents") as? Map<*, *>)?.mapNotNull { (k, v) ->
                (k as? String)?.let { it to v }
            }?.toMap() ?: emptyMap()
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private suspend fun readGuardianMap(guardianId: String): Map<String, Any?> {
        return try {
            val snap = db.collection("guardians").document(guardianId).get().await()
            if (!snap.exists()) emptyMap() else snap.data ?: emptyMap()
        } catch (_: Exception) { emptyMap() }
    }

    private suspend fun readPrimaryGuardianIdFor(adherentId: String): String? {
        return try {
            val snap = db.collection("adherents").document(adherentId).get().await()
            snap.getString("primaryGuardianId")
        } catch (_: Exception) { null }
    }

    private fun consentsMapFor(s: FormUiState, acceptedByGuardianId: String?): Map<String, Any?> {
        if (!s.isPractitioner) return emptyMap()
        val now = System.currentTimeMillis()
        return if (s.isMinor) {
            mapOf(
                "minorAccidentEvac" to s.minorConsentAccidentEvac,
                "minorPhotoSocial" to s.minorConsentPhotoSocial,
                "minorPhotoOfficial" to s.minorConsentPhotoOfficial,
                "gRiAccepted" to s.gRiAccepted,
                "riAcceptedAt" to now,
                "acceptedByGuardianId" to acceptedByGuardianId
            )
        } else {
            mapOf(
                "accidentEvac" to s.adultConsentAccidentEvac,
                "photoSocial" to s.adultConsentPhotoSocial,
                "photoOfficial" to s.adultConsentPhotoOfficial,
                "riAccepted" to s.riAccepted,
                "riAcceptedAt" to now
            )
        }
    }

    // ====== Guardian: lookup & upsert ======

    private fun normEmail(s: String?) = s?.trim()?.lowercase(Locale.ROOT).orEmpty()
    private fun normTel(s: String?) = s?.filter { it.isDigit() } ?: ""

    /** Déclenche un lookup auto (debounce) à chaque saisie email/tel du responsable */
    private fun scheduleGuardianLookup(delayMs: Long = 350L) {
        guardianLookupJob?.cancel()
        guardianLookupJob = viewModelScope.launch {
            delay(delayMs)
            val s = ui.value
            if (!s.isPractitioner || !s.isMinor) return@launch
            val id = findGuardianIdByEmailOrTel(s.gEmail, s.gTelephone)
            if (id.isNullOrBlank()) {
                // pas de match → pas de pré-remplissage
                _ui.update { it.copy(matchedGuardianId = null, matchedGuardianLabel = null) }
                return@launch
            }
            val g = readGuardianMap(id)
            _ui.update {
                it.copy(
                    matchedGuardianId = id,
                    matchedGuardianLabel = "Responsable existant chargé",
                    gNom = (g["nom"] as? String).orEmpty().ifBlank { it.gNom },
                    gPrenom = (g["prenom"] as? String).orEmpty().ifBlank { it.gPrenom },
                    gTelephone = (g["telephone"] as? String).orEmpty().ifBlank { it.gTelephone },
                    gEmail = (g["email"] as? String).orEmpty().ifBlank { it.gEmail },
                    gRelation = (g["relation"] as? String).orEmpty().ifBlank { it.gRelation },
                    gAdresse = (g["adresse"] as? String).orEmpty().ifBlank { it.gAdresse },
                    gCodePostal = (g["codePostal"] as? String).orEmpty().ifBlank { it.gCodePostal },
                    gVille = (g["ville"] as? String).orEmpty().ifBlank { it.gVille }
                )
            }
        }
    }

    /** Recherche d’un guardian existant par email ou téléphone normalisés */
    private suspend fun findGuardianIdByEmailOrTel(email: String?, tel: String?): String? {
        val emailLower = normEmail(email)
        val telNorm = normTel(tel)

        // 1) Match par email normalisé (nouvelle structure)
        if (emailLower.isNotEmpty()) {
            val q = db.collection("guardians")
                .whereEqualTo("emailLower", emailLower)
                .limit(1).get().await()
            q.documents.firstOrNull()?.let { return it.id }
        }

        // 2) Match par email brut (anciens docs sans emailLower)
        if (emailLower.isNotEmpty()) {
            val q = db.collection("guardians")
                .whereEqualTo("email", emailLower) // on compare déjà en lower
                .limit(1).get().await()
            q.documents.firstOrNull()?.let { return it.id }
        }

        // 3) Match par téléphone normalisé (nouvelle structure)
        if (telNorm.isNotEmpty()) {
            val q = db.collection("guardians")
                .whereEqualTo("telephoneNorm", telNorm)
                .limit(1).get().await()
            q.documents.firstOrNull()?.let { return it.id }
        }

        // 4) Match par téléphone brut (anciens docs sans telephoneNorm)
        val rawTel = tel?.trim().orEmpty()
        if (rawTel.isNotEmpty()) {
            val q = db.collection("guardians")
                .whereEqualTo("telephone", rawTel)
                .limit(1).get().await()
            q.documents.firstOrNull()?.let { return it.id }
        }

        return null
    }

    /** Map d’écriture d’un guardian depuis les champs du formulaire */
    private fun guardianWriteMapFromForm(s: FormUiState, nowTs: FieldValue): Map<String, Any?> {
        val emailLower = normEmail(s.gEmail).ifBlank { null }
        val telNorm = normTel(s.gTelephone).ifBlank { null }

        return hashMapOf(
            "nom" to toUpperNameFR(s.gNom),
            "prenom" to toUcFirstNameFR(s.gPrenom),
            "telephone" to s.gTelephone.trim().ifBlank { null },
            "telephoneNorm" to telNorm,                     // ← normalisé
            "email" to s.gEmail.trim().lowercase().ifBlank { null },
            "emailLower" to emailLower,                     // ← normalisé
            "relation" to s.gRelation.ifBlank { null },
            "adresse" to s.gAdresse.trim().ifBlank { null },
            "codePostal" to s.gCodePostal.trim().ifBlank { null },
            "ville" to toUcFirstCityFR(s.gVille),
            "updatedAt" to nowTs
        )
    }

    /** Upsert guardian : trouve par email/tel, sinon crée ; renvoie l’id */
    private suspend fun upsertGuardianFromForm(s: FormUiState, nowTs: FieldValue): String {
        // 1) Lookup tolérant (voit anciens et nouveaux docs)
        var targetId = findGuardianIdByEmailOrTel(s.gEmail, s.gTelephone)

        // 2) Si rien trouvé, on tente UNE seconde fois après avoir normalisé localement les inputs
        //    (utile si l’utilisateur a modifié la casse/espaces entre deux frappes)
        if (targetId.isNullOrBlank()) {
            targetId = findGuardianIdByEmailOrTel(
                s.gEmail.trim().lowercase(Locale.ROOT),
                normTel(s.gTelephone)
            )
        }

        // 3) Si toujours rien : on crée un ID
        val gId = targetId ?: db.collection("guardians").document().id
        val gRef = db.collection("guardians").document(gId)

        // 4) On merge les champs (et on ajoute createdAt si création)
        val base = guardianWriteMapFromForm(s, nowTs).toMutableMap()
        if (targetId == null) base["createdAt"] = nowTs

        // ⚠️ Important : on remet à jour emailLower/telephoneNorm même pour les anciens docs
        gRef.set(base, SetOptions.merge()).await()

        return gId
    }
}
