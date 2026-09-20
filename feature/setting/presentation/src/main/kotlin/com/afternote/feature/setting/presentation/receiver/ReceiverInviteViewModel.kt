package com.afternote.feature.setting.presentation.receiver

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.common.result.runCatchingCancellable
import com.afternote.core.domain.error.ReceiverInvitationFailure
import com.afternote.core.domain.repository.MyProfileRepository
import com.afternote.core.domain.repository.ReceiverInvitationRepository
import com.afternote.core.domain.repository.UserProfileCacheRepository
import com.afternote.core.ui.UiText
import com.afternote.core.ui.mvi.MviViewModel
import com.afternote.feature.setting.presentation.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 수신자 등록 화면의 카카오톡 초대 발급 (#944).
 *
 * 초대 발급(`POST receiver-invitations`)까지만 담당한다. 공유 자체는 카카오 SDK 가 Activity 를
 * 요구해 화면이 [ReceiverInviteUiState.shareRequest] 를 받아 띄운다.
 *
 * 템플릿 제목이 «{senderName}님이 …» 라 내 이름이 비면 문구가 깨진다. 홈 캐시(`UserProfileCacheRepository`)를
 * 먼저 보고, 없으면 서버 프로필을 읽는다. 둘 다 못 읽으면 공유를 열지 않고 실패로 알린다.
 *
 * 공유를 띄운 초대(토큰·내 이름·수신자 이름)는 [SavedStateHandle] 에 둔다 — «초대 링크 다시 보내기» 가
 * 같은 토큰으로 공유를 다시 열어야 하고, 회전으로 ViewModel 이 재생성돼도 «보냈어요» phase 가 남아야 한다.
 * 토큰은 비밀값이라 로그·리포팅에 싣지 않는다.
 */
@HiltViewModel
internal class ReceiverInviteViewModel
    @Inject
    constructor(
        private val invitationRepository: ReceiverInvitationRepository,
        private val profileCacheRepository: UserProfileCacheRepository,
        private val myProfileRepository: MyProfileRepository,
        private val errorReporter: ErrorReporter,
        private val savedStateHandle: SavedStateHandle,
    ) : MviViewModel<ReceiverInviteIntent, ReceiverInviteUiState, ReceiverInviteReducerEvent>(
            ReceiverInviteUiState(sentInvitation = savedStateHandle.restoreSentInvitation()),
        ) {
        /** 가장 최근에 발급한 초대 — 공유가 뜬 것이 확인되면 [markSent] 가 SavedState 로 올린다. */
        private var lastCreated: ReceiverInviteShareRequest? = null

        override fun onIntent(intent: ReceiverInviteIntent) {
            when (intent) {
                is ReceiverInviteIntent.OpenSheet -> {
                    dispatch(ReceiverInviteReducerEvent.SheetOpened(intent.receiverName))
                }

                ReceiverInviteIntent.CloseSheet -> {
                    dispatch(ReceiverInviteReducerEvent.SheetClosed)
                }

                ReceiverInviteIntent.SendInvite -> {
                    sendInvite()
                }

                ReceiverInviteIntent.ShareLaunched -> {
                    markSent()
                }

                is ReceiverInviteIntent.ShareFailed -> {
                    // 카카오 SDK 오류·startActivity 실패의 원문은 여기서만 남는다(토큰은 실리지 않는다).
                    errorReporter.recordFailure(intent.cause, mapOf(KEY_STAGE to STAGE_SHARE))
                    dispatch(ReceiverInviteReducerEvent.ShareFailed(intent.message))
                }

                ReceiverInviteIntent.Resend -> {
                    resend()
                }

                ReceiverInviteIntent.ConsumeShareRequest -> {
                    dispatch(ReceiverInviteReducerEvent.ShareRequestConsumed)
                }

                ReceiverInviteIntent.ConsumeError -> {
                    dispatch(ReceiverInviteReducerEvent.ErrorConsumed)
                }
            }
        }

        override fun reduce(
            state: ReceiverInviteUiState,
            event: ReceiverInviteReducerEvent,
        ): ReceiverInviteUiState =
            when (event) {
                is ReceiverInviteReducerEvent.SheetOpened -> state.copy(sheetReceiverName = event.receiverName, errorMessage = null)
                ReceiverInviteReducerEvent.SheetClosed -> state.copy(sheetReceiverName = null, isCreating = false)
                ReceiverInviteReducerEvent.Creating -> state.copy(isCreating = true, errorMessage = null)
                is ReceiverInviteReducerEvent.Created -> state.copy(isCreating = false, shareRequest = event.request)
                is ReceiverInviteReducerEvent.CreateFailed -> state.copy(isCreating = false, errorMessage = event.message)
                is ReceiverInviteReducerEvent.ShareFailed -> state.copy(errorMessage = event.message)
                is ReceiverInviteReducerEvent.Sent -> state.copy(sheetReceiverName = null, sentInvitation = event.invitation)
                is ReceiverInviteReducerEvent.ResendRequested -> state.copy(shareRequest = event.request)
                ReceiverInviteReducerEvent.ShareRequestConsumed -> state.copy(shareRequest = null)
                ReceiverInviteReducerEvent.ErrorConsumed -> state.copy(errorMessage = null)
            }

        private fun sendInvite() {
            val receiverName = currentState.sheetReceiverName ?: return
            if (currentState.isCreating) return
            viewModelScope.launch {
                dispatch(ReceiverInviteReducerEvent.Creating)
                val senderName = resolveSenderName()
                if (senderName == null) {
                    dispatch(
                        ReceiverInviteReducerEvent.CreateFailed(
                            UiText.Resource(R.string.setting_receiver_invite_sender_name_unavailable),
                        ),
                    )
                    return@launch
                }
                invitationRepository
                    .create()
                    .onSuccess { created ->
                        val request =
                            ReceiverInviteShareRequest(
                                token = created.token,
                                senderName = senderName,
                                receiverName = receiverName,
                            )
                        lastCreated = request
                        dispatch(ReceiverInviteReducerEvent.Created(request))
                    }.onFailure { failure ->
                        val messageRes =
                            if (failure is ReceiverInvitationFailure.Unauthenticated) {
                                R.string.setting_receiver_invite_create_unauthenticated
                            } else {
                                R.string.setting_receiver_invite_create_failed
                            }
                        errorReporter.recordFailure(failure, mapOf(KEY_STAGE to STAGE_CREATE))
                        dispatch(ReceiverInviteReducerEvent.CreateFailed(UiText.Resource(messageRes)))
                    }
            }
        }

        /** 공유가 떴다 — 마지막 발급분을 보냈어요 phase 로 올리고 SavedState 에 남긴다. */
        private fun markSent() {
            val request = lastCreated ?: return
            savedStateHandle.storeSentInvitation(request)
            dispatch(ReceiverInviteReducerEvent.Sent(request))
        }

        private fun resend() {
            val sent = currentState.sentInvitation ?: return
            dispatch(ReceiverInviteReducerEvent.ResendRequested(sent))
        }

        private suspend fun resolveSenderName(): String? {
            val cached = runCatchingCancellable { profileCacheRepository.getCachedUserName() }.getOrNull()
            if (!cached.isNullOrBlank()) return cached
            return runCatchingCancellable { myProfileRepository.getMyProfile().name }
                .onFailure { errorReporter.recordFailure(it, mapOf(KEY_STAGE to STAGE_SENDER_NAME)) }
                .getOrNull()
                ?.takeIf { it.isNotBlank() }
        }

        private companion object {
            const val KEY_STAGE = "stage"
            const val STAGE_CREATE = "receiver_invite_create"
            const val STAGE_SENDER_NAME = "receiver_invite_sender_name"
            const val STAGE_SHARE = "receiver_invite_share"
        }
    }

private const val SAVED_SENT_TOKEN = "receiver_invite_sent_token"
private const val SAVED_SENT_SENDER_NAME = "receiver_invite_sent_sender_name"
private const val SAVED_SENT_RECEIVER_NAME = "receiver_invite_sent_receiver_name"

private fun SavedStateHandle.restoreSentInvitation(): ReceiverInviteShareRequest? {
    val token = get<String>(SAVED_SENT_TOKEN) ?: return null
    val senderName = get<String>(SAVED_SENT_SENDER_NAME) ?: return null
    val receiverName = get<String>(SAVED_SENT_RECEIVER_NAME) ?: return null
    return ReceiverInviteShareRequest(token = token, senderName = senderName, receiverName = receiverName)
}

private fun SavedStateHandle.storeSentInvitation(request: ReceiverInviteShareRequest) {
    this[SAVED_SENT_TOKEN] = request.token
    this[SAVED_SENT_SENDER_NAME] = request.senderName
    this[SAVED_SENT_RECEIVER_NAME] = request.receiverName
}
