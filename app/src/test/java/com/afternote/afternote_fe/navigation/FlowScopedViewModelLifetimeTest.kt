package com.afternote.afternote_fe.navigation

import android.app.Application
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavBackStackEntry
import com.afternote.core.ui.Route
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.presentation.navigation.model.AfternoteRoute
import com.afternote.feature.receiver.presentation.navigation.model.ReceiverRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * flow-scoped ViewModel 수명 회귀 기준 (#1601).
 *
 * 에디터 3화면(`AfternoteEditorViewModel`)과 열람 신청 5화면
 * (`DeliveryVerificationFlowViewModel`)은 **중첩 그래프 엔트리의 ViewModelStore** 를 공유한다.
 * 즉 «자식 화면 사이에서는 같은 인스턴스, 그래프가 백스택에서 빠질 때 정리» 가 계약이고,
 * 그 계약은 프로덕션 코드가 아니라 Navigation 의 백스택 수명이 지키고 있다. Navigation 3 전환은
 * 정확히 그 기전을 바꾸므로 여기에 현행 동작을 못박는다.
 *
 * 실제 ViewModel 대신 정리 시점만 기록하는 [ClearRecordingViewModel] 을 같은 스토어에 올려
 * Hilt·Repository 없이 수명만 잰다.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], application = Application::class)
class FlowScopedViewModelLifetimeTest {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var harness: NavBackStackHarness
    private var clearedCount = 0

    private fun start(startDestination: Route) {
        composeRule.setContent {
            SkeletonAppNavigation(startDestination = startDestination) { harness = it }
        }
        composeRule.waitForIdle()
    }

    /** [entry] 의 ViewModelStore 에 올라간(없으면 새로 만드는) 기록용 ViewModel. */
    private fun flowScopedViewModel(entry: NavBackStackEntry): ClearRecordingViewModel =
        ViewModelProvider(
            entry,
            viewModelFactory {
                initializer { ClearRecordingViewModel { clearedCount += 1 } }
            },
        )[ClearRecordingViewModel::class.java]

    private inline fun <reified T : Any> flowScopedViewModel(): ClearRecordingViewModel =
        composeRule.runOnIdle {
            flowScopedViewModel(harness.navController.getBackStackEntry<T>())
        }

    @Test
    fun `에디터 흐름 ViewModel 은 자식 화면 사이에서 같은 인스턴스로 유지된다`() {
        start(Route.Home)
        openEditorFlow()

        val onEditor = flowScopedViewModel<AfternoteRoute.EditorFlowRoute>()

        composeRule.runOnIdle { harness.afternoteActions.navigateToMemorialPlaylist() }
        assertSame(onEditor, flowScopedViewModel<AfternoteRoute.EditorFlowRoute>())

        composeRule.runOnIdle { harness.afternoteActions.navigateToAddSong() }
        assertSame(onEditor, flowScopedViewModel<AfternoteRoute.EditorFlowRoute>())

        composeRule.runOnIdle { harness.afternoteActions.popBack() }
        composeRule.runOnIdle { harness.afternoteActions.popBack() }
        assertSame(onEditor, flowScopedViewModel<AfternoteRoute.EditorFlowRoute>())
        assertEquals(0, clearedCount)
    }

    @Test
    fun `에디터 흐름은 저장 후 홈 복귀에서 정리된다`() {
        start(Route.Home)
        openEditorFlow()
        flowScopedViewModel<AfternoteRoute.EditorFlowRoute>()

        composeRule.runOnIdle { harness.afternoteActions.navigateToMemorialPlaylist() }
        composeRule.runOnIdle { harness.afternoteActions.popToAfternoteHome() }

        assertEquals(
            listOf("NavHostRoot", "Home", "Afternote", "AfternoteHomeRoute"),
            composeRule.runOnIdle { harness.navController.backStackRouteNames() },
        )
        assertEquals(1, clearedCount)
    }

    @Test
    fun `그래프 밖으로 나가도 백스택에 남아 있는 동안은 에디터 흐름이 정리되지 않는다`() {
        start(Route.Home)
        openEditorFlow()

        val onEditor = flowScopedViewModel<AfternoteRoute.EditorFlowRoute>()

        // 설정은 에디터 흐름 밖의 top-level 라우트지만 흐름 엔트리를 pop 하지 않는다.
        composeRule.runOnIdle { harness.afternoteActions.navigateToSetting() }
        assertEquals(0, clearedCount)

        composeRule.runOnIdle { harness.afternoteActions.popBack() }
        assertEquals("EditorRoute", composeRule.runOnIdle { harness.navController.currentRouteName() })
        assertSame(onEditor, flowScopedViewModel<AfternoteRoute.EditorFlowRoute>())
        assertEquals(0, clearedCount)
    }

    @Test
    fun `열람 신청 흐름 ViewModel 은 단계 이동으로 정리되지 않고 흐름 종료에서만 정리된다`() {
        start(Route.Receiver)

        composeRule.runOnIdle { harness.receiverActions.navigateToSenderDetail(SENDER_ID) }
        composeRule.runOnIdle { harness.receiverActions.navigateToDeliveryVerificationFlow(SENDER_ID) }

        val onIntro = flowScopedViewModel<ReceiverRoute.DeliveryVerificationFlowRoute>()

        composeRule.runOnIdle { harness.receiverActions.navigateToIdentityVerificationEmail() }
        composeRule.runOnIdle { harness.receiverActions.proceedToMasterKey() }
        composeRule.runOnIdle { harness.receiverActions.proceedToDocumentUpload() }
        composeRule.runOnIdle { harness.receiverActions.proceedToDeliveryVerificationComplete() }

        // 본인확인 화면들이 백스택에서 사라지는 동안에도 흐름 엔트리는 유지된다.
        assertSame(onIntro, flowScopedViewModel<ReceiverRoute.DeliveryVerificationFlowRoute>())
        assertEquals(0, clearedCount)

        composeRule.runOnIdle { harness.receiverActions.popToReceivedRecords() }

        assertEquals(
            listOf("NavHostRoot", "Receiver", "ReceivedRecordsRoute"),
            composeRule.runOnIdle { harness.navController.backStackRouteNames() },
        )
        assertEquals(1, clearedCount)
    }

    /** 지문 관문을 지나 새 에디터 흐름에 진입한다. */
    private fun openEditorFlow() {
        composeRule.runOnIdle { harness.appState.navigateToBottomBarRoute(Route.Afternote) }
        composeRule.runOnIdle { harness.afternoteActions.replaceFingerprintLoginWithAfternoteHome() }
        composeRule.runOnIdle {
            harness.afternoteActions.navigateToNewEditor(AfternoteType.SOCIAL_NETWORK)
        }
        assertEquals("EditorRoute", composeRule.runOnIdle { harness.navController.currentRouteName() })
    }

    private class ClearRecordingViewModel(
        private val onClearedRecorder: () -> Unit,
    ) : ViewModel() {
        override fun onCleared() {
            onClearedRecorder()
        }
    }

    private companion object {
        const val SENDER_ID = "sender-1601"
    }
}
