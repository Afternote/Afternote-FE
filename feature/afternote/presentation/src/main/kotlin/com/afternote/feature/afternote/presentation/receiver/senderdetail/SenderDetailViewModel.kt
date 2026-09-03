package com.afternote.feature.afternote.presentation.receiver.senderdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.afternote.feature.afternote.domain.model.receiver.ReceivedRecordVerification
import com.afternote.feature.afternote.domain.model.receiver.ReceivedRecordViewStatus
import com.afternote.feature.afternote.domain.repository.receiver.ReceiverRepository
import com.afternote.feature.afternote.presentation.receiver.navigation.model.ReceiverRoute
import com.afternote.feature.afternote.presentation.receiver.recordsbox.ReceivedRecordItem
import com.afternote.feature.afternote.presentation.receiver.recordsbox.ReceivedRecordStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 발신자 상세(designs 11·12) ViewModel.
 *
 * `record-boxes` 응답의 열람 상태와 신청·승인 시각으로 상세 상태를 구성한다. 해당 항목의 접근 코드는
 * "기록 열람하기"를 선택할 때만 글로벌 헤더 컨텍스트로 저장한다.
 */
@HiltViewModel
class SenderDetailViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        receivedRecordStore: ReceivedRecordStore,
        private val receiverRepository: ReceiverRepository,
    ) : ViewModel() {
        private val recordBoxId: Long =
            savedStateHandle.toRoute<ReceiverRoute.SenderDetailRoute>().recordBoxId
        private val item: ReceivedRecordItem? = receivedRecordStore.findByRecordBoxId(recordBoxId)

        private val _uiState = MutableStateFlow<SenderDetailUiState>(SenderDetailUiState.Loading)
        val uiState: StateFlow<SenderDetailUiState> = _uiState.asStateFlow()

        init {
            load()
        }

        /**
         * "기록 열람하기"(디자인 12) 트리거 — 글로벌 헤더에 해당 발신자 authCode 를 복원한 뒤
         * [SenderDetailUiState.Success.shouldOpenReceiverHome] 플래그를 true 로 갱신.
         * UI 가 LaunchedEffect 로 수신자 홈 이동 후 [onOpenReceiverHomeConsumed] 로 reset.
         *
         * authCode 가 없는 경우(미인증) 호출되어선 안 되지만 방어적으로 no-op.
         */
        fun openReceiverHome() {
            val authCode = item?.accessCode ?: return
            viewModelScope.launch {
                receiverRepository.saveAuthCode(authCode)
                _uiState.update { current ->
                    if (current is SenderDetailUiState.Success) {
                        current.copy(shouldOpenReceiverHome = true)
                    } else {
                        current
                    }
                }
            }
        }

        fun onOpenReceiverHomeConsumed() {
            _uiState.update { current ->
                if (current is SenderDetailUiState.Success) {
                    current.copy(shouldOpenReceiverHome = false)
                } else {
                    current
                }
            }
        }

        private fun load() {
            val currentItem = item
            if (currentItem == null) {
                _uiState.value = SenderDetailUiState.SenderNotFound
                return
            }
            _uiState.value = currentItem.toRecordBoxSuccessState()
        }
    }

internal fun ReceivedRecordItem.toRecordBoxSuccessState(): SenderDetailUiState.Success {
    val isPending =
        viewStatus == ReceivedRecordViewStatus.Pending ||
            verification is ReceivedRecordVerification.Pending
    val verificationState =
        when {
            viewStatus == ReceivedRecordViewStatus.Viewable -> SenderVerificationState.Approved
            isPending -> SenderVerificationState.Pending
            verification is ReceivedRecordVerification.Rejected -> SenderVerificationState.Rejected
            else -> SenderVerificationState.NotRequested
        }
    val requestedAt =
        when (val current = this.verification) {
            is ReceivedRecordVerification.Pending -> current.requestedAt
            is ReceivedRecordVerification.Rejected -> current.requestedAt
            is ReceivedRecordVerification.Approved -> current.requestedAt
            ReceivedRecordVerification.NotRequested -> null
            ReceivedRecordVerification.Unknown -> null
        }
    val approvedAt =
        (this.verification as? ReceivedRecordVerification.Approved)?.approvedAt
    return SenderDetailUiState.Success(
        displayName = senderName,
        verification = verificationState,
        requestedAt = formatDate(requestedAt),
        approvedAt = formatDate(approvedAt),
    )
}

/**
 * 백엔드의 `createdAt` 은 ISO-8601 형식("2026-05-03T10:00:00Z" 등) 으로 가정. 디자인 표기 "yyyy.MM.dd." 로 변환.
 * 파싱 실패 시 원본 문자열 유지 — 형식이 바뀌어도 화면이 깨지지 않도록.
 */
private fun formatDate(raw: String?): String? {
    if (raw.isNullOrBlank()) return null
    val datePart = raw.substringBefore('T').takeIf { it.length >= 10 } ?: return raw
    val parts = datePart.split('-')
    if (parts.size < 3) return raw
    val (year, month, day) = parts
    return "$year.$month.$day."
}
