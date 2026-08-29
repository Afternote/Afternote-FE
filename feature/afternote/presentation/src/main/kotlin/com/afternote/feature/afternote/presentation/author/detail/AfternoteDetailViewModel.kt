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
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
 * [AfternoteDetailUiState] 를 그대로 들고 [uiState] 로 노출한다 — Loading/Success/Error 3분기.
 * 삭제 결과(성공/실패)는 [AfternoteDetailUiState.Success.deleteResult] nullable 필드에 흡수한다 —
 * UI 가 LaunchedEffect 로 소비한 뒤 [onDeleteResultConsumed] 로 reset.
 *
 * 사용자 가시 메시지는 VM 에 하드코딩하지 않고 [androidx.annotation.StringRes] id 로만 노출한다.
 * 실패 시 예외 원문(`Throwable.message`)은 UI 로 넘기지 않는다 — 서버 5xx 본문·역직렬화 예외에
 * 내부 SQL·응답 원문 발췌가 섞여 오므로 사용자에게 노출하면 안 된다.
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

        private val _uiState = MutableStateFlow<AfternoteDetailUiState>(AfternoteDetailUiState.Loading)
        val uiState: StateFlow<AfternoteDetailUiState> = _uiState.asStateFlow()

        /**
         * 조회해 둔 상세 원본. 작성자 표시명이 상세보다 늦게 도착하면 Success 를 다시 매핑해야 하는데,
         * [AfternoteDetailUiState.Success] 는 변환이 끝난 [DetailContentUiModel] 만 들고 있어 원본이 필요하다.
         */
        private var loadedDetail: Detail? = null

        /** 작성자 표시명 — 상세와 별도로 조회되므로 빈 문자열로 시작해, 도착하면 Success 에 반영한다. */
        private var authorDisplayName: String = ""

        /** 진행 중인 상세 조회 — 첫 진입 이후의 ON_RESUME 이 실행 중인 로드와 겹치면 건너뛰기 위한 가드. */
        private var loadJob: Job? = null

        /**
         * 다음 [refreshOnReturn] 이 첫 ON_RESUME(진입 자체)인지. 첫 resume 은 init 로드와 같은
         * 진입이므로 갱신하지 않는다 — Job 가드만으로는 init 로드가 (특히 실패로) 빨리 끝난 뒤
         * 도착한 첫 resume 이 순차 재조회를 걸어, 에러 화면과 수동 재시도가 통째로 건너뛰어진다.
         * 컴포지션 플래그(rememberSaveable)가 아니라 VM 필드인 이유: 프로세스 사망 후 복원에서
         * SavedState 는 살아 돌아오는데 VM 은 새로 만들어져 수명이 어긋난다 — VM 필드는 init 로드와
         * 같은 수명이라 복원 직후의 첫 resume 도 정확히 스킵된다.
         */
        private var isFirstResume = true

        init {
            viewModelScope.launch {
                runCatching { userRepository.getMyProfile() }
                    .onSuccess { profile ->
                        applyAuthorDisplayName(profile.name)
                    }.onFailure {
                        // 의도된 폴백: 표시명은 장식 정보라 실패해도 화면을 차단하지 않는다.
                        // authorDisplayName 이 빈 문자열로 남으면 TitleSection 이 이름 세그먼트를 생략해 렌더한다.
                    }
            }
            loadDetail(afternoteIdFromNav)
        }

        // region Data Loading

        /**
         * 수정 화면 등 다른 화면에서 상세로 복귀했을 때의 자동 갱신 (#701).
         *
         * 최초 진입 로드와 두 가지가 다르다.
         * - 로딩을 방출하지 않는다. 화면이 살아 있는 채로 발화하므로 스피너가 재진입마다 번쩍인다.
         * - 실패해도 보고 있던 상세를 유지한다. 일시적 실패로 잘 보던 화면이 에러로 대체되면
         *   사용자 입장에서는 인과가 설명되지 않는다.
         *
         * 첫 ON_RESUME(진입 자체)은 [isFirstResume] 로 스킵하고, 그 이후의 resume 이 실행 중인
         * 로드와 겹치면(빠른 resume 연타 등) 진행 중인 Job 으로 건너뛴다.
         */
        fun refreshOnReturn() {
            if (isFirstResume) {
                isFirstResume = false
                return
            }
            if (loadJob?.isActive == true) return
            loadDetail(afternoteIdFromNav, showsLoading = false, keepsStateOnFailure = true)
        }

        private fun loadDetail(
            afternoteId: Long,
            showsLoading: Boolean = true,
            keepsStateOnFailure: Boolean = false,
        ) {
            // 지금은 취소할 로드가 없다 — 호출자가 init 과 [refreshOnReturn] 뿐이고 후자는 Job 가드를
            // 통과한 뒤에만 여기 닿는다. 수신 상세처럼 가드를 거치지 않는 retry 가 붙는 순간
            // 실효가 생기므로, 그때 조용히 깨지지 않게 남겨 둔다.
            loadJob?.cancel()
            loadJob =
                viewModelScope.launch {
                    if (showsLoading) {
                        _uiState.value = AfternoteDetailUiState.Loading
                    }
                    val result = afternoteRepository.getDetail(id = afternoteId)
                    // 위 cancel 과 같은 이유로 남기는 가드 — 취소된 로드가 값을 들고 돌아와 새 화면을
                    // 덮는 창을 닫는다 (수신 상세에서는 retry 로 실제 재현된다).
                    ensureActive()
                    result
                        .onSuccess { detail ->
                            loadedDetail = detail
                            _uiState.update { current ->
                                // 진행 중인 삭제와 미소비 삭제 결과는 갱신이 덮지 않는다 — 새 Success 의
                                // 기본값이 삭제 진행 표시를 풀고 결과 안내를 지운다.
                                val previous = current as? AfternoteDetailUiState.Success
                                AfternoteDetailUiState.Success(
                                    detailId = detail.id,
                                    isDeleting = previous?.isDeleting ?: false,
                                    contentUiModel = detail.toDetailContentUiModel(authorDisplayName),
                                    deleteResult = previous?.deleteResult,
                                )
                            }
                        }.onFailure { e ->
                            // 화면을 유지하는 자동 갱신 실패도 기록한다 — 사용자에게 안 보이는 만큼
                            // 콘솔이 유일한 관측 지점이다.
                            errorReporter.recordAfternoteFailure(AfternoteFailureStage.DETAIL_LOAD, e)
                            _uiState.update { current ->
                                if (keepsStateOnFailure && current is AfternoteDetailUiState.Success) {
                                    current
                                } else {
                                    AfternoteDetailUiState.Error(
                                        messageRes = R.string.afternote_detail_load_error,
                                    )
                                }
                            }
                        }
                }
        }

        fun deleteAfternote(afternoteId: Long) {
            if ((_uiState.value as? AfternoteDetailUiState.Success)?.isDeleting == true) return
            viewModelScope.launch {
                updateSuccess { it.copy(isDeleting = true) }
                afternoteRepository
                    .delete(id = afternoteId)
                    .onSuccess {
                        updateSuccess {
                            it.copy(
                                isDeleting = false,
                                deleteResult = AfternoteDetailDeleteResult.Succeeded(afternoteId),
                            )
                        }
                    }.onFailure { e ->
                        errorReporter.recordAfternoteFailure(AfternoteFailureStage.DETAIL_DELETE, e)
                        updateSuccess {
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
            updateSuccess { it.copy(deleteResult = null) }
        }

        // endregion

        // region State shaping

        /**
         * 늦게 도착한 작성자 표시명을 반영한다. 상세를 이미 받아 둔 뒤면 보고 있는 Success 의
         * [DetailContentUiModel] 을 새 이름으로 다시 매핑한다.
         */
        private fun applyAuthorDisplayName(name: String) {
            authorDisplayName = name
            val detail = loadedDetail ?: return
            updateSuccess { it.copy(contentUiModel = detail.toDetailContentUiModel(name)) }
        }

        /** 삭제 진행·결과는 Success 에만 존재하므로, 그 외 상태에서는 갱신하지 않는다. */
        private fun updateSuccess(transform: (AfternoteDetailUiState.Success) -> AfternoteDetailUiState) {
            _uiState.update { current ->
                if (current is AfternoteDetailUiState.Success) {
                    transform(current)
                } else {
                    current
                }
            }
        }

        // endregion
    }
