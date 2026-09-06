package com.afternote.feature.afternote.presentation.navigation.model

/**
 * SavedStateHandle key for receiver selection when returning from a sub-flow.
 *
 * 값은 확정된 수신자 id 전체를 담은 [LongArray] 다 — 선택 화면이 한 번에 여러 명을
 * 돌려줄 수 있게 복수로 열었다 (#1426). 빈 배열은 «아무도 선택하지 않음» 으로,
 * 키 부재(= 선택 화면을 거치지 않음)와 구분된다.
 */
const val SELECTED_RECEIVER_IDS_KEY = "selected_receiver_ids"
