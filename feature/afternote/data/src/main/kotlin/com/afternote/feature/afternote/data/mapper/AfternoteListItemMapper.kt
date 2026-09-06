package com.afternote.feature.afternote.data.mapper

import android.util.Log
import com.afternote.feature.afternote.data.dto.AfternoteListItemDto
import com.afternote.feature.afternote.domain.model.author.ListItem

private const val TAG = "AfternoteListMapper"

/**
 * 서버 DTO → 도메인 변환은 이 경계에서만 한다.
 *
 * 서버 `category` 를 해석하지 못한 항목은 목록에 올리지 않는다. 와이어는 관용하고 도메인은
 * 엄격하게 둔다 — [ListItem.type] 은 non-null 이라 «모르는 종류» 를 담을 자리가 없고,
 * 임의의 종류로 메우면 목록·필터·아이콘이 조용히 틀어진다(#1048).
 * 실패의 폭은 목록 전체가 아니라 그 항목 하나다.
 */
fun List<AfternoteListItemDto>.toDomainList(): List<ListItem> =
    mapNotNull { dto ->
        val resolvedType =
            afternoteTypeFromServerCategory(dto.category) ?: run {
                Log.w(TAG, "서버 category 를 해석하지 못해 목록에서 제외한다: id=${dto.afternoteId} category=${dto.category}")
                return@mapNotNull null
            }
        ListItem(
            id = dto.afternoteId,
            serviceName = dto.title,
            date = formatDateFromServer(dto.createdAt),
            type = resolvedType,
            isDraft = dto.isDraft,
        )
    }
