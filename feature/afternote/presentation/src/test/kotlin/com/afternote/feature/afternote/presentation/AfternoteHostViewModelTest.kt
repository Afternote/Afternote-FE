package com.afternote.feature.afternote.presentation

import com.afternote.core.domain.testing.FakeUserProfileCacheRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AfternoteHostViewModelTest {
    @After
    fun resetDispatcher() = Dispatchers.resetMain()

    @Test
    fun `프로필은 화면 구독 때 시작하고 빠른 복귀는 기존 구독을 유지한다`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository = FakeUserProfileCacheRepository()
            val viewModel = AfternoteHostViewModel(repository)
            assertNull(viewModel.uiState.value.isPasskeyRegistered)
            assertEquals(0, repository.isPasskeyRegisteredFlowCalls)

            viewModel.onIntent(AfternoteHostIntent.ObserveProfile)
            runCurrent()
            assertEquals(false, viewModel.uiState.value.isPasskeyRegistered)
            assertEquals(1, repository.passkeyRegisteredState.subscriptionCount.value)

            viewModel.onIntent(AfternoteHostIntent.StopObservingProfile)
            runCurrent()
            advanceTimeBy(4_999)
            viewModel.onIntent(AfternoteHostIntent.ObserveProfile)
            repository.passkeyRegisteredState.value = true
            runCurrent()
            assertEquals(true, viewModel.uiState.value.isPasskeyRegistered)
            assertEquals(1, repository.isPasskeyRegisteredFlowCalls)

            viewModel.onIntent(AfternoteHostIntent.StopObservingProfile)
            runCurrent()
            advanceTimeBy(5_000)
            runCurrent()
            assertEquals(0, repository.passkeyRegisteredState.subscriptionCount.value)
            assertEquals(true, viewModel.uiState.value.isPasskeyRegistered)

            repository.passkeyRegisteredState.value = false
            viewModel.onIntent(AfternoteHostIntent.ObserveProfile)
            runCurrent()
            assertEquals(false, viewModel.uiState.value.isPasskeyRegistered)
            assertEquals(2, repository.isPasskeyRegisteredFlowCalls)
            viewModel.onIntent(AfternoteHostIntent.StopObservingProfile)
            advanceTimeBy(5_001)
            runCurrent()
        }
}
