package com.afternote.feature.receiver.presentation.senderdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.feature.afternote.presentation.reporting.AfternoteFailureStage
import com.afternote.feature.afternote.presentation.reporting.recordAfternoteFailure
import com.afternote.feature.receiver.domain.model.DeliveryVerification
import com.afternote.feature.receiver.domain.model.DeliveryVerificationStatus
import com.afternote.feature.receiver.domain.model.ReceivedRecordBox
import com.afternote.feature.receiver.domain.repository.ReceiverAuthRepository
import com.afternote.feature.receiver.domain.repository.ReceiverRepository
import com.afternote.feature.receiver.presentation.navigation.model.ReceiverRoute
import com.afternote.feature.receiver.presentation.recordsbox.SenderEntry
import com.afternote.feature.receiver.presentation.recordsbox.SenderRegistry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 발신자 상세(designs 11·12) ViewModel.
 *
 * 진입 시점에 SenderRegistry 에서 카드 식별 + masterKey 를 얻고, masterKey 가 있으면
 * [ReceiverRepository.saveMasterKey] 로 글로벌 헤더 컨텍스트를 교체한 뒤
 * [ReceiverAuthRepository.getDeliveryVerificationStatus] 로 상태를 받아 정보 박스 데이터를 만든다.
 *
 * masterKey 가 없으면(마스터 키 미입력) 무조건 [SenderVerificationState.NotRequested] — API 호출 자체 생략.
 */
