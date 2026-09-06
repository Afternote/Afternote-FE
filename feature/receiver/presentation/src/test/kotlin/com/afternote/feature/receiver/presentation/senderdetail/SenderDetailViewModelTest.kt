package com.afternote.feature.receiver.presentation.senderdetail

import androidx.lifecycle.SavedStateHandle
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.feature.receiver.domain.model.DeliveryVerification
import com.afternote.feature.receiver.domain.model.DeliveryVerificationStatus
import com.afternote.feature.receiver.domain.model.ReceiverIdentity
import com.afternote.feature.receiver.domain.testing.FakeReceiverAuthRepository
import com.afternote.feature.receiver.domain.testing.FakeReceiverRepository
import com.afternote.feature.receiver.presentation.recordsbox.SenderRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

/** 발신자 상세 재진입 갱신([SenderDetailViewModel.refreshOnReturn]) 계약 가드 (#701). */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SenderDetailViewModelTest {
    private lateinit var dispatcher: TestDispatcher

    @Before
    fun setUp() {
        dispatcher = StandardTestDispatcher()
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `refreshOnReturn - 열람 신청 흐름 복귀 시 로딩 없이 새 상태로 갱신한다`() =
        runTest(dispatcher) {
            val fixture = Fixture()
            fixture.statusResults += Result.success(verification(DeliveryVerificationStatus.PENDING))
            fixture.statusResults += Result.success(verification(DeliveryVerificationStatus.APPROVED))
            val viewModel = fixture.viewModel()
            viewModel.refreshOnReturn() // 첫 진입의 ON_RESUME — 스킵
            advanceUntilIdle()
            assertEquals(
                SenderVerificationState.Pending,
                (viewModel.uiState.value as SenderDetailUiState.Success).verification,
            )

            viewModel.refreshOnReturn() // 열람 신청 흐름에서 복귀한 ON_RESUME
            // 로딩을 방출하지 않는다 — 갱신이 도는 동안에도 기존 정보 박스를 유지한다.
            assertTrue(viewModel.uiState.value is SenderDetailUiState.Success)
            advanceUntilIdle()

            val refreshed = viewModel.uiState.value as SenderDetailUiState.Success
            assertEquals(SenderVerificationState.Approved, refreshed.verification)
            assertEquals(2, fixture.auth.getDeliveryVerificationStatusCalls)
        }

    @Test
    fun `refreshOnReturn - 상태 조회가 실패해도 보고 있던 정보 박스를 유지한다`() =
        runTest(dispatcher) {
            val fixture = Fixture()
            fixture.statusResults += Result.success(verification(DeliveryVerificationStatus.PENDING))
            fixture.statusResults += Result.failure(IOException("일시적 실패"))
            val viewModel = fixture.viewModel()
            viewModel.refreshOnReturn() // 첫 진입의 ON_RESUME — 스킵
            advanceUntilIdle()

            viewModel.refreshOnReturn() // 백스택 복귀의 ON_RESUME
            advanceUntilIdle()

            // StatusLoadFailed 로 대체되지 않는다.
            val state = viewModel.uiState.value as SenderDetailUiState.Success
            assertEquals(SenderVerificationState.Pending, state.verification)
        }

    @Test
    fun `첫 진입 resume 은 재조회를 트리거하지 않는다`() =
        runTest(dispatcher) {
            val fixture = Fixture()
            fixture.statusResults += Result.success(verification(DeliveryVerificationStatus.PENDING))
            val viewModel = fixture.viewModel()
            advanceUntilIdle()

            // 첫 진입 화면의 ON_RESUME (init 로드는 이미 종료됨) — 재조회가 걸리면 안 된다.
            viewModel.refreshOnReturn()
            advanceUntilIdle()

            assertEquals(1, fixture.auth.getDeliveryVerificationStatusCalls)
            assertTrue(viewModel.uiState.value is SenderDetailUiState.Success)
        }

    @Test
    fun `refreshOnReturn - 진행 중인 로드와 겹치면 건너뛴다`() =
        runTest(dispatcher) {
            val fixture = Fixture()
            fixture.statusResults += Result.success(verification(DeliveryVerificationStatus.PENDING))
            val viewModel = fixture.viewModel()

            // init 로드가 아직 도는 중 — 첫 resume(스킵) 뒤 또 한 번 resume 이 와도 중복이 없어야 한다.
            viewModel.refreshOnReturn()
            viewModel.refreshOnReturn()
            advanceUntilIdle()

            assertEquals(1, fixture.auth.getDeliveryVerificationStatusCalls)
            assertTrue(viewModel.uiState.value is SenderDetailUiState.Success)
        }
}

private class Fixture {
    val registry = SenderRegistry()
    val statusResults = ArrayDeque<Result<DeliveryVerification>>()
    val receiver =
        FakeReceiverRepository.strict().apply {
            onSaveMasterKey = {}
        }
    val auth =
        FakeReceiverAuthRepository(
            onGetDeliveryVerificationStatus = { statusResults.removeFirst() },
        )
    val senderId: String

    init {
        val entry = registry.register("아버지")
        registry.attachIdentity(
            id = entry.id,
            masterKey = "auth-1",
            identity =
                ReceiverIdentity(
                    receiverId = 1L,
                    receiverName = "김수신",
                    senderName = "김발신",
                    relation = "가족",
                ),
        )
        senderId = entry.id
    }

    fun viewModel(): SenderDetailViewModel =
        SenderDetailViewModel(
            savedStateHandle = SavedStateHandle(mapOf("senderId" to senderId)),
            senderRegistry = registry,
            receiverRepository = receiver,
            receiverAuthRepository = auth,
            errorReporter = NoopErrorReporter,
        )
}

private object NoopErrorReporter : ErrorReporter {
    override fun writeFailure(
        throwable: Throwable,
        attributes: Map<String, String>,
    ) = Unit
}

private fun verification(status: DeliveryVerificationStatus): DeliveryVerification =
    DeliveryVerification(
        id = 1L,
        status = status,
        deathCertificateUrl = null,
        familyRelationCertificateUrl = null,
        adminNote = null,
        createdAt = "2026-08-01T10:00:00Z",
    )
