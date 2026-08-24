package com.afternote.feature.afternote.presentation.author.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.domain.repository.UserRepository
import com.afternote.feature.afternote.domain.model.author.Detail
import com.afternote.feature.afternote.domain.repository.author.AfternoteRepository
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.author.navigation.model.AfternoteRoute
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
 * 애프터노트 상세 화면 ViewModel.
 *
 * - 상세 조회: GET /api/afternotes/{id}
 * - 삭제: DELETE /api/afternotes/{id}
 * - 작성자 표시명: [UserRepository.getMyProfile] (네비게이션 인자로 전달하지 않음)
 * - 상세 ID: [SavedStateHandle.toRoute]로 복원한 타입 안전 [AfternoteRoute.DetailRoute]에서 조회.
 *
 * 내부 [InternalState] (flat) 로 조회·작성자·삭제 진행 단계를 관리하고, public [uiState] 는
 * [AfternoteDetailUiState] 로 매핑해 Loading/Success/Error 3분기로 노출한다.
 * 삭제 결과(성공/실패)는 [AfternoteDetailUiState.Success.deleteResult] nullable 필드에 흡수한다 —
 * UI 가 LaunchedEffect 로 소비한 뒤 [onDeleteResultConsumed] 로 reset.
 *
 * 사용자 가시 메시지는 VM 에 하드코딩하지 않고 [androidx.annotation.StringRes] id 로만 노출한다.
 * 실패 시 예외 원문(`Throwable.message`)은 UI 로 넘기지 않는다 — 서버 5xx 본문·역직렬화 예외에
 * 내부 SQL·응답 원문 발췌가 섞여 오므로 사용자에게 노출하면 안 된다.
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
        private val errorReporter: ErrorReporter,
    ) : ViewModel() {
        private val afternoteIdFromNav: Long =
            savedStateHandle.toRoute<AfternoteRoute.DetailRoute>().itemId
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
                    }.onFailure {
                        // 의도된 폴백: 표시명은 장식 정보라 실패해도 화면을 차단하지 않는다.
                        // authorDisplayName 이 빈 문자열로 남으면 TitleSection 이 이름 세그먼트를 생략해 렌더한다.
                    }
            }
            loadDetail(afternoteIdFromNav)
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
                        errorReporter.recordAfternoteFailure(AfternoteFailureStage.DETAIL_LOAD, e)
                        internalState.update {
                            it.copy(
                                loadPhase = LoadPhase.Failed(messageRes = R.string.afternote_detail_load_error),
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
                        errorReporter.recordAfternoteFailure(AfternoteFailureStage.DETAIL_DELETE, e)
                        internalState.update {
                            it.copy(
                                isDeleting = false,
                                deleteResult =
                                    AfternoteDetailDeleteResult.Failed(
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
                    AfternoteDetailUiState.Error(messageRes = phase.messageRes)
                }
            }

        // endregion
    }
