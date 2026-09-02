package com.afternote.afternote_fe.navigation

import android.app.Application
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.afternote.core.ui.Route
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * 인증 전후 백스택 경계 회귀 기준 (#1601).
 *
 * 이 앱의 인증 게이트는 세 겹이다 — 로그인(`MainViewModel.startRoute`) · 지문
 * (`AfternoteNavGraph` 의 `startDestination`) · 수신자 본인 확인(`ReceiverNavGraph` 의 nested flow).
 * 각 게이트를 통과한 뒤 **뒤로가기로 게이트에 돌아갈 수 없는지**를 `popUpTo` 조합의 결과인
 * 백스택 모양으로 못박는다.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], application = Application::class)
class AuthBoundaryBackStackTest {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var harness: NavBackStackHarness

    private fun start(startDestination: Route) {
        composeRule.setContent {
            SkeletonAppNavigation(startDestination = startDestination) { harness = it }
        }
        composeRule.waitForIdle()
    }

    private fun routes(): List<String> = composeRule.runOnIdle { harness.navController.backStackRouteNames() }

    @Test
    fun `로그인 성공은 온보딩 스택을 전부 비우고 홈만 남긴다`() {
        start(Route.Onboarding)

        composeRule.runOnIdle { harness.onboardingActions.navigateToLogin() }
        composeRule.runOnIdle { harness.onboardingActions.navigateToSignUp() }
        composeRule.runOnIdle { harness.onboardingActions.proceedToSignUpResidentNumber() }

        composeRule.runOnIdle { harness.onboardingActions.replaceOnboardingWithHome() }

        // popUpTo(0) { inclusive } — 온보딩 그래프 엔트리까지 사라진다.
        assertEquals(listOf("NavHostRoot", "Home"), routes())
        // 홈에서 뒤로가기는 앱을 벗어난다(=pop 실패). 로그인 화면으로 되돌아가지 않는다.
        assertEquals(false, composeRule.runOnIdle { harness.navController.popBackStack() })
    }

    @Test
    fun `로그아웃은 인증 이후 스택을 전부 비우고 온보딩 시작 화면만 남긴다`() {
        start(Route.Home)

        composeRule.runOnIdle { harness.homeActions.onSettingClick() }
        composeRule.runOnIdle { harness.settingActions.onNavigateToWithdrawGuide() }

        composeRule.runOnIdle { harness.settingActions.onLogoutSuccess() }

        assertEquals(listOf("NavHostRoot", "Onboarding", "WelcomeRoute"), routes())
        assertEquals(false, composeRule.runOnIdle { harness.navController.popBackStack() })
    }

    @Test
    fun `탈퇴는 로그아웃과 같은 경계로 스택을 비운다`() {
        start(Route.Home)

        composeRule.runOnIdle { harness.homeActions.onSettingClick() }
        composeRule.runOnIdle { harness.settingActions.onNavigateToWithdrawGuide() }
        composeRule.runOnIdle { harness.settingActions.onNavigateToWithdrawConfirm() }

        composeRule.runOnIdle { harness.settingActions.onWithdrawSuccess() }

        assertEquals(listOf("NavHostRoot", "Onboarding", "WelcomeRoute"), routes())
        assertEquals(false, composeRule.runOnIdle { harness.navController.popBackStack() })
    }

    @Test
    fun `로그인 화면 교체 이동은 로그인을 백스택에 남기지 않는다`() {
        start(Route.Onboarding)

        composeRule.runOnIdle { harness.onboardingActions.navigateToLogin() }
        composeRule.runOnIdle { harness.onboardingActions.replaceLoginWithSignUp() }

        assertEquals(
            listOf("NavHostRoot", "Onboarding", "WelcomeRoute", "SignUpRoute"),
            routes(),
        )
        // 가입 화면에서 뒤로가기는 로그인이 아니라 Welcome 으로 간다.
        composeRule.runOnIdle { harness.onboardingActions.popBack() }
        assertEquals("WelcomeRoute", composeRule.runOnIdle { harness.navController.currentRouteName() })
    }

    @Test
    fun `소셜 신규 가입 분기는 Welcome 을 새로 세우고 로그인을 남기지 않는다`() {
        start(Route.Onboarding)

        composeRule.runOnIdle { harness.onboardingActions.navigateToLogin() }
        composeRule.runOnIdle { harness.onboardingActions.replaceLoginWithWelcome() }

        assertEquals(listOf("NavHostRoot", "Onboarding", "WelcomeRoute"), routes())
        assertEquals(false, composeRule.runOnIdle { harness.navController.popBackStack() })
    }

    @Test
    fun `지문 관문은 인증 성공 뒤 백스택에서 사라진다`() {
        start(Route.Home)

        composeRule.runOnIdle { harness.appState.navigateToBottomBarRoute(Route.Afternote) }
        assertEquals(
            listOf("NavHostRoot", "Home", "Afternote", "FingerprintLoginRoute"),
            routes(),
        )

        composeRule.runOnIdle { harness.afternoteActions.replaceFingerprintLoginWithAfternoteHome() }

        assertEquals(
            listOf("NavHostRoot", "Home", "Afternote", "AfternoteHomeRoute"),
            routes(),
        )
        // 애프터노트 홈에서 뒤로가기는 지문 관문이 아니라 홈 탭으로 나간다.
        composeRule.runOnIdle { harness.afternoteActions.popBack() }
        assertEquals("Home", composeRule.runOnIdle { harness.navController.currentRouteName() })
    }

    @Test
    fun `수신자 열람 신청은 소비한 본인확인 단계를 즉시 백스택에서 지운다`() {
        start(Route.Receiver)

        composeRule.runOnIdle { harness.receiverActions.navigateToSenderDetail(SENDER_ID) }
        composeRule.runOnIdle { harness.receiverActions.navigateToDeliveryVerificationFlow(SENDER_ID) }
        composeRule.runOnIdle { harness.receiverActions.navigateToIdentityVerificationEmail() }

        composeRule.runOnIdle { harness.receiverActions.proceedToMasterKey() }
        assertEquals(
            listOf(
                "NavHostRoot",
                "Receiver",
                "ReceivedRecordsRoute",
                "SenderDetailRoute",
                "DeliveryVerificationFlowRoute",
                "MasterKeyRoute",
            ),
            routes(),
        )

        composeRule.runOnIdle { harness.receiverActions.proceedToDocumentUpload() }
        assertEquals(
            listOf(
                "NavHostRoot",
                "Receiver",
                "ReceivedRecordsRoute",
                "SenderDetailRoute",
                "DeliveryVerificationFlowRoute",
                "DocumentUploadRoute",
            ),
            routes(),
        )

        composeRule.runOnIdle { harness.receiverActions.proceedToDeliveryVerificationComplete() }
        assertEquals(
            listOf(
                "NavHostRoot",
                "Receiver",
                "ReceivedRecordsRoute",
                "SenderDetailRoute",
                "DeliveryVerificationFlowRoute",
                "DeliveryVerificationCompleteRoute",
            ),
            routes(),
        )

        composeRule.runOnIdle { harness.receiverActions.popToReceivedRecords() }
        assertEquals(listOf("NavHostRoot", "Receiver", "ReceivedRecordsRoute"), routes())
    }

    private companion object {
        const val SENDER_ID = "sender-1601"
    }
}
