package com.afternote.core.domain.push

/**
 * 서버가 등록을 받고 돌려준 판정 (#2321).
 *
 * @property requiresReset 서버가 이 식별자로 보낸 발송에서 FCM UNREGISTERED 를 받았다. 같은 값을 다시
 *   등록해도 신호가 지워지지 않으므로, 기기가 새 식별자를 발급받아 등록해야 푸시가 다시 닿는다.
 */
data class PushRegistration(
    val requiresReset: Boolean,
)
