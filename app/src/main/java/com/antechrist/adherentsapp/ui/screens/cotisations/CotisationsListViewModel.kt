package com.antechrist.adherentsapp.ui.screens.cotisations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antechrist.adherentsapp.core.utils.SeasonUtils
import com.antechrist.adherentsapp.domain.finance.calculateHouseholdAmounts
import com.antechrist.adherentsapp.domain.model.finance.AidEntry
import com.antechrist.adherentsapp.domain.model.finance.AidType
import com.antechrist.adherentsapp.domain.model.finance.FinanceSeasonConfig
import com.antechrist.adherentsapp.domain.model.finance.HouseholdStatus
import com.antechrist.adherentsapp.domain.repository.CotisationsHouseholdsRepository
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class HouseholdUi(
    val guardianId: String,
    val displayName: String,
    val email: String?,
    val telephone: String?,
    val memberCount: Int,
    val memberNames: List<String>,
    val amountDueCents: Long?,
    val status: HouseholdStatus?
)

data class CotisationsListUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val seasonKey: String = SeasonUtils.currentSeasonKey(),
    val query: String = "",
    val items: List<HouseholdUi> = emptyList()
)

@HiltViewModel
class CotisationsListViewModel @Inject constructor(
    private val db: FirebaseFirestore,
    private val householdsRepo: CotisationsHouseholdsRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(CotisationsListUiState())
    val ui: StateFlow<CotisationsListUiState> = _ui

    private var reg: ListenerRegistration? = null

    init { observeSeason(SeasonUtils.currentSeasonKey()) }

    fun onQueryChange(q: String) { _ui.update { it.copy(query = q) } }

    fun filteredItems(): List<HouseholdUi> {
        val q = ui.value.query.trim().lowercase()
        val list = ui.value.items
        if (q.isBlank()) return list.sortedWith(ITEMS_ORDER)
        return list.filter { item ->
            item.displayName.lowercase().contains(q) ||
                    (item.email ?: "").lowercase().contains(q) ||
                    (item.telephone ?: "").lowercase().contains(q) ||
                    item.memberNames.any { it.lowercase().contains(q) }
        }.sortedWith(ITEMS_ORDER)
    }

    private fun observeSeason(seasonKey: String) {
        reg?.remove()
        _ui.update { it.copy(loading = true, error = null, seasonKey = seasonKey) }

        reg = db.collection("cotisations_families")
            .document(seasonKey)
            .collection("households")
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    _ui.update { it.copy(loading = false, error = err.message ?: "Erreur Firestore") }
                    return@addSnapshotListener
                }
                if (snap == null) {
                    _ui.update { it.copy(loading = false, items = emptyList()) }
                    return@addSnapshotListener
                }

                viewModelScope.launch {
                    try {
                        val docs = snap.documents
                        if (docs.isEmpty()) {
                            _ui.update { it.copy(loading = false, items = emptyList()) }
                            return@launch
                        }

                        // 1) Tous les guardianIds
                        val guardianIds = docs.mapNotNull { it.getString("guardianId") }.distinct()

                        // 2) Charger les guardians
                        val guardiansMap = mutableMapOf<String, Map<String, Any?>>()
                        for (chunk in guardianIds.chunked(10)) {
                            val gSnap = db.collection("guardians")
                                .whereIn(FieldPath.documentId(), chunk)
                                .get().await()
                            gSnap.documents.forEach { gDoc ->
                                guardiansMap[gDoc.id] = gDoc.data ?: emptyMap()
                            }
                        }

                        // 3) Récupérer tous les memberIds de tous les foyers
                        val allMemberIds: List<String> = docs.flatMap { d ->
                            (d.get("memberIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                        }.distinct()

                        // 4) Charger les adhérents pour construire memberNamesMap
                        val memberNamesMap = mutableMapOf<String, String>()
                        if (allMemberIds.isNotEmpty()) {
                            for (chunk in allMemberIds.chunked(10)) {
                                val mSnap = db.collection("adherents")
                                    .whereIn(FieldPath.documentId(), chunk)
                                    .get().await()
                                mSnap.documents.forEach { mDoc ->
                                    val nomM = (mDoc.getString("nom") ?: "").trim()
                                    val prenomM = (mDoc.getString("prenom") ?: "").trim()
                                    val displayM = listOf(prenomM, nomM).joinToString(" ").trim()
                                        .ifBlank { "(Adhérent)" }
                                    memberNamesMap[mDoc.id] = displayM
                                }
                            }
                        }

                        // 5) Construire la liste finale des items
                        val items = docs.map { d ->
                            val gid = d.getString("guardianId") ?: ""
                            val g = guardiansMap[gid] ?: emptyMap()
                            val nom = (g["nom"] as? String)?.trim().orEmpty()
                            val prenom = (g["prenom"] as? String)?.trim().orEmpty()
                            val display = listOf(prenom, nom).joinToString(" ").trim().ifBlank { "(Responsable)" }
                            val email = g["email"] as? String
                            val tel = g["telephone"] as? String

                            val memberIds = (d.get("memberIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                            val memberNames = memberIds.mapNotNull { memberNamesMap[it] }

                            val amountDue = (d.get("amountDueCents") as? Number)?.toLong()
                            val status = (d.get("status") as? String)?.let {
                                runCatching { HouseholdStatus.valueOf(it) }.getOrNull()
                            }

                            HouseholdUi(
                                guardianId = gid,
                                displayName = display,
                                email = email,
                                telephone = tel,
                                memberCount = memberIds.size,
                                memberNames = memberNames,
                                amountDueCents = amountDue,
                                status = status
                            )
                        }

                        _ui.update { it.copy(loading = false, items = items) }
                    } catch (e: Exception) {
                        _ui.update { it.copy(loading = false, error = e.message ?: "Erreur chargement") }
                    }
                }
            }
    }

    // ——————————————————— Actions ———————————————————

    /**
     * Crée un dossier pour chaque responsable principal manquant.
     * ✅ Corrigé : on ne prend que les **adhérents pratiquants**.
     */
    fun createMissingHouseholds(onDone: (created: Int) -> Unit = {}) {
        viewModelScope.launch {
            val season = ui.value.seasonKey
            try {
                // 1) Tous les pratiquants ayant un primaryGuardianId (filtre côté client)
                val adhSnap = db.collection("adherents")
                    .whereEqualTo("isPractitioner", true)
                    .get().await()

                val byGuardian = mutableMapOf<String, MutableList<Pair<String, Map<String, Any?>>>>()
                adhSnap.documents.forEach { d ->
                    val data = d.data ?: emptyMap<String, Any?>()

                    val primary = d.getString("primaryGuardianId")
                    val gids = (data["guardianIds"] as? List<*>)?.mapNotNull { it as? String }.orEmpty()

                    val gid = primary ?: gids.firstOrNull() ?: return@forEach

                    byGuardian.getOrPut(gid) { mutableListOf() }.add(d.id to data)
                }


                // 2) Households existants
                val existingSnap = db.collection("cotisations_families")
                    .document(season)
                    .collection("households")
                    .get().await()
                val existingIds = existingSnap.documents.mapNotNull { it.getString("guardianId") }.toSet()

                // 3) Config saison
                val cfg = loadSeasonConfig(season)

                // 4) Créer dossiers manquants (uniquement avec les membres pratiquants)
                var created = 0
                byGuardian.forEach { (gid, pairs) ->
                    if (gid in existingIds) return@forEach

                    val memberIds: List<String> = pairs.map { it.first }
                    val ages = pairs.map { (_, m) -> parseAge(m["dateNaissance"] as? String) }

                    val res = calculateHouseholdAmounts(
                        ages = ages,
                        cfg = cfg,
                        oneClassPerWeek = false,
                        aids = emptyList()
                    )

                    val init = mapOf(
                        "guardianId" to gid,
                        "seasonKey" to season,
                        "memberIds" to memberIds,
                        "status" to HouseholdStatus.A_REGLER.name,
                        "oneClassPerWeek" to false,
                        "amountBaseCents" to res.amountBaseCents,
                        "discountCents" to res.discountCents,
                        "aids" to emptyList<Map<String, Any?>>(),
                        "amountDueCents" to res.amountDueCents,
                        "licenceCount" to res.licenceCount,
                        "licenceAmountCents" to res.licenceAmountCents,
                        "paymentsPlan" to emptyList<Map<String, Any?>>(),
                        "paymentsReceived" to emptyList<Map<String, Any?>>(),
                        "updatedAt" to System.currentTimeMillis()
                    )

                    db.collection("cotisations_families").document(season)
                        .collection("households").document(gid)
                        .set(init, SetOptions.merge()).await()

                    created++
                }

                onDone(created)
            } catch (e: Exception) {
                _ui.update { it.copy(error = e.message ?: "Erreur création dossiers") }
                onDone(0)
            }
        }
    }

    /**
     * Recalcule les montants de tous les dossiers.
     * ✅ Corrigé : on reconstruit **seulement** les membres pratiquants du foyer.
     */
    fun recalcAmountsForAll(onDone: (updated: Int) -> Unit = {}) {
        viewModelScope.launch {
            val season = ui.value.seasonKey
            try {
                val cfg = loadSeasonConfig(season)
                val hhSnap = db.collection("cotisations_families")
                    .document(season)
                    .collection("households")
                    .get().await()
                var updated = 0

                for (doc in hhSnap.documents) {
                    val gid = doc.getString("guardianId") ?: continue

                    // 1) 🧩 Rebuild members (pratiquants uniquement)
                    val freshMemberIds = loadMemberIdsByGuardian(gid)

                    // 2) Lire options existantes
                    val oneClass = doc.getBoolean("oneClassPerWeek") ?: false
                    val aidsRaw = (doc.get("aids") as? List<*>) ?: emptyList<Any?>()
                    val aids = aidsRaw.mapNotNull { it.toAidEntryOrNull() }

                    // 3) Recalcul
                    val ages = loadMembersAges(freshMemberIds)
                    val res = calculateHouseholdAmounts(
                        ages = ages,
                        cfg = cfg,
                        oneClassPerWeek = oneClass,
                        aids = aids
                    )

                    // 4) Patch + MAJ memberIds
                    val patch = mapOf(
                        "memberIds" to freshMemberIds,
                        "amountBaseCents" to res.amountBaseCents,
                        "discountCents" to res.discountCents,
                        "amountDueCents" to res.amountDueCents,
                        "licenceCount" to res.licenceCount,
                        "licenceAmountCents" to res.licenceAmountCents,
                        "updatedAt" to System.currentTimeMillis()
                    )
                    doc.reference.set(patch, SetOptions.merge()).await()
                    updated++
                }

                onDone(updated)
            } catch (e: Exception) {
                _ui.update { it.copy(error = e.message ?: "Erreur recalcul") }
                onDone(0)
            }
        }
    }

    // ——————————————————— Helpers ———————————————————

    private suspend fun loadSeasonConfig(seasonKey: String): FinanceSeasonConfig {
        return try {
            val snap = db.collection("finance_seasons").document(seasonKey).get().await()
            if (snap.exists()) {
                val m = snap.data ?: emptyMap<String, Any?>()
                FinanceSeasonConfig(
                    seasonKey = seasonKey,
                    priceAdultCents = (m["priceAdultCents"] as? Number)?.toLong() ?: 18300,
                    priceChild6to16Cents = (m["priceChild6to16Cents"] as? Number)?.toLong() ?: 16800,
                    priceBabyUnder6Cents = (m["priceBabyUnder6Cents"] as? Number)?.toLong() ?: 11100,
                    bundle2AllOver5Cents = (m["bundle2AllOver5Cents"] as? Number)?.toLong() ?: 30600,
                    bundle3AllOver5Cents = (m["bundle3AllOver5Cents"] as? Number)?.toLong() ?: 43200,
                    oneClassPerWeekDiscountCents = (m["oneClassPerWeekDiscountCents"] as? Number)?.toLong() ?: 3000,
                    licencePaidByClub = m["licencePaidByClub"] as? Boolean ?: true,
                    licenceAmountCents = (m["licenceAmountCents"] as? Number)?.toLong() ?: 3900,
                    defaultDueDate = m["defaultDueDate"] as? String
                )
            } else {
                defaultSeasonConfig(seasonKey)
            }
        } catch (_: Exception) {
            defaultSeasonConfig(seasonKey)
        }
    }

    private fun defaultSeasonConfig(seasonKey: String) = FinanceSeasonConfig(
        seasonKey = seasonKey,
        priceAdultCents = 18300,
        priceChild6to16Cents = 16800,
        priceBabyUnder6Cents = 11100,
        bundle2AllOver5Cents = 30600,
        bundle3AllOver5Cents = 43200,
        oneClassPerWeekDiscountCents = 3000,
        licencePaidByClub = true,
        licenceAmountCents = 3900,
        defaultDueDate = null
    )

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

    private fun parseAge(dob: String?): Int? {
        if (dob.isNullOrBlank()) return null
        return try {
            val fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            val birth = LocalDate.parse(dob, fmt)
            Period.between(birth, LocalDate.now()).years
        } catch (_: Exception) { null }
    }

    private fun Any?.toAidEntryOrNull(): AidEntry? {
        val m = this as? Map<*, *> ?: return null
        val type = (m["type"] as? String)?.let { runCatching { AidType.valueOf(it) }.getOrNull() } ?: return null
        val amount = (m["amountCents"] as? Number)?.toLong() ?: return null
        val code = m["code"] as? String
        return AidEntry(type = type, code = code, amountCents = amount)
    }

    /**
     * ✅ Ne retourne que les **adhérents pratiquants** liés au foyer :
     *    - primaryGuardianId == guardianId (ET isPractitioner == true)
     *    - UNION whereArrayContains("guardianIds", guardianId) (ET isPractitioner == true)
     */
    private suspend fun loadMemberIdsByGuardian(guardianId: String): List<String> {
        // Liés en principal + pratiquants
        val primDocs = db.collection("adherents")
            .whereEqualTo("primaryGuardianId", guardianId)
            .whereEqualTo("isPractitioner", true)
            .get().await()
            .documents

        // Liés par le tableau + pratiquants
        val arrDocs = db.collection("adherents")
            .whereArrayContains("guardianIds", guardianId)
            .whereEqualTo("isPractitioner", true)
            .get().await()
            .documents

        return (primDocs + arrDocs).map { it.id }.distinct()
    }

    override fun onCleared() { reg?.remove(); super.onCleared() }
}

/** Tri : soldés en bas, puis en retard, à régler, annulés, puis alpha. */
private val ITEMS_ORDER = compareBy<HouseholdUi>(
    { statusRank(it.status) },
    { it.displayName.lowercase() }
)

private fun statusRank(s: HouseholdStatus?): Int = when (s) {
    HouseholdStatus.EN_RETARD -> 0
    HouseholdStatus.A_REGLER -> 1
    HouseholdStatus.ANNULE   -> 2
    HouseholdStatus.SOLDE    -> 3
    null                     -> 4
}