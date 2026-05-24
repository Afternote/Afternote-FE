package com.afternote.feature.afternote.presentation.receiver.deliveryverification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.feature.afternote.domain.repository.receiver.ReceiverAuthRepository
import com.afternote.feature.afternote.domain.repository.receiver.ReceiverRepository
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.receiver.recordsbox.SenderRegistry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 마스터 키 입력 화면(5) ViewModel — 발신자별 authCode 검증 (이슈 #215, #220 후속).
 *
 * `verify(authCode)` 성공 시:
 * 1) [ReceiverRepository.saveAuthCode] 로 글로벌 헤더 컨텍스트에 저장 (이후 서류 업로드·신청 제출 API 가
 *    동일 발신자 컨텍스트로 호출되도록).
 * 2) [SenderRegistry.attachIdentity] 로 카드에 authCode + ReceiverIdentity 결합.
 * 3) [MasterKeyEvent.Verified] 이벤트 발행 → UI 가 다음 단계(서류 업로드) 로 이동.
 *
 * `MasterKeyEvent.Verified` 직후 본 ViewModel 인스턴스는 화면 pop 과 함께 사라지므로, 후속 화면은
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
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(MasterKeyUiState())
        val uiState: StateFlow<MasterKeyUiState> = _uiState.asStateFlow()

        private val _events = Channel<MasterKeyEvent>(Channel.BUFFERED)
        val events: Flow<MasterKeyEvent> = _events.receiveAsFlow()

        fun submit(
            senderId: String,
            authCode: String,
        ) {
            val trimmed = authCode.trim()
            if (trimmed.isEmpty() || _uiState.value.isSubmitting) return

            _uiState.update { it.copy(isSubmitting = true, errorMessageRes = null) }
            viewModelScope.launch {
                receiverAuthRepository
                    .verify(trimmed)
                    .onSuccess { identity ->
                        receiverRepository.saveAuthCode(trimmed)
                        senderRegistry.attachIdentity(senderId, trimmed, identity)
                        _uiState.update { it.copy(isSubmitting = false) }
                        _events.send(MasterKeyEvent.Verified)
                    }.onFailure {
                        _uiState.update {
                            it.copy(
                                isSubmitting = false,
                                errorMessageRes = R.string.receiver_verify_error_unknown,
                            )
                        }
                    }
            }
        }

        fun consumeError() {
            _uiState.update { it.copy(errorMessageRes = null) }
        }
    }
