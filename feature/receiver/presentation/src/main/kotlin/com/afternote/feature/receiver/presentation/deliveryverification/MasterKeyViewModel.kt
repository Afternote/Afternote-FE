package com.afternote.feature.receiver.presentation.deliveryverification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.ui.UiText
import com.afternote.feature.afternote.presentation.reporting.AfternoteFailureStage
import com.afternote.feature.afternote.presentation.reporting.recordAfternoteFailure
import com.afternote.feature.afternote.presentation.reporting.shouldReportInReceiverFlow
import com.afternote.feature.receiver.domain.repository.ReceiverAuthRepository
import com.afternote.feature.receiver.domain.repository.ReceiverRepository
import com.afternote.feature.receiver.presentation.R
import com.afternote.feature.receiver.presentation.error.toReceiverErrorUiText
import com.afternote.feature.receiver.presentation.recordsbox.SenderRegistry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 마스터 키 입력 화면(5) ViewModel — 발신자별 masterKey 검증 (이슈 #215, #220 후속).
 *
 * `verifyMasterKey(masterKey)` 성공 시:
 * 1) [ReceiverRepository.saveMasterKey] 로 글로벌 헤더 컨텍스트에 저장 (이후 서류 업로드·신청 제출 API 가
 *    동일 발신자 컨텍스트로 호출되도록).
 * 2) [SenderRegistry.attachIdentity] 로 카드에 masterKey + ReceiverIdentity 결합.
 * 3) [MasterKeyUiState.isVerified] 를 true 로 갱신 → UI 가 다음 단계(서류 업로드) 로 이동 후
 *    [onVerifiedConsumed] 로 소비 처리.
 *
 * 검증 성공 직후 본 ViewModel 인스턴스는 화면 pop 과 함께 사라지므로, 후속 화면은
 * SenderRegistry 의 갱신된 SenderEntry 를 참조해 컨텍스트를 잇는다.
 *
 * `senderId` 는 자체 SavedStateHandle 이 아니라 parent backStackEntry 의
 * [DeliveryVerificationFlowViewModel] 에서 받아 [submit] 호출 시점에 전달된다 — 자식 라우트에서 senderId 를
 * 중복 보유하지 않기 위함(#220).
 *
 * 입력 중인 텍스트는 UI 의 `TextFieldState` 가 보유 — submit() 호출 시점에만 값 전달.
 */
@HiltViewModel
class MasterKeyViewModel
    @Inject
    constructor(
        private val senderRegistry: SenderRegistry,
        private val receiverRepository: ReceiverRepository,
        private val receiverAuthRepository: ReceiverAuthRepository,
        private val errorReporter: ErrorReporter,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(MasterKeyUiState())
        val uiState: StateFlow<MasterKeyUiState> = _uiState.asStateFlow()

        fun submit(
            senderId: String,
            masterKey: String,
        ) {
            // BE @Pattern 은 소문자 UUID 만 받는다 — 대소문자 무의미 값이라 대문자 입력은 거절 대신 정규화한다 (#887).
            val trimmed = masterKey.trim().lowercase()
            if (trimmed.isEmpty() || _uiState.value.isSubmitting) return
            if (!MASTER_KEY_UUID_REGEX.matches(trimmed)) {
                _uiState.update {
                    it.copy(errorMessage = UiText.Resource(R.string.receiver_verify_master_key_invalid_format))
                }
                return
            }

            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            viewModelScope.launch {
                receiverAuthRepository
                    .verifyMasterKey(trimmed)
                    .onSuccess { identity ->
                        receiverRepository.saveMasterKey(trimmed)
                        senderRegistry
                            .attachIdentity(senderId, trimmed, identity)
                            .fold(
                                onSuccess = { sender ->
                                    _uiState.update {
                                        if (sender == null) {
                                            it.copy(
                                                isSubmitting = false,
                                                error = UiText.Resource(R.string.receiver_verify_error_unknown),
                                            )
                                        } else {
                                            it.copy(isSubmitting = false, isVerified = true)
                                        }
                                    }
                                },
                                onFailure = { throwable ->
                                    errorReporter.recordAfternoteFailure(AfternoteFailureStage.MASTER_KEY_VERIFY, throwable)
                                    _uiState.update {
                                        it.copy(
                                            isSubmitting = false,
                                            error = UiText.Resource(R.string.receiver_verify_error_unknown),
                                        )
                                    }
                                },
                            )
                    }.onFailure { throwable ->
                        if (throwable.shouldReportInReceiverFlow()) {
                            errorReporter.recordAfternoteFailure(AfternoteFailureStage.MASTER_KEY_VERIFY, throwable)
                        }
                        _uiState.update {
                            it.copy(
                                isSubmitting = false,
                                errorMessage = throwable.toReceiverErrorUiText(R.string.receiver_verify_error_unknown),
                            )
                        }
                    }
            }
        }

        fun consumeError() {
            _uiState.update { it.copy(errorMessage = null) }
        }

        fun onVerifiedConsumed() {
            _uiState.update { it.copy(isVerified = false) }
        }

        private companion object {
            val MASTER_KEY_UUID_REGEX =
                Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
        }
    }
