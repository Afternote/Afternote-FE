package com.afternote.feature.onboarding.presentation

import androidx.annotation.StringRes
import com.afternote.core.domain.error.EmailAlreadyRegisteredException
import com.afternote.core.domain.error.LoginRejectedException
import com.afternote.core.domain.error.NetworkUnavailableException
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

        // 유일하게 서버 문구를 그대로 쓰는 갈래다 — #517 로 머지된 기존 계약이라 여기서는 유지하고,
        // 위 원칙에 맞춰 code 기반으로 옮기는 일은 #687 이 맡는다.
        is LoginRejectedException -> {
            UiText.Dynamic(displayMessage)
        }

        else -> {
            UiText.Resource(fallbackResId)
        }
    }
