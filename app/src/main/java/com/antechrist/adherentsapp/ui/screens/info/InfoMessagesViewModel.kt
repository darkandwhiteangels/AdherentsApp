package com.antechrist.adherentsapp.ui.screens.info

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antechrist.adherentsapp.domain.model.Audience
import com.antechrist.adherentsapp.domain.model.InfoMessage
import com.antechrist.adherentsapp.domain.model.MessagePriority
import com.antechrist.adherentsapp.domain.model.NotificationGroup
import com.antechrist.adherentsapp.domain.repository.InfoMessagesRepository
import com.antechrist.adherentsapp.domain.repository.NotificationGroupsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UserPermissionsUi(
    val audienceSet: Set<Audience>,
    val canPublish: Boolean
)

data class InfoMessagesUiState(
    val isLoading: Boolean = true,
    val messages: List<InfoMessage> = emptyList(),
    val canPublish: Boolean = false,
    val showNewDialog: Boolean = false,
    val showEditDialog: Boolean = false,
    val selectedMessage: InfoMessage? = null,
    val groups: List<NotificationGroup> = emptyList() // ✅ NEW
)

private data class UiFlags(
    val perms: UserPermissionsUi,
    val showNewDialog: Boolean,
    val showEditDialog: Boolean,
    val selectedMessage: InfoMessage?
)

private data class UiFlagsWithMessages(
    val flags: UiFlags,
    val messages: List<InfoMessage>
)

@HiltViewModel
class InfoMessagesViewModel @Inject constructor(
    private val repo: InfoMessagesRepository,
    private val groupsRepo: NotificationGroupsRepository // ✅ NEW
) : ViewModel() {

    private val _permissions = MutableStateFlow(
        UserPermissionsUi(
            audienceSet = setOf(Audience.ALL_REGISTERED),
            canPublish = false
        )
    )

    private val _showNewDialog = MutableStateFlow(false)
    private val _showEditDialog = MutableStateFlow(false)
    private val _selectedMessage = MutableStateFlow<InfoMessage?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val messagesFlow: StateFlow<List<InfoMessage>> =
        _permissions.flatMapLatest { perms ->
            repo.listenMessagesForAudience(perms.audienceSet)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )

    // ✅ NEW : stream des groupes (actifs)
    private val groupsFlow: StateFlow<List<NotificationGroup>> =
        groupsRepo.streamGroups()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                emptyList()
            )

    val uiState: StateFlow<InfoMessagesUiState> =
        combine(_permissions, _showNewDialog, _showEditDialog, _selectedMessage) { perms, showNew, showEdit, selected ->
            UiFlags(
                perms = perms,
                showNewDialog = showNew,
                showEditDialog = showEdit,
                selectedMessage = selected
            )
        }.combine(messagesFlow) { flags, msgs ->
            UiFlagsWithMessages(flags = flags, messages = msgs)
        }.combine(groupsFlow) { fw, groups ->
            InfoMessagesUiState(
                isLoading = false,
                messages = fw.messages,
                canPublish = fw.flags.perms.canPublish,
                showNewDialog = fw.flags.showNewDialog,
                showEditDialog = fw.flags.showEditDialog,
                selectedMessage = fw.flags.selectedMessage,
                groups = groups
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            InfoMessagesUiState()
        )

    fun openNewDialog() { _showNewDialog.value = true }
    fun closeNewDialog() { _showNewDialog.value = false }

    fun submitNewMessage(
        title: String,
        body: String,
        audience: Audience,
        priority: MessagePriority,
        color: String?,
        targetGroupIds: List<String>,
        createdByUid: String
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()

            val msg = InfoMessage(
                id = "",
                title = title.trim(),
                body = body.trim(),
                createdAt = now,
                createdByUid = createdByUid,
                audience = audience,
                active = true,
                priority = priority,
                color = color,
                targetGroupIds = targetGroupIds
            )

            try {
                repo.publishMessage(msg)
                _showNewDialog.value = false
            } catch (e: Exception) {
                android.util.Log.e("InfoVM", "publishMessage() FAILED", e)
                _showNewDialog.value = false
            }
        }
    }

    fun setUserPermissions(newPerms: UserPermissionsUi) {
        _permissions.value = newPerms
    }

    fun openEditDialog(message: InfoMessage) {
        _selectedMessage.value = message
        _showEditDialog.value = true
    }

    fun closeEditDialog() {
        _showEditDialog.value = false
        _selectedMessage.value = null
    }

    fun updateMessage(messageId: String, title: String, body: String, editorUid: String) {
        viewModelScope.launch {
            try {
                repo.editMessage(
                    messageId = messageId,
                    title = title,
                    body = body,
                    editorUid = editorUid
                )
                closeEditDialog()
            } catch (e: Exception) {
                android.util.Log.e("InfoVM", "Failed to update message", e)
                closeEditDialog()
            }
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            try {
                repo.deleteMessage(messageId)
            } catch (e: Exception) {
                android.util.Log.e("InfoVM", "Failed to delete message", e)
            }
        }
    }
}
