package com.afternote.feature.onboarding.presentation

/**
 * 온보딩이 새 비밀번호에 요구하는 조합 규칙.
 *
 * 회원가입 3단계와 비밀번호 찾기의 변경 화면이 **같은 규칙·같은 안내 문구**
 * (`onboarding_signup_password_rule_combination`)를 쓴다 — 시안 `2383:16789` 의 안내 2줄이
 * 회원가입 시안과 글자까지 동일하다. 두 화면이 각자 정규식을 들면 한쪽만 고쳐질 자리라 여기 모은다.
 *
 * **서버 규칙과 완전히 같지는 않다.** BE `PasswordValidation.REGEX` 는
 * `^(?=.*[A-Za-z])(?=.*\d)(?=.*[@$!%*#?&])[A-Za-z\d@$!%*#?&]{8,15}$` 로 최대 15자이고
 * 특수문자를 8종으로 한정한다. 이쪽이 16자·전체 특수문자를 허용하므로 서버가 400 으로
 * 되돌릴 수 있는 입력이 통과한다. 문구("8 ~ 16자")가 시안 정본이라 클라를 시안에 맞춰 두고,
 * 규칙 통일은 BE 와 별도로 합의할 항목이다(회원가입도 같은 상태 — #457 에서 기존 동작 유지).
 */
internal object OnboardingPasswordRule {
    /** 8~16자, 영문 대소문자 + 숫자 + 특수문자 각 1개 이상. */
    private val REGEX = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,16}$")

    fun isSatisfied(password: String): Boolean = REGEX.matches(password)
}
