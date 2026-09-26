package com.afternote.afternote_fe

import androidx.lifecycle.SavedStateHandle
import com.afternote.core.common.deeplink.NavigationTarget
import com.afternote.core.domain.testing.FakeAuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * 링크 목적지의 관문 대기·1회성 재개·재생성 복원·폐기 회귀 기준 (#924).
 *
 * 판정 대상은 «언제 재개할 수 있는 값이 되는가» 다. 실제 이동은 `AppLinkTargetBackStackTest` 가
 * 루트 백스택으로 잰다 — 두 축을 한 테스트에 섞으면 어느 쪽이 깨졌는지 보이지 않는다.
 *
 * [MainViewModel.resumableAppLinkTarget] 은 `WhileSubscribed` 라 구독자가 없으면 값이 흐르지
 * 않는다. 그래서 모든 테스트가 [startCollecting] 으로 구독을 먼저 연다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelAppLinkTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `로그인 관문을 통과하지 못한 목적지는 재개하지 않는다`() =
        runTest(dispatcher) {
            val authRepository = FakeAuthRepository(loggedIn = false)
            val viewModel = viewModel(authRepository)
            startCollecting(viewModel)

            viewModel.enqueueAppLinkTarget(TARGET)
            runCurrent()

            assertNull(viewModel.resumableAppLinkTarget.value)
        }

    @Test
    fun `로그인 관문을 통과하면 기다리던 목적지가 재개된다`() =
        runTest(dispatcher) {
            val authRepository = FakeAuthRepository(loggedIn = false)
            val viewModel = viewModel(authRepository)
            startCollecting(viewModel)
            viewModel.enqueueAppLinkTarget(TARGET)

            authRepository.loggedIn = true
            runCurrent()

            assertEquals(TARGET, viewModel.resumableAppLinkTarget.value)
        }

    @Test
    fun `재개를 마친 목적지는 한 번만 열린다`() =
        runTest(dispatcher) {
            val viewModel = viewModel(FakeAuthRepository(loggedIn = true))
            startCollecting(viewModel)
            viewModel.enqueueAppLinkTarget(TARGET)
            runCurrent()

            viewModel.consumeAppLinkTarget(TARGET)
            runCurrent()

            assertNull(viewModel.resumableAppLinkTarget.value)
        }

    @Test
    fun `재개 도중 도착한 새 링크는 옛 목적지의 소비로 지워지지 않는다`() =
        runTest(dispatcher) {
            val viewModel = viewModel(FakeAuthRepository(loggedIn = true))
            startCollecting(viewModel)

            viewModel.enqueueAppLinkTarget(TARGET)
            viewModel.enqueueAppLinkTarget(LATEST_TARGET)
            viewModel.consumeAppLinkTarget(TARGET)
            runCurrent()

            assertEquals(LATEST_TARGET, viewModel.resumableAppLinkTarget.value)
        }

    @Test
    fun `프로세스 재생성 뒤에도 재개하지 않은 목적지는 복원된다`() =
        runTest(dispatcher) {
            val savedStateHandle = SavedStateHandle()
            viewModel(FakeAuthRepository(loggedIn = true), savedStateHandle)
                .enqueueAppLinkTarget(TARGET)

            val restored = viewModel(FakeAuthRepository(loggedIn = true), savedStateHandle.snapshot())
            startCollecting(restored)
            runCurrent()

            assertEquals(TARGET, restored.resumableAppLinkTarget.value)
        }

    @Test
    fun `프로세스 재생성 뒤에 이미 재개한 목적지는 되살아나지 않는다`() =
        runTest(dispatcher) {
            val savedStateHandle = SavedStateHandle()
            val viewModel = viewModel(FakeAuthRepository(loggedIn = true), savedStateHandle)
            viewModel.enqueueAppLinkTarget(TARGET)
            viewModel.consumeAppLinkTarget(TARGET)

            val restored = viewModel(FakeAuthRepository(loggedIn = true), savedStateHandle.snapshot())
            startCollecting(restored)
            runCurrent()

            assertNull(restored.resumableAppLinkTarget.value)
        }

    @Test
    fun `로그아웃은 기다리던 목적지를 폐기한다`() =
        runTest(dispatcher) {
            val authRepository = FakeAuthRepository(loggedIn = true)
            val viewModel = viewModel(authRepository)
            startCollecting(viewModel)
            viewModel.enqueueAppLinkTarget(TARGET)
            runCurrent()

            authRepository.loggedIn = false
            authRepository.loggedIn = true
            runCurrent()

            assertNull(viewModel.resumableAppLinkTarget.value)
        }

    /** 로그인 전부터 기다리던 목적지는 «로그아웃» 이 아니다 — 첫 false 에 버리면 재개 자체가 없어진다. */
    @Test
    fun `로그인 전부터 기다리던 목적지는 버리지 않는다`() =
        runTest(dispatcher) {
            val authRepository = FakeAuthRepository(loggedIn = false)
            val viewModel = viewModel(authRepository)
            startCollecting(viewModel)
            viewModel.enqueueAppLinkTarget(TARGET)
            runCurrent()

            authRepository.loggedIn = true
            runCurrent()

            assertEquals(TARGET, viewModel.resumableAppLinkTarget.value)
        }

    private fun viewModel(
        authRepository: FakeAuthRepository,
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
    ): MainViewModel =
        MainViewModel(
            authRepository = authRepository,
            savedStateHandle = savedStateHandle,
        )

    private fun TestScope.startCollecting(viewModel: MainViewModel) {
        backgroundScope.collectForever(viewModel)
    }

    private fun CoroutineScope.collectForever(viewModel: MainViewModel) {
        launch(UnconfinedTestDispatcher()) { viewModel.resumableAppLinkTarget.collect { } }
    }

    /** Activity 재생성이 저장된 Bundle 로 새 핸들을 만드는 것과 같은 모양. */
    private fun SavedStateHandle.snapshot(): SavedStateHandle = SavedStateHandle(keys().associateWith { key -> get<Any?>(key) })

    private companion object {
        val TARGET: NavigationTarget = NavigationTarget.NotificationSettings
        val LATEST_TARGET: NavigationTarget = NavigationTarget.DailyQuestionCompose
    }
}
