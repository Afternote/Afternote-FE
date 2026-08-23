package com.afternote.feature.mindrecord.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.afternote.core.domain.repository.PhotoUploadRepository
import com.afternote.core.domain.repository.UserRepository
import com.afternote.core.ui.UiText
import com.afternote.feature.mindrecord.domain.model.DiaryCreatePayload
import com.afternote.feature.mindrecord.domain.model.DiaryUpdatePayload
import com.afternote.feature.mindrecord.domain.model.TodayMood
import com.afternote.feature.mindrecord.domain.repository.DiaryRepository
import com.afternote.feature.mindrecord.presentation.R
import com.afternote.feature.mindrecord.presentation.mapper.toUi
import com.afternote.feature.mindrecord.presentation.navigation.MindRecordRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/**
 * 일기 작성/이어쓰기 ViewModel.
 *
 * - 신규 작성: `POST /diary` (+ 선택한 수신자 ID 를 `receiverIds` 로 함께 전송).
 * - 수정: 라우트의 `recordId` 로 해당 달 목록에서 항목을 찾아 프리필하고, 저장 시
 *   `PATCH /diary/{diaryId}` 로 수정한다. 임시저장 이어쓰기(`isDraft=true`)와 정식 기록
 *   수정(`isDraft=false`)이 같은 경로를 쓰며, 조회하는 목록의 `draftOnly` 만 다르다 (#582).
 */
@HiltViewModel
class DiaryWriteViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val repository: DiaryRepository,
        private val photoUploadRepository: PhotoUploadRepository,
        private val userRepository: UserRepository,
    ) : ViewModel() {
        private val route = savedStateHandle.toRoute<MindRecordRoute.DiaryWriteRoute>()

        /** 수정 대상 일기 ID. 임시저장 이어쓰기와 정식 기록 수정이 같은 값을 쓴다 (#582). */
        private val editingDiaryId: Long? = route.recordId

        private val _uiState = MutableStateFlow(DiaryWriteUiState())
        val uiState: StateFlow<DiaryWriteUiState> = _uiState.asStateFlow()

        init {
            loadReceivers()
            editingDiaryId?.let { loadExisting(it, route.yearMonth, route.isDraft) }
        }

        fun onTitleChanged(value: String) {
            _uiState.update { it.copy(title = value) }
        }

        fun onContentChanged(value: String) {
            _uiState.update { it.copy(content = value) }
        }

        fun onMoodSelected(mood: TodayMood) {
            _uiState.update { it.copy(mood = mood) }
        }

        fun onDateSelected(date: LocalDate) {
            _uiState.update { it.copy(date = date) }
        }

        fun onReceiverToggled(receiverId: Long) {
            _uiState.update {
                val selected = it.selectedReceiverIds
                it.copy(
                    selectedReceiverIds =
                        if (receiverId in selected) selected - receiverId else selected + receiverId,
                )
            }
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
            if (!state.canSubmit) return
            val mood = state.mood ?: return

            viewModelScope.launch {
                _uiState.update { it.copy(submitState = SubmitState.InProgress) }
                val result =
                    if (editingDiaryId != null) {
                        repository.update(
                            id = editingDiaryId,
                            payload =
                                DiaryUpdatePayload(
                                    title = state.title,
                                    content = state.content,
                                    isDraft = isDraft,
                                    todayMood = mood,
                                    date = state.date.toString(),
                                    imageUrl = state.imageUrl,
                                ),
                        )
                    } else {
                        repository.create(
                            DiaryCreatePayload(
                                title = state.title,
                                content = state.content,
                                isDraft = isDraft,
                                todayMood = mood,
                                imageUrl = state.imageUrl,
                                receiverIds = state.selectedReceiverIds.toList().takeIf { it.isNotEmpty() },
                            ),
                        )
                    }
                result
                    .onSuccess {
                        _uiState.update { it.copy(submitState = SubmitState.Succeeded) }
                    }.onFailure { e ->
                        _uiState.update {
                            it.copy(
                                submitState =
                                    SubmitState.Failed(
                                        UiText.DynamicOrResource(
                                            value = e.message,
                                            fallbackResId = R.string.mindrecord_error_diary_submit_failed,
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

        // 실패해도 수신자 행이 "수신자 설정하기" 로 남을 뿐 작성 자체는 가능 — 에러는 조용히 무시.
        private fun loadReceivers() {
            viewModelScope.launch {
                runCatching { userRepository.getReceivers() }
                    .onSuccess { receivers ->
                        _uiState.update { it.copy(receivers = receivers) }
                    }
            }
        }

        // 단건 조회 엔드포인트가 없어 해당 달의 draft 목록에서 id 로 찾는다.

        /**
         * 수정 대상 일기를 프리필한다.
         *
         * 임시저장과 정식 기록은 **같은 목록 API 의 `draftOnly` 만 다르다.** 종전에는 임시저장
         * 경로만 있어서 목록의 "수정하기" 가 갈 곳이 없었다 (#582).
         */
        private fun loadExisting(
            diaryId: Long,
            yearMonth: String?,
            isDraft: Boolean,
        ) {
            viewModelScope.launch {
                _uiState.update { it.copy(isDraftLoading = true, draftLoadError = null) }
                repository
                    .getList(
                        yearMonth = yearMonth ?: YearMonth.now().toString(),
                        draftOnly = if (isDraft) true else null,
                    ).mapCatching { list -> list.diaries.first { it.diaryId == diaryId } }
                    .onSuccess { draft ->
                        _uiState.update {
                            it.copy(
                                title = draft.title,
                                content = draft.content,
                                mood = draft.todayMood,
                                date = draft.toUi().date,
                                imageUrl = draft.imageUrl,
                                isDraftLoading = false,
                                draftLoaded = true,
                            )
                        }
                    }.onFailure { e ->
                        _uiState.update {
                            it.copy(
                                isDraftLoading = false,
                                draftLoadError =
                                    UiText.DynamicOrResource(
                                        value = e.message,
                                        fallbackResId = R.string.mindrecord_error_diary_draft_load_failed,
                                    ),
                            )
                        }
                    }
            }
        }
    }
