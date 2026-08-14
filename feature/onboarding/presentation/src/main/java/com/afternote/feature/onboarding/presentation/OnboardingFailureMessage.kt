package com.afternote.feature.onboarding.presentation

import androidx.annotation.StringRes
import com.afternote.core.domain.error.EmailAlreadyRegisteredException
import com.afternote.core.domain.error.NetworkUnavailableException
import com.afternote.core.domain.error.SocialLoginRejectedException
import com.afternote.core.ui.UiText

/**
 * 온보딩 실패를 표시 문구로 옮긴다. data 계층이 사유를 확인해 준 타입만 각자의 문구를 갖고
 * 나머지는 [fallbackResId] 로 내려앉는다.
 *
 * 문구의 출처는 서버가 아니라 이쪽 리소스다 — 서버 `message` 는 사용자 노출용이라는 규정이 없고
 * (BE#92), `exception.message` 에는 클라 디버그 문구·5xx 본문(내부 SQL, #511 실측)까지 섞인다.
 */
internal fun Throwable.toDisplayMessage(
    @StringRes fallbackResId: Int,
): UiText =
    when (this) {
        is NetworkUnavailableException -> {
            UiText.Resource(R.string.onboarding_network_error)
        }

        is EmailAlreadyRegisteredException -> {
            UiText.Resource(R.string.signup_email_already_registered)
        }

        is SocialLoginRejectedException -> {
            UiText.Resource(R.string.login_social_rejected)
        }

        else -> {
            UiText.Resource(fallbackResId)
        }
    }
