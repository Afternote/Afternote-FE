package com.afternote.feature.afternote.presentation.receiver.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.feature.afternote.domain.repository.receiver.ReceiverRepository
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.reporting.AfternoteFailureStage
import com.afternote.feature.afternote.presentation.reporting.recordAfternoteFailure
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 수신 애프터노트 상세 ViewModel.
 *
 * - 상세 조회: [ReceiverRepository.getReceivedAfternoteDetail] (Retrofit baseUrl 기준
 *   `receiver-auth/after-notes/{afternoteId}` 경로 — 실제 경로는 data 모듈의 `ReceiverAfternoteApiService`).
 * - 상세 ID: [SavedStateHandle] 의 `afternoteId` (수신자 라우트 인자명).
 *
 * 발신자 [com.afternote.feature.afternote.presentation.author.detail.AfternoteDetailViewModel] 과
 * 동일한 단일 [ReceivedAfternoteDetailUiState] + StateFlow 패턴을 따른다. 다만 받은 입장이라
 * 수정·삭제·작성자 표시명·수신자 목록은 보유하지 않는다.
 */
@HiltViewModel
class ReceivedAfternoteDetailViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val receiverRepository: ReceiverRepository,
        private val errorReporter: ErrorReporter,
    ) : ViewModel() {
        private val afternoteIdFromNav: Long? =
            savedStateHandle.get<String>(NAV_ARG_AFTERNOTE_ID)?.toLongOrNull()

        private val internalState = MutableStateFlow(InternalState())

        val uiState: StateFlow<ReceivedAfternoteDetailUiState> =
            internalState
                .map { it.toUiState() }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = ReceivedAfternoteDetailUiState.Loading,
                )

        init {
            val id = afternoteIdFromNav
            if (id != null) {
                loadDetail(id)
            } else {
                internalState.update {
                    it.copy(
                        loadPhase = LoadPhase.Failed(messageRes = R.string.afternote_detail_invalid_id),
                    )
                }
            }
        }

        private fun loadDetail(afternoteId: Long) {
            viewModelScope.launch {
                internalState.update { it.copy(loadPhase = LoadPhase.Loading, detailId = afternoteId) }
                receiverRepository
                    .getReceivedAfternoteDetail(afternoteId = afternoteId)
                    .onSuccess { detail ->
                        internalState.update {
                            it.copy(loadPhase = LoadPhase.Loaded(detail.toReceivedDetailContentUiModel()))
                        }
                    }.onFailure { e ->
                        errorReporter.recordAfternoteFailure(AfternoteFailureStage.RECEIVED_DETAIL_LOAD, e)
                        internalState.update {
                            it.copy(
                                loadPhase =
                                    LoadPhase.Failed(
                                        messageRes = R.string.afternote_detail_load_error,
                                    ),
                            )
                        }
                    }
            }
        }

        private companion object {
            private const val NAV_ARG_AFTERNOTE_ID = "afternoteId"
        }

        private data class InternalState(
            val loadPhase: LoadPhase = LoadPhase.Loading,
            val detailId: Long = 0L,
        )

        private sealed interface LoadPhase {
            data object Loading : LoadPhase

            data class Loaded(
                val contentUiModel: ReceivedDetailContentUiModel,
            ) : LoadPhase

            data class Failed(
                val messageRes: Int? = null,
            ) : LoadPhase
        }

        private fun InternalState.toUiState(): ReceivedAfternoteDetailUiState =
            when (val phase = loadPhase) {
                LoadPhase.Loading -> {
                    ReceivedAfternoteDetailUiState.Loading
                }

                is LoadPhase.Loaded -> {
                    ReceivedAfternoteDetailUiState.Success(
                        detailId = detailId,
                        contentUiModel = phase.contentUiModel,
                    )
                }

                is LoadPhase.Failed -> {
                    ReceivedAfternoteDetailUiState.Error(
                        messageRes = phase.messageRes,
                    )
                }
            }
    }
