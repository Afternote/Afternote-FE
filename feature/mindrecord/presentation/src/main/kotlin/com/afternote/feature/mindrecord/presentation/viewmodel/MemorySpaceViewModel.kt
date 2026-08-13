package com.afternote.feature.mindrecord.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.ui.UiText
import com.afternote.feature.mindrecord.domain.model.DailyQuestion
import com.afternote.feature.mindrecord.domain.model.Diary
import com.afternote.feature.mindrecord.domain.repository.DailyQuestionRepository
import com.afternote.feature.mindrecord.domain.repository.DiaryRepository
import com.afternote.feature.mindrecord.presentation.R
import com.afternote.feature.mindrecord.presentation.mapper.toUi
import com.afternote.feature.mindrecord.presentation.model.memoryspace.MemoryItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * 추억 공간(MEMORY SPACE) 화면.
 *
 * 서버에 "추억" 전용 계약이 없어 사용자의 실제 기록을 모아 카드로 만든다 — 일기와
 * 데일리질문 답변 두 출처를 합쳐 최신순으로 [MEMORY_CARD_LIMIT] 장까지 노출한다.
 * 종전에는 `기억 1~4` / `https://mock.image/N` 더미를 방출했다 (#559).
 *
 * 일기 조회는 `yearMonth` 가 필수라 기간 전체를 한 번에 받을 수 없다. 카드가 몇 장뿐이라
 * 최근 [DIARY_MONTH_WINDOW] 개월만 조회한다 — 그보다 과거까지 훑으면 화면에 쓰지도 않을
 * 요청이 달마다 한 건씩 늘어난다. 데일리질문은 `date` 생략 시 전체 기간이 온다.
 */
@HiltViewModel
class MemorySpaceViewModel
    @Inject
    constructor(
        private val diaryRepository: DiaryRepository,
        private val dailyQuestionRepository: DailyQuestionRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<MemorySpaceUiState>(MemorySpaceUiState.Loading)
        val uiState: StateFlow<MemorySpaceUiState> = _uiState.asStateFlow()

        init {
            load()
        }

        fun retry() = load()

        private fun load() {
            viewModelScope.launch {
                _uiState.value = MemorySpaceUiState.Loading
                runCatching { collectMemories() }
                    .onSuccess { memories -> _uiState.value = MemorySpaceUiState.Success(memories) }
                    .onFailure { e ->
                        _uiState.value =
                            MemorySpaceUiState.Error(
                                UiText.DynamicOrResource(
                                    value = e.message,
                                    fallbackResId = R.string.mindrecord_error_memory_space_failed,
                                ),
                            )
                    }
            }
        }

        /**
         * 두 출처를 병렬 조회해 최신순 카드로 만든다.
         *
         * 한쪽이 실패해도 나머지로 화면을 채운다 — 카드 4장짜리 장식 화면을 부분 실패로
         * 통째 비우면 잃는 것이 더 크다. **전부** 실패했을 때만 에러로 올린다.
         */
        private suspend fun collectMemories(): List<MemoryItem> =
            coroutineScope {
                val diaryDeferred =
                    recentMonths().map { month ->
                        async { diaryRepository.getList(yearMonth = month.toString()) }
                    }
                val questionDeferred = async { dailyQuestionRepository.getList() }

                val diaryResults = diaryDeferred.awaitAll()
                val questionResult = questionDeferred.await()

                if (diaryResults.all { it.isFailure } && questionResult.isFailure) {
                    questionResult.getOrThrow()
                }

                val diaries =
                    diaryResults
                        .mapNotNull { it.getOrNull() }
                        .flatMap { it.diaries }
                        // 쓰다 만 초안은 전시하지 않는다. `GET /diary` 는 `draftOnly` 를 생략하면
                        // 그 달 **전체**(임시저장 포함)를 내려주므로 클라가 걸러야 한다.
                        // 데일리질문(`GET /daily-questions`)은 생략 시 서버가 제출 완료만 주므로
                        // 같은 필터가 필요 없다 — 두 API 의 기본값이 다르다.
                        .filterNot { it.isDraft }
                val questions = questionResult.getOrNull().orEmpty()

                (diaries.mapNotNull { it.toDatedMemory() } + questions.map { it.toDatedMemory() })
                    .sortedByDescending { it.date }
                    .take(MEMORY_CARD_LIMIT)
                    .map { it.item }
            }

        private fun recentMonths(): List<YearMonth> {
            val thisMonth = YearMonth.now()
            return (0 until DIARY_MONTH_WINDOW).map { thisMonth.minusMonths(it.toLong()) }
        }

        /** 두 출처를 한 줄로 정렬하기 위한 운반 타입 — 표시용 날짜 문자열로는 비교가 안 된다. */
        private data class DatedMemory(
            val date: LocalDate,
            val item: MemoryItem,
        )

        /** 날짜를 못 정한 일기는 정렬 키가 없어 카드로 만들지 않는다 ([toUi] 가 null 을 돌린다). */
        private fun Diary.toDatedMemory(): DatedMemory? {
            val ui = toUi() ?: return null
            return DatedMemory(
                date = ui.date,
                item =
                    MemoryItem(
                        id = ui.id,
                        imageUrl = ui.imageUrl,
                        title = ui.title,
                        date = ui.date.format(CARD_DATE_FORMATTER),
                        content = ui.content,
                        tags = listOfNotNull(ui.emotion),
                    ),
            )
        }

        private fun DailyQuestion.toDatedMemory(): DatedMemory {
            val ui = toUi()
            return DatedMemory(
                date = ui.date,
                item =
                    MemoryItem(
                        // 두 출처의 ID 공간이 겹친다 — 카드 선택이 엉뚱한 기록을 열지 않도록
                        // 데일리질문은 음수로 접어 일기와 갈라 둔다 (서버 ID 는 양수).
                        id = -ui.id,
                        imageUrl = ui.imageUrl,
                        title = ui.title,
                        date = ui.date.format(CARD_DATE_FORMATTER),
                        content = ui.content,
                        tags = emptyList(),
                    ),
            )
        }

        companion object {
            /** 카드 배치(`MemorySpaceCardField`)가 4장까지만 자리를 잡는다. */
            private const val MEMORY_CARD_LIMIT = 4
            private const val DIARY_MONTH_WINDOW = 3

            private val CARD_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
        }
    }
