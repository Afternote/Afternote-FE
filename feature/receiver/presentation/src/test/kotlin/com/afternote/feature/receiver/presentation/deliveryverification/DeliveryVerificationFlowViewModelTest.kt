package com.afternote.feature.receiver.presentation.deliveryverification

import androidx.lifecycle.SavedStateHandle
import com.afternote.feature.receiver.domain.testing.FakeIdentityVerificationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 열람 신청 흐름의 Intro 스킵 관문이 발신자별로 격리되는지 본다 (#597).
 *
 * 이전에는 전역 boolean 캐시라 발신자 A 인증만으로 발신자 B 흐름의 Intro·이메일 단계까지
 * 건너뛰었다 — 관문은 반드시 자신의 senderId 인증만 봐야 한다.
 *
 * Robolectric 인 이유: `SavedStateHandle.toRoute` 복원이 Bundle 기반이라 JVM 단독으로는 안 돈다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DeliveryVerificationFlowViewModelTest {
    // runTest 본문의 launch(collector)와 viewModelScope(stateIn 공유)가 같은 unconfined
    // 스케줄러를 타야 구독 → upstream 시작 → 값 반영이 단언 전에 동기로 끝난다.
    private val mainDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `A 발신자 인증이 B 발신자 관문을 열지 않는다`() =
        runTest(mainDispatcher) {
            val repository =
                FakeIdentityVerificationRepository(initialVerifiedSenderIds = setOf("sender-a"))

            val viewModelA = flowViewModel(senderId = "sender-a", repository = repository)
            val viewModelB = flowViewModel(senderId = "sender-b", repository = repository)

            // WhileSubscribed 라 구독이 있어야 upstream(repository 캐시 대역) 을 collect 한다.
            val jobs =
                listOf(
                    launch { viewModelA.isIdentityVerified.collect {} },
                    launch { viewModelB.isIdentityVerified.collect {} },
                )

            assertTrue(viewModelA.isIdentityVerified.value)
            assertFalse(viewModelB.isIdentityVerified.value)

            jobs.forEach { it.cancel() }
        }

    private fun flowViewModel(
        senderId: String,
        repository: FakeIdentityVerificationRepository,
    ): DeliveryVerificationFlowViewModel =
        DeliveryVerificationFlowViewModel(
            savedStateHandle = SavedStateHandle(mapOf("senderId" to senderId)),
            identityVerificationRepository = repository,
        )
}
