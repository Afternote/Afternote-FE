package com.afternote.feature.receiver.presentation.senderdetail

import androidx.lifecycle.SavedStateHandle
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.feature.receiver.domain.model.DeliveryVerification
import com.afternote.feature.receiver.domain.model.DeliveryVerificationStatus
import com.afternote.feature.receiver.domain.model.ReceivedRecordBox
import com.afternote.feature.receiver.domain.model.SenderEntry
import com.afternote.feature.receiver.domain.testing.FakeReceiverAuthRepository
import com.afternote.feature.receiver.domain.testing.FakeReceiverRepository
import com.afternote.feature.receiver.domain.testing.FakeSenderRegistryRepository
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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

/**
 * 발신자 상세 '승인일' 표시 계약 가드 (#612).
 *
 * 종전에는 `delivery-verification/status` 응답에 승인 일시가 없어 승인 이후에도 칸이 비어 있었다.
 * 그 값을 주는 `record-boxes` 를 읽어 채우되, 같은 이메일에 등록된 다른 발신자의 칸이 함께 오므로
 * 접근 코드로 자기 칸을 골라야 한다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SenderDetailApprovedAtTest {
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
    fun `승인 이후에는 record-boxes 의 승인일을 표시한다`() =
        runTest(dispatcher) {
            val fixture = Fixture(DeliveryVerificationStatus.APPROVED)
            fixture.auth.recordBoxes = listOf(recordBox(MY_AUTH_CODE, approvedAt = "2026-07-29T16:00:10"))

            val viewModel = fixture.viewModel()
            advanceUntilIdle()
            val state = viewModel.uiState.value as SenderDetailUiState.Success

            assertEquals("2026.07.29.", state.approvedAt)
            assertEquals(SenderVerificationState.Approved, state.verification)
        }

    /**
     * 실서버는 시각을 마이크로초 6자리까지 보낸다 (dev 실측 2026-08-30, `2026-08-25T18:44:02.585799`).
     * 표시는 `T` 앞만 쓰므로 무사하다 — 소수부가 표시로 새지 않는지 실제 형태로 못박는다.
     */
    @Test
    fun `실서버 형태의 마이크로초 시각도 날짜만 표시한다`() =
        runTest(dispatcher) {
            val fixture = Fixture(DeliveryVerificationStatus.APPROVED)
            fixture.auth.recordBoxes = listOf(recordBox(MY_AUTH_CODE, approvedAt = "2026-08-25T18:44:02.585799"))

            val viewModel = fixture.viewModel()
            advanceUntilIdle()
            val state = viewModel.uiState.value as SenderDetailUiState.Success

            assertEquals("2026.08.25.", state.approvedAt)
        }

    @Test
    fun `같은 이메일의 다른 발신자 칸이 섞여 와도 내 접근 코드의 승인일만 쓴다`() =
        runTest(dispatcher) {
            val fixture = Fixture(DeliveryVerificationStatus.APPROVED)
            fixture.auth.recordBoxes =
                listOf(
                    recordBox("other-sender-code", approvedAt = "2026-01-01T00:00:00"),
                    recordBox(MY_AUTH_CODE, approvedAt = "2026-07-29T16:00:10"),
                    recordBox("another-code", approvedAt = "2026-12-31T23:59:59"),
                )

            val viewModel = fixture.viewModel()
            advanceUntilIdle()
            val state = viewModel.uiState.value as SenderDetailUiState.Success

            assertEquals("2026.07.29.", state.approvedAt)
        }

    @Test
    fun `승인 전에는 record-boxes 를 부르지 않는다`() =
        runTest(dispatcher) {
            val fixture = Fixture(DeliveryVerificationStatus.PENDING)

            val viewModel = fixture.viewModel()
            advanceUntilIdle()
            val state = viewModel.uiState.value as SenderDetailUiState.Success

            assertNull(state.approvedAt)
            // 서버가 approvedAt 을 APPROVED 일 때만 채우므로, 그 밖의 상태에서는 왕복 자체가 낭비다.
            assertEquals(0, fixture.auth.getReceivedRecordBoxesCalls)
        }

    @Test
    fun `내 칸이 목록에 없으면 승인일만 빈다`() =
        runTest(dispatcher) {
            val fixture = Fixture(DeliveryVerificationStatus.APPROVED)
            fixture.auth.recordBoxes = listOf(recordBox("other-sender-code", approvedAt = "2026-01-01T00:00:00"))

            val viewModel = fixture.viewModel()
            advanceUntilIdle()
            val state = viewModel.uiState.value as SenderDetailUiState.Success

            assertNull(state.approvedAt)
            assertEquals(SenderVerificationState.Approved, state.verification)
        }

    @Test
    fun `record-boxes 조회가 실패해도 상태와 신청일은 그대로 보여준다`() =
        runTest(dispatcher) {
            val fixture = Fixture(DeliveryVerificationStatus.APPROVED)
            fixture.auth.onGetReceivedRecordBoxes = { Result.failure(IOException("일시적 실패")) }

            val viewModel = fixture.viewModel()
            advanceUntilIdle()
            val state = viewModel.uiState.value as SenderDetailUiState.Success

            assertNull(state.approvedAt)
            assertEquals(SenderVerificationState.Approved, state.verification)
            assertEquals("2026.08.01.", state.requestedAt)
        }

    private class Fixture(
        status: DeliveryVerificationStatus,
    ) {
        private val senderId = "sender-id"
        val registry =
            SenderRegistry(
                FakeSenderRegistryRepository(
                    initialSenders =
                        listOf(
                            SenderEntry(
                                id = senderId,
                                name = "아버지",
                                masterKey = MY_AUTH_CODE,
                                realSenderName = "김발신",
                                relation = "가족",
                            ),
                        ),
                ),
            )
        val receiver = FakeReceiverRepository.strict().apply { onSaveMasterKey = {} }
        val auth =
            FakeReceiverAuthRepository(
                onGetDeliveryVerificationStatus = {
                    Result.success(
                        DeliveryVerification(
                            id = 1L,
                            status = status,
                            deathCertificateUrl = null,
                            familyRelationCertificateUrl = null,
                            adminNote = null,
                            createdAt = "2026-08-01T10:00:00Z",
                        ),
                    )
                },
            )

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

    private companion object {
        const val MY_AUTH_CODE = "auth-1"

        fun recordBox(
            masterKey: String,
            approvedAt: String?,
        ) = ReceivedRecordBox(
            receiverId = 1L,
            masterKey = masterKey,
            senderName = "김발신",
            verificationStatus = DeliveryVerificationStatus.APPROVED,
            requestedAt = "2026-06-21T03:07:26",
            approvedAt = approvedAt,
        )
    }
}
