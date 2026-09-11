package com.afternote.feature.onboarding.presentation

/**
 * 온보딩이 새 비밀번호에 요구하는 조합 규칙.
 *
 * 회원가입 3단계와 비밀번호 찾기의 변경 화면이 **같은 규칙·같은 안내 문구**
 * (`onboarding_signup_password_rule_combination`)를 쓴다 — 시안 `2383:16789` 의 안내 2줄이
 * 회원가입 시안과 글자까지 동일하다. 두 화면이 각자 정규식을 들면 한쪽만 고쳐질 자리라 여기 모은다.
 *
 * **서버 규칙과 같다 (#1628).** BE `PasswordValidation.REGEX` 를 그대로 옮겼다 — 영문·숫자·허용
 * 특수문자(`@ $ ! % * # ? &`)를 각각 하나 이상 포함하고, 전체 문자는 그 ASCII 허용 목록 안에서만
 * 8~15자여야 한다. 서버는 이 상수 하나를 `SignupRequest`·`PasswordChangeRequest`·`PasswordFindRequest`
 * 세 곳에 `@Pattern` 으로 걸고 컨트롤러 진입 전에 거절하므로, 클라가 더 넓으면 사용자는 조건 충족
 * 표시를 다 받고 제출에서 400 을 맞는다.
 *
 * Android 정규식의 `\d` 는 Unicode 숫자까지 포함해 전각 `１`·아라비아-인도 `١` 을 통과시킨다.
 * 서버 JVM 의 기본 판정과 갈리지 않도록 숫자를 `[0-9]` 로 명시한다.
 */
internal object OnboardingPasswordRule {
    /** 8~15자, 영문·숫자·허용 특수문자 8종 각 1개 이상, 그 밖의 문자는 금지. */
    private val REGEX =
        Regex("^(?=.*[A-Za-z])(?=.*[0-9])(?=.*[@$!%*#?&])[A-Za-z0-9@$!%*#?&]{8,15}$")

    fun isSatisfied(password: String): Boolean = REGEX.matches(password)
}
