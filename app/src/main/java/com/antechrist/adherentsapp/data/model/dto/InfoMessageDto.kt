package com.antechrist.adherentsapp.data.model.dto

import com.antechrist.adherentsapp.domain.model.Audience
import com.antechrist.adherentsapp.domain.model.InfoMessage
import com.antechrist.adherentsapp.domain.model.MessagePriority
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

data class InfoMessageDto(
    @DocumentId
    val id: String? = null,

    @get:PropertyName("title")
    @set:PropertyName("title")
    var title: String? = null,

    @get:PropertyName("body")
    @set:PropertyName("body")
    var body: String? = null,

    @get:PropertyName("createdAt")
    @set:PropertyName("createdAt")
    var createdAt: Timestamp? = null,

    @get:PropertyName("createdByUid")
    @set:PropertyName("createdByUid")
    var createdByUid: String? = null,

    @get:PropertyName("audience")
    @set:PropertyName("audience")
    var audience: String? = null,

    @get:PropertyName("active")
    @set:PropertyName("active")
    var active: Boolean? = null,

    // 🆕 nouveaux champs (optionnels)
    @get:PropertyName("priority")
    @set:PropertyName("priority")
    var priority: String? = null,

    @get:PropertyName("color")
    @set:PropertyName("color")
    var color: String? = null,

    @get:PropertyName("targetGroupIds")
    @set:PropertyName("targetGroupIds")
    var targetGroupIds: List<String>? = null
)

fun InfoMessageDto.toDomain(): InfoMessage? {
    val safeId = id ?: return null
    val safeTitle = title ?: return null
    val safeBody = body ?: return null
    val safeTs = createdAt?.toDate()?.time ?: return null
    val safeUid = createdByUid ?: return null

    val safeAudience = audience?.let {
        runCatching { Audience.valueOf(it) }.getOrNull()
    } ?: return null

    val safeActive = active ?: false

    val safePriority = priority?.let {
        runCatching { MessagePriority.valueOf(it) }.getOrNull()
    } ?: MessagePriority.NORMAL

    return InfoMessage(
        id = safeId,
        title = safeTitle,
        body = safeBody,
        createdAt = safeTs,
        createdByUid = safeUid,
        audience = safeAudience,
        active = safeActive,
        priority = safePriority,
        color = color,
        targetGroupIds = targetGroupIds ?: emptyList()
    )
}

fun InfoMessage.toDto(): InfoMessageDto =
    InfoMessageDto(
        id = this.id.ifBlank { null },
        title = this.title,
        body = this.body,
        createdAt = Timestamp(this.createdAt / 1000, ((this.createdAt % 1000) * 1_000_000).toInt()),
        createdByUid = this.createdByUid,
        audience = this.audience.name,
        active = this.active,
        priority = this.priority.name,
        color = this.color,
        targetGroupIds = this.targetGroupIds
    )
