package com.afternote.core.ui.navigation

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * 로컬 Navigation 3 스택 표시부의 **기전** 회귀 기준 (#1698).
 *
 * 피처별 백스택 «모양» 은 각 `*LocalNavActionsTest` 가 본다. 여기서 잠그는 것은 그 모양을
 * 실제 화면 수명으로 옮기는 [FeatureNavDisplay] 자신의 계약이다 — 이관 전 Nav2 의 백스택
 * 엔트리가 지켜 주던 것들이라, 깨져도 컴파일은 통과하고 조용히 상태만 사라진다.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], application = Application::class)
class FeatureNavDisplayTest {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var backStack: NavBackStack<NavKey>
    private val atRootSignals = mutableListOf<Boolean>()
    private var exits = 0
    private var clearedEntryViewModels = 0

    private val boundary =
        object : FeatureStackBoundary {
            override fun exit() {
                exits += 1
            }

            override fun onAtRootChanged(isAtRoot: Boolean) {
                atRootSignals += isAtRoot
            }
        }

    @Composable
    private fun TestHost() {
        val stack = rememberNavBackStack(RootKey)
        SideEffect { backStack = stack }

        FeatureNavDisplay(
            backStack = stack,
            boundary = boundary,
            entryProvider =
                entryProvider {
                    entry<RootKey> { BasicText("root") }
                    entry<DetailKey> { key ->
                        // entry 범위 스토어에 올라간(없으면 새로 만드는) 기록용 ViewModel.
                        val owner = checkNotNull(LocalViewModelStoreOwner.current) { "entry 스코프 owner 가 없다" }
                        ViewModelProvider(
                            owner,
                            viewModelFactory { initializer { ClearRecordingViewModel { clearedEntryViewModels += 1 } } },
                        )[ClearRecordingViewModel::class.java]

                        var taps by rememberSaveable { mutableIntStateOf(0) }
                        BasicText(
                            text = "detail${key.id}#$taps",
                            modifier = Modifier.clickable { taps++ },
                        )
                    }
                },
        )
    }

    private fun start() {
        composeRule.setContent { TestHost() }
        composeRule.waitForIdle()
    }

    private fun push(key: NavKey) = composeRule.runOnIdle { backStack.add(key) }

    private fun pop() = composeRule.runOnIdle { backStack.removeAt(backStack.lastIndex) }

    @Test
    fun `위에 화면이 쌓였다 사라져도 아래 화면의 rememberSaveable 이 남는다`() {
        start()
        push(DetailKey(id = 1))
        composeRule.onNodeWithText("detail1#0").performClick()
        composeRule.onNodeWithText("detail1#1").assertIsDisplayed()

        push(DetailKey(id = 2))
        composeRule.onNodeWithText("detail2#0").assertIsDisplayed()
        pop()

        // entryDecorators 를 넘기면 기본 목록을 «대체» 하므로, 저장 홀더 데코레이터를 빠뜨리면
        // 여기서 detail1#0 으로 되돌아간다 — 컴파일은 통과하고 상태만 조용히 사라지는 함정이다.
        composeRule.onNodeWithText("detail1#1").assertIsDisplayed()
    }

    @Test
    fun `entry 범위 ViewModel 은 그 화면이 스택에서 빠질 때 정리된다`() {
        start()
        push(DetailKey(id = 1))
        composeRule.waitForIdle()
        assertEquals(0, clearedEntryViewModels)

        // 위에 다른 화면이 쌓이는 것만으로는 정리되지 않는다 — 아직 백스택에 살아 있다.
        push(DetailKey(id = 2))
        composeRule.waitForIdle()
        assertEquals(0, clearedEntryViewModels)

        pop()
        composeRule.waitForIdle()
        assertEquals(1, clearedEntryViewModels)

        pop()
        composeRule.waitForIdle()
        assertEquals(2, clearedEntryViewModels)
    }

    @Test
    fun `스택 깊이 변화가 셸로 올라간다`() {
        start()
        assertEquals(listOf(true), atRootSignals)

        push(DetailKey(id = 1))
        composeRule.waitForIdle()
        assertEquals(listOf(true, false), atRootSignals)

        pop()
        composeRule.waitForIdle()
        assertEquals(listOf(true, false, true), atRootSignals)
    }

    @Test
    fun `host 가 컴포지션에서 빠지면 깊이 신호를 되돌린다`() {
        var shown by mutableStateOf(true)
        composeRule.setContent { if (shown) TestHost() }
        composeRule.waitForIdle()
        push(DetailKey(id = 1))
        composeRule.waitForIdle()
        assertEquals(false, atRootSignals.last())

        // 탭 이탈 — 되돌리지 않으면 다른 탭의 바텀바 판정이 이 피처의 마지막 깊이에 오염된다.
        composeRule.runOnIdle { shown = false }
        composeRule.waitForIdle()

        assertEquals(true, atRootSignals.last())
    }

    @Test
    fun `프로세스 재생성 뒤 스택과 화면 상태가 함께 복원된다`() {
        val restorationTester = StateRestorationTester(composeRule)
        restorationTester.setContent { TestHost() }
        composeRule.waitForIdle()

        push(DetailKey(id = 1))
        composeRule.onNodeWithText("detail1#0").performClick()
        composeRule.onNodeWithText("detail1#1").assertIsDisplayed()

        restorationTester.emulateSavedInstanceStateRestore()
        composeRule.waitForIdle()

        // rememberNavBackStack 이 키를 직렬화해 되살린다 — NavKey 가 @Serializable 이어야 하는 이유.
        assertEquals(listOf<NavKey>(RootKey, DetailKey(id = 1)), composeRule.runOnIdle { backStack.toList() })
        composeRule.onNodeWithText("detail1#1").assertIsDisplayed()
    }

    private class ClearRecordingViewModel(
        private val onCleared: () -> Unit,
    ) : ViewModel() {
        override fun onCleared() = onCleared.invoke()
    }
}

/**
 * 테스트용 키.
 *
 * `private` 로 두면 안 된다 — `rememberNavBackStack` 이 키를 리플렉션으로 직렬화하는데,
 * kotlinx.serialization 이 private 선언의 `INSTANCE` 에 접근하지 못해 저장 시점에
 * `IllegalAccessException` 이 난다. 프로덕션 라우트는 모두 공개라 걸리지 않는 함정이다.
 */
@Serializable
internal data object RootKey : NavKey

@Serializable
internal data class DetailKey(
    val id: Int,
) : NavKey
