package com.afternote.core.common.deeplink

/**
 * 목적지에 들어가기 전에 통과해야 하는 인증 관문 (#924).
 *
 * 순서가 의미를 갖는다 — `ordinal` 이 낮을수록 먼저 통과해야 하는 관문이다. 로그인 없이 지문이나
 * 수신자 본인인증을 물을 수 없으므로 [LOGIN] 이 항상 맨 앞이고, 그 불변식은
 * `NavigationTargetGateTest` 가 목적지 전량에 대해 잠근다.
 *
 * 관문을 **판정**하는 것과 관문을 **띄우는** 것은 다른 일이다. 이 enum 과
 * [NavigationTarget.requiredGates] 는 앞엣것만 소유한다 — 어떤 화면으로 관문을 띄우고 통과 후
 * 어떻게 원래 목적지를 재개할지는 앱 루트가 갖는다.
 */
enum class AuthGate {
    /** 앱 계정 로그인. 모든 목적지의 최초 관문이다. */
    LOGIN,

    /** 지문(생체) 인증. 작성자 본인의 애프터노트 열람 경로에 걸린다. */
    BIOMETRIC,

    /** 수신자 본인인증 — 발신자별 이메일 인증 + 마스터 키. 받은 애프터노트 열람에 걸린다. */
    RECEIVER_IDENTITY,
}
