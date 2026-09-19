package com.afternote.feature.setting.presentation.viewmodel

import com.afternote.core.domain.error.CoreAuthFailure
import com.afternote.core.ui.UiText
import com.afternote.feature.setting.presentation.R

/**
 * 비밀번호 변경 실패를 표시 문구로 옮긴다 — **타입만** 보고 고르고 서버 `message` 는 쓰지 않는다
 * (BE#92 — 서버 문구가 사용자 노출용이라는 규정이 없다). 같은 규칙의 선례가
 * [toReceiverFailureMessage] 다.
 *
 * 사유가 확인된 것만 전용 문구를 갖고 나머지는 폴백으로 내려앉는다. 폴백으로 내려앉는 것 중 가장
 * 흔한 것은 **현재 비밀번호 불일치**(BE `PASSWORD_MISMATCH`, code 1202)다 — 이 코드는
 * `AccountRepositoryImpl.mapAccountFailure` 가 아직 번역하지 않아 원본 예외로 도착한다. 그 번역표는
 * PR #2020 이 파일째 옮기고 있어 같은 줄을 두 번 고치지 않으려고 이 변경의 범위에서 뺐다.
 */
internal fun Throwable.toPasswordChangeMessage(): UiText =
    UiText.Resource(
        when (this) {
            is CoreAuthFailure.NetworkUnavailable -> R.string.settings_password_change_network_error
            is CoreAuthFailure.PasswordUnchanged -> R.string.settings_password_change_unchanged
            is CoreAuthFailure.SocialSignUpAccount -> R.string.settings_password_change_social_blocked
            else -> R.string.settings_password_change_failed
        },
    )
