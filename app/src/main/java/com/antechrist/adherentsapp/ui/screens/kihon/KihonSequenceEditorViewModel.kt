package com.antechrist.adherentsapp.ui.screens.kihon

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antechrist.adherentsapp.domain.model.*
import com.antechrist.adherentsapp.domain.model.kihon.*
import com.antechrist.adherentsapp.domain.usecase.GetKihonCatalog
import com.antechrist.adherentsapp.domain.usecase.GetKihonSequencesByGrade
import com.antechrist.adherentsapp.domain.usecase.PublishKihonSequence
import com.antechrist.adherentsapp.domain.usecase.SaveKihonSequence
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG_SAVE = "[KihonSave]"

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class KihonSequenceEditorViewModel @Inject constructor(
    private val getByGrade: GetKihonSequencesByGrade,
    private val getCatalog: GetKihonCatalog,
    private val save: SaveKihonSequence,
    private val publish: PublishKihonSequence,
    private val auth: FirebaseAuth
) : ViewModel() {

    /* ────────────────────────────── Logs ────────────────────────────── */
    private companion object {
        private const val TAG = "KihonEditorVM"
    }

    /* ────────────────────────────── UI State principale ────────────────────────────── */

    data class UiState(
        val loading: Boolean = true,
        val catalog: Map<String, TechniqueRef> = emptyMap(),
        val sequence: KihonSequence? = null,
        val saving: Boolean = false,
        val error: String? = null,
        // Drafts méta
        val nameDraft: String = "",
        val objectiveDraft: String = "",
        val tagsDraft: List<String> = emptyList(),
        val examDraft: Boolean = false
    )

    private val gradeKey = MutableStateFlow<String?>(null)
    private val sequenceId = MutableStateFlow<String?>(null)
    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    /* ────────────────────────────── Draft "à tokens" ────────────────────────────── */

    // Étape en cours d’édition via la barre d’outils (draft à tokens)
    // IMPORTANT: state Compose -> toute réassignation déclenche la recomposition
    var stepDraft by mutableStateOf(StepDraft())
        private set

    // Flux optionnel pour observer le draft si besoin côté UI
    private val _draftState = MutableStateFlow(stepDraft)
    val draftState = _draftState.asStateFlow()

    /** Force une recomposition après mutation interne (append/remove/move...). */
    private fun bumpDraft(reason: String) {
        // on clone les tokens (immutabilité) et on *renouvelle* le stamp
        val cloned = stepDraft.tokens.toMutableList().toList()
        stepDraft = stepDraft.copy(tokens = cloned, stamp = System.nanoTime())
        _draftState.value = stepDraft
        Log.d(TAG, "Draft bump: $reason -> tokens=${stepDraft.tokens.size} selectedIndex=${stepDraft.selectedIndex}")
    }

    /** Dump lisible du contenu du draft pour logcat. */
    private fun logDraft(prefix: String) {
        val tokensStr = stepDraft.tokens.joinToString(" | ") { t ->
            when (t) {
                is DraftToken.MovementToken  -> "MVT:${t.movement}"
                is DraftToken.PositionToken  -> "POS:${t.position.id}"
                is DraftToken.TechniqueToken -> "TEC:${t.technique.id}"
                is DraftToken.OptionToken    -> "OPT:${t.option}=${t.value}"
            }
        }
        Log.d(TAG, "$prefix tokens=[$tokensStr] selectedIndex=${stepDraft.selectedIndex}")
    }

    /* ─────────── Handlers BO -> draft (append / replace / remove / move) ─────────── */

    // ───────── Handlers BO → Draft (corrigés : réaffectation + log) ─────────

    fun onPickMovement(m: Movement) {
        Log.d(TAG, "onPickMovement($m) selectedIndex=${stepDraft.selectedIndex}")
        stepDraft = stepDraft.appendOrReplace(DraftToken.MovementToken(m))
        logDraft("after movement")
        bumpDraft("movement")
    }

    fun onPickPosition(p: TechniqueRef) {
        Log.d(TAG, "onPickPosition(${p.id}) selectedIndex=${stepDraft.selectedIndex}")
        stepDraft = stepDraft.appendOrReplace(DraftToken.PositionToken(position = p))
        logDraft("after position")
        bumpDraft("position")
    }

    fun onPickTechnique(t: TechniqueRef) {
        Log.d(TAG, "onPickTechnique(${t.id}) selectedIndex=${stepDraft.selectedIndex}")
        stepDraft = stepDraft.appendOrReplace(DraftToken.TechniqueToken(technique = t))
        logDraft("after technique")
        bumpDraft("technique")
    }

    fun onPickHeight(h: Height) {
        Log.d(TAG, "onPickHeight($h) selectedIndex=${stepDraft.selectedIndex}")
        stepDraft = stepDraft.appendOrReplace(DraftToken.OptionToken(OptionKind.HEIGHT, h))
        logDraft("after height")
        bumpDraft("height")
    }

    fun onPickOptions(role: ExecutingLimbRole, landing: FootLanding) {
        Log.d(TAG, "onPickOptions(role=$role, landing=$landing) selectedIndex=${stepDraft.selectedIndex}")
        stepDraft = stepDraft.appendOrReplace(DraftToken.OptionToken(OptionKind.EXECUTING_LIMB, role))
        stepDraft = stepDraft.appendOrReplace(DraftToken.OptionToken(OptionKind.FOOT_LANDING, landing))
        logDraft("after options")
        bumpDraft("options")
    }

    fun onPickDirection(dir: Direction) {
        Log.d(TAG, "onPickDirection($dir) selectedIndex=${stepDraft.selectedIndex}")
        stepDraft = stepDraft.appendOrReplace(DraftToken.OptionToken(OptionKind.DIRECTION, dir))
        logDraft("after direction")
        bumpDraft("direction")
    }

    fun onPickTempo(tempo: Tempo) {
        Log.d(TAG, "onPickTempo($tempo) selectedIndex=${stepDraft.selectedIndex}")
        stepDraft = stepDraft.appendOrReplace(DraftToken.OptionToken(OptionKind.TEMPO, tempo))
        logDraft("after tempo")
        bumpDraft("tempo")
    }

    fun toggleKiai() {
        Log.d(TAG, "toggleKiai() selectedIndex=${stepDraft.selectedIndex}")

        val idx = stepDraft.tokens.indexOfLast { it is DraftToken.OptionToken && it.option == OptionKind.KIAI }
        if (idx >= 0) {
            // OFF : supprime le dernier KIAI
            stepDraft = stepDraft.removeAt(idx)
            logDraft("after kiai OFF")
            bumpDraft("kiai_off")
        } else {
            // ON : ajoute le flag
            stepDraft = stepDraft.appendOrReplace(DraftToken.OptionToken(OptionKind.KIAI, true))
            logDraft("after kiai ON")
            bumpDraft("kiai_on")
        }
    }

    fun selectDraftToken(index: Int) {
        Log.d(TAG, "selectDraftToken($index)")
        stepDraft = stepDraft.select(index)
        bumpDraft("select")
    }

    fun removeDraftToken(index: Int) {
        Log.d(TAG, "removeDraftToken($index)")
        stepDraft = stepDraft.removeAt(index)
        logDraft("after remove")
        bumpDraft("remove")
    }

    fun moveDraftToken(from: Int, to: Int) {
        Log.d(TAG, "moveDraftToken($from -> $to)")
        stepDraft = stepDraft.move(from, to)
        logDraft("after move")
        bumpDraft("move")
    }

    fun commitDraftAsStep() {
        Log.d(TAG, "commitDraftAsStep() isComplete=${stepDraft.isComplete} size=${stepDraft.tokens.size}")
        val seq = _ui.value.sequence ?: run {
            Log.w(TAG, "No sequence in UI state")
            return
        }
        if (!stepDraft.isComplete) {
            Log.w(TAG, "Draft not complete -> skip")
            return
        }

        val start = seq.steps.lastOrNull()?.endPosition
            ?: TechniqueRef(
                id = "hidari_kamae",
                nameFr = "Hidari Kamae",
                nameJa = "Hidari Kamae",
                kind = TechniqueRef.Kind.POSITION
            )

        val newStep = stepDraft.toKihonStep(start)
        if (newStep == null) {
            Log.w(TAG, "toKihonStep returned null")
            return
        }

        Log.d(TAG, "Add step: movement=${newStep.movement} technique=${newStep.technique.id} end=${newStep.endPosition.id} foot=${newStep.footLanding} exec=${newStep.executingLimbRole} tempo=${newStep.tempo} kiai=${newStep.kiai}")

        val updatedSeq = seq.copy(steps = seq.steps + newStep)
        _ui.update { it.copy(sequence = updatedSeq) }

        stepDraft = stepDraft.reset()
        bumpDraft("reset")
    }


    /* ────────────────────────────── Initialisation catalogue / séquence ────────────────────────────── */

    init {
        Log.d(TAG, "VM init")
        val catalogFlow: Flow<Map<String, TechniqueRef>> = getCatalog()
            .onStart { Log.d(TAG, "Catalog: onStart") }
            .onEach { Log.d(TAG, "Catalog: loaded size=${it.size}") }
            .catch { e ->
                Log.e(TAG, "Catalog: error=${e.message}", e)
                _ui.update { it.copy(error = e.message ?: "Erreur catalogue") }
            }

        val sequencesFlow: Flow<List<KihonSequence>> =
            gradeKey
                .onEach { Log.d(TAG, "gradeKey -> $it") }
                .filterNotNull()
                .flatMapLatest { g ->
                    Log.d(TAG, "Fetch sequences for grade=$g")
                    getByGrade(g)
                        .onStart { Log.d(TAG, "Sequences($g): onStart") }
                        .onEach { Log.d(TAG, "Sequences($g): loaded size=${it.size}") }
                        .catch { e ->
                            Log.e(TAG, "Sequences($g): error=${e.message}", e)
                            _ui.update {
                                it.copy(
                                    loading = false,
                                    error = e.message ?: "Erreur Firestore",
                                    sequence = null
                                )
                            }
                            emit(emptyList())
                        }
                }

        val selectedSeqFlow: Flow<KihonSequence?> =
            combine(sequencesFlow, sequenceId.onEach { Log.d(TAG, "sequenceId -> $it") }.filterNotNull()) { list, id ->
                val found = list.firstOrNull { it.id == id }
                Log.d(TAG, "Select sequence id=$id -> ${if (found == null) "null" else "OK"}")
                found
            }

        viewModelScope.launch {
            combine(catalogFlow, selectedSeqFlow) { catalog, seq -> catalog to seq }
                .onStart { _ui.update { it.copy(loading = true, error = null) } }
                .collect { (catalog, seq) ->
                    if (seq == null) {
                        Log.w(TAG, "Selected sequence is null (deleted ?)")
                        _ui.update {
                            it.copy(
                                loading = false,
                                catalog = catalog,
                                sequence = null,
                                error = "Séquence introuvable (supprimée ?)"
                            )
                        }
                    } else {
                        Log.d(TAG, "Bind sequence id=${seq.id} steps=${seq.steps.size}")
                        _ui.update {
                            it.copy(
                                loading = false,
                                catalog = catalog,
                                sequence = seq,
                                error = null,
                                nameDraft = if (it.sequence?.id == seq.id) it.nameDraft else seq.name,
                                objectiveDraft = if (it.sequence?.id == seq.id) it.objectiveDraft else (seq.objective ?: ""),
                                tagsDraft = if (it.sequence?.id == seq.id) it.tagsDraft else seq.tags,
                                examDraft = if (it.sequence?.id == seq.id) it.examDraft else seq.isExamRequired
                            )
                        }
                    }
                }
        }
    }

    fun setArgs(grade: String, id: String) {
        Log.d(TAG, "setArgs grade=$grade id=$id")
        gradeKey.value = grade
        sequenceId.value = id
    }

    /* ────────────────────────────── META DRAFTS ────────────────────────────── */

    fun onNameChange(v: String) {
        Log.d(TAG, "meta name -> $v")
        _ui.update { it.copy(nameDraft = v) }
    }

    fun onObjectiveChange(v: String) {
        Log.d(TAG, "meta objective -> $v")
        _ui.update { it.copy(objectiveDraft = v) }
    }

    fun onTagsChange(v: List<String>) {
        Log.d(TAG, "meta tags -> ${v.joinToString()}")
        _ui.update { it.copy(tagsDraft = v) }
    }

    fun onExamRequiredChange(v: Boolean) {
        Log.d(TAG, "meta examRequired -> $v")
        _ui.update { it.copy(examDraft = v) }
    }

    // ───────── Options dédiées (BO) ─────────

    fun onPickExecutingLimb(role: ExecutingLimbRole) {
        Log.d(TAG, "onPickExecutingLimb($role)")
        stepDraft = stepDraft.appendOrReplace(DraftToken.OptionToken(OptionKind.EXECUTING_LIMB, role))
        logDraft("after exec limb")
        bumpDraft("exec_limb")
    }

    fun onPickFootLanding(landing: FootLanding) {
        Log.d(TAG, "onPickFootLanding($landing)")
        stepDraft = stepDraft.appendOrReplace(DraftToken.OptionToken(OptionKind.FOOT_LANDING, landing))
        logDraft("after foot landing")
        bumpDraft("foot_landing")
    }

    fun toggleSameArm() {
        Log.d(TAG, "toggleSameArm()")
        // exclusif avec SAME_LEG
        val idxArm = stepDraft.tokens.indexOfLast { it is DraftToken.OptionToken && it.option == OptionKind.SAME_ARM }
        val idxLeg = stepDraft.tokens.indexOfLast { it is DraftToken.OptionToken && it.option == OptionKind.SAME_LEG }
        stepDraft = when {
            idxArm >= 0 -> stepDraft.removeAt(idxArm)                   // désactive
            else -> {
                var tmp = stepDraft
                if (idxLeg >= 0) tmp = tmp.removeAt(idxLeg)             // retire SAME_LEG si présent
                tmp.appendOrReplace(DraftToken.OptionToken(OptionKind.SAME_ARM, true))
            }
        }
        logDraft("after same_arm toggle")
        bumpDraft("same_arm")
    }

    fun toggleSameLeg() {
        Log.d(TAG, "toggleSameLeg()")
        // exclusif avec SAME_ARM
        val idxArm = stepDraft.tokens.indexOfLast { it is DraftToken.OptionToken && it.option == OptionKind.SAME_ARM }
        val idxLeg = stepDraft.tokens.indexOfLast { it is DraftToken.OptionToken && it.option == OptionKind.SAME_LEG }
        stepDraft = when {
            idxLeg >= 0 -> stepDraft.removeAt(idxLeg)                   // désactive
            else -> {
                var tmp = stepDraft
                if (idxArm >= 0) tmp = tmp.removeAt(idxArm)             // retire SAME_ARM si présent
                tmp.appendOrReplace(DraftToken.OptionToken(OptionKind.SAME_LEG, true))
            }
        }
        logDraft("after same_leg toggle")
        bumpDraft("same_leg")
    }


    fun setMeta(
        name: String? = null,
        objective: String? = null,
        tags: List<String>? = null,
        isExamRequired: Boolean? = null
    ) {
        Log.d(TAG, "setMeta(name=${name != null}, objective=${objective != null}, tags=${tags != null}, exam=${isExamRequired != null})")
        _ui.update {
            it.copy(
                nameDraft = name ?: it.nameDraft,
                objectiveDraft = objective ?: it.objectiveDraft,
                tagsDraft = tags ?: it.tagsDraft,
                examDraft = isExamRequired ?: it.examDraft
            )
        }
    }

    /* ────────────────────────────── STEPS CRUD (UI seulement) ────────────────────────────── */

    fun updateStep(index: Int, step: KihonStep) {
        val seq = _ui.value.sequence ?: return
        if (index !in seq.steps.indices) return
        val mutable = seq.steps.toMutableList()
        mutable[index] = step
        _ui.update { it.copy(sequence = seq.copy(steps = mutable)) }
        Log.d(TAG, "updateStep($index) -> technique=${step.technique.id}")
    }

    fun removeStep(index: Int) {
        val seq = _ui.value.sequence ?: return
        if (index !in seq.steps.indices) return
        val mutable = seq.steps.toMutableList()
        mutable.removeAt(index)
        _ui.update { it.copy(sequence = seq.copy(steps = mutable)) }
        Log.d(TAG, "removeStep($index)")
    }

    fun moveStepUp(index: Int) {
        val seq = _ui.value.sequence ?: return
        if (index <= 0 || index >= seq.steps.size) return
        val mutable = seq.steps.toMutableList()
        val item = mutable.removeAt(index)
        mutable.add(index - 1, item)
        _ui.update { it.copy(sequence = seq.copy(steps = mutable)) }
        Log.d(TAG, "moveStepUp($index)")
    }

    fun moveStepDown(index: Int) {
        val seq = _ui.value.sequence ?: return
        if (index < 0 || index >= seq.steps.lastIndex) return
        val mutable = seq.steps.toMutableList()
        val item = mutable.removeAt(index)
        mutable.add(index + 1, item)
        _ui.update { it.copy(sequence = seq.copy(steps = mutable)) }
        Log.d(TAG, "moveStepDown($index)")
    }

    /* ────────────────────────────── ACTIONS PERSISTANTES ────────────────────────────── */

    /** Résumé compact d’un step pour Logcat (évite murs de texte). */
    private fun stepSummary(s: KihonStep, idx: Int): String =
        "#$idx(mvt=${s.movement}, dir=${s.direction}, h=${s.height}, exec=${s.executingLimbRole}, foot=${s.footLanding}, tempo=${s.tempo}, kiai=${s.kiai}, tech=${s.technique.id}, start=${s.startPosition.id}, end=${s.endPosition.id})"

    /** Résumé compact de la séquence pour Logcat. Tronque à 8 steps pour la lisibilité. */
    private fun sequenceSummary(seq: KihonSequence): String {
        val head = seq.steps.take(8).mapIndexed { i, st -> stepSummary(st, i + 1) }.joinToString(" | ")
        val tail = if (seq.steps.size > 8) " …(+${seq.steps.size - 8})" else ""
        return "id=${seq.id}, grade=${seq.gradeKey}, name='${seq.name}', tags=${seq.tags.size}, exam=${seq.isExamRequired}, steps=${seq.steps.size} :: $head$tail"
    }

    /** Handler simple pour le bouton 💾 côté UI. */
    fun onSaveClick() {
        val snap = _ui.value.sequence
        Log.d(TAG, "onSaveClick() seq=${snap?.id ?: "null"}")
        viewModelScope.launch {
            val r = saveNow()
            if (r.isSuccess) {
                Log.d(TAG, "onSaveClick() -> OK id=${r.getOrNull()}")
            } else {
                Log.e(TAG, "onSaveClick() -> FAIL ${r.exceptionOrNull()?.message}", r.exceptionOrNull())
            }
        }
    }


//    suspend fun saveNow(): Result<String> {
//        val current = _ui.value.sequence ?: return Result.failure(IllegalStateException("Aucune séquence"))
//        val updated = current.copy(
//            name = _ui.value.nameDraft,
//            objective = _ui.value.objectiveDraft.ifBlank { null },
//            tags = _ui.value.tagsDraft,
//            isExamRequired = _ui.value.examDraft,
//            updatedAt = System.currentTimeMillis()
//        )
//        _ui.update { it.copy(saving = true, error = null) }
//        Log.d(TAG, "saveNow(id=${updated.id})")
//        val res: Result<Unit> = save(updated)
//        _ui.update { it.copy(saving = false) }
//        return res
//            .onSuccess {
//                _ui.update { it.copy(sequence = updated) }
//                Log.d(TAG, "saveNow OK")
//            }
//            .map { updated.id }
//            .onFailure { e ->
//                _ui.update { it.copy(error = e.message ?: "Échec d'enregistrement") }
//                Log.e(TAG, "saveNow FAIL: ${e.message}", e)
//            }
//    }

    suspend fun saveNow(): Result<String> {
        val current = _ui.value.sequence ?: return Result.failure(IllegalStateException("Aucune séquence"))

        // Snapshot des drafts meta appliqués
        val updated = current.copy(
            name = _ui.value.nameDraft,
            objective = _ui.value.objectiveDraft.ifBlank { null },
            tags = _ui.value.tagsDraft,
            isExamRequired = _ui.value.examDraft,
            updatedAt = System.currentTimeMillis()
        )

        // Logs détaillés avant persist
        Log.d(TAG, "saveNow(): PREP " + sequenceSummary(updated))
        if (updated.tags.isEmpty()) Log.d(TAG, "saveNow(): note -> aucun tag")
        if (updated.objective.isNullOrBlank()) Log.d(TAG, "saveNow(): note -> pas d'objectif")

        _ui.update { it.copy(saving = true, error = null) }
        Log.d(TAG, "saveNow(): saving=true (dispatch usecase)")

        val t0 = System.currentTimeMillis()
        val res: Result<Unit> = save(updated)
        val dt = System.currentTimeMillis() - t0

        _ui.update { it.copy(saving = false) }
        Log.d(TAG, "saveNow(): saving=false (usecase finished in ${dt}ms)")

        return res
            .onSuccess {
                _ui.update { it.copy(sequence = updated) }
                Log.d(TAG, "saveNow(): OK " + sequenceSummary(updated))
            }
            .map { updated.id }
            .onFailure { e ->
                _ui.update { it.copy(error = e.message ?: "Échec d'enregistrement") }
                Log.e(TAG, "saveNow(): FAIL ${e.message}", e)
            }
    }

    suspend fun publishNow(): Result<Unit> {
        val seq = _ui.value.sequence ?: return Result.failure(IllegalStateException("Aucune séquence"))
        val uid = auth.currentUser?.uid ?: "unknown"
        Log.d(TAG, "publishNow(id=${seq.id}, by=$uid)")
        val res = publish(seq.id, uid)
        return res.onSuccess {
            _ui.update {
                it.copy(
                    sequence = seq.copy(
                        status = KihonSequence.Status.PUBLISHED,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
            Log.d(TAG, "publishNow OK")
        }.onFailure { e ->
            _ui.update { it.copy(error = e.message ?: "Publication impossible") }
            Log.e(TAG, "publishNow FAIL: ${e.message}", e)
        }
    }

//    fun markReady() {
//        val seq = _ui.value.sequence ?: return
//        if (seq.steps.isEmpty()) return
//        _ui.update {
//            it.copy(
//                sequence = seq.copy(
//                    status = KihonSequence.Status.READY,
//                    updatedAt = System.currentTimeMillis()
//                )
//            )
//        }
//        Log.d(TAG, "markReady()")
//    }
    fun markReady() {
        val seq = _ui.value.sequence ?: return
        if (seq.steps.isEmpty()) return
        val next = seq.copy(
            status = KihonSequence.Status.READY,
            updatedAt = System.currentTimeMillis()
        )
        _ui.update { it.copy(sequence = next) }
        Log.d(TAG, "markReady(): " + sequenceSummary(next))
    }

}
