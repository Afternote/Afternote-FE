package com.afternote.feature.onboarding.presentation.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.afternote.core.ui.navigation.FeatureStackBoundary
import com.afternote.core.ui.navigation.popOrExit
import com.afternote.core.ui.navigation.replaceAllWith

/** 비밀번호 찾기 흐름 안에서만 의미가 있는 이동. 바깥 스택을 건드리는 하나는 콜백으로 위임한다. */
internal interface FindPasswordFlowNavActions {
    fun popBack()

    /** 이메일 인증 통과 → 새 비밀번호 화면. 인증 화면은 남는다 — 아직 되돌아갈 수 있다. */
    fun proceedToPasswordReset()

    /**
     * 비밀번호 변경 성공 → 완료 화면. 인증·변경 화면은 남기지 않는다 —
     * 인증번호를 서버가 이미 소비해 되돌아가면 다시 쓸 수 없다.
     */
    fun proceedToComplete()

    /** 완료의 "로그인". */
    fun popToLogin()
}

internal class FindPasswordFlowLocalNavActions(
    private val stepStack: NavBackStack<NavKey>,
    private val boundary: FeatureStackBoundary,
    private val onLoginAfterReset: () -> Unit,
) : FindPasswordFlowNavActions {
    override fun popBack(): Unit = stepStack.popOrExit(boundary)

    override fun proceedToPasswordReset() {
        if (stepStack.lastOrNull() != OnboardingRoute.FindPasswordResetRoute) {
            stepStack.add(OnboardingRoute.FindPasswordResetRoute)
        }
    }

    override fun proceedToComplete(): Unit = stepStack.replaceAllWith(OnboardingRoute.FindPasswordCompleteRoute)

    override fun popToLogin(): Unit = onLoginAfterReset()
}
