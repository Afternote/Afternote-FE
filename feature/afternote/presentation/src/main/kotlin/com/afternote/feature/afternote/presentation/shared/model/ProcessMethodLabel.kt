package com.afternote.feature.afternote.presentation.shared.model

/**
 * 서버가 과거 `processMethod` enum 으로 내려주던 코드 문자열을 사용자 표시 라벨로 변환한다.
 *
 * 백엔드가 `processMethod` 를 제거하고 `actions` 리스트로 통합한 이후 본 매퍼의 호출 지점은 모두 사라졌다.
 * 다만 디버그·테스트 시 과거 응답 형태를 재구성해야 할 때를 대비해 매핑 테이블만 유지한다.
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
