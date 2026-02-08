package com.antechrist.adherentsapp.ui.screens.finance

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antechrist.adherentsapp.core.pdf.MinutesTemplatePdf
import com.antechrist.adherentsapp.data.remote.ClubConfigRemote
import com.antechrist.adherentsapp.ui.utils.FileShare
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.Timestamp
import java.util.Date
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

data class SeasonAdminUi(
    val seasonKey: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val message: String? = null,

    // Option A: stocké en Timestamp dans Firestore
    val agAt: Timestamp? = null,

    val minutesDraftId: String = "",
    val minutesDraftUpdatedAt: Timestamp? = null,
    val minutesDraftUpdatedByUid: String? = null,

    val minutesPdfGeneratedAt: Timestamp? = null,
    val minutesPdfVersion: Int = 0,

    val minutesApprovedAt: Timestamp? = null,
    val minutesApprovedByUid: String? = null,
    val minutesApprovedByName: String? = null,
    val minutesForm: MinutesForm = MinutesForm(),

) {
    private fun agMillis(): Long? = agAt?.toDate()?.time

    fun canGeneratePdf(nowMillis: Long): Boolean {
        val ag = agMillis() ?: return false
        return nowMillis <= (ag - 24L * 3600L * 1000L)
    }

    fun canApprove(nowMillis: Long): Boolean {
        val ag = agMillis() ?: return false
        return nowMillis >= ag
    }
}

data class MinutesForm(
    val trainingsSummary: String = "",
    val technicalFocus: String = "",
    val eventsList: List<String> = emptyList(),
    val sportAssessment: String = "",
    val financialStatus: String = "",
    val financialComment: String = "",
    val issuesSummary: String = "",
    val nextSeasonGoals: String = "",
    val generalConclusion: String = ""
)

