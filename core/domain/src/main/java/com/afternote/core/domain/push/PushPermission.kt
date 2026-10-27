package com.afternote.core.domain.push

/**
 * 이 기기에서 앱 알림이 사용자에게 보이는지 (#2321).
 *
 * 서버는 OS 권한을 직접 읽을 수 없어 앱이 보고한 이 값으로 권한 거부 기기의 푸시를 건너뛴다.
 * [UNKNOWN] 은 서버의 레거시 기본값과 같은 이름이라 계약에 남겨 두지만 앱은 보내지 않는다 —
 * 앱은 언제든 지금 값을 읽을 수 있다.
 */
enum class PushPermission {
    UNKNOWN,
    GRANTED,
    DENIED,
}
