package com.afternote.feature.onboarding.presentation.signup

/**
 * 회원가입 4단계 진행 인디케이터의 단계 상수.
 *
 * 이메일(1) → 주민번호(2) → 비밀번호(3) → 약관(4). 진행 인디케이터의 `currentStep` 인자에 전달된다.
 * `core/ui` 의 공용 [com.afternote.core.ui.scaffold.FlowStepScaffold] 사용 시 [SIGN_UP_TOTAL_STEPS]
 * 를 `totalSteps` 로 함께 넘긴다.
 */
object SignUpStep {
    const val EMAIL: Int = 1
    const val RESIDENT_NUMBER: Int = 2
    const val PASSWORD: Int = 3
    const val TERMS: Int = 4
}

/** 회원가입 진행 단계 총수 (이메일·주민번호·비밀번호·약관). */
const val SIGN_UP_TOTAL_STEPS: Int = 4
