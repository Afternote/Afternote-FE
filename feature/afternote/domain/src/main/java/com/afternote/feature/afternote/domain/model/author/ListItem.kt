package com.afternote.feature.afternote.domain.model.author

import com.afternote.feature.afternote.domain.AfternoteType

/**
 * 애프터노트 아이템 도메인 모델
 *
 * @param id 고유 식별자
 * @param serviceName 서비스명
 * @param date 날짜 (yyyy.MM.dd 형식)
 * @param type 서비스 타입 (필터링용)
 * @param isDraft 임시저장 여부. 목록에서 여는 방향이 갈린다 — 발행분은 상세 화면, 임시저장은 에디터 이어쓰기.
 */
data class ListItem(
    val id: Long,
    val serviceName: String,
    val date: String,
    val type: AfternoteType,
    val isDraft: Boolean = false,
    val account: Account = Account(),
    val processing: ItemProcessing = ItemProcessing(),
)

data class Account(
    val id: String = "",
    val password: String = "",
)

data class ItemProcessing(
    val message: String = "",
    val accountMethod: String = "",
    val informationMethod: String = "",
    val methods: List<ProcessingMethod> = emptyList(),
    val galleryMethods: List<ProcessingMethod> = emptyList(),
)

data class ProcessingMethod(
    val id: String,
    val text: String,
)
