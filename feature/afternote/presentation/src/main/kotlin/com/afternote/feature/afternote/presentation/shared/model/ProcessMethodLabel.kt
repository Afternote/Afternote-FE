package com.afternote.feature.afternote.presentation.shared.model

/**
 * 서버 `processMethod` enum 문자열 → 사용자 표시 라벨.
 *
 * 발신자/수신자 Detail 매퍼 공용. 메모리 룰 [Detail screen UI models] 적용 —
 * Detail 은 Editor 측 enum (`InformationProcessingMethod`, `AccountProcessingMethod`) 을
 * 직접 import 하지 않고 본 공용 매퍼만 호출한다.
 *
 * 라벨 문자열은 Editor enum 의 `.title` 과 동일하게 유지되어야 한다 (단일 source of truth 정리는
 * `#190` processMethod 통합 작업에서 진행 예정).
 *
 * 매핑되지 않은 값은 raw 문자열을 그대로 반환해 호환성을 유지한다.
 */
fun mapProcessMethodLabel(serverValue: String): String =
    when (serverValue) {
        "MEMORIAL" -> "추모 계정으로 전환"
        "TRANSFER" -> "수신자에게 정보 전달"
        "PERMANENT_DELETE" -> "계정 영구 삭제"
        "TRANSFER_TO_AFTERNOTE_EDIT_RECEIVER" -> "수신자에게 정보 전달"
        "TRANSFER_TO_ADDITIONAL_AFTERNOTE_EDIT_RECEIVER" -> "추가 수신자에게 정보 전달"
        else -> serverValue
    }
