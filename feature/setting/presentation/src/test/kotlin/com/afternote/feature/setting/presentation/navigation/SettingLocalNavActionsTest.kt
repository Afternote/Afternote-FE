package com.afternote.feature.setting.presentation.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.afternote.core.ui.navigation.FeatureNavigationCallbacks
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 설정 로컬 백스택 경계 회귀 기준 (#1695).
 *
 * Nav2 시절 `AuthBoundaryBackStackTest` 와 `SettingImplementedCoverageAndroidTest` 가 루트
 * 백스택으로 보던 설정 안쪽 모양을 여기서 본다 — 어느 화면에서든 back 은 한 칸이고, 바닥에서는
 * 스택을 비우지 않고 셸로 나가며, 인증 상태를 바꾸는 둘은 스택을 건드리지 않는다.
 *
 * 키 직렬화 왕복은 `SavedState` 가 Bundle 이라 Robolectric 이 필요해 [SettingRouteSerializationTest] 에 따로 둔다.
 */
class SettingLocalNavActionsTest {
    private var exits = 0
    private val external = RecordingExternalActions()

    private fun actionsOn(vararg keys: SettingRoute): Pair<NavBackStack<NavKey>, SettingLocalNavActions> {
        val backStack = NavBackStack<NavKey>(*keys)
        val actions =
            SettingLocalNavActions(
                backStack = backStack,
                navigationCallbacks = FeatureNavigationCallbacks { exits += 1 },
                externalActions = external,
            )
        return backStack to actions
    }

    private fun NavBackStack<NavKey>.names(): List<String> = map { it::class.simpleName!! }

    @Test
    fun `설정 홈에서 시작하고 바닥의 back 은 스택을 비우지 않고 셸로 나간다`() {
        val (backStack, actions) = actionsOn(SettingRoute.SettingHomeRoute)

        actions.popBack()

        assertEquals(listOf("SettingHomeRoute"), backStack.names())
        assertEquals(1, exits)
    }

    @Test
    fun `홈 칩으로 들어온 등록은 바닥이라 성공의 pop 이 곧 host 이탈이다`() {
        val (backStack, actions) = actionsOn(SettingRoute.RecipientRegisterRoute)

        // ReceiverRegisterScreen 의 onRegisterSuccess 는 popBack 이다.
        actions.popBack()

        assertEquals(listOf("RecipientRegisterRoute"), backStack.names())
        assertEquals("부른 곳(홈)으로 돌아간다", 1, exits)
    }

    @Test
    fun `설정 홈과 목록에서 들어온 등록은 그 위에 쌓여 성공 뒤 거기로 돌아간다`() {
        val (backStack, actions) = actionsOn(SettingRoute.SettingHomeRoute)

        actions.onRecipientRegisterClick()
        actions.popBack()
        assertEquals(listOf("SettingHomeRoute"), backStack.names())

        actions.onRecipientListClick()
        actions.onRecipientRegisterClick()
        actions.popBack()
        assertEquals(listOf("SettingHomeRoute", "RecipientListRoute"), backStack.names())
        assertEquals(SettingRoute.RecipientListRoute(selectForDeliveryConditions = false), backStack.last())
        assertEquals(0, exits)
    }

    @Test
    fun `사후 전달 조건은 선택 목록 → 조건 → 마지막 인사말 수정으로 receiverId 를 나르고 back 은 한 칸씩이다`() {
        val (backStack, actions) = actionsOn(SettingRoute.SettingHomeRoute)

        actions.onDeliveryConditionsClick()
        actions.onDeliveryConditionsRecipientSelected(37L)
        actions.onRecipientEditClick(37L)

        assertEquals(
            listOf(
                SettingRoute.SettingHomeRoute,
                SettingRoute.RecipientListRoute(selectForDeliveryConditions = true),
                SettingRoute.AfterDeliveryRoute(37L),
                SettingRoute.RecipientEditRoute(37L),
            ),
            backStack.toList(),
        )

        // 수정 저장 성공(onEditSuccess)·조건 저장 성공(onSaveSuccess)은 둘 다 한 칸 pop 이다.
        actions.popBack()
        assertEquals(SettingRoute.AfterDeliveryRoute(37L), backStack.last())
        actions.popBack()
        assertEquals(SettingRoute.RecipientListRoute(selectForDeliveryConditions = true), backStack.last())
        actions.popBack()
        assertEquals(listOf("SettingHomeRoute"), backStack.names())
        assertEquals(0, exits)
    }

    @Test
    fun `패스키·앱 잠금·알림은 한 칸씩 쌓이고 PIN 완료는 한 칸 pop 이다`() {
        val (backStack, actions) = actionsOn(SettingRoute.SettingHomeRoute)

        actions.onPasskeyClick()
        actions.onPasskeyRegisterClick()
        actions.onPasswordAuthClick()
        assertEquals(listOf("SettingHomeRoute", "PasskeyRoute", "PasskeyMakingRoute", "PasskeyPasswordRoute"), backStack.names())
        // PassKeyPasswordScreen 의 onPinComplete 는 popBack 이다.
        actions.popBack()
        assertEquals("PasskeyMakingRoute", backStack.names().last())
        repeat(2) { actions.popBack() }

        actions.onAppLockClick()
        actions.popBack()
        assertEquals(listOf("SettingHomeRoute"), backStack.names())

        actions.onNotificationClick()
        actions.onPushNotificationClick()
        actions.popBack()
        assertEquals(listOf("SettingHomeRoute", "NotificationRoute"), backStack.names())
        assertEquals(0, exits)
    }

    @Test
    fun `프로필 수정과 설정 홈 양쪽에서 탈퇴 안내로 간다`() {
        val (backStack, actions) = actionsOn(SettingRoute.SettingHomeRoute)

        actions.onProfileEditClick()
        actions.onWithdrawGuideClick()
        assertEquals(listOf("SettingHomeRoute", "ProfileEditRoute", "WithdrawGuideRoute"), backStack.names())
        repeat(2) { actions.popBack() }

        actions.onWithdrawGuideClick()
        actions.onWithdrawConfirmClick()
        assertEquals(listOf("SettingHomeRoute", "WithdrawGuideRoute", "WithdrawConfirmRoute"), backStack.names())
    }

    @Test
    fun `로그아웃과 탈퇴 완료는 스택을 건드리지 않고 셸에만 알린다`() {
        val (backStack, actions) = actionsOn(SettingRoute.SettingHomeRoute)
        actions.onWithdrawGuideClick()
        actions.onWithdrawConfirmClick()
        val before = backStack.toList()

        actions.onWithdrawSuccess()
        actions.onLogoutSuccess()

        assertEquals(1, external.withdrawals)
        assertEquals(1, external.logouts)
        assertEquals(before, backStack.toList())
        assertEquals(0, exits)
    }

    @Test
    fun `클릭 하나마다 목적지 하나가 쌓인다`() {
        val (backStack, actions) = actionsOn(SettingRoute.SettingHomeRoute)

        actions.onPasswordChangeClick()
        actions.onLinkedAccountClick()
        actions.onNoticeClick()

        assertEquals(
            listOf("SettingHomeRoute", "PasswordChangeRoute", "LinkedAccountRoute", "NoticeRoute"),
            backStack.names(),
        )
    }

    private class RecordingExternalActions : SettingExternalActions {
        var logouts = 0
        var withdrawals = 0

        override fun onLogoutSuccess() {
            logouts += 1
        }

        override fun onWithdrawSuccess() {
            withdrawals += 1
        }
    }
}
