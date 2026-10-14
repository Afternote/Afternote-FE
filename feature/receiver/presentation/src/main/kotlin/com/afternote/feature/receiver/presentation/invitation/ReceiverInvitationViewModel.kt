package com.afternote.feature.receiver.presentation.invitation

import androidx.lifecycle.viewModelScope
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.domain.error.ReceiverInvitationFailure
import com.afternote.core.domain.repository.PendingReceiverInvitationStore
import com.afternote.core.domain.repository.ReceiverInvitationRepository
import com.afternote.core.ui.UiText
import com.afternote.core.ui.mvi.MviViewModel
import com.afternote.feature.receiver.presentation.R
import com.afternote.feature.receiver.presentation.error.ReceiverErrorPopup
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

/**
 * 초대 랜딩·수락 ViewModel (#944).
 *
 * 토큰은 [PendingReceiverInvitationStore] 에서 읽는다 — 화면 인자로 나르지 않는다. 처분 규칙:
 *
 * | 결과 | 토큰 | 화면 |
 * |---|---|---|
 * | 수락 성공 | 지움 | 완료 화면 |
 * | 없음·만료·다른 사용자 수락·본인 초대 | 지움 | 안내 뒤 닫기 |
 * | 이미 등록됨 | 지움 | 받은 기록함 |
 * | 미인증 | 유지 | 로그인 |
 * | 네트워크·5xx | 유지 | 재시도 팝업 |
 * | 나중에 결정하기 | 유지 | 닫기 |
 */
