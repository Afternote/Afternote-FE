package com.afternote.feature.afternote.presentation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.common.result.runCatchingCancellable
import com.afternote.core.domain.repository.MyProfileRepository
import com.afternote.core.domain.repository.UserProfileCacheRepository
import com.afternote.feature.afternote.domain.repository.author.AfternoteRepository
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.navigation.model.AfternoteRoute
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
 * - 작성자 표시명: [UserProfileCacheRepository.getCachedUserName] 으로 즉시 채우고
 *   [MyProfileRepository.getMyProfile] 로 재검증한다 (네비게이션 인자로 전달하지 않음)
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
        private val myProfileRepository: MyProfileRepository,
        private val userProfileRepository: UserProfileCacheRepository,
        private val errorReporter: ErrorReporter,
    ) : ViewModel() {
        private val afternoteIdFromNav: Long =
            savedStateHandle.toRoute<AfternoteRoute.DetailRoute>().itemId

        private val _uiState = MutableStateFlow<AfternoteDetailUiState>(AfternoteDetailUiState.Loading)
        val uiState: StateFlow<AfternoteDetailUiState> = _uiState.asStateFlow()

        /**
         * 작성자 표시명 — 상세와 별도로 조회된다. 도착 전에는 빈 문자열이고, 도착하면 보고 있는 Success 와
         * 이후 갱신이 만드는 Success 양쪽에 실린다. 상세보다 먼저 도착하는 경로가 있어 Success 밖에도 필요하다.
         */
        private var authorDisplayName: String = ""

        /**
         * 진행 중인 상세 조회 — 첫 진입 이후의 ON_RESUME 이 실행 중인 로드와 겹치면 건너뛰기 위한 가드.
         * 상세 조회는 한 번에 하나라는 규칙을 이 필드가 지킨다. 새 호출자(수신 상세 같은 retry)를 붙일 때는
         * 이 가드를 통과시키거나, 취소·중복 응답 처리를 그 자리에서 함께 정해야 한다.
         */
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
            viewModelScope.launch { loadAuthorDisplayName() }
            loadDetail(afternoteIdFromNav, LoadTrigger.Entry)
        }

        /**
         * 작성자 표시명을 «캐시 먼저, 원격으로 재검증» 순으로 채운다 (#1497).
         *
         * 원격만 쓰면 왕복이 끝나기 전까지 제목이 «추억 노트에 대한 기록» 으로 그려졌다가
         * «…OO님의 기록» 으로 눈앞에서 다시 쓰인다. 홈이 콜드스타트 placeholder 로 쓰는 캐시가
         * 같은 값이라 그대로 읽는다 (#135 · #136).
         *
         * 캐시·저장 실패는 표시에 영향이 없어 삼킨다 — 다음 진입의 placeholder 가 한 번 더 비는 것뿐이다.
         * 원격까지 실패하고 캐시도 없으면 빈 문자열로 남아 TitleSection 이 이름 세그먼트를 생략해 렌더한다.
         */
        private suspend fun loadAuthorDisplayName() {
            runCatchingCancellable { userProfileRepository.getCachedUserName() }
                .getOrNull()
                ?.takeIf { it.isNotBlank() }
                ?.let(::applyAuthorDisplayName)

            runCatchingCancellable { myProfileRepository.getMyProfile() }
                .onSuccess { profile ->
                    applyAuthorDisplayName(profile.name)
                    runCatchingCancellable { userProfileRepository.saveUserName(profile.name) }
                }
        }

        // region Data Loading

        /**
         * 로드 실패 화면의 «다시 시도» (#1510).
         *
         * [refreshOnReturn] 과 달리 사용자가 직접 누른 동작이라, 진행 중인 로드가 있어도 건너뛰지 않는다 —
         * 누른 결과가 조용히 사라지면 안 되므로 그 로드를 자르고 새로 시작한다.
         */
        fun retry() {
            loadDetail(afternoteIdFromNav, LoadTrigger.UserRequested)
        }

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
            loadDetail(afternoteIdFromNav, LoadTrigger.AutoRefresh)
        }

        /**
         * 상세 조회를 시작한 계기. 로딩을 방출할지, 실패를 화면으로 말할지가 계기마다 다르고 **함께** 정해진다 —
         * 호출부가 두 boolean 을 각각 고르면 «스피너는 띄우고 실패는 삼킨다» 같은 뜻 없는 조합이 표현 가능해진다.
         * 사용자가 직접 누르는 재조회가 생기면 여기에 `UserRequested` 를 더하고 규칙은 [loadDetail] 안에서만 정한다.
         */
        private enum class LoadTrigger {
            /** 화면 진입(init). 보여 줄 것이 없으니 로딩을 띄우고, 실패는 에러 화면으로 말한다. */
            Entry,

            /** 백스택 복귀(ON_RESUME). 화면이 살아 있으니 조용히 갱신하고, 실패해도 보던 상세를 지킨다. */
            AutoRefresh,

            /** 사용자가 누른 «다시 시도». 기다림을 인지해야 하니 로딩을 띄우고, 실패는 반드시 화면으로 말한다. */
            UserRequested,
        }

        private fun loadDetail(
            afternoteId: Long,
            trigger: LoadTrigger,
        ) {
            val showsLoading =
                when (trigger) {
                    LoadTrigger.Entry, LoadTrigger.UserRequested -> true
                    LoadTrigger.AutoRefresh -> false
                }
            val keepsStateOnFailure = trigger == LoadTrigger.AutoRefresh
            // 재시도는 진행 중인 갱신을 자르고 들어온다 — 자르지 않으면 두 로드가 같은 화면을 두고 경합한다.
            loadJob?.cancel()
            loadJob =
                viewModelScope.launch {
                    if (showsLoading) {
                        _uiState.value = AfternoteDetailUiState.Loading
                    }
                    val result = afternoteRepository.getDetail(id = afternoteId)
                    // 잘린 로드가 값을 들고 돌아와 새 화면을 덮는 창을 닫는다. repository 는 취소를 다시
                    // 던지므로 조회 «도중» 취소는 여기까지 오지 않지만, 값이 나온 뒤 잘린 경우가 남는다.
                    ensureActive()
                    result
                        .onSuccess { detail ->
                            // 매핑은 update 밖에서 한 번만 한다 — update 의 람다는 경합 시 재실행된다.
                            val contentUiModel = detail.toDetailContentUiModel()
                            _uiState.update { current ->
                                // 갱신은 «상세 부분만» 바꾼다. 진행 중인 삭제(isDeleting)와 미소비 삭제
                                // 결과(deleteResult)는 이 로드와 무관한 다른 작업의 상태라, 새 Success 로
                                // 덮으면 그 기본값이 삭제 진행 표시를 풀고 결과 안내를 지운다.
                                if (current is AfternoteDetailUiState.Success) {
                                    current.copy(
                                        detailId = detail.id,
                                        contentUiModel = contentUiModel,
                                        authorDisplayName = authorDisplayName,
                                    )
                                } else {
                                    AfternoteDetailUiState.Success(
                                        detailId = detail.id,
                                        contentUiModel = contentUiModel,
                                        authorDisplayName = authorDisplayName,
                                    )
                                }
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

        /**
         * 삭제는 상세를 보고 있을 때만 뜻이 있다 — 그 전제를 상태에서 직접 확인한다.
         *
         * 상태 갱신은 [updateSuccess] 가 Success 밖에서 no-op 이라 이미 안전하지만, **서버 호출은 아니다.**
         * Success 가 아닐 때 이 함수가 불리면 노트는 지워지는데 UI 는 아무것도 모른다(진행 표시도,
         * 결과 안내도, 화면 pop 도 없다). 중복 호출 가드도 non-Success 에서는 늘 통과한다.
         * 지금은 결선상 Success 에서만 호출되지만, 그 불변식은 VM 밖(내비게이션 분기)에 있다.
         *
         * 지울 id 도 인자로 받지 않고 그 Success 에서 꺼낸다 — 화면에 보이는 것과 다른 항목을 지우는
         * 경우를 시그니처에서 없앤다. 호출부가 넘기던 값도 같은 Success 의 `detailId` 였다.
         */
        fun deleteAfternote() {
            val current = _uiState.value as? AfternoteDetailUiState.Success ?: return
            if (current.isDeleting) return
            val afternoteId = current.detailId
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

        /** 늦게 도착한 작성자 표시명을 반영한다. 아직 Loading 이면 필드에만 남아 다음 Success 에 실린다. */
        private fun applyAuthorDisplayName(name: String) {
            authorDisplayName = name
            updateSuccess { it.copy(authorDisplayName = name) }
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
