package com.afternote.feature.setting.presentation.password

/**
 * 설정의 비밀번호 변경이 **새 비밀번호** 에 요구하는 조합 규칙.
 *
 * **서버 규칙과 같다.** BE `domain/auth/dto/PasswordValidation.REGEX` 를 그대로 옮겼다(2026-09-13
 * Afternote-BE main 실코드 확인) — 영문·숫자·허용 특수문자(`@ $ ! % * # ? &`)를 각각 하나 이상
 * 포함하고, 전체 문자는 그 ASCII 허용 목록 안에서만 8~15자여야 한다. 서버는 이 상수 하나를
 * `SignupRequest`·`PasswordChangeRequest`·`PasswordFindRequest` 세 곳에 `@Pattern` 으로 걸고
 * 컨트롤러 진입 전에 거절하므로, 클라가 더 넓으면 사용자는 조건 충족 표시를 다 받고 제출에서 400 을 맞는다.
 *
 * Android 정규식의 `\d` 는 Unicode 숫자까지 포함해 전각 `１`·아라비아-인도 `١` 을 통과시킨다.
 * 서버 JVM 의 기본 판정과 갈리지 않도록 숫자를 `[0-9]` 로 명시한다(#1628).
 *
 * ### 온보딩의 `OnboardingPasswordRule` 과 사본이 둘이다
 * #1644 는 「규칙 사본은 한 곳」을 목표로 두었고 그때 설정의 비밀번호 변경은 화면·호출부가 없어
 * 대상이 아니었다. 지금 그 소비처가 생겼지만 온보딩 쪽 객체는 `feature:onboarding:presentation`
 * 의 `internal` 이라 모듈 밖에서 쓸 수 없고, 공용 자리로 옮기는 일은 남의 모듈 프로덕션을 고치는
 * 일이라 이 변경의 범위 밖이다. 값을 고칠 땐 **BE 실코드를 정본으로** 두 사본을 함께 고친다.
 *
 * 현재 비밀번호에는 이 규칙을 걸지 않는다 — 옛 규칙으로 만든 비밀번호가 그대로 살아 있을 수 있어
 * 클라가 막으면 변경 자체가 불가능해진다(로그인에 규칙을 걸지 않는 것과 같은 판단).
 */
internal object SettingPasswordRule {
    /** 8~15자, 영문·숫자·허용 특수문자 8종 각 1개 이상, 그 밖의 문자는 금지. */
    private val REGEX =
        Regex("^(?=.*[A-Za-z])(?=.*[0-9])(?=.*[@$!%*#?&])[A-Za-z0-9@$!%*#?&]{8,15}$")

    fun isSatisfied(password: String): Boolean = REGEX.matches(password)
}
