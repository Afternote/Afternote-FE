package com.afternote.feature.onboarding.presentation

import android.util.Patterns

/**
 * 온보딩이 이메일 입력에 요구하는 형식 규칙.
 *
 * 회원가입 1단계·아이디 찾기·비밀번호 찾기 세 화면이 **같은 판정**을 쓴다. 종전에는 세 UiState 가
 * 각자 같은 한 줄을 들고 KDoc 으로 서로를 가리키는 것이 동기화의 전부였다 — 한쪽만 고쳐질 자리라
 * 여기 모은다 (#1851). 바로 옆의 [OnboardingPasswordRule] 과 같은 자리·같은 모양이다.
 *
 * [Patterns.EMAIL_ADDRESS] 는 컴파일된 정규식(`Pattern`) 상수라 `matcher(입력)` 으로 그 문자열
 * 전용 실행기를 만든 뒤 `matches()`(**전체 일치** — 부분 검색 `find()` 와 다름)로 판정하는
 * Java regex 2단계 API 를 쓴다.
 *
 * **정규식 사본으로 되돌리지 않는다.** `d5cc062d4`(#273)가 `SignUpViewModel` 의 AOSP 사본
 * `EMAIL_ADDRESS_REGEX` 를 지우고 이 상수로 옮긴 결정이 이미 있다 — 사본은 AOSP 원본과 조용히
 * 갈라지고, 직접 쓴 Android 정규식은 문자 클래스를 Unicode 로 판정해 서버 검증과 어긋날 여지를
 * 새로 만든다. 그래서 이 추출로도 소비처는 여전히 Robolectric 이 필요하다 — 이 함수의 목적은
 * 중복 제거이지 테스트 러너 경량화가 아니다.
 */
internal object OnboardingEmailRule {
    fun isValid(email: String): Boolean = email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
}
