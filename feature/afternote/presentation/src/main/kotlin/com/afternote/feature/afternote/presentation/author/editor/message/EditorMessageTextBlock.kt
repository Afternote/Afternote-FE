package com.afternote.feature.afternote.presentation.author.editor.message

/**
 * "남기실 말씀" 한 덩어리의 편집용 값.
 *
 * 편집 중에는 제목·본문이 빈 문자열일 수 있어 도메인
 * [com.afternote.feature.afternote.domain.model.LeaveMessageBlock] 과 달리 둘 다 non-null 이다.
 * [isRegistered]는 에디터 표시 상태라 서버 DTO에는 매핑하지 않는다.
 */
data class EditorMessageTextBlock(
    val title: String,
    val body: String,
    val isRegistered: Boolean = false,
)
