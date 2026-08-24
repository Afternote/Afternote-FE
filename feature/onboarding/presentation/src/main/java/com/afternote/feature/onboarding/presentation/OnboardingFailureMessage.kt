package com.afternote.feature.onboarding.presentation

import androidx.annotation.StringRes
import com.afternote.core.domain.error.CoreAuthFailure
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
): UiText = UiText.Resource((this as? CoreAuthFailure)?.displayMessageResOrNull() ?: fallbackResId)

/**
 * 사유에 대응하는 전용 문구. 루트로 좁혀 `when` 을 exhaustive 하게 만든다 — 사유가 늘면 여기가
 * 컴파일 에러로 잡힌다. `else` 로 뭉개 두면 새 사유가 조용히 폴백 문구로 흘러 화면이 틀린 안내를 낸다.
 *
 * null 은 "전용 문구 없음" 이고, 폴백은 호출처가 [toDisplayMessage] 에 넘긴 작업별 리소스가 맡는다.
 */
@StringRes
private fun CoreAuthFailure.displayMessageResOrNull(): Int? =
    when (this) {
        is CoreAuthFailure.NetworkUnavailable -> R.string.onboarding_network_error

        is CoreAuthFailure.EmailAlreadyRegistered -> R.string.signup_email_already_registered

        is CoreAuthFailure.SocialLoginRejected -> R.string.login_social_rejected

        // 셋 다 이 함수 밖에서 갈리므로 전용 문구를 갖지 않는다 — 자격 거절·인증번호 무효는 화면이
        // 입력 필드 인라인으로, 인증 취소는 소비처가 무시한다. `else` 가 아니라 열거해 두는 건
        // 사유가 새로 늘 때 컴파일러가 여기를 잡게 하려는 것이다.
        is CoreAuthFailure.InvalidLoginCredentials,
        is CoreAuthFailure.EmailVerification,
        is CoreAuthFailure.UserCancelledAuth,
        -> null
    }
