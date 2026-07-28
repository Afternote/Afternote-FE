package com.afternote.feature.mindrecord.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.domain.repository.PhotoUploadRepository
import com.afternote.core.ui.UiText
import com.afternote.feature.mindrecord.domain.model.DailyQuestion
import com.afternote.feature.mindrecord.domain.model.DailyQuestionCreatePayload
import com.afternote.feature.mindrecord.domain.model.DailyQuestionUpdatePayload
import com.afternote.feature.mindrecord.domain.repository.DailyQuestionRepository
import com.afternote.feature.mindrecord.presentation.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class DailyQuestionWriteViewModel
    @Inject
    constructor(
        private val repository: DailyQuestionRepository,
        private val photoUploadRepository: PhotoUploadRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(DailyQuestionWriteUiState())
        val uiState: StateFlow<DailyQuestionWriteUiState> = _uiState.asStateFlow()

        init {
            loadTodayQuestion()
        }

        private fun loadTodayQuestion() {
            viewModelScope.launch {
                _uiState.update { it.copy(isQuestionLoading = true, questionLoadError = null) }
                repository
                    .getToday()
                    .onSuccess { today ->
                        _uiState.update {
                            it.copy(
                                questionId = today.questionId,
                                questionDay = today.day,
                                questionContent = today.content,
                                isQuestionLoading = false,
                            )
                        }
                        if (today.isDraft) resumeDraft()
                    }.onFailure { e ->
                        _uiState.update {
                            it.copy(
                                isQuestionLoading = false,
                                questionLoadError =
                                    UiText.DynamicOrResource(
                                        value = e.message,
                                        fallbackResId = R.string.mindrecord_error_daily_question_today_failed,
                                    ),
                            )
                        }
                    }
            }
        }

        /**
         * 당일 임시저장 레코드를 조회한다.
         *
         * 서버는 `draftOnly` 없이 조회하면 임시저장을 제외한 답변만 내려주므로 반드시 `draftOnly = true` 로 보낸다.
         * 파라미터를 무시하는 서버를 만나도 오답을 잡지 않도록 `isDraft` 재확인은 남겨둔다.
         */
        private suspend fun findTodayDraft(): DailyQuestion? =
            repository
                .getList(date = LocalDate.now().toString(), draftOnly = true)
                .getOrNull()
                ?.firstOrNull { it.isDraft }

        /** today 응답이 draft 존재를 알려주면 그 레코드를 찾아 이어쓰기 상태로 프리필한다. 없으면 신규 작성으로 폴백. */
        private suspend fun resumeDraft() {
            val draft = findTodayDraft() ?: return
            _uiState.update {
                it.copy(
                    draftId = draft.dailyQuestionId,
                    answer = draft.content,
                    imageUrl = draft.imageUrl ?: it.imageUrl,
                )
            }
        }

        /**
         * 임시저장 POST 직후 생성된 레코드의 id 를 확보한다.
         *
         * `create` 가 `Result<Unit>` 이라 생성 응답에서 id 를 받을 수 없어 재조회로 채운다.
         * 이게 없으면 화면을 벗어나지 않고 두 번째로 임시저장할 때 `draftId` 가 여전히 null 이라
         * POST 가 한 번 더 나가 draft 가 중복 생성된다.
         *
         * 사용자가 이어서 편집 중일 수 있으므로 본문·이미지는 건드리지 않고 id 만 채운다.
         */
        private suspend fun adoptCreatedDraftId() {
            val draft = findTodayDraft() ?: return
            _uiState.update { if (it.draftId == null) it.copy(draftId = draft.dailyQuestionId) else it }
        }

        fun onAnswerChanged(text: String) {
            _uiState.update { it.copy(answer = text) }
        }

        /**
         * 에디터에서 고른 이미지를 presigned URL 로 업로드하고 영구 URL 을 반환한다 (실패 시 null).
         * 첫 업로드 이미지는 등록 payload 의 `imageUrl` (목록 카드 썸네일) 로도 쓴다.
         */
        suspend fun uploadImage(uriString: String): String? =
            photoUploadRepository
                .upload(uriString = uriString, directory = MIND_RECORD_UPLOAD_DIRECTORY)
                .onSuccess { url ->
                    _uiState.update { if (it.imageUrl == null) it.copy(imageUrl = url) else it }
                }.getOrNull()

        fun submit(isDraft: Boolean = false) {
            val state = _uiState.value
            val questionId = state.questionId ?: return
            if (!state.canSubmit) return

            val isCreating = state.draftId == null

            viewModelScope.launch {
                _uiState.update { it.copy(submitState = SubmitState.InProgress) }
                val result =
                    if (state.draftId != null) {
                        // 기존 임시저장 레코드가 있으면 새로 만들지 않고 PATCH 로 갱신/전환한다.
                        repository.update(
                            id = state.draftId,
                            payload =
                                DailyQuestionUpdatePayload(
                                    content = state.answer,
                                    isDraft = isDraft,
                                    questionId = questionId,
                                    imageUrl = state.imageUrl,
                                ),
                        )
                    } else {
                        repository.create(
                            DailyQuestionCreatePayload(
                                content = state.answer,
                                isDraft = isDraft,
                                questionId = questionId,
                                imageUrl = state.imageUrl,
                            ),
                        )
                    }
                result
                    .onSuccess {
                        // 임시저장을 새로 만든 경우에만 id 를 확보한다. 같은 화면에서 이어서 저장할 때
                        // 다시 POST 가 나가 draft 가 중복 생성되는 것을 막는다.
                        // Succeeded 로 바꾸기 전에 채워야 연속 저장이 확보 전의 상태를 읽지 않는다.
                        if (isCreating && isDraft) adoptCreatedDraftId()
                        _uiState.update { it.copy(submitState = SubmitState.Succeeded) }
                    }.onFailure { e ->
                        _uiState.update {
                            it.copy(
                                submitState =
                                    SubmitState.Failed(
                                        UiText.DynamicOrResource(
                                            value = e.message,
                                            fallbackResId = R.string.mindrecord_error_daily_question_submit_failed,
                                        ),
                                    ),
                            )
                        }
                    }
            }
        }

        fun consumeSubmitResult() {
            _uiState.update { it.copy(submitState = SubmitState.Idle) }
        }
    }
