package com.afternote.feature.afternote.data.mapper

import android.util.Log
import com.afternote.feature.afternote.data.dto.AfternoteListItemDto
import com.afternote.feature.afternote.domain.model.author.ListItem

private const val TAG = "AfternoteListMapper"

/**
 * 서버 DTO → 도메인 변환은 이 경계에서만 한다.
 *
 * 서버 `category` 를 해석하지 못하면 항목을 도메인으로 올리지 않는다.
 *
 * 와이어는 관용하고 도메인은 엄격하게 둔다 — [ListItem.type] 은 non-null 이라 «모르는 종류» 를
 * 담을 자리가 없고, 임의의 종류로 메우면 목록·필터·아이콘이 조용히 틀어진다(#1048).
 * 실패의 폭은 목록 전체가 아니라 그 항목 하나로 좁힌다.
 */
fun AfternoteListItemDto.toDomainOrNull(): ListItem? {
    val resolvedType =
        afternoteTypeFromServerCategory(category) ?: run {
            Log.w(TAG, "서버 category 를 해석하지 못해 목록에서 제외한다: id=$afternoteId category=$category")
            return null
        }
    return ListItem(
        id = afternoteId,
        serviceName = title,
        date = formatDateFromServer(createdAt),
        type = resolvedType,
    )
}

fun List<AfternoteListItemDto>.toDomainList(): List<ListItem> = mapNotNull { it.toDomainOrNull() }
