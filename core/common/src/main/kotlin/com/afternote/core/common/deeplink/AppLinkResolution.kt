package com.afternote.core.common.deeplink

/**
 * 링크 한 건을 해석한 결과 (#924).
 *
 * `null` 을 돌려주고 소비처가 폴백을 고르게 두지 않는다. 그렇게 두면 호출부마다 폴백이 갈리고,
 * 「왜 홈으로 왔는지」가 아무 데도 남지 않는다. 거절도 **값**이라 [Rejected] 가 안전한 기본 진입과
 * 사유를 함께 들고 온다 — 소비처는 `when` 두 갈래로 끝내면 되고, 사유를 버릴지 리포팅할지만 고른다.
 */
sealed interface AppLinkResolution {
    /** 소비처가 실제로 이동할 목적지. 거절이어도 [Rejected.fallback] 이 있어 항상 값이 있다. */
    val target: NavigationTarget

    /** 계약 표의 한 행으로 해석됐다. */
    data class Resolved(
        override val target: NavigationTarget,
    ) : AppLinkResolution

    /**
     * 계약 밖이라 거절했다. [fallback] 으로 보내되 [reason] 을 관측에 남긴다.
     *
     * [fallback] 은 [NavigationTarget.Home] 이라 여전히 로그인 관문을 지난다 — 거절이 관문을
     * 건너뛰는 우회로가 되지 않는다.
     */
    data class Rejected(
        val reason: AppLinkRejectionReason,
        val fallback: NavigationTarget = NavigationTarget.Home,
    ) : AppLinkResolution {
        override val target: NavigationTarget get() = fallback
    }
}
