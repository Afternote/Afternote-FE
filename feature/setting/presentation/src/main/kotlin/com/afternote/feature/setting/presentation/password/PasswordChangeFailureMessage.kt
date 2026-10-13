package com.afternote.feature.setting.presentation.password

import com.afternote.core.domain.error.CoreAuthFailure
import com.afternote.core.ui.UiText
import com.afternote.feature.setting.presentation.R

/**
 * 비밀번호 변경 실패를 표시 문구로 옮긴다 — **타입만** 보고 고르고 서버 `message` 는 쓰지 않는다
 * (BE#92 — 서버 문구가 사용자 노출용이라는 규정이 없다). 같은 규칙의 선례가
 * [com.afternote.feature.setting.presentation.receiver.toReceiverFailureMessage] 다.
 *
 * 사유가 확인된 것만 전용 문구를 갖고 나머지는 폴백으로 내려앉는다. 폴백으로 내려앉는 것 중 가장
 * 흔한 것은 **현재 비밀번호 불일치**(BE `PASSWORD_MISMATCH`, code 1202)다 — 이 코드는
 * `mapAccountFailure` 가 아직 번역하지 않아 원본 예외로 도착한다. 이 화면은 현재의 실패 계약을
 * 그대로 소비하며, 1202에 대한 전용 번역은 추가하지 않는다.
 */
internal fun Throwable.toPasswordChangeMessage(): UiText =
    UiText.Resource(
        when (this) {
            is CoreAuthFailure.NetworkUnavailable -> R.string.setting_password_change_network_error
            is CoreAuthFailure.PasswordUnchanged -> R.string.setting_password_change_unchanged
            is CoreAuthFailure.SocialSignUpAccount -> R.string.setting_password_change_social_blocked
            else -> R.string.setting_password_change_failed
        },
    )