@HiltViewModel
class SeasonAdminViewModel @Inject constructor(
    private val db: FirebaseFirestore
) : ViewModel() {

    private val _ui = MutableStateFlow(SeasonAdminUi())
    val ui: StateFlow<SeasonAdminUi> = _ui

    fun load(seasonKey: String) {
        viewModelScope.launch {
            _ui.update { it.copy(seasonKey = seasonKey, loading = true, error = null, message = null) }
            try {
                val doc = db.collection("season_admin").document(seasonKey).get().await()
                val m = doc.data ?: emptyMap<String, Any?>()

                val minutes = m["minutes"] as? Map<*, *>

                val form = MinutesForm(
                    trainingsSummary = minutes?.get("trainingsSummary") as? String ?: "",
                    technicalFocus = minutes?.get("technicalFocus") as? String ?: "",
                    eventsList = (minutes?.get("eventsList") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                    sportAssessment = minutes?.get("sportAssessment") as? String ?: "",
                    financialStatus = minutes?.get("financialStatus") as? String ?: "",
                    financialComment = minutes?.get("financialComment") as? String ?: "",
                    issuesSummary = minutes?.get("issuesSummary") as? String ?: "",
                    nextSeasonGoals = minutes?.get("nextSeasonGoals") as? String ?: "",
                    generalConclusion = minutes?.get("generalConclusion") as? String ?: ""
                )

                _ui.update {
                    it.copy(
                        loading = false,

                        // Option A (Timestamp)
                        agAt = (m["agAt"] as? Timestamp),

                        minutesDraftId = (m["minutesDraftId"] as? String) ?: "",
                        minutesDraftUpdatedAt = (m["minutesDraftUpdatedAt"] as? Timestamp),
                        minutesDraftUpdatedByUid = (m["minutesDraftUpdatedByUid"] as? String),

                        minutesPdfGeneratedAt = (m["minutesPdfGeneratedAt"] as? Timestamp),
                        minutesPdfVersion = (m["minutesPdfVersion"] as? Number)?.toInt() ?: 0,

                        minutesApprovedAt = (m["minutesApprovedAt"] as? Timestamp),
                        minutesApprovedByUid = (m["minutesApprovedByUid"] as? String),
                        minutesApprovedByName = (m["minutesApprovedByName"] as? String),
                        minutesForm = form
                    )
                }
            } catch (e: Exception) {
                _ui.update { it.copy(loading = false, error = e.message ?: "Erreur chargement season_admin") }
            }
        }
    }

    fun saveAgDate(seasonKey: String, agDateMillis: Long) {
        viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null, message = null) }
            try {
                val agAt = Timestamp(Date(agDateMillis))

                db.collection("season_admin").document(seasonKey)
                    .set(
                        mapOf(
                            "seasonKey" to seasonKey,
                            "agAt" to agAt
                        ),
                        SetOptions.merge()
                    ).await()

                _ui.update { it.copy(loading = false, agAt = agAt, message = "Date AG enregistrée ✔") }
            } catch (e: Exception) {
                _ui.update { it.copy(loading = false, error = e.message ?: "Erreur enregistrement AG") }
            }
        }
    }

    fun saveMinutesForm(seasonKey: String, form: MinutesForm) {
        if (seasonKey.isBlank()) return

        val agAt = _ui.value.agAt
        if (agAt == null) {
            _ui.update { it.copy(error = "AG: date AG manquante (définis l'AG avant d'enregistrer le CR)") }
            return
        }

        viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null, message = null) }
            try {
                db.collection("season_admin").document(seasonKey)
                    .set(
                        mapOf(
                            "seasonKey" to seasonKey,
                            "agAt" to agAt, // ✅ IMPORTANT : permet CREATE si doc absent
                            "minutes" to mapOf(
                                "trainingsSummary" to form.trainingsSummary,
                                "technicalFocus" to form.technicalFocus,
                                "eventsList" to form.eventsList,
                                "sportAssessment" to form.sportAssessment,
                                "financialStatus" to form.financialStatus,
                                "financialComment" to form.financialComment,
                                "issuesSummary" to form.issuesSummary,
                                "nextSeasonGoals" to form.nextSeasonGoals,
                                "generalConclusion" to form.generalConclusion
                            )
                        ),
                        SetOptions.merge()
                    ).await()

                _ui.update { it.copy(loading = false, minutesForm = form, message = "CR enregistré ✔") }
            } catch (e: Exception) {
                _ui.update { it.copy(loading = false, error = e.message ?: "Erreur enregistrement CR") }
            }
        }
    }

    fun debugSave() {
        viewModelScope.launch {
            try {
                val user = FirebaseAuth.getInstance().currentUser
                val token = user?.getIdToken(true)?.await()

                Log.d("DEBUG_CR", "=== USER INFO ===")
                Log.d("DEBUG_CR", "UID: ${user?.uid}")
                Log.d("DEBUG_CR", "Email: ${user?.email}")

                Log.d("DEBUG_CR", "=== CUSTOM CLAIMS ===")
                token?.claims?.forEach { (key, value) ->
                    Log.d("DEBUG_CR", "$key: $value")
                }

                Log.d("DEBUG_CR", "=== USER DOC ===")
                val userDoc = db.collection("users").document(user?.uid!!).get().await()
                Log.d("DEBUG_CR", "Exists: ${userDoc.exists()}")
                Log.d("DEBUG_CR", "Data: ${userDoc.data}")

            } catch (e: Exception) {
                Log.e("DEBUG_CR", "Error: ${e.message}", e)
            }
        }
    }

    fun saveAgDetails(
        seasonKey: String,
        location: String?,
        agendaItems: List<String>?,
        notes: String?
    ) {
        viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null, message = null) }
            try {
                val data = mutableMapOf<String, Any?>(
                    "seasonKey" to seasonKey,
                    "agLocation" to location?.takeIf { it.isNotBlank() },
                    "agAgendaItems" to agendaItems?.filter { it.isNotBlank() }?.takeIf { it.isNotEmpty() },
                    "agNotes" to notes?.takeIf { it.isNotBlank() }
                )

                // Firestore n'aime pas trop les nulls, on les enlève
                data.entries.removeIf { it.value == null }

                db.collection("season_admin").document(seasonKey)
                    .set(data, SetOptions.merge())
                    .await()

                _ui.update { it.copy(loading = false, message = "Infos AG enregistrées ✔") }
            } catch (e: Exception) {
                _ui.update { it.copy(loading = false, error = e.message ?: "Erreur enregistrement AG (infos)") }
            }
        }
    }

