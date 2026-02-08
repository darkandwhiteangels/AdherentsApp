package com.antechrist.adherentsapp.ui.screens.cotisations

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import kotlin.math.max
import com.antechrist.adherentsapp.core.pdf.AttestationPdf
import com.antechrist.adherentsapp.core.utils.SeasonUtils
import com.antechrist.adherentsapp.domain.finance.calculateHouseholdAmounts
import com.antechrist.adherentsapp.domain.model.finance.*
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.core.graphics.toColorInt
import kotlinx.coroutines.Dispatchers
import java.net.URL
import androidx.core.graphics.scale

data class HouseholdDetailUi(
    val loading: Boolean = true,
    val error: String? = null,

    val seasonKey: String = SeasonUtils.currentSeasonKey(),
    val guardianId: String = "",
    val guardianName: String = "",
    val guardianEmail: String? = null,
    val guardianTel: String? = null,
    val guardianAddress: String? = null,
    val guardianPostalCode: String? = null,
    val guardianCity: String? = null,
    val memberIds: List<String> = emptyList(),

    val status: HouseholdStatus = HouseholdStatus.A_REGLER,
    val oneClassPerWeek: Boolean = false,
    val controlledDiscountCents: Long = 0L,
    val discountBlackBeltEnabled: Boolean = false,
    val discountFamilyGradedEnabled: Boolean = false,
    val discountAssistantProfEnabled: Boolean = false,

    // ✅ NOUVEAU : exonération & remise manuelle
    val exemptFromFee: Boolean = false,
    val manualDiscountCents: Long = 0L,
    val manualDiscountReason: String = "",

    // Montants calculés
    val amountBaseCents: Long = 0L,
    val discountCents: Long = 0L,          // remise auto (1 cours/semaine)
    val aidsTotalCents: Long = 0L,
    val amountDueCents: Long = 0L,
    val totalPaidCents: Long = 0L,      // ✅ total encaissé (plan + reçu)
    val remainingCents: Long = 0L,      // ✅ reste à payer = amountDue - totalPaid

    val licenceCount: Int = 0,
    val licenceAmountCents: Long = 0L,
    val licencesTotalCents: Long = 0L,     // ✅ NOUVEAU

    val cfgLicencePaidByClub: Boolean = true,

    // Paiements
    val paymentsPlan: List<PaymentEntry> = emptyList(),
    val paymentsReceived: List<PaymentEntry> = emptyList(),

    // Aides
    val aids: List<AidEntry> = emptyList(),
    val aidType: AidType? = null,
    val aidCode: String = "",
    val aidAmountCents: Long? = null,

    // UI inputs
    val selectedMethod: PaymentMethod? = null,
    val chequeCount: Int = 1,
    val inputAmountCents: Long? = null,
    val inputDateIso: String? = null
)

