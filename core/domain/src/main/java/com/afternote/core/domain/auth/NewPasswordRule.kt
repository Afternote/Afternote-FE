package com.afternote.core.domain.auth

/**
 * 새 비밀번호에 요구하는 조합 규칙의 클라 정본 (#2191).
 *
 * **서버 규칙과 같다.** BE `domain/auth/dto/PasswordValidation.REGEX` 를 그대로 옮겼다 — 영문·숫자·허용
 * 특수문자(`@ $ ! % * # ? &`)를 각각 하나 이상 포함하고, 전체 문자는 그 ASCII 허용 목록 안에서만
 * 8~15자여야 한다. 서버는 이 상수 하나를 `SignupRequest`·`PasswordChangeRequest`·`PasswordFindRequest`
 * 세 곳에 `@Pattern` 으로 걸고 컨트롤러 진입 전에 거절하므로, 클라가 더 넓으면 사용자는 조건 충족
 * 표시를 다 받고 제출에서 400 을 맞는다. 서버가 상수 하나를 쓰듯 클라도 여기 하나만 둔다 — 화면마다
 * 사본을 들면 규칙이 바뀔 때 한쪽만 고쳐져 가입과 변경의 판정이 조용히 갈린다.
 *
 * Android 정규식의 `\d` 는 Unicode 숫자까지 포함해 전각 `１`·아라비아-인도 `١` 을 통과시킨다.
 * 서버 JVM 의 기본 판정과 갈리지 않도록 숫자를 `[0-9]` 로 명시한다(#1628).
 *
 * **현재 비밀번호와 로그인 입력에는 걸지 않는다.** 옛 규칙으로 만든 비밀번호가 그대로 살아 있을 수
 * 있어, 클라가 막으면 그 계정은 로그인도 변경도 못 한다.
 */
object NewPasswordRule {
    /** 8~15자, 영문·숫자·허용 특수문자 8종 각 1개 이상, 그 밖의 문자는 금지. */
    private val REGEX =
        Regex("^(?=.*[A-Za-z])(?=.*[0-9])(?=.*[@$!%*#?&])[A-Za-z0-9@$!%*#?&]{8,15}$")

    fun isSatisfied(password: String): Boolean = REGEX.matches(password)
}
