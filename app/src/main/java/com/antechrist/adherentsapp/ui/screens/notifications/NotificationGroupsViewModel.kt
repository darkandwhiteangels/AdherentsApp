package com.antechrist.adherentsapp.ui.screens.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antechrist.adherentsapp.domain.model.Guardian
import com.antechrist.adherentsapp.domain.model.NotificationGroup
import com.antechrist.adherentsapp.domain.repository.GuardiansRepository
import com.antechrist.adherentsapp.domain.repository.NotificationGroupsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationGroupsUiState(
    val isLoading: Boolean = true,
    val guardians: List<Guardian> = emptyList(),
    val groups: List<NotificationGroup> = emptyList(),
    val showCreateDialog: Boolean = false
)

@HiltViewModel
class NotificationGroupsViewModel @Inject constructor(
    private val guardiansRepo: GuardiansRepository,
    private val groupsRepo: NotificationGroupsRepository
) : ViewModel() {

    private val showCreate = MutableStateFlow(false)

    private val guardiansFlow = guardiansRepo.streamAll()
    private val groupsFlow = groupsRepo.streamGroups()

    val uiState: StateFlow<NotificationGroupsUiState> =
        combine(guardiansFlow, groupsFlow, showCreate) { guardians, groups, showDialog ->
            NotificationGroupsUiState(
                isLoading = false,
                guardians = guardians,
                groups = groups,
                showCreateDialog = showDialog
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NotificationGroupsUiState())

    fun createGroup(currentUserUid: String, name: String, guardianIds: List<String>) {
        viewModelScope.launch {
            try { groupsRepo.createGroup(name, guardianIds, currentUserUid) }
            catch (e: Exception) { android.util.Log.e("GroupsVM", "createGroup failed", e) }
            finally { showCreate.value = false }
        }
    }

    fun openCreateDialog() { showCreate.value = true }
    fun closeCreateDialog() { showCreate.value = false }

    fun deleteGroup(groupId: String) {
        viewModelScope.launch {
            try {
                groupsRepo.deleteGroup(groupId)
                android.util.Log.d("GroupsVM", "deleteGroup OK: $groupId")
            } catch (e: Exception) {
                android.util.Log.e("GroupsVM", "deleteGroup FAILED: $groupId", e)
            }
        }
    }

    fun updateGroup(groupId: String, name: String, memberGuardianIds: List<String>) {
        viewModelScope.launch {
            try {
                // si tu veux gérer rename séparé, on fait les deux
                groupsRepo.rename(groupId, name)
                groupsRepo.updateMembers(groupId, memberGuardianIds)
                android.util.Log.d("GroupsVM", "updateGroup OK: $groupId")
            } catch (e: Exception) {
                android.util.Log.e("GroupsVM", "updateGroup FAILED: $groupId", e)
            }
        }
    }
}
