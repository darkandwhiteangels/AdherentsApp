package com.antechrist.adherentsapp.domain.repository

import com.antechrist.adherentsapp.domain.model.NotificationGroup
import kotlinx.coroutines.flow.Flow

interface NotificationGroupsRepository {
    fun streamGroups(): Flow<List<NotificationGroup>>

    suspend fun createGroup(name: String, memberGuardianIds: List<String>, createdByUid: String): String
    suspend fun rename(groupId: String, name: String)
    suspend fun updateMembers(groupId: String, memberGuardianIds: List<String>)
    suspend fun deleteGroup(groupId: String)
}
