package com.afternote.core.ui.receiver

/**
 * [ReceiverSelectScreen] 이 그리는 수신자 한 명.
 *
 * 기능 전용 모델(설정 `ReceiverListItem`, 애프터노트 `AfternoteEditorReceiver` 등)을
 * 이 UI 모델로 매핑해 넘긴다 — 공용 UI 가 특정 기능의 데이터에 의존하지 않게 하기 위한 경계다 (#791).
 */
data class ReceiverSelectItem(
    val id: Long,
    val name: String,
    val relation: String,
)
