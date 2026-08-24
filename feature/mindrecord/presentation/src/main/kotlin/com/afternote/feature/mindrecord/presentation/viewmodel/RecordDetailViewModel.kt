package com.afternote.feature.mindrecord.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.afternote.core.ui.UiText
import com.afternote.feature.mindrecord.domain.repository.DailyQuestionRepository
import com.afternote.feature.mindrecord.domain.repository.DiaryRepository
import com.afternote.feature.mindrecord.presentation.R
import com.afternote.feature.mindrecord.presentation.mapper.toEmoji
import com.afternote.feature.mindrecord.presentation.mapper.toUi
import com.afternote.feature.mindrecord.presentation.navigation.MindRecordRoute
import com.afternote.feature.mindrecord.presentation.util.RecordContentBlock
import com.afternote.feature.mindrecord.presentation.util.toRecordContentBlocks
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

/**
 * 기록 상세 화면 ViewModel (#759).
 *
 * 목록이 이미 본문을 갖고 있지만, 화면을 옮기면 그 상태가 따라오지 않으므로 같은 목록
 * API 로 다시 불러 대상 하나를 고른다 — 작성 화면의 프리필과 같은 방식이다.
 */
@HiltViewModel
class RecordDetailViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val diaryRepository: DiaryRepository,
        private val dailyQuestionRepository: DailyQuestionRepository,
    ) : ViewModel() {
        private val route = savedStateHandle.toRoute<MindRecordRoute.RecordDetailRoute>()

        private val _uiState = MutableStateFlow<RecordDetailUiState>(RecordDetailUiState.Loading)
        val uiState: StateFlow<RecordDetailUiState> = _uiState.asStateFlow()

        init {
            if (route.isDiary) loadDiary() else loadDailyQuestion()
        }

        private fun loadDiary() {
            viewModelScope.launch {
                diaryRepository
                    .getList(yearMonth = route.yearMonth ?: YearMonth.now().toString(), draftOnly = null)
                    .mapCatching { list -> list.diaries.first { it.diaryId == route.recordId } }
                    .onSuccess { diary ->
                        val ui = diary.toUi()
                        val blocks = diary.content.toRecordContentBlocks()
                        _uiState.value =
                            RecordDetailUiState.Success(
                                title = diary.title,
                                date = ui?.date ?: YearMonth.now().atDay(1),
                                receiverNames = diary.receiverNames,
                                heroImageUrl = blocks.firstImageUrl(),
                                blocks = blocks,
                                mood = diary.todayMood?.toEmoji(),
                            )
                    }.onFailure { failWithLoadError() }
            }
        }

        private fun loadDailyQuestion() {
            viewModelScope.launch {
                dailyQuestionRepository
                    .getList()
                    .mapCatching { list -> list.first { it.dailyQuestionId == route.recordId } }
                    .onSuccess { answer ->
                        val ui = answer.toUi()
                        val blocks = answer.content.toRecordContentBlocks()
                        _uiState.value =
                            RecordDetailUiState.Success(
                                title = answer.title,
                                date = ui.date,
                                receiverNames = answer.receiverNames,
                                heroImageUrl = blocks.firstImageUrl(),
                                blocks = blocks,
                                // 데일리질문에는 오늘의 기분이 없다 — 시안에도 그 줄이 없다.
                                mood = null,
                            )
                    }.onFailure { failWithLoadError() }
            }
        }

        private fun failWithLoadError() {
            _uiState.value =
                RecordDetailUiState.Error(UiText.Resource(R.string.mindrecord_detail_load_failed))
        }
    }

/** 헤더 배경에 깔 이미지 — 본문 첫 이미지를 쓴다. 없으면 시안의 "이미지 X" 변형이다 (#759). */
internal fun List<RecordContentBlock>.firstImageUrl(): String? = filterIsInstance<RecordContentBlock.Image>().firstOrNull()?.url
