package com.antechrist.adherentsapp.data.firestore

import com.antechrist.adherentsapp.domain.model.NotificationGroup
import com.antechrist.adherentsapp.domain.repository.NotificationGroupsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class NotificationGroupsRepositoryImpl @Inject constructor(
    private val remote: NotificationGroupsRemoteDataSource
) : NotificationGroupsRepository {

    override fun streamGroups(): Flow<List<NotificationGroup>> = remote.streamGroups()

    override suspend fun createGroup(name: String, memberGuardianIds: List<String>, createdByUid: String): String =
        remote.createGroup(name, memberGuardianIds, createdByUid)

    override suspend fun rename(groupId: String, name: String) = remote.rename(groupId, name)

    override suspend fun updateMembers(groupId: String, memberGuardianIds: List<String>) =
        remote.updateMembers(groupId, memberGuardianIds)

    override suspend fun deleteGroup(groupId: String) = remote.deleteGroup(groupId)
}
