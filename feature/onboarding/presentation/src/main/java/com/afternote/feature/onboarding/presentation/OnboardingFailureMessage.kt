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
): UiText = UiText.Resource(displayMessageResOrFallback(fallbackResId))

/**
 * [toDisplayMessage] 와 같은 매핑을 리소스 ID 로 준다.
 *
 * `UiText.asString()` 은 `@Composable` 이라 코루틴 콜백 안에서는 부를 수 없다. 그런 자리(소셜 로그인
 * 결과 처리 등)는 이걸로 ID 를 받아 `LocalResources.current` 의 `getString` 으로 푼다 — 매핑을 복제하지
 * 않기 위한 것이다. `LocalContext.current` 로 조회하지 않는다(lint LocalContextGetResourceValueCall).
 */
@StringRes
internal fun Throwable.displayMessageResOrFallback(
    @StringRes fallbackResId: Int,
): Int = (this as? CoreAuthFailure)?.displayMessageResOrNull() ?: fallbackResId

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

        is CoreAuthFailure.EmailAlreadyRegistered -> R.string.onboarding_signup_email_already_registered

        is CoreAuthFailure.SocialLoginRejected -> R.string.onboarding_login_social_rejected

        is CoreAuthFailure.PasswordUnchanged -> R.string.onboarding_find_password_unchanged

        // 시안(`2383:16667`)은 이 사유를 차단 팝업으로 그린다. 그 표시는 화면이 하고, 여기 문구는
        // 팝업이 없는 자리(비밀번호 변경 제출)로 같은 사유가 흘러왔을 때의 스낵바 몫이다.
        // 로그인 화면은 이 매핑을 타지 않는다 — 같은 사유라도 안내가 "비밀번호 찾기를 쓸 수 없다" 가
        // 아니라 "소셜로 로그인하라" 여서 `LoginViewModel` 이 자기 문구로 가른다.
        is CoreAuthFailure.SocialSignUpAccount -> R.string.onboarding_find_password_social_blocked

        // 셋 다 이 함수 밖에서 갈리므로 전용 문구를 갖지 않는다 — 자격 거절·인증번호 무효는 화면이
        // 입력 필드 인라인으로, 인증 취소는 소비처가 무시한다. `else` 가 아니라 열거해 두는 건
        // 사유가 새로 늘 때 컴파일러가 여기를 잡게 하려는 것이다.
        is CoreAuthFailure.InvalidLoginCredentials,
        is CoreAuthFailure.EmailVerification,
        is CoreAuthFailure.UserCancelledAuth,
        -> null
    }
