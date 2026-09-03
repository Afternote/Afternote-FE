package com.afternote.feature.onboarding.presentation.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.afternote.core.ui.navigation.FeatureStackBoundary
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 비밀번호 찾기 흐름의 백스택 회귀 기준 (#457 · #1789).
 *
 * 인증번호는 서버가 이미 소비했으므로, 변경이 끝나면 뒤로가기로 인증·변경 화면에 돌아갈 수
 * 없어야 한다. Nav2 의 `popUpTo(inclusive = true)` 가 하던 일을 로컬 스택이 그대로 한다.
 */
class FindPasswordFlowLocalNavActionsTest {
    private var exits = 0
    private var loginConverges = 0
    private val stepStack = NavBackStack<NavKey>(OnboardingRoute.FindPasswordRoute)
    private val actions =
        FindPasswordFlowLocalNavActions(
            stepStack = stepStack,
            boundary = FeatureStackBoundary { exits += 1 },
            onLoginAfterReset = { loginConverges += 1 },
        )

    private fun stack(): List<String> = stepStack.map { it::class.simpleName!! }

    @Test
    fun `인증을 통과하면 새 비밀번호 화면이 인증 화면 위에 쌓인다`() {
        actions.proceedToPasswordReset()

        // 아직 되돌아갈 수 있다 — 인증번호를 다시 입력할 여지를 남긴다.
        assertEquals(listOf("FindPasswordRoute", "FindPasswordResetRoute"), stack())
    }

    @Test
    fun `변경 성공 뒤에는 인증도 변경도 백스택에 남지 않는다`() {
        actions.proceedToPasswordReset()

        actions.proceedToComplete()

        assertEquals(listOf("FindPasswordCompleteRoute"), stack())
    }

    @Test
    fun `완료 화면에서의 뒤로가기는 흐름을 통째로 벗어난다`() {
        actions.proceedToPasswordReset()
        actions.proceedToComplete()

        actions.popBack()

        assertEquals(listOf("FindPasswordCompleteRoute"), stack())
        assertEquals(1, exits)
    }

    @Test
    fun `첫 화면에서의 뒤로가기도 흐름을 벗어난다`() {
        actions.popBack()

        assertEquals(listOf("FindPasswordRoute"), stack())
        assertEquals(1, exits)
    }

    @Test
    fun `완료의 로그인은 바깥 스택이 수렴시킨다`() {
        actions.proceedToComplete()

        actions.popToLogin()

        assertEquals(1, loginConverges)
    }

    @Test
    fun `같은 단계를 연달아 요청해도 두 번 쌓이지 않는다`() {
        actions.proceedToPasswordReset()
        actions.proceedToPasswordReset()

        assertEquals(listOf("FindPasswordRoute", "FindPasswordResetRoute"), stack())
    }
}