//    fun generateDraftPdfAndShare(context: Context) {
//        val s = _ui.value
//        val seasonKey = s.seasonKey
//        val agAt = s.agAt
//        val agMillis = agAt?.toDate()?.time
//
//        if (seasonKey.isBlank() || agAt == null || agMillis == null) {
//            _ui.update { it.copy(error = "AG: date AG manquante") }
//            return
//        }
//
//
//        val now = System.currentTimeMillis()
//        if (!s.canGeneratePdf(now)) {
//            _ui.update { it.copy(error = "CR: génération PDF refusée (moins de 24h avant l'AG)") }
//            return
//        }
//
//        viewModelScope.launch {
//            _ui.update { it.copy(loading = true, error = null, message = null) }
//
//            try {
//                val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
//                val club = ClubConfigRemote.get()
//                val clubName = club?.clubName ?: ""
//
//                val draftId = s.minutesDraftId.ifBlank { UUID.randomUUID().toString() }
//                val nextVersion = (s.minutesPdfVersion + 1).coerceAtLeast(1)
//
//                val clubCity = club?.clubCity ?: ""
//                val presidentName = club?.presidentName ?: ""
//                val signatureUrl = club?.presidentSignatureUrl
//
//// Pour l’instant : on met des valeurs “safe”.
//// Ensuite on branchera le vrai formulaire CR complet (minutes.*) + stats adhérents.
//                val values = mapOf(
//                    "associationName" to clubName,
//                    "city" to clubCity,
//                    "seasonKey" to seasonKey,
//
//                    "membersTotal" to "-",
//                    "membersChildren" to "-",
//                    "membersAdults" to "-",
//
//                    "trainingsSummary" to "Entraînements réguliers assurés toute la saison",
//                    "technicalFocus" to "Travail technique (kihon, katas, kumité adapté)",
//
//                    "eventsList" to "-",
//                    "sportAssessment" to "-",
//                    "financialStatus" to "Saine",
//                    "financialComment" to "Cotisations et licences à jour (détail : rapport trésorier).",
//                    "issuesSummary" to "-",
//                    "nextSeasonGoals" to "-",
//                    "generalConclusion" to "Saison positive pour l’association grâce à l’implication de tous.",
//
//                    // Synthèse
//                    "eventsShort" to "-",
//                    "sportAssessmentShort" to "-",
//                    "nextSeasonGoalsShort" to "-",
//                    "generalConclusionShort" to "Saison positive, grâce à l’implication des adhérents, bénévoles et encadrants.",
//
//                    "reportDate" to SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE).format(Date(now)),
//                    "presidentName" to presidentName,
//                    "signatureUrl" to (signatureUrl ?: "")
//                )
//
//// 👉 Tu peux choisir FULL ou SHORT ici.
//// Je te conseille de commencer par SHORT pour l’AG, et on ajoutera un 2e bouton pour FULL.
//                val file = MinutesTemplatePdf.generate(
//                    context = context,
//                    mode = MinutesTemplatePdf.Mode.SHORT,
//                    seasonKey = seasonKey,
//                    draftId = draftId,
//                    version = nextVersion,
//                    generatedAtMillis = now,
//                    values = values,
//                    signatureUrl = signatureUrl
//                )
//
//
//
//                // Persiste meta (même draftId, version++)
//                val nowTs = Timestamp(Date(now))
//
//                db.collection("season_admin").document(seasonKey)
//                    .set(
//                        mapOf(
//                            "seasonKey" to seasonKey,
//                            "agAt" to agAt,
//                            "minutesDraftId" to draftId,
//                            "minutesDraftUpdatedAt" to nowTs,
//                            "minutesDraftUpdatedByUid" to uid,
//                            "minutesPdfGeneratedAt" to nowTs,
//                            "minutesPdfVersion" to nextVersion
//                        ),
//                        SetOptions.merge()
//                    ).await()
//
//
//                _ui.update {
//                    it.copy(
//                        loading = false,
//                        minutesDraftId = draftId,
//                        minutesDraftUpdatedAt = nowTs,
//                        minutesDraftUpdatedByUid = uid,
//                        minutesPdfGeneratedAt = nowTs,
//                        minutesPdfVersion = nextVersion,
//                        message = "PDF draft généré ✔ (v$nextVersion)"
//                    )
//                }
//
//                FileShare.sharePdf(context, file, "Partager CR draft")
//
//            } catch (e: Exception) {
//                _ui.update { it.copy(loading = false, error = e.message ?: "Erreur génération PDF") }
//            }
//        }
//    }

    fun generateDraftPdfShortAndShare(context: Context) {
        generateDraftPdfInternalAndShare(
            context = context,
            mode = MinutesTemplatePdf.Mode.SHORT
        )
    }

    fun generateDraftPdfFullAndShare(context: Context) {
        generateDraftPdfInternalAndShare(
            context = context,
            mode = MinutesTemplatePdf.Mode.FULL
        )
    }

    private fun generateDraftPdfInternalAndShare(
        context: Context,
        mode: MinutesTemplatePdf.Mode
    ) {
        val s = _ui.value
        val seasonKey = s.seasonKey
        val agAt = s.agAt
        val agMillis = agAt?.toDate()?.time

        if (seasonKey.isBlank() || agAt == null || agMillis == null) {
            _ui.update { it.copy(error = "AG: date AG manquante") }
            return
        }

        val now = System.currentTimeMillis()
        if (!s.canGeneratePdf(now)) {
            _ui.update { it.copy(error = "CR: génération PDF refusée (moins de 24h avant l'AG)") }
            return
        }

        viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null, message = null) }

            try {
                val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                val club = ClubConfigRemote.get()
                val clubName = club?.clubName ?: ""
                val clubCity = club?.clubCity ?: ""
                val presidentName = club?.presidentName ?: ""
                val signatureUrl = club?.presidentSignatureUrl

                val draftId = s.minutesDraftId.ifBlank { UUID.randomUUID().toString() }
                val nextVersion = (s.minutesPdfVersion + 1).coerceAtLeast(1)

                // --- Values venant du CR complet (minutesForm) ---
                val form = s.minutesForm

                val eventsListText = form.eventsList
                    .filter { it.isNotBlank() }
                    .joinToString("\n") { "• $it" }
                    .ifBlank { "-" }

                // versions "courtes" pour la synthèse
                fun shortOrDefault(text: String, fallback: String) =
                    text.trim().ifBlank { fallback }.take(400)

                val values = mapOf(
                    "associationName" to clubName,
                    "city" to clubCity,
                    "seasonKey" to seasonKey,

                    // TODO: on branchera les vrais effectifs ensuite
                    "membersTotal" to "-",
                    "membersChildren" to "-",
                    "membersAdults" to "-",

                    "trainingsSummary" to shortOrDefault(form.trainingsSummary, "Entraînements réguliers assurés toute la saison"),
                    "technicalFocus" to shortOrDefault(form.technicalFocus, "Travail technique (kihon, katas, kumité adapté)"),

                    "eventsList" to eventsListText,
                    "sportAssessment" to form.sportAssessment.ifBlank { "-" },
                    "financialStatus" to form.financialStatus.ifBlank { "Saine" },
                    "financialComment" to form.financialComment.ifBlank { "Cotisations et licences à jour (détail : rapport trésorier)." },
                    "issuesSummary" to form.issuesSummary.ifBlank { "-" },
                    "nextSeasonGoals" to form.nextSeasonGoals.ifBlank { "-" },
                    "generalConclusion" to form.generalConclusion.ifBlank { "Saison positive pour l’association grâce à l’implication de tous." },

                    // Synthèse (dérivée du complet)
                    "eventsShort" to shortOrDefault(eventsListText, "-"),
                    "sportAssessmentShort" to shortOrDefault(form.sportAssessment, "Bonne assiduité globale, progression satisfaisante."),
                    "nextSeasonGoalsShort" to shortOrDefault(form.nextSeasonGoals, "Maintien et développement des effectifs."),
                    "generalConclusionShort" to shortOrDefault(form.generalConclusion, "Saison positive, grâce à l’implication des adhérents, bénévoles et encadrants."),

                    "reportDate" to SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE).format(Date(now)),
                    "presidentName" to presidentName,
                    "signatureUrl" to (signatureUrl ?: "")
                )

                val file = MinutesTemplatePdf.generate(
                    context = context,
                    mode = mode,
                    seasonKey = seasonKey,
                    draftId = draftId,
                    version = nextVersion,
                    generatedAtMillis = now,
                    values = values,
                    signatureUrl = signatureUrl
                )

                // Persiste meta (même draftId, version++)
                val nowTs = Timestamp(Date(now))

                db.collection("season_admin").document(seasonKey)
                    .set(
                        mapOf(
                            "seasonKey" to seasonKey,
                            "agAt" to agAt,
                            "minutesDraftId" to draftId,
                            "minutesDraftUpdatedAt" to nowTs,
                            "minutesDraftUpdatedByUid" to uid,
                            "minutesPdfGeneratedAt" to nowTs,
                            "minutesPdfVersion" to nextVersion
                        ),
                        SetOptions.merge()
                    ).await()

                _ui.update {
                    it.copy(
                        loading = false,
                        minutesDraftId = draftId,
                        minutesDraftUpdatedAt = nowTs,
                        minutesDraftUpdatedByUid = uid,
                        minutesPdfGeneratedAt = nowTs,
                        minutesPdfVersion = nextVersion,
                        message = when (mode) {
                            MinutesTemplatePdf.Mode.SHORT -> "PDF synthèse généré ✔ (v$nextVersion)"
                            MinutesTemplatePdf.Mode.FULL -> "PDF complet généré ✔ (v$nextVersion)"
                        }
                    )
                }

                FileShare.sharePdf(
                    context,
                    file,
                    when (mode) {
                        MinutesTemplatePdf.Mode.SHORT -> "Partager CR synthèse"
                        MinutesTemplatePdf.Mode.FULL -> "Partager CR complet"
                    }
                )
            } catch (e: Exception) {
                _ui.update { it.copy(loading = false, error = e.message ?: "Erreur génération PDF") }
            }
        }
    }

    fun approveDraft(role: String) {
        val s = _ui.value
        val seasonKey = s.seasonKey
        val agAt = s.agAt
        val agMillis = agAt?.toDate()?.time

        if (seasonKey.isBlank() || agAt == null || agMillis == null) {
            _ui.update { it.copy(error = "Validation: date AG manquante") }
            return
        }

        val now = System.currentTimeMillis()
        if (!s.canApprove(now)) {
            _ui.update { it.copy(error = "Validation: impossible avant la date de l'AG") }
            return
        }

        val allowed = role == "SUPERADMIN" || role == "ADMIN"
        if (!allowed) {
            _ui.update { it.copy(error = "Validation: rôle non autorisé") }
            return
        }

        viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null, message = null) }
            try {
                val user = FirebaseAuth.getInstance().currentUser
                val uid = user?.uid?.trim().orEmpty()
                val email = user?.email?.trim()?.lowercase()

                if (uid.isBlank()) {
                    _ui.update { it.copy(loading = false, error = "Validation: UID manquant") }
                    return@launch
                }

                // 1) Résoudre "Prénom Nom" depuis la fiche adhérent via email
                var approvedByName: String = uid // fallback
                if (!email.isNullOrBlank()) {
                    val snap = db.collection("adherents")
                        .whereEqualTo("email", email)
                        .limit(1)
                        .get()
                        .await()

                    val doc = snap.documents.firstOrNull()
                    if (doc != null) {
                        val prenom = (doc.getString("prenom") ?: "").trim()
                        val nom = (doc.getString("nom") ?: "").trim()
                        val fullName = "$prenom $nom".trim()
                        if (fullName.isNotBlank()) approvedByName = fullName
                    } else {
                        // si pas trouvé (casse email, espace, etc.), on tente une 2e chance (email non-lowercased)
                        val snap2 = db.collection("adherents")
                            .whereEqualTo("email", user?.email?.trim())
                            .limit(1)
                            .get()
                            .await()

                        val doc2 = snap2.documents.firstOrNull()
                        if (doc2 != null) {
                            val prenom = (doc2.getString("prenom") ?: "").trim()
                            val nom = (doc2.getString("nom") ?: "").trim()
                            val fullName = "$prenom $nom".trim()
                            if (fullName.isNotBlank()) approvedByName = fullName
                        }
                    }
                }

                val nowTs = Timestamp(Date(now))

                // 2) Écriture season_admin (Option A Timestamp + snapshot name)
                db.collection("season_admin").document(seasonKey)
                    .set(
                        mapOf(
                            "seasonKey" to seasonKey,
                            "agAt" to agAt,
                            "minutesApprovedAt" to nowTs,
                            "minutesApprovedByUid" to uid,
                            "minutesApprovedByName" to approvedByName
                        ),
                        SetOptions.merge()
                    ).await()

                _ui.update {
                    it.copy(
                        loading = false,
                        minutesApprovedAt = nowTs,
                        minutesApprovedByUid = uid,
                        minutesApprovedByName = approvedByName,
                        message = "CR approuvé ✔"
                    )
                }

            } catch (e: Exception) {
                _ui.update { it.copy(loading = false, error = e.message ?: "Erreur approbation") }
            }
        }
    }

}
