package com.afternote.feature.receiver.presentation.deliveryverification

import androidx.annotation.StringRes
import androidx.lifecycle.viewModelScope
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.ui.mvi.MviViewModel
import com.afternote.feature.afternote.presentation.reporting.AfternoteFailureStage
import com.afternote.feature.afternote.presentation.reporting.recordAfternoteFailure
import com.afternote.feature.afternote.presentation.reporting.shouldReportInReceiverFlow
import com.afternote.feature.receiver.domain.repository.IdentityVerificationRepository
import com.afternote.feature.receiver.domain.repository.ReceiverAuthRepository
import com.afternote.feature.receiver.presentation.R
import com.afternote.feature.receiver.presentation.error.toReceiverErrorPopupOrNull
import com.afternote.feature.receiver.presentation.error.toReceiverErrorUiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 수신자 본인 확인 이메일 인증(designs 3·4) ViewModel — 인증번호 발송 + 코드 검증 (이슈 #215, #407).
 *
 * [ReceiverAuthRepository.sendEmailAuthCode]·[ReceiverAuthRepository.verifyEmailAuthCode] 로 실 API 를
 * 호출한다. 실패 문구를 서버 문구로 낼지 정적 리소스로 낼지는 [toReceiverErrorUiText] 가 가른다 (#651).
 *
 * 검증 성공 시 [IdentityVerificationRepository.markVerified] 로 캐시를 켜고 isVerified 신호 발행 →
 * UI 가 마스터 키(5) 단계로 이동. 이메일 인증은 신원 확인까지만 담당하며 마스터 키를 대신 획득하지
 * 않는다 — 그랬다면 마스터 키 단계가 무력화된다 (#454).
 *
 * `senderId` 는 [MasterKeyIntent.Submit] 과 같은 규약으로 자체 SavedStateHandle 이 아니라 parent
 * backStackEntry 의 [DeliveryVerificationFlowViewModel] 에서 받아 [verifyAndProceed] 호출 시점에
 * 전달된다 — 인증 캐시가 발신자별 키에 기록되어 다른 발신자의 관문을 열지 않는다 (#597).
 *
 * 메모리 정책상 ViewModel 은 [androidx.compose.foundation.text.input.TextFieldState] 를 보유하지 않는다.
 * UI 가 입력값을 [IdentityVerificationIntent.UpdateEmail]·[IdentityVerificationIntent.UpdateCode] 로 흘려주고 본 VM 은 String 만 관리.
 *
 * 입력 trim 은 presentation(본 VM) 책임 — 사용자 실수 공백 제거는 입력 UX 보정이지 비즈니스 규칙이
 * 아니라 domain/data 로 내리지 않는다. state 에는 raw 를 두고(타이핑 중간 상태 보존) 검증·전송 시점에 적용.
 */
@HiltViewModel
internal class IdentityVerificationViewModel
    @Inject
    constructor(
        private val receiverAuthRepository: ReceiverAuthRepository,
        private val identityVerificationRepository: IdentityVerificationRepository,
        private val errorReporter: ErrorReporter,
    ) : MviViewModel<IdentityVerificationIntent, IdentityVerificationUiState, IdentityVerificationReducerEvent>(
            IdentityVerificationUiState(),
        ) {
        override fun onIntent(intent: IdentityVerificationIntent) {
            when (intent) {
                is IdentityVerificationIntent.UpdateEmail -> dispatch(IdentityVerificationReducerEvent.EmailChanged(intent.value))
                is IdentityVerificationIntent.UpdateCode -> dispatch(IdentityVerificationReducerEvent.CodeChanged(intent.value))
                IdentityVerificationIntent.RequestCode -> requestVerificationCode()
                is IdentityVerificationIntent.Verify -> verifyAndProceed(intent.senderId)
                IdentityVerificationIntent.RetryFailedRequest -> retryFailedRequest()
                IdentityVerificationIntent.DismissErrorPopup -> onErrorPopupDismissed()
                IdentityVerificationIntent.ConsumeError -> dispatch(IdentityVerificationReducerEvent.ErrorConsumed)
                IdentityVerificationIntent.ConsumeVerified -> dispatch(IdentityVerificationReducerEvent.VerifiedConsumed)
            }
        }

        override fun reduce(
            state: IdentityVerificationUiState,
            event: IdentityVerificationReducerEvent,
        ): IdentityVerificationUiState = reduceIdentityVerification(state, event)

        /**
         * 팝업의 "다시 시도하기" 가 되돌릴 마지막 시도 (#446). 인증번호 발송과 코드 검증 중 **어느
         * 쪽이 실패했는지** 를 담는다 — 하나로 뭉뚱그리면 코드 검증이 서버 오류로 실패했을 때
         * 재시도가 엉뚱하게 인증번호를 다시 보낸다.
         */
        private var pendingRetry: (() -> Unit)? = null

        private fun requestVerificationCode() {
            val state = currentState
            if (!state.isEmailFormatValid || state.isSendingCode) return
            dispatch(IdentityVerificationReducerEvent.CodeSending)
            viewModelScope.launch {
                receiverAuthRepository
                    .sendEmailAuthCode(state.email.trim())
                    .onSuccess {
                        dispatch(IdentityVerificationReducerEvent.CodeSent)
                    }.onFailure { throwable ->
                        if (throwable.shouldReportInReceiverFlow()) {
                            errorReporter.recordAfternoteFailure(
                                AfternoteFailureStage.RECEIVER_EMAIL_CODE_SEND,
                                throwable,
                            )
                        }
                        dispatch(IdentityVerificationReducerEvent.CodeSendFinished)
                        showFailure(throwable, R.string.receiver_verify_code_send_failed, ::requestVerificationCode)
                    }
            }
        }

        private fun verifyAndProceed(senderId: String) {
            val state = currentState
            if (!state.canSubmit) return
            dispatch(IdentityVerificationReducerEvent.VerificationStarted)
            viewModelScope.launch {
                receiverAuthRepository
                    .verifyEmailAuthCode(email = state.email.trim(), authCode = state.code.trim())
                    .onSuccess {
                        identityVerificationRepository.markVerified(senderId)
                        dispatch(IdentityVerificationReducerEvent.Verified)
                    }.onFailure { throwable ->
                        if (throwable.shouldReportInReceiverFlow()) {
                            errorReporter.recordAfternoteFailure(
                                AfternoteFailureStage.RECEIVER_EMAIL_VERIFY,
                                throwable,
                            )
                        }
                        dispatch(IdentityVerificationReducerEvent.VerificationFinished)
                        showFailure(throwable, R.string.receiver_verify_code_verify_failed) {
                            verifyAndProceed(senderId)
                        }
                    }
            }
        }

        /** 팝업의 "다시 시도하기" — 팝업을 닫고 실패한 그 요청을 그대로 다시 보낸다 (#446). */
        private fun retryFailedRequest() {
            val retry = pendingRetry
            dispatch(IdentityVerificationReducerEvent.PopupDismissed)
            pendingRetry = null
            retry?.invoke()
        }

        /** 팝업의 닫기 — 재시도 없이 입력 화면으로 돌아간다. */
        private fun onErrorPopupDismissed() {
            dispatch(IdentityVerificationReducerEvent.PopupDismissed)
            pendingRetry = null
        }

        /**
         * 실패를 팝업(서버 작업 실패)과 스낵바(서버가 준 거절 사유) 중 한쪽으로만 보낸다 — 둘 다
         * 세우면 모달 뒤에서 스낵바가 혼자 떴다 사라진다.
         */
        private fun showFailure(
            throwable: Throwable,
            @StringRes fallbackRes: Int,
            retry: () -> Unit,
        ) {
            val popup = throwable.toReceiverErrorPopupOrNull()
            pendingRetry = if (popup == null) null else retry
            dispatch(
                if (popup == null) {
                    IdentityVerificationReducerEvent.ErrorRaised(throwable.toReceiverErrorUiText(fallbackRes))
                } else {
                    IdentityVerificationReducerEvent.PopupRaised(popup)
                },
            )
        }
    }
