package com.afternote.feature.receiver.presentation.invitation

import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.domain.error.ReceiverInvitationFailure
import com.afternote.core.domain.testing.FakePendingReceiverInvitationStore
import com.afternote.core.domain.testing.FakeReceiverInvitationRepository
import com.afternote.core.model.user.ReceiverInvitationLookup
import com.afternote.core.ui.UiText
import com.afternote.feature.receiver.presentation.R
import com.afternote.feature.receiver.presentation.error.ReceiverErrorPopup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

/** 초대 랜딩·수락의 토큰 처분 규칙 (#944) — ViewModel KDoc 의 표를 그대로 잠근다. */
@OptIn(ExperimentalCoroutinesApi::class)
class ReceiverInvitationViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `진입하면 보관 토큰으로 조회해 초대자 이름을 그린다`() =
        runTest(dispatcher) {
            val fixture = Fixture()
            val viewModel = fixture.viewModel()
            advanceUntilIdle()

            assertEquals(ReceiverInvitationPhase.Ready("김혜성"), viewModel.uiState.value.phase)
            assertEquals(listOf(TOKEN), fixture.repository.lookupTokens)
        }

    @Test
    fun `만료된 초대는 토큰을 지우고 안내로 끝난다`() =
        runTest(dispatcher) {
            val fixture = Fixture()
            fixture.repository.lookup = ReceiverInvitationLookup(inviterName = "김혜성", isExpired = true)
            val viewModel = fixture.viewModel()
            advanceUntilIdle()

            assertEquals(
                ReceiverInvitationPhase.Notice(UiText.Resource(R.string.receiver_invitation_notice_expired)),
                viewModel.uiState.value.phase,
            )
            assertNull(fixture.store.tokenState.value)
        }

    @Test
    fun `수락 성공은 토큰을 지우고 완료 신호를 낸다`() =
        runTest(dispatcher) {
            val fixture = Fixture()
            val viewModel = fixture.viewModel()
            advanceUntilIdle()

            viewModel.onIntent(ReceiverInvitationIntent.Accept)
            advanceUntilIdle()

            assertEquals("김혜성", viewModel.uiState.value.acceptedInviterName)
            assertEquals(listOf(TOKEN), fixture.repository.acceptTokens)
            assertNull(fixture.store.tokenState.value)
            assertFalse(viewModel.uiState.value.isAccepting)
        }

    @Test
    fun `다른 사용자가 수락한 초대는 토큰을 지우고 안내로 끝난다`() =
        runTest(dispatcher) {
            val fixture = Fixture()
            fixture.repository.onAccept = { Result.failure(ReceiverInvitationFailure.AcceptedByOther(IllegalStateException())) }
            val viewModel = fixture.viewModel()
            advanceUntilIdle()

            viewModel.onIntent(ReceiverInvitationIntent.Accept)
            advanceUntilIdle()

            assertEquals(
                ReceiverInvitationPhase.Notice(UiText.Resource(R.string.receiver_invitation_notice_accepted_by_other)),
                viewModel.uiState.value.phase,
            )
            assertNull(fixture.store.tokenState.value)
        }

    @Test
    fun `이미 등록된 수신자면 토큰을 지우고 받은 기록함으로 보낸다`() =
        runTest(dispatcher) {
            val fixture = Fixture()
            fixture.repository.onAccept = { Result.failure(ReceiverInvitationFailure.AlreadyRegistered(IllegalStateException())) }
            val viewModel = fixture.viewModel()
            advanceUntilIdle()

            viewModel.onIntent(ReceiverInvitationIntent.Accept)
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.openReceivedRecords)
            assertNull(fixture.store.tokenState.value)
        }

    @Test
    fun `미인증 거절은 토큰을 남기고 로그인 신호를 낸다`() =
        runTest(dispatcher) {
            val fixture = Fixture()
            fixture.repository.onAccept = { Result.failure(ReceiverInvitationFailure.Unauthenticated(IllegalStateException())) }
            val viewModel = fixture.viewModel()
            advanceUntilIdle()

            viewModel.onIntent(ReceiverInvitationIntent.Accept)
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.loginRequired)
            assertEquals(TOKEN, fixture.store.tokenState.value)
        }

    @Test
    fun `전송 실패는 토큰을 남기고 네트워크 팝업을 띄우며 재시도는 수락을 다시 한다`() =
        runTest(dispatcher) {
            val fixture = Fixture()
            var failOnce = true
            fixture.repository.onAccept = {
                if (failOnce) {
                    failOnce = false
                    Result.failure(ReceiverInvitationFailure.Other(IOException("offline"), isRetryable = true))
                } else {
                    Result.success(fixture.repository.accepted)
                }
            }
            val viewModel = fixture.viewModel()
            advanceUntilIdle()

            viewModel.onIntent(ReceiverInvitationIntent.Accept)
            advanceUntilIdle()
            assertEquals(ReceiverErrorPopup.NETWORK, viewModel.uiState.value.errorPopup)
            assertEquals(TOKEN, fixture.store.tokenState.value)

            viewModel.onIntent(ReceiverInvitationIntent.Retry)
            advanceUntilIdle()
            assertNull(viewModel.uiState.value.errorPopup)
            assertEquals("김혜성", viewModel.uiState.value.acceptedInviterName)
            assertEquals(2, fixture.repository.acceptTokens.size)
        }

    @Test
    fun `5xx 는 서버 팝업이고 미등재 4xx 는 토큰을 남긴 채 안내로 끝난다`() =
        runTest(dispatcher) {
            val server = Fixture()
            server.repository.onLookup = { Result.failure(ReceiverInvitationFailure.Other(IllegalStateException(), isRetryable = true)) }
            val serverViewModel = server.viewModel()
            advanceUntilIdle()
            assertEquals(ReceiverErrorPopup.SERVER, serverViewModel.uiState.value.errorPopup)
            assertEquals(TOKEN, server.store.tokenState.value)

            val client = Fixture()
            client.repository.onLookup = { Result.failure(ReceiverInvitationFailure.Other(IllegalStateException(), isRetryable = false)) }
            val clientViewModel = client.viewModel()
            advanceUntilIdle()
            assertEquals(
                ReceiverInvitationPhase.Notice(UiText.Resource(R.string.receiver_invitation_notice_unavailable)),
                clientViewModel.uiState.value.phase,
            )
            assertEquals(TOKEN, client.store.tokenState.value)
        }

    @Test
    fun `나중에 결정하기는 토큰을 남기고 닫기 신호만 낸다`() =
        runTest(dispatcher) {
            val fixture = Fixture()
            val viewModel = fixture.viewModel()
            advanceUntilIdle()

            viewModel.onIntent(ReceiverInvitationIntent.Defer)

            assertTrue(viewModel.uiState.value.close)
            assertEquals(TOKEN, fixture.store.tokenState.value)
            assertEquals(0, fixture.store.clearCalls)
        }

    private class Fixture {
        val repository = FakeReceiverInvitationRepository()
        val store = FakePendingReceiverInvitationStore(token = TOKEN)

        fun viewModel() =
            ReceiverInvitationViewModel(
                invitationRepository = repository,
                pendingInvitationStore = store,
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
        const val TOKEN = "abcDEF123_-token"
    }
}
