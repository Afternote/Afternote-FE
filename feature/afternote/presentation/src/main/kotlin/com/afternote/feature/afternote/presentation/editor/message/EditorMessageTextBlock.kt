package com.afternote.feature.afternote.presentation.editor.message

/**
 * "남기실 말씀" 한 덩어리를 프리필하거나 저장 요청으로 넘길 때 사용하는 일반 값.
 *
 * 화면에서 편집 중인 실제 상태는 [LeaveMessageEditorItem]이 가진다.
 * 이 값은 제목·본문이 빈 문자열일 수 있어 도메인
 * [com.afternote.feature.afternote.domain.model.LeaveMessageBlock] 과 달리 둘 다 non-null 이다.
 * [isRegistered]는 프리필된 항목의 표시 상태를 전달하며 서버 DTO에는 매핑하지 않는다.
 */
data class EditorMessageTextBlock(
    val title: String,
    val body: String,
    val isRegistered: Boolean = false,
)
