package com.afternote.feature.onboarding.presentation.navigation

/**
 * 온보딩 그래프로 전달되는 네비게이션 명령 모음.
 *
 * 작명 컨벤션 (#239):
 * - `navigateTo<Where>` — 단순 navigate
 * - `popBack` / `popTo<Where>` — pop 동작
 * - `replace<X>With<Y>` — popUpTo + navigate (replace)
 * - `proceedTo<Next>` — 회원가입 같은 흐름 내부 stepping
 *
 * Screen 콜백 인자는 `on<도메인 이벤트>` 자리라 본 인터페이스와 별 책임. NavGraph 가 둘을 매핑한다.
 */
interface OnboardingNavActions {
    /** 회원가입/로그인 성공 → Onboarding 그래프 전체 비우고 Home 진입 (replace). */
    fun replaceOnboardingWithHome()

    /** 소셜 신규 가입자 로그인 성공 → Login 비우고 Welcome 진입 (온보딩 시작). */
    fun replaceLoginWithWelcome()

    fun navigateToSignUp()

    fun navigateToLogin()

    /** Welcome 의 "전달 받은 기록 확인하기" → 수신자 흐름 (받은 기록함) 진입. */
    fun navigateToReceivedRecords()

    /** Login → SignUp 으로 화면 교체 (뒤로가기 시 Login 으로 돌아가지 않도록). */
    fun replaceLoginWithSignUp()

    /** 화면 단위 동작이 동일한 단순 뒤로가기 — 호출 화면별로 분기할 필요 없을 때 공통 사용. */
    fun popBack()

    fun proceedToSignUpResidentNumber()

    fun proceedToSignUpPassword()

    fun proceedToTerms()

    fun proceedToProfile()

    fun navigateToTermsDetail()

    /** 로그인 → 아이디 찾기. */
    fun navigateToFindId()

    /** 로그인 → 비밀번호 찾기 (이메일 인증). */
    fun navigateToFindPassword()

    /** 비밀번호 찾기 — 이메일 인증 → 새 비밀번호 입력. */
    fun proceedToFindPasswordReset()

    /**
     * 비밀번호 재설정 성공 → 완료 안내로 교체.
     *
     * 찾기 흐름을 백스택에서 비운다 — 비밀번호는 이미 바뀌었고 인증번호는 서버가 소비해서,
     * 뒤로가기로 앞 두 화면에 돌아가면 유효하지 않은 입력만 남는다.
     */
    fun replaceFindPasswordWithComplete()

    /** 완료 안내 → 로그인. 찾기 흐름의 마지막 화면까지 비우고 로그인으로 돌려보낸다. */
    fun replaceFindPasswordWithLogin()
}
