package com.afternote.feature.setting.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.afternote.core.domain.error.ReceiverInvitationFailure
import com.afternote.core.domain.testing.FakeMyProfileRepository
import com.afternote.core.domain.testing.FakeReceiverInvitationRepository
import com.afternote.core.domain.testing.FakeUserProfileCacheRepository
import com.afternote.core.model.user.User
import com.afternote.core.ui.UiText
import com.afternote.feature.setting.presentation.NoOpErrorReporter
import com.afternote.feature.setting.presentation.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.io.IOException

/** 수신자 등록 화면의 초대 발급 — 내 이름 확보와 실패 안내 (#944). */
@OptIn(ExperimentalCoroutinesApi::class)
class ReceiverInviteViewModelTest {
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
    fun `시트를 열고 보내면 캐시된 내 이름과 토큰으로 공유 요청을 낸다`() =
        runTest(dispatcher) {
            val fixture = Fixture(cachedName = "홍발신")
            val viewModel = fixture.viewModel()

            viewModel.onIntent(ReceiverInviteIntent.OpenSheet("김민서"))
            viewModel.onIntent(ReceiverInviteIntent.SendInvite)
            advanceUntilIdle()

            assertEquals(
                ReceiverInviteShareRequest(
                    token = FakeReceiverInvitationRepository.DEFAULT_TOKEN,
                    senderName = "홍발신",
                    receiverName = "김민서",
                ),
                viewModel.uiState.value.shareRequest,
            )
            assertEquals(1, fixture.invitations.createCalls)
            assertEquals(0, fixture.profile.getProfileCalls)
        }

    @Test
    fun `캐시에 이름이 없으면 서버 프로필에서 읽는다`() =
        runTest(dispatcher) {
            val fixture = Fixture(cachedName = null)
            fixture.profile.profile = User(name = "서버이름", email = "a@b.c", phone = null, profileImageUrl = null)
            val viewModel = fixture.viewModel()

            viewModel.onIntent(ReceiverInviteIntent.OpenSheet("김민서"))
            viewModel.onIntent(ReceiverInviteIntent.SendInvite)
            advanceUntilIdle()

            assertEquals(
                "서버이름",
                viewModel.uiState.value.shareRequest
                    ?.senderName,
            )
        }

    @Test
    fun `내 이름을 어디서도 못 읽으면 초대를 만들지 않고 실패로 알린다`() =
        runTest(dispatcher) {
            val fixture = Fixture(cachedName = null)
            fixture.profile.onGetMyProfile = { throw IOException("offline") }
            val viewModel = fixture.viewModel()

            viewModel.onIntent(ReceiverInviteIntent.OpenSheet("김민서"))
            viewModel.onIntent(ReceiverInviteIntent.SendInvite)
            advanceUntilIdle()

            assertNull(viewModel.uiState.value.shareRequest)
            assertEquals(UiText.Resource(R.string.receiver_invite_sender_name_unavailable), viewModel.uiState.value.errorMessage)
            assertEquals(0, fixture.invitations.createCalls)
            assertEquals("김민서", viewModel.uiState.value.sheetReceiverName)
        }

    @Test
    fun `발급 실패는 시트를 유지한 채 안내하고 미인증은 로그인 안내다`() =
        runTest(dispatcher) {
            val fixture = Fixture(cachedName = "홍발신")
            fixture.invitations.onCreate = { Result.failure(ReceiverInvitationFailure.Unauthenticated(IllegalStateException())) }
            val viewModel = fixture.viewModel()

            viewModel.onIntent(ReceiverInviteIntent.OpenSheet("김민서"))
            viewModel.onIntent(ReceiverInviteIntent.SendInvite)
            advanceUntilIdle()

            assertEquals(UiText.Resource(R.string.receiver_invite_create_unauthenticated), viewModel.uiState.value.errorMessage)
            assertEquals("김민서", viewModel.uiState.value.sheetReceiverName)
        }

    @Test
    fun `공유가 뜨면 보냈어요 phase 로 가고 다시 보내기는 새 발급 없이 같은 토큰을 다시 올린다`() =
        runTest(dispatcher) {
            val fixture = Fixture(cachedName = "홍발신")
            val viewModel = fixture.viewModel()
            viewModel.onIntent(ReceiverInviteIntent.OpenSheet("김민서"))
            viewModel.onIntent(ReceiverInviteIntent.SendInvite)
            advanceUntilIdle()
            val request = requireNotNull(viewModel.uiState.value.shareRequest)
            viewModel.onIntent(ReceiverInviteIntent.ConsumeShareRequest)

            viewModel.onIntent(ReceiverInviteIntent.ShareLaunched)

            assertEquals(request, viewModel.uiState.value.sentInvitation)
            assertNull(viewModel.uiState.value.sheetReceiverName)

            viewModel.onIntent(ReceiverInviteIntent.Resend)
            assertEquals(request, viewModel.uiState.value.shareRequest)
            assertEquals(1, fixture.invitations.createCalls)
        }

    @Test
    fun `보냈어요 phase 는 SavedState 로 재생성 뒤에도 남는다`() =
        runTest(dispatcher) {
            val savedStateHandle = SavedStateHandle()
            val fixture = Fixture(cachedName = "홍발신", savedStateHandle = savedStateHandle)
            val viewModel = fixture.viewModel()
            viewModel.onIntent(ReceiverInviteIntent.OpenSheet("김민서"))
            viewModel.onIntent(ReceiverInviteIntent.SendInvite)
            advanceUntilIdle()
            viewModel.onIntent(ReceiverInviteIntent.ShareLaunched)

            val restoredValues = savedStateHandle.keys().associateWith { key -> savedStateHandle.get<Any?>(key) }
            val restored = Fixture(cachedName = "홍발신", savedStateHandle = SavedStateHandle(restoredValues)).viewModel()

            assertEquals(viewModel.uiState.value.sentInvitation, restored.uiState.value.sentInvitation)
        }

    @Test
    fun `시트가 닫혀 있으면 보내기는 무시된다`() =
        runTest(dispatcher) {
            val fixture = Fixture(cachedName = "홍발신")
            val viewModel = fixture.viewModel()

            viewModel.onIntent(ReceiverInviteIntent.SendInvite)
            advanceUntilIdle()

            assertEquals(0, fixture.invitations.createCalls)
        }

    private class Fixture(
        cachedName: String?,
        val savedStateHandle: SavedStateHandle = SavedStateHandle(),
    ) {
        val invitations = FakeReceiverInvitationRepository()
        val cache = FakeUserProfileCacheRepository(cachedUserName = cachedName)
        val profile = FakeMyProfileRepository()

        fun viewModel() =
            ReceiverInviteViewModel(
                invitationRepository = invitations,
                profileCacheRepository = cache,
                myProfileRepository = profile,
                errorReporter = NoOpErrorReporter,
                savedStateHandle = savedStateHandle,
            )
    }
}