@HiltViewModel
class ReceiverInvitationViewModel
    @Inject
    constructor(
        private val invitationRepository: ReceiverInvitationRepository,
        private val pendingInvitationStore: PendingReceiverInvitationStore,
        private val errorReporter: ErrorReporter,
    ) : MviViewModel<ReceiverInvitationIntent, ReceiverInvitationUiState, ReceiverInvitationReducerEvent>(
            ReceiverInvitationUiState(),
        ) {
        /** 마지막으로 실패한 작업 — «다시 시도» 가 무엇을 다시 할지 정한다. */
        private var retryAction: (() -> Unit)? = null

        init {
            lookup()
        }

        override fun onIntent(intent: ReceiverInvitationIntent) {
            when (intent) {
                ReceiverInvitationIntent.Accept -> accept()
                ReceiverInvitationIntent.Defer -> dispatch(ReceiverInvitationReducerEvent.CloseRequested)
                ReceiverInvitationIntent.AcknowledgeNotice -> dispatch(ReceiverInvitationReducerEvent.CloseRequested)
                ReceiverInvitationIntent.Retry -> retry()
                ReceiverInvitationIntent.DismissErrorPopup -> dispatch(ReceiverInvitationReducerEvent.ErrorPopupDismissed)
                ReceiverInvitationIntent.ConsumeAccepted -> dispatch(ReceiverInvitationReducerEvent.AcceptedConsumed)
                ReceiverInvitationIntent.ConsumeOpenReceivedRecords -> dispatch(ReceiverInvitationReducerEvent.OpenReceivedRecordsConsumed)
                ReceiverInvitationIntent.ConsumeLoginRequired -> dispatch(ReceiverInvitationReducerEvent.LoginRequiredConsumed)
                ReceiverInvitationIntent.ConsumeClose -> dispatch(ReceiverInvitationReducerEvent.CloseConsumed)
            }
        }

        override fun reduce(
            state: ReceiverInvitationUiState,
            event: ReceiverInvitationReducerEvent,
        ): ReceiverInvitationUiState =
            when (event) {
                ReceiverInvitationReducerEvent.LookupStarted -> {
                    state.copy(phase = ReceiverInvitationPhase.Loading)
                }

                is ReceiverInvitationReducerEvent.LookupSucceeded -> {
                    state.copy(phase = ReceiverInvitationPhase.Ready(event.inviterName))
                }

                is ReceiverInvitationReducerEvent.NoticeShown -> {
                    state.copy(
                        phase = ReceiverInvitationPhase.Notice(event.message),
                        isAccepting = false,
                    )
                }

                is ReceiverInvitationReducerEvent.ErrorPopupShown -> {
                    state.copy(errorPopup = event.popup, isAccepting = false)
                }

                ReceiverInvitationReducerEvent.ErrorPopupDismissed -> {
                    state.copy(errorPopup = null)
                }

                ReceiverInvitationReducerEvent.AcceptStarted -> {
                    state.copy(isAccepting = true)
                }

                is ReceiverInvitationReducerEvent.AcceptSucceeded -> {
                    state.copy(
                        isAccepting = false,
                        acceptedInviterName = event.inviterName,
                    )
                }

                ReceiverInvitationReducerEvent.AcceptFinished -> {
                    state.copy(isAccepting = false)
                }

                ReceiverInvitationReducerEvent.AlreadyRegistered -> {
                    state.copy(isAccepting = false, openReceivedRecords = true)
                }

                ReceiverInvitationReducerEvent.LoginRequired -> {
                    state.copy(isAccepting = false, loginRequired = true)
                }

                ReceiverInvitationReducerEvent.CloseRequested -> {
                    state.copy(close = true)
                }

                ReceiverInvitationReducerEvent.AcceptedConsumed -> {
                    state.copy(acceptedInviterName = null)
                }

                ReceiverInvitationReducerEvent.OpenReceivedRecordsConsumed -> {
                    state.copy(openReceivedRecords = false)
                }

                ReceiverInvitationReducerEvent.LoginRequiredConsumed -> {
                    state.copy(loginRequired = false)
                }

                ReceiverInvitationReducerEvent.CloseConsumed -> {
                    state.copy(close = false)
                }
            }

        private fun lookup() {
            retryAction = ::lookup
            viewModelScope.launch {
                dispatch(ReceiverInvitationReducerEvent.LookupStarted)
                val token = pendingInvitationStore.pendingToken.first()
                if (token == null) {
                    // 랜딩이 떠 있는 동안 다른 경로가 토큰을 지웠다 — 보여 줄 초대가 없다.
                    dispatch(ReceiverInvitationReducerEvent.NoticeShown(UiText.Resource(R.string.receiver_invitation_notice_not_found)))
                    return@launch
                }
                invitationRepository
                    .lookup(token)
                    .onSuccess { lookup ->
                        if (lookup.isExpired) {
                            pendingInvitationStore.clear()
                            dispatch(
                                ReceiverInvitationReducerEvent.NoticeShown(UiText.Resource(R.string.receiver_invitation_notice_expired)),
                            )
                        } else {
                            dispatch(ReceiverInvitationReducerEvent.LookupSucceeded(lookup.inviterName))
                        }
                    }.onFailure { failure -> handleFailure(failure, stage = STAGE_LOOKUP) }
            }
        }

        private fun accept() {
            if (currentState.isAccepting) return
            retryAction = ::accept
            viewModelScope.launch {
                dispatch(ReceiverInvitationReducerEvent.AcceptStarted)
                val token = pendingInvitationStore.pendingToken.first()
                if (token == null) {
                    dispatch(ReceiverInvitationReducerEvent.NoticeShown(UiText.Resource(R.string.receiver_invitation_notice_not_found)))
                    return@launch
                }
                invitationRepository
                    .accept(token)
                    .onSuccess { accepted ->
                        pendingInvitationStore.clear()
                        dispatch(ReceiverInvitationReducerEvent.AcceptSucceeded(accepted.inviterName))
                    }.onFailure { failure -> handleFailure(failure, stage = STAGE_ACCEPT) }
            }
        }

        private fun retry() {
            dispatch(ReceiverInvitationReducerEvent.ErrorPopupDismissed)
            retryAction?.invoke()
        }

        /**
         * 실패 처분. [ReceiverInvitationFailure] 가 아닌 것은 Data 계층이 번역하지 못한 로컬 실패라
         * 서버 오류 팝업으로 보낸다 — 토큰은 남긴다.
         */
        private suspend fun handleFailure(
            failure: Throwable,
            stage: String,
        ) {
            when (failure) {
                is ReceiverInvitationFailure.NotFound -> {
                    notice(R.string.receiver_invitation_notice_not_found)
                }

                is ReceiverInvitationFailure.Expired -> {
                    notice(R.string.receiver_invitation_notice_expired)
                }

                is ReceiverInvitationFailure.AcceptedByOther -> {
                    notice(R.string.receiver_invitation_notice_accepted_by_other)
                }

                is ReceiverInvitationFailure.SelfAccept -> {
                    notice(R.string.receiver_invitation_notice_self_accept)
                }

                is ReceiverInvitationFailure.AlreadyRegistered -> {
                    pendingInvitationStore.clear()
                    dispatch(ReceiverInvitationReducerEvent.AlreadyRegistered)
                }

                is ReceiverInvitationFailure.Unauthenticated -> {
                    dispatch(ReceiverInvitationReducerEvent.LoginRequired)
                }

                is ReceiverInvitationFailure.Other -> {
                    errorReporter.recordFailure(failure, mapOf(KEY_STAGE to stage))
                    when {
                        // 미등재 4xx — 다시 보내도 같은 거절이라 재시도 팝업이 아니라 안내로 끝낸다.
                        // 사유를 모르므로 토큰은 지우지 않는다(다음 진입에서 다시 판정한다).
                        !failure.isRetryable -> {
                            dispatch(
                                ReceiverInvitationReducerEvent.NoticeShown(
                                    UiText.Resource(R.string.receiver_invitation_notice_unavailable),
                                ),
                            )
                        }

                        failure.cause is IOException -> {
                            dispatch(ReceiverInvitationReducerEvent.ErrorPopupShown(ReceiverErrorPopup.NETWORK))
                        }

                        else -> {
                            dispatch(ReceiverInvitationReducerEvent.ErrorPopupShown(ReceiverErrorPopup.SERVER))
                        }
                    }
                }

                else -> {
                    errorReporter.recordFailure(failure, mapOf(KEY_STAGE to stage))
                    dispatch(ReceiverInvitationReducerEvent.ErrorPopupShown(ReceiverErrorPopup.SERVER))
                }
            }
        }

        private suspend fun notice(messageRes: Int) {
            pendingInvitationStore.clear()
            dispatch(ReceiverInvitationReducerEvent.NoticeShown(UiText.Resource(messageRes)))
        }

        private companion object {
            const val KEY_STAGE = "receiver_stage"
            const val STAGE_LOOKUP = "receiver_invitation_lookup"
            const val STAGE_ACCEPT = "receiver_invitation_accept"
        }
    }