@HiltViewModel
class SenderDetailViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val senderRegistry: SenderRegistry,
        private val receiverRepository: ReceiverRepository,
        private val receiverAuthRepository: ReceiverAuthRepository,
        private val errorReporter: ErrorReporter,
    ) : ViewModel() {
        private val senderId: String =
            savedStateHandle.toRoute<ReceiverRoute.SenderDetailRoute>().senderId

        private val _uiState = MutableStateFlow<SenderDetailUiState>(SenderDetailUiState.Loading)
        val uiState: StateFlow<SenderDetailUiState> = _uiState.asStateFlow()

        /** 진행 중인 상태 조회 — 첫 진입 이후의 ON_RESUME 이 실행 중인 로드와 겹치면 건너뛰기 위한 가드. */
        private var loadJob: Job? = null

        /**
         * 다음 [refreshOnReturn] 이 첫 ON_RESUME(진입 자체)인지. 첫 resume 은 init 로드와 같은
         * 진입이므로 갱신하지 않는다 — Job 가드만으로는 init 로드가 빨리 끝난 뒤 도착한 첫
         * resume 이 순차 재조회를 건다. VM 필드인 이유는
         * [com.afternote.feature.afternote.presentation.receiver.detail.ReceivedAfternoteDetailViewModel] 과
         * 동일 — 프로세스 사망 후 복원에서도 init 로드와 수명이 일치한다.
         */
        private var isFirstResume = true

        init {
            load()
        }

        /**
         * 열람 신청 흐름 등 다른 화면에서 복귀했을 때의 자동 갱신 (#701) — 신청 직후 돌아온 화면이
         * 옛 상태(예: "신청 전")를 그대로 보여주지 않게 한다.
         *
         * 최초 진입 로드와 두 가지가 다르다 — 로딩을 방출하지 않고, 상태 조회가 실패해도 보고 있던
         * 정보 박스를 유지한다. 첫 ON_RESUME(진입 자체)은 [isFirstResume] 로 스킵하고, 그 이후의
         * resume 이 실행 중인 로드와 겹치면 진행 중인 Job 으로 건너뛴다.
         */
        fun refreshOnReturn() {
            if (isFirstResume) {
                isFirstResume = false
                return
            }
            if (loadJob?.isActive == true) return
            load(showsLoading = false, keepsStateOnFailure = true)
        }

        /**
         * "기록 열람하기"(디자인 12) 트리거 — 글로벌 헤더에 해당 발신자 masterKey 를 복원한 뒤
         * [SenderDetailUiState.Success.shouldOpenReceiverHome] 플래그를 true 로 갱신.
         * UI 가 LaunchedEffect 로 수신자 홈 이동 후 [onOpenReceiverHomeConsumed] 로 reset.
         *
         * masterKey 가 없는 경우(미인증) 호출되어선 안 되지만 방어적으로 no-op.
         */
        fun openReceiverHome() {
            val masterKey = senderRegistry.findById(senderId)?.masterKey
            if (masterKey.isNullOrBlank()) return
            viewModelScope.launch {
                receiverRepository.saveMasterKey(masterKey)
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

        private fun load(
            showsLoading: Boolean = true,
            keepsStateOnFailure: Boolean = false,
        ) {
            loadJob?.cancel()
            val sender = senderRegistry.findById(senderId)
            if (sender == null) {
                _uiState.value = SenderDetailUiState.SenderNotFound
                return
            }
            if (showsLoading) {
                _uiState.value = SenderDetailUiState.Loading
            }
            loadJob =
                viewModelScope.launch {
                    val resolved = resolveState(sender)
                    _uiState.update { current ->
                        when {
                            // 자동 갱신의 조회 실패: 잘 보고 있던 정보 박스를 에러로 대체하지 않는다.
                            keepsStateOnFailure &&
                                resolved is SenderDetailUiState.StatusLoadFailed &&
                                current is SenderDetailUiState.Success -> {
                                current
                            }

                            // 갱신이 화면을 교체해도 미소비 네비게이션 신호는 잃지 않는다 — "기록 열람하기"
                            // 클릭과 갱신 완료가 겹치면 새 Success 의 기본값 false 가 이동을 삼킨다.
                            resolved is SenderDetailUiState.Success && current is SenderDetailUiState.Success -> {
                                resolved.copy(shouldOpenReceiverHome = current.shouldOpenReceiverHome)
                            }

                            else -> {
                                resolved
                            }
                        }
                    }
                }
        }

        /**
         * 열람 승인일을 받아 온다 (#612).
         *
         * `delivery-verification/status` 응답에는 승인 일시가 없다 — 그 값을 주는 건 `record-boxes` 뿐이라
         * 화면이 오래 «승인 기록이 없습니다» 로 비어 있었다. 서버는 같은 이메일에 등록된 기록함을 모두
         * 내려주므로 지금 보고 있는 발신자의 칸은 [ReceivedRecordBox.masterKey] 로 골라낸다.
         *
         * **이건 서버가 고칠 때까지의 우회다 — BE#96.** 한 줄을 표시하려고 목록 API 를 통째로 왕복하고
         * 있다. `status` 응답이 `approvedAt` 을 내려주기 시작하면 이 함수와 호출부, 골라내는 코드가
         * 함께 사라진다.
         *
         * **승인 상태에서만 부른다.** 서버가 `approvedAt` 을 APPROVED 일 때만 채우므로, 그 밖의 상태에서는
         * 확실히 null 인 값을 받으려고 왕복을 한 번 더 하는 셈이다.
         *
         * 조회가 실패해도 화면을 실패로 떨어뜨리지 않는다 — 승인일은 정보 박스의 한 줄이고, 상태·신청일은
         * 이미 받아 둔 응답으로 그릴 수 있다. 실패는 조용히 삼키지 않고 텔레메트리에 남긴다.
         */
        private suspend fun resolveApprovedAt(
            status: DeliveryVerificationStatus,
            masterKey: String,
        ): String? {
            if (status != DeliveryVerificationStatus.APPROVED) return null
            return receiverAuthRepository
                .getReceivedRecordBoxes()
                .onFailure { e ->
                    errorReporter.recordAfternoteFailure(AfternoteFailureStage.SENDER_STATUS_LOAD, e)
                }.getOrNull()
                ?.firstOrNull { it.masterKey == masterKey }
                ?.approvedAt
        }

        private suspend fun resolveState(sender: SenderEntry): SenderDetailUiState {
            val displayName = sender.name
            val masterKey = sender.masterKey

            if (masterKey.isNullOrBlank()) {
                return SenderDetailUiState.Success(
                    displayName = displayName,
                    verification = SenderVerificationState.NotRequested,
                    requestedAt = null,
                    approvedAt = null,
                )
            }

            receiverRepository.saveMasterKey(masterKey)
            val statusResult = receiverAuthRepository.getDeliveryVerificationStatus()
            return statusResult.fold(
                onSuccess = { verification ->
                    senderRegistry.updateVerificationStatus(sender.id, verification.status)
                    verification.toSuccessState(
                        displayName = displayName,
                        approvedAt = resolveApprovedAt(verification.status, masterKey),
                    )
                },
                onFailure = { e ->
                    errorReporter.recordAfternoteFailure(AfternoteFailureStage.SENDER_STATUS_LOAD, e)
                    SenderDetailUiState.StatusLoadFailed(displayName = displayName)
                },
            )
        }
    }

private fun DeliveryVerification.toSuccessState(
    displayName: String,
    approvedAt: String?,
): SenderDetailUiState.Success =
    SenderDetailUiState.Success(
        displayName = displayName,
        verification = status.toUiState(),
        requestedAt = formatDate(createdAt),
        approvedAt = formatDate(approvedAt),
    )

private fun DeliveryVerificationStatus.toUiState(): SenderVerificationState =
    when (this) {
        DeliveryVerificationStatus.PENDING -> SenderVerificationState.Pending
        DeliveryVerificationStatus.APPROVED -> SenderVerificationState.Approved
        DeliveryVerificationStatus.REJECTED -> SenderVerificationState.Rejected
        DeliveryVerificationStatus.UNKNOWN -> SenderVerificationState.NotRequested
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
