package com.afternote.feature.afternote.presentation.author.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.domain.repository.UserRepository
import com.afternote.feature.afternote.domain.model.author.Detail
import com.afternote.feature.afternote.domain.repository.author.AfternoteRepository
import com.afternote.feature.afternote.presentation.R
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
 * - 작성자 표시명: [UserRepository.getMyProfile] (네비게이션 인자로 전달하지 않음)
 * - 상세 ID: [SavedStateHandle]의 `itemId` (타입 안전 상세 라우트의 직렬화 인자명과 동일).
 *
 * 내부 [InternalState] (flat) 로 조회·작성자·삭제 진행 단계를 관리하고, public [uiState] 는
 * [AfternoteDetailUiState] 로 매핑해 Loading/Success/Error 3분기로 노출한다.
 * 삭제 결과(성공/실패)는 영속 상태가 아니라 [events] [Channel] 로 노출한다 — UI는 [com.afternote.core.ui.ObserveAsEvents] 로만 수집.
 *
 * 사용자 가시 메시지는 VM에 하드코딩하지 않고 [androidx.annotation.StringRes] id 로 노출한다 (서버 raw 메시지가 있으면 그쪽을 우선).
 * [com.afternote.feature.afternote.presentation.author.editor.AfternoteEditorViewModel] 의 `error: String?` + `errorRes: Int?` 페어 패턴과 동일.
 *
 * [SharingStarted.WhileSubscribed] 로 UI 구독이 없을 때 업스트림 [map] 을 중지해 백그라운드 리소스를 절약한다.
 */
@HiltViewModel
class AfternoteDetailViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val afternoteRepository: AfternoteRepository,
        private val userRepository: UserRepository,
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
                runCatching { userRepository.getMyProfile() }
                    .onSuccess { profile ->
                        internalState.update { it.copy(authorDisplayName = profile.name) }
                    }
            }
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
                                loadPhase =
                                    LoadPhase.Failed(
                                        rawMessage = e.message,
                                        messageRes = R.string.afternote_detail_load_error,
                                    ),
                            )
                        }
                    }
            }
        }

        fun deleteAfternote(afternoteId: Long) {
            if (internalState.value.isDeleting) return
            viewModelScope.launch {
                internalState.update { it.copy(isDeleting = true) }
                afternoteRepository
                    .delete(id = afternoteId)
                    .onSuccess {
                        internalState.update {
                            it.copy(
                                isDeleting = false,
                                deleteResult = AfternoteDetailDeleteResult.Succeeded(afternoteId),
                            )
                        }
                    }.onFailure { e ->
                        internalState.update {
                            it.copy(
                                isDeleting = false,
                                deleteResult =
                                    AfternoteDetailDeleteResult.Failed(
                                        rawMessage = e.message,
                                        messageRes = R.string.afternote_detail_delete_failed,
                                    ),
                            )
                        }
                    }
            }
        }

        fun onDeleteResultConsumed() {
            internalState.update { it.copy(deleteResult = null) }
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
            val isDeleting: Boolean = false,
            val deleteResult: AfternoteDetailDeleteResult? = null,
        )

        private sealed interface LoadPhase {
            data object Loading : LoadPhase

            data class Loaded(
                val detail: Detail,
            ) : LoadPhase

            data class Failed(
                val rawMessage: String? = null,
                val messageRes: Int? = null,
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
                        isDeleting = isDeleting,
                        contentUiModel = detail.toDetailContentUiModel(authorDisplayName),
                        deleteResult = deleteResult,
                    )
                }

                is LoadPhase.Failed -> {
                    AfternoteDetailUiState.Error(
                        rawMessage = phase.rawMessage,
                        messageRes = phase.messageRes,
                    )
                }
            }

        // endregion
    }
