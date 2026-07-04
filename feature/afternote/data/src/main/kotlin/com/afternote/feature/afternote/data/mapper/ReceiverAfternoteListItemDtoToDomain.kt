package com.afternote.feature.afternote.data.mapper

import com.afternote.feature.afternote.data.dto.ReceivedAfternoteResponse
import com.afternote.feature.afternote.domain.model.receiver.AfterNoteListItem

/**
 * 서버 카테고리 enum(SOCIAL/GALLERY/PLAYLIST)을 프레젠테이션이 그대로 typeKey로 쓸 수 있는
 * 카테고리 키(SOCIAL_NETWORK/GALLERY_AND_FILES/MEMORIAL)로 정규화한다.
 */
fun ReceivedAfternoteResponse.toDomain(): AfterNoteListItem =
    AfterNoteListItem(
        id = id,
        title = title,
        sourceType = category?.let { serverCategoryToTypeKey(it) },
        lastUpdatedAt = createdAt?.let { formatDateFromServer(it) },
    )

fun List<ReceivedAfternoteResponse>.toReceiverDomainList(): List<AfterNoteListItem> = map { it.toDomain() }

private fun serverCategoryToTypeKey(serverCategory: String): String =
    when (serverCategory.uppercase()) {
        "SOCIAL" -> "SOCIAL_NETWORK"
        "GALLERY" -> "GALLERY_AND_FILES"
        "PLAYLIST", "MUSIC" -> "MEMORIAL"
        else -> serverCategory
    }