@HiltViewModel
class CotisationHouseholdViewModel @Inject constructor(
    private val db: FirebaseFirestore
) : ViewModel() {

    private val _ui = MutableStateFlow(HouseholdDetailUi())
    val ui: StateFlow<HouseholdDetailUi> = _ui

    private fun hhRef() = db.collection("cotisations_families")
        .document(_ui.value.seasonKey)
        .collection("households")
        .document(_ui.value.guardianId)

    // ---------------------- Chargement ----------------------

    fun load(guardianId: String, seasonKey: String) {
        viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null, guardianId = guardianId, seasonKey = seasonKey) }
            try {
                // Responsable
                val guardian = db.collection("guardians").document(guardianId).get().await()
                val gNom = (guardian.getString("nom") ?: "").trim()
                val gPrenom = (guardian.getString("prenom") ?: "").trim()
                val gName = listOf(gPrenom, gNom).joinToString(" ").trim()
                val gEmail = guardian.getString("email")
                val gTel = guardian.getString("telephone")

                val gAddr = guardian.getString("adresse")
                val gCp   = guardian.getString("codePostal")
                val gVille= guardian.getString("ville")

                // Config saison
                val cfg = loadSeasonConfig(seasonKey)

                // Dossier
                val ref = hhRef()
                val hhDoc = ref.get().await()

                val (memberIds, hhData) = if (!hhDoc.exists()) {
                    // ✅ Ne prendre que les adhérents pratiquants rattachés au responsable
                    val q1 = db.collection("adherents")
                        .whereEqualTo("primaryGuardianId", guardianId)
                        .whereEqualTo("isPractitioner", true)
                        .get().await()

                    val q2 = db.collection("adherents")
                        .whereArrayContains("guardianIds", guardianId)
                        .whereEqualTo("isPractitioner", true)
                        .get().await()

                    val merged = (q1.documents + q2.documents)
                        .distinctBy { it.id }

                    val mids = merged.map { it.id }
                    val ages = merged.map { parseAge(it.getString("dateNaissance")) }

                    val res = calculateHouseholdAmounts(
                        ages = ages,
                        cfg = cfg,
                        oneClassPerWeek = false,
                        aids = emptyList(),
                        manualDiscountCents = 0L,
                        exemptFromFee = false,

                        // ✅ NOUVEAU : remises contrôlables (par défaut OFF)
                        discountBlackBeltEnabled = false,
                        discountFamilyGradedEnabled = false,
                        discountAssistantProfEnabled = false
                    )

                    val init = mutableMapOf<String, Any?>(
                        "guardianId" to guardianId,
                        "seasonKey" to seasonKey,
                        "memberIds" to mids,
                        "status" to HouseholdStatus.A_REGLER.name,

                        "oneClassPerWeek" to false,
                        "exemptFromFee" to false,
                        "manualDiscountCents" to 0L,
                        "manualDiscountReason" to null,

                        // ✅ NOUVEAU : persistance des cases à cocher
                        "discountBlackBeltEnabled" to false,
                        "discountFamilyGradedEnabled" to false,
                        "discountAssistantProfEnabled" to false,

                        "amountBaseCents" to res.amountBaseCents,
                        "discountCents" to res.discountCents,
                        "aids" to emptyList<Map<String, Any?>>(),
                        "amountDueCents" to res.amountDueCents,

                        "licenceCount" to res.licenceCount,
                        "licenceAmountCents" to res.licenceAmountCents,
                        "licencesTotalCents" to res.licencesTotalCents,

                        "paymentsPlan" to emptyList<Map<String, Any?>>(),
                        "paymentsReceived" to emptyList<Map<String, Any?>>(),
                        "updatedAt" to System.currentTimeMillis()
                    )


                    ref.set(init, SetOptions.merge()).await()
                    mids to init
                } else {
                    val data = hhDoc.data ?: emptyMap()
                    val mids = (data["memberIds"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                    mids to data
                }

                val status = runCatching { HouseholdStatus.valueOf(hhData["status"] as? String ?: "A_REGLER") }
                    .getOrDefault(HouseholdStatus.A_REGLER)

                val oneClass = hhData["oneClassPerWeek"] as? Boolean ?: false
                val exemptFromFee = hhData["exemptFromFee"] as? Boolean ?: false

                val manualDiscountCents = (hhData["manualDiscountCents"] as? Number)?.toLong() ?: 0L
                val manualDiscountReason = (hhData["manualDiscountReason"] as? String) ?: ""

                // ✅ NOUVEAU : lecture des cases à cocher (defaults false)
                val discountBlackBeltEnabled = hhData["discountBlackBeltEnabled"] as? Boolean ?: false
                val discountFamilyGradedEnabled = hhData["discountFamilyGradedEnabled"] as? Boolean ?: false
                val discountAssistantProfEnabled = hhData["discountAssistantProfEnabled"] as? Boolean ?: false

                val amountBase = (hhData["amountBaseCents"] as? Number)?.toLong() ?: 0L
                val discount = (hhData["discountCents"] as? Number)?.toLong() ?: 0L
                val amountDue = (hhData["amountDueCents"] as? Number)?.toLong() ?: 0L
                val licenceCount = (hhData["licenceCount"] as? Number)?.toInt() ?: 0
                val licenceAmount = (hhData["licenceAmountCents"] as? Number)?.toLong() ?: 0L

                val plan = (hhData["paymentsPlan"] as? List<*>).orEmpty().mapNotNull { it.toPaymentEntryOrNull() }
                val received = (hhData["paymentsReceived"] as? List<*>).orEmpty().mapNotNull { it.toPaymentEntryOrNull() }
                val aids = (hhData["aids"] as? List<*>).orEmpty().mapNotNull { it.toAidEntryOrNull() }
                val aidsTotal = aids.sumOf { it.amountCents }

                // Traçabilité licence : fallback calcul si pas stocké
                val licencesTotalCentsStored = (hhData["licencesTotalCents"] as? Number)?.toLong()
                val licencesTotalFallback = licenceCount.toLong() * cfg.licenceAmountCents
                val licencesTotalCents = licencesTotalCentsStored ?: licencesTotalFallback

                val ages = loadMembersAges(memberIds)
                val recalculated = calculateHouseholdAmounts(
                    ages = ages,
                    cfg = cfg,
                    oneClassPerWeek = oneClass,
                    aids = aids,
                    manualDiscountCents = manualDiscountCents,
                    exemptFromFee = exemptFromFee,

                    // ✅ NOUVEAU : passage des toggles
                    discountBlackBeltEnabled = discountBlackBeltEnabled,
                    discountFamilyGradedEnabled = discountFamilyGradedEnabled,
                    discountAssistantProfEnabled = discountAssistantProfEnabled
                )
                val (paid, remaining) = computePaidAndRemaining(
                    dueCents = recalculated.amountDueCents,
                    paymentsPlan = plan,
                    paymentsReceived = received
                )

                // Si les montants stockés sont anciens / incohérents, on patch automatiquement
                val needsPatch =
                    amountBase != recalculated.amountBaseCents ||
                            discount != recalculated.discountCents ||
                            amountDue != recalculated.amountDueCents ||
                            licenceCount != recalculated.licenceCount ||
                            licenceAmount != recalculated.licenceAmountCents ||
                            (licencesTotalCentsStored ?: 0L) != recalculated.licencesTotalCents ||
                            // ✅ on force aussi l'écriture des nouveaux bool si absents
                            !hhData.containsKey("discountBlackBeltEnabled") ||
                            !hhData.containsKey("discountFamilyGradedEnabled") ||
                            !hhData.containsKey("discountAssistantProfEnabled")

                if (needsPatch) {
                    hhRef().set(
                        mapOf(
                            "amountBaseCents" to recalculated.amountBaseCents,
                            "discountCents" to recalculated.discountCents,
                            "amountDueCents" to recalculated.amountDueCents,
                            "licenceCount" to recalculated.licenceCount,
                            "licenceAmountCents" to recalculated.licenceAmountCents,
                            "licencesTotalCents" to recalculated.licencesTotalCents,

                            // ✅ NOUVEAU : garantir la présence des 3 bool côté Firestore
                            "discountBlackBeltEnabled" to discountBlackBeltEnabled,
                            "discountFamilyGradedEnabled" to discountFamilyGradedEnabled,
                            "discountAssistantProfEnabled" to discountAssistantProfEnabled,

                            "updatedAt" to System.currentTimeMillis()
                        ),
                        SetOptions.merge()
                    ).await()
                }

                _ui.update {
                    it.copy(
                        loading = false, error = null,
                        guardianName = gName,
                        guardianEmail = gEmail,
                        guardianTel = gTel,
                        guardianAddress = gAddr,
                        guardianPostalCode = gCp,
                        guardianCity = gVille,
                        memberIds = memberIds,
                        status = status,

                        oneClassPerWeek = oneClass,
                        exemptFromFee = exemptFromFee,
                        manualDiscountCents = manualDiscountCents,
                        manualDiscountReason = manualDiscountReason,

                        // ✅ NOUVEAU : état UI des cases à cocher
                        discountBlackBeltEnabled = discountBlackBeltEnabled,
                        discountFamilyGradedEnabled = discountFamilyGradedEnabled,
                        discountAssistantProfEnabled = discountAssistantProfEnabled,

                        amountBaseCents = recalculated.amountBaseCents,
                        discountCents = recalculated.discountCents,
                        controlledDiscountCents = recalculated.controlledDiscountCents, // ✅ NOUVEAU
                        aidsTotalCents = aidsTotal,
                        amountDueCents = recalculated.amountDueCents,
                        totalPaidCents = paid,
                        remainingCents = remaining,

                        licenceCount = recalculated.licenceCount,
                        licenceAmountCents = recalculated.licenceAmountCents,
                        licencesTotalCents = recalculated.licencesTotalCents,

                        cfgLicencePaidByClub = cfg.licencePaidByClub,
                        paymentsPlan = plan,
                        paymentsReceived = received,
                        aids = aids
                    )
                }


                recomputeAutoStatus()
            } catch (e: Exception) {
                _ui.update { it.copy(loading = false, error = e.message ?: "Erreur de chargement") }
            }
        }
    }

    private suspend fun loadSeasonConfig(seasonKey: String): FinanceSeasonConfig {
        val snap = db.collection("finance_seasons").document(seasonKey).get().await()
        if (!snap.exists()) return FinanceSeasonConfig(seasonKey)

        val m = snap.data ?: emptyMap()

        return FinanceSeasonConfig(
            seasonKey = seasonKey,

            priceAdultCents = (m["priceAdultCents"] as? Number)?.toLong() ?: 18300,
            priceChild6to16Cents = (m["priceChild6to16Cents"] as? Number)?.toLong() ?: 16800,
            priceBabyUnder6Cents = (m["priceBabyUnder6Cents"] as? Number)?.toLong() ?: 11100,

            bundle2AllOver5Cents = (m["bundle2AllOver5Cents"] as? Number)?.toLong() ?: 30600,
            bundle3AllOver5Cents = (m["bundle3AllOver5Cents"] as? Number)?.toLong() ?: 43200,

            oneClassPerWeekDiscountCents =
                (m["oneClassPerWeekDiscountCents"] as? Number)?.toLong() ?: 3000,

            // ✅ NOUVEAU : remises contrôlées (SINON elles restent à 0)
            discountBlackBeltCents =
                (m["discountBlackBeltCents"] as? Number)?.toLong() ?: 0L,
            discountFamilyGradedCents =
                (m["discountFamilyGradedCents"] as? Number)?.toLong() ?: 0L,
            discountAssistantProfCents =
                (m["discountAssistantProfCents"] as? Number)?.toLong() ?: 0L,

            licencePaidByClub = m["licencePaidByClub"] as? Boolean ?: true,
            licenceAmountCents = (m["licenceAmountCents"] as? Number)?.toLong() ?: 3900,

            defaultDueDate = m["defaultDueDate"] as? String
        )
    }


    // ---------------------- Saisie paiements ----------------------

    fun setSelectedMethod(m: PaymentMethod?) { _ui.update { it.copy(selectedMethod = m) } }
    fun setChequeCount(n: Int) { _ui.update { it.copy(chequeCount = n.coerceIn(1,3)) } }
    fun setInputAmountCents(amountCents: Long?) { _ui.update { it.copy(inputAmountCents = amountCents?.coerceAtLeast(0)) } }
    fun setInputDateIso(iso: String?) { _ui.update { it.copy(inputDateIso = iso) } }

    fun addChequePlan(totalCents: Long) {
        viewModelScope.launch {
            try {
                val count = _ui.value.chequeCount.coerceIn(1,3)
                val parts = splitCentsInParts(totalCents.coerceAtLeast(0), count)
                val entries = parts.mapIndexed { idx, amt ->
                    PaymentEntry(type = PaymentMethod.CHQ, amountCents = amt, index = idx + 1)
                }
                val next = (_ui.value.paymentsPlan + entries).sortedBy { it.index ?: 0 }
                hhRef().set(mapOf(
                    "paymentsPlan" to next.map { it.asMap() },
                    "updatedAt" to System.currentTimeMillis()
                ), SetOptions.merge()).await()
                _ui.update { it.copy(paymentsPlan = next) }
                recomputeAutoStatus()
            } catch (e: Exception) {
                _ui.update { it.copy(error = e.message ?: "Erreur ajout chèques") }
            }
        }
    }

    fun addReceived(method: PaymentMethod, amountCents: Long, dateIso: String?) {
        viewModelScope.launch {
            try {
                val entry = PaymentEntry(type = method, amountCents = amountCents.coerceAtLeast(0), date = dateIso)
                val next = _ui.value.paymentsReceived + entry
                hhRef().set(mapOf(
                    "paymentsReceived" to next.map { it.asMap() },
                    "updatedAt" to System.currentTimeMillis()
                ), SetOptions.merge()).await()
                _ui.update { it.copy(paymentsReceived = next) }
                recomputeAutoStatus()
            } catch (e: Exception) {
                _ui.update { it.copy(error = e.message ?: "Erreur ajout paiement") }
            }
        }
    }

    fun editPlanCheque(index: Int, amountCents: Long) {
        viewModelScope.launch {
            try {
                val next = _ui.value.paymentsPlan.toMutableList().also { list ->
                    val pos = list.indexOfFirst { it.index == index }
                    if (pos >= 0) list[pos] = list[pos].copy(amountCents = amountCents.coerceAtLeast(0))
                }.sortedBy { it.index ?: 0 }
                hhRef().set(mapOf(
                    "paymentsPlan" to next.map { it.asMap() },
                    "updatedAt" to System.currentTimeMillis()
                ), SetOptions.merge()).await()
                _ui.update { it.copy(paymentsPlan = next) }
                recomputeAutoStatus()
            } catch (e: Exception) {
                _ui.update { it.copy(error = e.message ?: "Erreur édition chèque") }
            }
        }
    }

    fun removePlanCheque(index: Int) {
        viewModelScope.launch {
            try {
                val next = _ui.value.paymentsPlan.filterNot { it.index == index }
                hhRef().set(mapOf(
                    "paymentsPlan" to next.map { it.asMap() },
                    "updatedAt" to System.currentTimeMillis()
                ), SetOptions.merge()).await()
                _ui.update { it.copy(paymentsPlan = next) }
                recomputeAutoStatus()
            } catch (e: Exception) {
                _ui.update { it.copy(error = e.message ?: "Erreur suppression chèque") }
            }
        }
    }

    fun editReceivedAt(position: Int, method: PaymentMethod, amountCents: Long, dateIso: String?) {
        viewModelScope.launch {
            try {
                val next = _ui.value.paymentsReceived.toMutableList().also {
                    if (position in it.indices) {
                        it[position] = PaymentEntry(type = method, amountCents = amountCents.coerceAtLeast(0), date = dateIso)
                    }
                }.toList()
                hhRef().set(mapOf(
                    "paymentsReceived" to next.map { it.asMap() },
                    "updatedAt" to System.currentTimeMillis()
                ), SetOptions.merge()).await()
                _ui.update { it.copy(paymentsReceived = next) }
                recomputeAutoStatus()
            } catch (e: Exception) {
                _ui.update { it.copy(error = e.message ?: "Erreur édition paiement") }
            }
        }
    }

    fun removeReceivedAt(position: Int) {
        viewModelScope.launch {
            try {
                val next = _ui.value.paymentsReceived.toMutableList().also {
                    if (position in it.indices) it.removeAt(position)
                }.toList()
                hhRef().set(mapOf(
                    "paymentsReceived" to next.map { it.asMap() },
                    "updatedAt" to System.currentTimeMillis()
                ), SetOptions.merge()).await()
                _ui.update { it.copy(paymentsReceived = next) }
                recomputeAutoStatus()
            } catch (e: Exception) {
                _ui.update { it.copy(error = e.message ?: "Erreur suppression paiement") }
            }
        }
    }

    private suspend fun recalcAndPersist(
        seasonKey: String,
        memberIds: List<String>,
        oneClassPerWeek: Boolean,
        aids: List<AidEntry>,
        manualDiscountCents: Long,
        manualDiscountReason: String,
        exemptFromFee: Boolean,

        // ✅ NOUVEAU : remises “contrôlables”
        discountBlackBeltEnabled: Boolean,
        discountFamilyGradedEnabled: Boolean,
        discountAssistantProfEnabled: Boolean,

        extraPatch: Map<String, Any?> = emptyMap()
    ) {
        val cfg = loadSeasonConfig(seasonKey)
        val ages = loadMembersAges(memberIds)

        val res = calculateHouseholdAmounts(
            ages = ages,
            cfg = cfg,
            oneClassPerWeek = oneClassPerWeek,
            aids = aids,
            manualDiscountCents = manualDiscountCents,
            exemptFromFee = exemptFromFee,

            // ✅ NOUVEAU : passage des toggles
            discountBlackBeltEnabled = discountBlackBeltEnabled,
            discountFamilyGradedEnabled = discountFamilyGradedEnabled,
            discountAssistantProfEnabled = discountAssistantProfEnabled
        )

        val patch = mutableMapOf<String, Any?>(
            "oneClassPerWeek" to oneClassPerWeek,
            "exemptFromFee" to exemptFromFee,

            "manualDiscountCents" to manualDiscountCents,
            "manualDiscountReason" to manualDiscountReason.ifBlank { null },

            // ✅ NOUVEAU : persistance toggles
            "discountBlackBeltEnabled" to discountBlackBeltEnabled,
            "discountFamilyGradedEnabled" to discountFamilyGradedEnabled,
            "discountAssistantProfEnabled" to discountAssistantProfEnabled,

            "amountBaseCents" to res.amountBaseCents,
            "discountCents" to res.discountCents,
            "amountDueCents" to res.amountDueCents,

            "licenceCount" to res.licenceCount,
            "licenceAmountCents" to res.licenceAmountCents,
            "licencesTotalCents" to res.licencesTotalCents,

            "updatedAt" to System.currentTimeMillis()
        )

        extraPatch.forEach { (k, v) -> patch[k] = v }

        hhRef().set(patch, SetOptions.merge()).await()

        _ui.update { ui ->
            ui.copy(
                oneClassPerWeek = oneClassPerWeek,
                exemptFromFee = exemptFromFee,

                manualDiscountCents = manualDiscountCents,
                manualDiscountReason = manualDiscountReason,

                // ✅ NOUVEAU : état UI toggles
                discountBlackBeltEnabled = discountBlackBeltEnabled,
                discountFamilyGradedEnabled = discountFamilyGradedEnabled,
                discountAssistantProfEnabled = discountAssistantProfEnabled,

                amountBaseCents = res.amountBaseCents,
                discountCents = res.discountCents,
                controlledDiscountCents = res.controlledDiscountCents, // ✅ NOUVEAU
                aidsTotalCents = aids.sumOf { it.amountCents },
                amountDueCents = res.amountDueCents,

                licenceCount = res.licenceCount,
                licenceAmountCents = res.licenceAmountCents,
                licencesTotalCents = res.licencesTotalCents,

                cfgLicencePaidByClub = cfg.licencePaidByClub
            )
        }
    }

    // ---------------------- One-class/week & Aides ----------------------

    fun toggleOneClassPerWeek(enabled: Boolean) {
        viewModelScope.launch {
            try {
                val s = _ui.value
                recalcAndPersist(
                    seasonKey = s.seasonKey,
                    memberIds = s.memberIds,
                    oneClassPerWeek = enabled,
                    aids = s.aids,
                    manualDiscountCents = s.manualDiscountCents,
                    manualDiscountReason = s.manualDiscountReason,
                    exemptFromFee = s.exemptFromFee,

                    discountBlackBeltEnabled = s.discountBlackBeltEnabled,
                    discountFamilyGradedEnabled = s.discountFamilyGradedEnabled,
                    discountAssistantProfEnabled = s.discountAssistantProfEnabled
                )
                recomputeAutoStatus()
            } catch (e: Exception) {
                _ui.update { it.copy(error = e.message ?: "Erreur recalcul") }
            }
        }
    }

    fun toggleBlackBeltDiscount(enabled: Boolean) {
        viewModelScope.launch {
            try {
                val s = _ui.value
                recalcAndPersist(
                    seasonKey = s.seasonKey,
                    memberIds = s.memberIds,
                    oneClassPerWeek = s.oneClassPerWeek,
                    aids = s.aids,
                    manualDiscountCents = s.manualDiscountCents,
                    manualDiscountReason = s.manualDiscountReason,
                    exemptFromFee = s.exemptFromFee,

                    discountBlackBeltEnabled = enabled,
                    discountFamilyGradedEnabled = s.discountFamilyGradedEnabled,
                    discountAssistantProfEnabled = s.discountAssistantProfEnabled
                )
                recomputeAutoStatus()
            } catch (e: Exception) {
                _ui.update { it.copy(error = e.message ?: "Erreur remise ceinture noire") }
            }
        }
    }

    fun toggleFamilyGradedDiscount(enabled: Boolean) {
        viewModelScope.launch {
            try {
                val s = _ui.value
                recalcAndPersist(
                    seasonKey = s.seasonKey,
                    memberIds = s.memberIds,
                    oneClassPerWeek = s.oneClassPerWeek,
                    aids = s.aids,
                    manualDiscountCents = s.manualDiscountCents,
                    manualDiscountReason = s.manualDiscountReason,
                    exemptFromFee = s.exemptFromFee,

                    discountBlackBeltEnabled = s.discountBlackBeltEnabled,
                    discountFamilyGradedEnabled = enabled,
                    discountAssistantProfEnabled = s.discountAssistantProfEnabled
                )
                recomputeAutoStatus()
            } catch (e: Exception) {
                _ui.update { it.copy(error = e.message ?: "Erreur remise famille gradés") }
            }
        }
    }

    fun toggleAssistantProfDiscount(enabled: Boolean) {
        viewModelScope.launch {
            try {
                val s = _ui.value
                recalcAndPersist(
                    seasonKey = s.seasonKey,
                    memberIds = s.memberIds,
                    oneClassPerWeek = s.oneClassPerWeek,
                    aids = s.aids,
                    manualDiscountCents = s.manualDiscountCents,
                    manualDiscountReason = s.manualDiscountReason,
                    exemptFromFee = s.exemptFromFee,

                    discountBlackBeltEnabled = s.discountBlackBeltEnabled,
                    discountFamilyGradedEnabled = s.discountFamilyGradedEnabled,
                    discountAssistantProfEnabled = enabled
                )
                recomputeAutoStatus()
            } catch (e: Exception) {
                _ui.update { it.copy(error = e.message ?: "Erreur remise jeune assistant prof") }
            }
        }
    }

    fun setManualDiscount(cents: Long, reason: String) {
        viewModelScope.launch {
            try {
                val s = _ui.value
                val cleanCents = cents.coerceAtLeast(0L)
                val cleanReason = reason.trim()

                if (cleanCents > 0L && cleanReason.isBlank()) {
                    _ui.update { it.copy(error = "Motif obligatoire pour une remise manuelle.") }
                    return@launch
                }

                recalcAndPersist(
                    seasonKey = s.seasonKey,
                    memberIds = s.memberIds,
                    oneClassPerWeek = s.oneClassPerWeek,
                    aids = s.aids,
                    manualDiscountCents = cleanCents,
                    manualDiscountReason = cleanReason,
                    exemptFromFee = s.exemptFromFee,

                    discountBlackBeltEnabled = s.discountBlackBeltEnabled,
                    discountFamilyGradedEnabled = s.discountFamilyGradedEnabled,
                    discountAssistantProfEnabled = s.discountAssistantProfEnabled
                )
                recomputeAutoStatus()
            } catch (e: Exception) {
                _ui.update { it.copy(error = e.message ?: "Erreur remise manuelle") }
            }
        }
    }


    fun toggleExemptFromFee(exempt: Boolean) {
        viewModelScope.launch {
            try {
                val s = _ui.value
                recalcAndPersist(
                    seasonKey = s.seasonKey,
                    memberIds = s.memberIds,
                    oneClassPerWeek = s.oneClassPerWeek,
                    aids = s.aids,
                    manualDiscountCents = s.manualDiscountCents,
                    manualDiscountReason = s.manualDiscountReason,
                    exemptFromFee = exempt,

                    discountBlackBeltEnabled = s.discountBlackBeltEnabled,
                    discountFamilyGradedEnabled = s.discountFamilyGradedEnabled,
                    discountAssistantProfEnabled = s.discountAssistantProfEnabled
                )
                recomputeAutoStatus()
            } catch (e: Exception) {
                _ui.update { it.copy(error = e.message ?: "Erreur exonération") }
            }
        }
    }


    fun setAidType(t: AidType?) { _ui.update { it.copy(aidType = t) } }
    fun setAidCode(code: String) { _ui.update { it.copy(aidCode = code.trim()) } }
    fun setAidAmountCents(cents: Long?) { _ui.update { it.copy(aidAmountCents = cents?.coerceAtLeast(0)) } }

    fun addAid() {
        viewModelScope.launch {
            try {
                val s = _ui.value
                val t = s.aidType ?: return@launch
                val amt = (s.aidAmountCents ?: 0L).coerceAtLeast(0)
                if (amt <= 0L) return@launch

                val entry = AidEntry(type = t, code = s.aidCode.ifBlank { null }, amountCents = amt)
                val next = s.aids + entry

                hhRef().set(
                    mapOf(
                        "aids" to next.map { it.asMap() },
                        "updatedAt" to System.currentTimeMillis()
                    ),
                    SetOptions.merge()
                ).await()

                val cfg = loadSeasonConfig(s.seasonKey)
                val ages = loadMembersAges(s.memberIds)
                val res = calculateHouseholdAmounts(
                    ages = ages,
                    cfg = cfg,
                    oneClassPerWeek = s.oneClassPerWeek,
                    aids = next,
                    manualDiscountCents = s.manualDiscountCents,
                    exemptFromFee = s.exemptFromFee,

                    // ✅ NOUVEAU : remises contrôlées
                    discountBlackBeltEnabled = s.discountBlackBeltEnabled,
                    discountFamilyGradedEnabled = s.discountFamilyGradedEnabled,
                    discountAssistantProfEnabled = s.discountAssistantProfEnabled
                )

                hhRef().set(
                    mapOf(
                        "amountBaseCents" to res.amountBaseCents,
                        "discountCents" to res.discountCents,
                        "amountDueCents" to res.amountDueCents,

                        "licenceCount" to res.licenceCount,
                        "licenceAmountCents" to res.licenceAmountCents,
                        "licencesTotalCents" to res.licencesTotalCents,

                        "updatedAt" to System.currentTimeMillis()
                    ),
                    SetOptions.merge()
                ).await()

                _ui.update {
                    it.copy(
                        aids = next,
                        aidType = null,
                        aidCode = "",
                        aidAmountCents = null,

                        amountBaseCents = res.amountBaseCents,
                        discountCents = res.discountCents,
                        controlledDiscountCents = res.controlledDiscountCents, // ✅ NOUVEAU
                        aidsTotalCents = next.sumOf { a -> a.amountCents },
                        amountDueCents = res.amountDueCents,

                        licenceCount = res.licenceCount,
                        licenceAmountCents = res.licenceAmountCents,
                        licencesTotalCents = res.licencesTotalCents
                    )
                }

                recomputeAutoStatus()
            } catch (e: Exception) {
                _ui.update { it.copy(error = e.message ?: "Erreur ajout aide") }
            }
        }
    }


    fun removeAidAt(index: Int) {
        viewModelScope.launch {
            try {
                val s = _ui.value
                if (index !in s.aids.indices) return@launch

                val next = s.aids.toMutableList().also { it.removeAt(index) }.toList()

                hhRef().set(
                    mapOf(
                        "aids" to next.map { it.asMap() },
                        "updatedAt" to System.currentTimeMillis()
                    ),
                    SetOptions.merge()
                ).await()

                val cfg = loadSeasonConfig(s.seasonKey)
                val ages = loadMembersAges(s.memberIds)
                val res = calculateHouseholdAmounts(
                    ages = ages,
                    cfg = cfg,
                    oneClassPerWeek = s.oneClassPerWeek,
                    aids = next,
                    manualDiscountCents = s.manualDiscountCents,
                    exemptFromFee = s.exemptFromFee,

                    // ✅ NOUVEAU : remises contrôlées
                    discountBlackBeltEnabled = s.discountBlackBeltEnabled,
                    discountFamilyGradedEnabled = s.discountFamilyGradedEnabled,
                    discountAssistantProfEnabled = s.discountAssistantProfEnabled
                )

                hhRef().set(
                    mapOf(
                        "amountBaseCents" to res.amountBaseCents,
                        "discountCents" to res.discountCents,
                        "amountDueCents" to res.amountDueCents,

                        "licenceCount" to res.licenceCount,
                        "licenceAmountCents" to res.licenceAmountCents,
                        "licencesTotalCents" to res.licencesTotalCents,

                        "updatedAt" to System.currentTimeMillis()
                    ),
                    SetOptions.merge()
                ).await()

                _ui.update {
                    it.copy(
                        aids = next,

                        amountBaseCents = res.amountBaseCents,
                        discountCents = res.discountCents,
                        controlledDiscountCents = res.controlledDiscountCents, // ✅ NOUVEAU
                        aidsTotalCents = next.sumOf { a -> a.amountCents },
                        amountDueCents = res.amountDueCents,

                        licenceCount = res.licenceCount,
                        licenceAmountCents = res.licenceAmountCents,
                        licencesTotalCents = res.licencesTotalCents
                    )
                }

                recomputeAutoStatus()
            } catch (e: Exception) {
                _ui.update { it.copy(error = e.message ?: "Erreur suppression aide") }
            }
        }
    }


    // ---------------------- Statut ----------------------

    fun setStatusManual(newStatus: HouseholdStatus) {
        viewModelScope.launch {
            try {
                hhRef().set(
                    mapOf("status" to newStatus.name, "updatedAt" to System.currentTimeMillis()),
                    SetOptions.merge()
                ).await()
                setCotisationPaidForMembers(newStatus == HouseholdStatus.SOLDE)
                _ui.update { it.copy(status = newStatus) }
            } catch (e: Exception) {
                _ui.update { it.copy(error = e.message ?: "Erreur statut") }
            }
        }
    }

    private fun computePaidAndRemaining(
        dueCents: Long,
        paymentsPlan: List<PaymentEntry>,
        paymentsReceived: List<PaymentEntry>
    ): Pair<Long, Long> {
        val planned = paymentsPlan.sumOf { it.amountCents }
        val received = paymentsReceived.sumOf { it.amountCents }
        val paid = planned + received
        val remaining = (dueCents - paid).coerceAtLeast(0L)
        return paid to remaining
    }

    private fun recomputeAutoStatus() {
        val s = _ui.value

        val planned = s.paymentsPlan.sumOf { it.amountCents }
        val received = s.paymentsReceived.sumOf { it.amountCents }
        val paid = planned + received

        val remaining = (s.amountDueCents - paid).coerceAtLeast(0L)

        val newStatus = when {
            s.status == HouseholdStatus.ANNULE -> HouseholdStatus.ANNULE
            s.status == HouseholdStatus.EN_RETARD && paid < s.amountDueCents -> HouseholdStatus.EN_RETARD
            s.amountDueCents <= 0L -> HouseholdStatus.SOLDE
            paid >= s.amountDueCents -> HouseholdStatus.SOLDE
            else -> HouseholdStatus.A_REGLER
        }

        // ✅ update UI des montants (toujours)
        _ui.update { it.copy(totalPaidCents = paid, remainingCents = remaining) }

        if (newStatus != s.status) {
            viewModelScope.launch {
                runCatching {
                    hhRef().set(
                        mapOf("status" to newStatus.name, "updatedAt" to System.currentTimeMillis()),
                        SetOptions.merge()
                    ).await()
                }
                setCotisationPaidForMembers(newStatus == HouseholdStatus.SOLDE)
                _ui.update { it.copy(status = newStatus) }
            }
        }
    }


    // ---------------------- Attestations ----------------------

    enum class AttestationMode { PER_MEMBER, HOUSEHOLD }

    data class MemberDetail(
        val id: String,
        val nom: String,
        val prenom: String,
        val dateNaissance: String?,
        val adresse: String?,
        val codePostal: String?,
        val ville: String?
    )

    private suspend fun loadMembersDetails(ids: List<String>): List<MemberDetail> {
        if (ids.isEmpty()) return emptyList()
        val res = mutableListOf<MemberDetail>()
        for (chunk in ids.chunked(10)) {
            val snap = db.collection("adherents")
                .whereIn(FieldPath.documentId(), chunk)
                .get().await()
            snap.documents.forEach { d ->
                res += MemberDetail(
                    id = d.id,
                    nom = d.getString("nom") ?: "",
                    prenom = d.getString("prenom") ?: "",
                    dateNaissance = d.getString("dateNaissance"),
                    adresse = d.getString("adresse"),
                    codePostal = d.getString("codePostal"),
                    ville = d.getString("ville")
                )
            }
        }
        return res
    }

    private suspend fun loadMembersAges(ids: List<String>): List<Int?> {
        if (ids.isEmpty()) return emptyList()
        val ages = mutableListOf<Int?>()
        for (chunk in ids.chunked(10)) {
            val snap = db.collection("adherents")
                .whereIn(FieldPath.documentId(), chunk)
                .get().await()
            snap.documents.forEach { d ->
                ages += parseAge(d.getString("dateNaissance"))
            }
        }
        return ages
    }

    private suspend fun loadClubProfile(): AttestationPdf.ClubProfile {
        // Lecture Firestore : /club_config/main
        val snap = db.collection("club_config").document("main").get().await()

        val president = snap.getString("presidentName") ?: "Le/La Président(e)"
        val secretary = snap.getString("secretaryName") // optionnel
        val clubName  = snap.getString("clubName") ?: "Club"
        val clubCity  = snap.getString("clubCity") ?: "Ville"

        // URLs (peuvent être null)
        val logoUrl = snap.getString("logoUrl")
        val signatureUrl = snap.getString("presidentSignatureUrl")

        // Bitmaps (réseau) — helpers supposés sûrs (IO)
        val logoBmp = logoUrl?.let { safeFetchBitmap(it) }
        val signatureBmp = signatureUrl?.let { safeFetchBitmap(it) }

        return AttestationPdf.ClubProfile(
            presidentName = president,
            secretaryName = secretary,
            clubName = clubName,
            clubCity = clubCity,
            logoBitmap = logoBmp,                          // affiché en haut à gauche
            presidentSignatureBitmap = signatureBmp        // affiché en bas à droite
        )
    }


    private fun parseColorOrDefault(hex: String?, defaultHex: String?): Int {
        return try {
            if (hex == null) {
                if (defaultHex == null) throw IllegalArgumentException()
                defaultHex.toColorInt()
            } else {
                hex.toColorInt()
            }
        } catch (_: Exception) {
            defaultHex?.toColorInt() ?: Color.BLACK
        }
    }

    fun generateAndEmailAttestations(context: Context, mode: AttestationMode) {
        viewModelScope.launch {
            try {
                val s = _ui.value
                if (s.status != HouseholdStatus.SOLDE) {
                    _ui.update { it.copy(error = "Le dossier n’est pas soldé.") }
                    return@launch
                }

                val club = loadClubProfile()

                val files: List<File> = when (mode) {
                    AttestationMode.PER_MEMBER -> {
                        val members = loadMembersDetails(s.memberIds)

                        val addrFromGuardian = listOfNotNull(
                            s.guardianAddress,
                            listOfNotNull(s.guardianPostalCode, s.guardianCity)
                                .joinToString(" ").trim().ifBlank { null }
                        ).joinToString("\n").ifBlank { null }

                        val addr = addrFromGuardian ?: members.firstOrNull()?.let { md ->
                            listOfNotNull(
                                md.adresse,
                                listOfNotNull(md.codePostal, md.ville).joinToString(" ").trim().ifBlank { null }
                            ).joinToString("\n").ifBlank { null }
                        }
                        val n = members.size.coerceAtLeast(1)
                        val per = s.amountDueCents / n
                        members.map { m ->
                            val person = AttestationPdf.Person(
                                nom = m.nom,
                                prenom = m.prenom,
                                dateNaissance = m.dateNaissance,
                                adresse = m.adresse ?: s.guardianAddress,
                                codePostal = m.codePostal ?: s.guardianPostalCode,
                                ville = m.ville ?: s.guardianCity
                            )
                            AttestationPdf.generateForMember(
                                context = context,
                                seasonKey = s.seasonKey,
                                club = club,
                                person = person,
                                amountCents = per
                            )
                        }
                    }
                    AttestationMode.HOUSEHOLD -> {
                        val addr = loadMembersDetails(s.memberIds).firstOrNull()?.let { md ->
                            listOfNotNull(
                                md.adresse,
                                "${md.codePostal ?: ""} ${md.ville ?: ""}".trim().ifBlank { null }
                            ).joinToString("\n").ifBlank { null }
                        }
                        listOf(
                            AttestationPdf.generateForHousehold(
                                context = context,
                                seasonKey = s.seasonKey,
                                club = club,
                                householdDisplayName = s.guardianName.ifBlank { "Foyer" },
                                postalAddress = addr,
                                totalAmountCents = s.amountDueCents
                            )
                        )
                    }
                }

                // Partage via FileProvider (pas d’URI file://)
                val uris = files.map { file ->
                    FileProvider.getUriForFile(
                        context,
                        context.packageName + ".fileprovider",
                        file
                    )
                }

                val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_EMAIL, arrayOf(s.guardianEmail ?: ""))
                    putExtra(Intent.EXTRA_SUBJECT, "Attestation de paiement ${s.seasonKey}")
                    putExtra(Intent.EXTRA_TEXT, "Veuillez trouver ci-joint l’attestation de paiement.")
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Envoyer par email"))
            } catch (e: Exception) {
                _ui.update { it.copy(error = e.message ?: "Erreur attestation") }
            }
        }
    }

    // ---------------------- Helpers ----------------------

    private suspend fun safeFetchBitmap(
        url: String,
        maxWidth: Int = 1024,
        maxHeight: Int = 1024
    ): Bitmap? = withContext(Dispatchers.IO) {
        runCatching {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 5_000
                readTimeout = 7_000
                instanceFollowRedirects = true
            }
            conn.inputStream.use { input ->
                // 1) Decode brute
                val original = BitmapFactory.decodeStream(input) ?: return@runCatching null

                // 2) Redimensionnement si nécessaire (préserve le ratio)
                val w = original.width
                val h = original.height
                val scale = max(w.toFloat() / maxWidth, h.toFloat() / maxHeight)
                if (scale > 1f) {
                    val newW = (w / scale).toInt().coerceAtLeast(1)
                    val newH = (h / scale).toInt().coerceAtLeast(1)
                    original.scale(newW, newH).also {
                        if (it != original) original.recycle()
                    }
                } else {
                    original
                }
            }.also {
                conn.disconnect()
            }
        }.getOrNull()
    }

    fun parseAge(dob: String?): Int? {
        if (dob.isNullOrBlank()) return null
        return try {
            val fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            val birth = LocalDate.parse(dob, fmt)
            Period.between(birth, LocalDate.now()).years
        } catch (_: Exception) { null }
    }

    private fun splitCentsInParts(total: Long, parts: Int): List<Long> {
        if (parts <= 1) return listOf(total)
        val q = total / parts
        val r = (total % parts).toInt()
        return (0 until parts).map { i -> if (i < r) q + 1 else q }
    }

    private fun PaymentEntry.asMap(): Map<String, Any?> = mapOf(
        "type" to type.name,
        "amountCents" to amountCents,
        "date" to date,
        "index" to index,
        "note" to note
    )

    private fun Map<*, *>.asPaymentEntryOrNull(): PaymentEntry? {
        val t = (this["type"] as? String)?.let { runCatching { PaymentMethod.valueOf(it) }.getOrNull() } ?: return null
        val amt = (this["amountCents"] as? Number)?.toLong() ?: return null
        val date = this["date"] as? String
        val index = (this["index"] as? Number)?.toInt()
        val note = this["note"] as? String
        return PaymentEntry(type = t, amountCents = amt, date = date, index = index, note = note)
    }

    private fun AidEntry.asMap(): Map<String, Any?> = mapOf(
        "type" to type.name,
        "code" to code,
        "amountCents" to amountCents
    )

    private fun Map<*, *>.asAidEntryOrNull(): AidEntry? {
        val t = (this["type"] as? String)?.let { runCatching { AidType.valueOf(it) }.getOrNull() } ?: return null
        val amt = (this["amountCents"] as? Number)?.toLong() ?: return null
        val code = this["code"] as? String
        return AidEntry(type = t, code = code, amountCents = amt)
    }

    private fun Any?.toPaymentEntryOrNull(): PaymentEntry? {
        val m = this as? Map<*, *> ?: return null
        val type = (m["type"] as? String)?.let { runCatching { PaymentMethod.valueOf(it) }.getOrNull() } ?: return null
        val amount = (m["amountCents"] as? Number)?.toLong() ?: return null
        val date = m["date"] as? String
        val index = (m["index"] as? Number)?.toInt()
        val note = m["note"] as? String
        return PaymentEntry(type = type, amountCents = amount, date = date, index = index, note = note)
    }

    private fun Any?.toAidEntryOrNull(): AidEntry? {
        val m = this as? Map<*, *> ?: return null
        val type = (m["type"] as? String)?.let { runCatching { AidType.valueOf(it) }.getOrNull() } ?: AidType.AUTRE
        val amount = (m["amountCents"] as? Number)?.toLong() ?: return null
        val code = m["code"] as? String
        return AidEntry(type = type, amountCents = amount, code = code)
    }

    private suspend fun setCotisationPaidForMembers(paid: Boolean) {
        val s = _ui.value
        if (s.memberIds.isEmpty()) return
        val now = System.currentTimeMillis()
        val batch = db.batch()
        s.memberIds.forEach { memberId ->
            val ref = db.collection("adherents").document(memberId)
            batch.set(ref, mapOf("cotisationPaid" to paid, "updatedAt" to now), SetOptions.merge())
        }
        batch.commit().await()
    }
}