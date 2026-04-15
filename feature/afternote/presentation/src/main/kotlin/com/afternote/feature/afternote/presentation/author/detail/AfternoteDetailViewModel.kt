package com.afternote.feature.afternote.presentation.author.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.domain.repository.HomeRepository
import com.afternote.feature.afternote.domain.model.author.Detail
import com.afternote.feature.afternote.domain.repository.AfternoteRepository
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
 * 애프터노트 상세 화면 ViewModel.
 *
 * - 상세 조회: GET /api/afternotes/{id}
 * - 삭제: DELETE /api/afternotes/{id}
 * - 작성자 표시명: [HomeRepository.getHomeSummary] (네비게이션 인자로 전달하지 않음)
 * - 상세 ID: [SavedStateHandle]의 `itemId` (타입 안전 상세 라우트의 직렬화 인자명과 동일).
 *   초기 로드는 [init]에서 한 번만 수행한다 (Compose `LaunchedEffect`로의 위임은 하지 않는다).
 *
 * 내부 [InternalState] (flat) 로 조회·작성자·삭제 단계를 각각 관리하고, public [uiState] 는
 * [AfternoteDetailUiState] 로 매핑해 Loading/Success/Error 3분기로 노출한다.
 * [SharingStarted.WhileSubscribed] 로 UI 구독이 없을 때 업스트림 [map] 을 중지해 백그라운드 리소스를 절약한다.
 * UI 액션은 [deleteAfternote]·[consumeDeleteResult] 등 명시 메서드로만 노출한다.
 */
@HiltViewModel
class AfternoteDetailViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val afternoteRepository: AfternoteRepository,
        private val homeRepository: HomeRepository,
    ) : ViewModel() {
        private val afternoteIdFromNav: Long? =
            savedStateHandle.get<String>(NAV_ARG_ITEM_ID)?.toLongOrNull()
        private val internalState = MutableStateFlow(InternalState())

        val uiState: StateFlow<AfternoteDetailUiState> =
            internalState
                .map { it.toUiState() }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = AfternoteDetailUiState.Loading,
                )

        init {
            viewModelScope.launch {
                homeRepository
                    .getHomeSummary()
                    .onSuccess { summary ->
                        internalState.update { it.copy(authorDisplayName = summary.userName) }
                    }
            }
            val id = afternoteIdFromNav
            if (id != null) {
                loadDetail(id)
            } else {
                internalState.update {
                    it.copy(
                        loadPhase =
                            LoadPhase.Failed(
                                message = "애프터노트 식별자가 올바르지 않습니다.",
                            ),
                    )
                }
            }
        }

        private companion object {
            private const val NAV_ARG_ITEM_ID = "itemId"
        }

        // region Data Loading

        private fun loadDetail(afternoteId: Long) {
            viewModelScope.launch {
                internalState.update { it.copy(loadPhase = LoadPhase.Loading) }
                afternoteRepository
                    .getDetail(id = afternoteId)
                    .onSuccess { detail ->
                        internalState.update { it.copy(loadPhase = LoadPhase.Loaded(detail)) }
                    }.onFailure { e ->
                        internalState.update {
                            it.copy(
                                loadPhase = LoadPhase.Failed(e.message ?: "상세 정보를 불러오지 못했습니다."),
                            )
                        }
                    }
            }
        }

        fun deleteAfternote(afternoteId: Long) {
            viewModelScope.launch {
                internalState.update { it.copy(deleteState = AfternoteDeleteState.InProgress) }
                afternoteRepository
                    .delete(id = afternoteId)
                    .onSuccess {
                        internalState.update { it.copy(deleteState = AfternoteDeleteState.Succeeded) }
                    }.onFailure { e ->
                        internalState.update {
                            it.copy(
                                deleteState = AfternoteDeleteState.Failed(e.message ?: "삭제에 실패했습니다."),
                            )
                        }
                    }
            }
        }

        // endregion

        // region Utility

        fun consumeDeleteResult() {
            internalState.update { it.copy(deleteState = AfternoteDeleteState.Idle) }
        }

        // endregion

        // region Internal state shaping

        /**
         * VM 내부에서만 다루는 평탄한 상태.
         * public [AfternoteDetailUiState] 는 이 값을 [toUiState] 로 매핑해 노출한다.
         */
        private data class InternalState(
            val loadPhase: LoadPhase = LoadPhase.Loading,
            val authorDisplayName: String = "",
            val deleteState: AfternoteDeleteState = AfternoteDeleteState.Idle,
        )

        private sealed interface LoadPhase {
            data object Loading : LoadPhase

            data class Loaded(
                val detail: Detail,
            ) : LoadPhase

            data class Failed(
                val message: String,
            ) : LoadPhase
        }

        private fun InternalState.toUiState(): AfternoteDetailUiState =
            when (val phase = loadPhase) {
                LoadPhase.Loading -> {
                    AfternoteDetailUiState.Loading
                }

                is LoadPhase.Loaded -> {
                    val detail = phase.detail
                    AfternoteDetailUiState.Success(
                        detailId = detail.id,
                        authorDisplayName = authorDisplayName,
                        deleteState = deleteState,
                        contentUiModel = detail.toDetailContentUiModel(authorDisplayName),
                    )
                }

                is LoadPhase.Failed -> {
                    AfternoteDetailUiState.Error(phase.message)
                }
            }

        // endregion
    }
