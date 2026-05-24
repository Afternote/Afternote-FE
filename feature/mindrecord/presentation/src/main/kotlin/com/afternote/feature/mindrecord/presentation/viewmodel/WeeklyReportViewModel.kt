package com.afternote.feature.mindrecord.presentation.viewmodel

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.domain.repository.UserRepository
import com.afternote.core.ui.UiText
import com.afternote.feature.mindrecord.domain.model.TodayMood
import com.afternote.feature.mindrecord.domain.model.WeeklyReport
import com.afternote.feature.mindrecord.domain.model.WeeklyReportDailyQuestion
import com.afternote.feature.mindrecord.domain.model.WeeklyReportDay
import com.afternote.feature.mindrecord.domain.model.WeeklyReportEmotion
import com.afternote.feature.mindrecord.domain.repository.WeeklyReportRepository
import com.afternote.feature.mindrecord.presentation.R
import com.afternote.feature.mindrecord.presentation.component.EmotionBubble
import com.afternote.feature.mindrecord.presentation.model.DailyQuestion
import com.afternote.feature.mindrecord.presentation.model.DayBackground
import com.afternote.feature.mindrecord.presentation.model.DayContent
import com.afternote.feature.mindrecord.presentation.model.DayItem
import com.afternote.feature.mindrecord.presentation.model.MindRecordCategoryUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class WeeklyReportViewModel
    @Inject
    constructor(
        private val repository: WeeklyReportRepository,
        private val userRepository: UserRepository,
    ) : ViewModel() {
        private val weekOptions: List<WeekOption> =
            buildWeekOptions(today = LocalDate.now(), count = WEEK_OPTION_COUNT)

        private val internalState = MutableStateFlow(InternalState())

        val uiState: StateFlow<WeeklyReportUiState> =
            internalState
                .map { it.toUiState() }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = WeeklyReportUiState.Loading,
                )

        init {
            load(weekOptions.first().monday)
        }

        fun selectWeek(monday: LocalDate) = load(monday)

        fun refresh() {
            val current =
                (internalState.value.loadPhase as? LoadPhase.Loaded)?.monday
                    ?: weekOptions.first().monday
            load(current)
        }

        private fun load(monday: LocalDate) {
            viewModelScope.launch {
                internalState.update { it.copy(loadPhase = LoadPhase.Loading) }
                val result =
                    runCatching {
                        coroutineScope {
                            val reportDeferred =
                                async {
                                    repository
                                        .getWeeklyReport(date = monday.format(API_DATE_FORMATTER))
                                        .getOrThrow()
                                }
                            val profileDeferred = async { userRepository.getMyProfile() }
                            reportDeferred.await() to profileDeferred.await()
                        }
                    }
                result
                    .onSuccess { (report, profile) ->
                        internalState.update {
                            it.copy(loadPhase = LoadPhase.Loaded(monday, report, profile.name))
                        }
                    }.onFailure { e ->
                        internalState.update {
                            it.copy(
                                loadPhase =
                                    LoadPhase.Failed(
                                        UiText.DynamicOrResource(
                                            value = e.message,
                                            fallbackResId = R.string.mindrecord_error_weekly_report_failed,
                                        ),
                                    ),
                            )
                        }
                    }
            }
        }

        private fun buildWeekOptions(
            today: LocalDate,
            count: Int,
        ): List<WeekOption> {
            val thisMonday = today.with(DayOfWeek.MONDAY)
            return (0 until count).map { weeksAgo ->
                WeekOption(monday = thisMonday.minusWeeks(weeksAgo.toLong()))
            }
        }

        private fun mapWeekDays(
            monday: LocalDate,
            week: List<WeeklyReportDay>,
        ): List<DayItem> =
            List(WEEK_LENGTH) { index ->
                val date = monday.plusDays(index.toLong())
                val apiDay = week.getOrNull(index)
                val dayOfMonth = apiDay?.day ?: date.dayOfMonth
                val isDiary = apiDay?.isDiary == true
                val emoji = apiDay?.emotion?.toEmoji()
                DayItem(
                    dayOfWeek = date.dayOfWeek,
                    content =
                        when {
                            emoji != null && isDiary -> DayContent.EmojiWithDot(emoji)
                            emoji != null -> DayContent.EmojiOnly(emoji)
                            isDiary -> DayContent.NumberWithDot(dayOfMonth)
                            else -> DayContent.NumberOnly(dayOfMonth)
                        },
                    background =
                        when (apiDay?.emotion) {
                            TodayMood.HAPPY -> DayBackground.Green
                            TodayMood.SAD -> DayBackground.Pink
                            else -> DayBackground.None
                        },
                )
            }

        private data class InternalState(
            val loadPhase: LoadPhase = LoadPhase.Loading,
        )

        private sealed interface LoadPhase {
            data object Loading : LoadPhase

            data class Loaded(
                val monday: LocalDate,
                val report: WeeklyReport,
                val userName: String,
            ) : LoadPhase

            data class Failed(
                val message: UiText,
            ) : LoadPhase
        }

        private fun InternalState.toUiState(): WeeklyReportUiState =
            when (val phase = loadPhase) {
                LoadPhase.Loading -> WeeklyReportUiState.Loading
                is LoadPhase.Failed -> WeeklyReportUiState.Error(phase.message)
                is LoadPhase.Loaded -> phase.toSuccessUiState()
            }

        private fun LoadPhase.Loaded.toSuccessUiState(): WeeklyReportUiState.Success {
            val sunday = monday.plusDays(6)
            return WeeklyReportUiState.Success(
                selectedMonday = monday,
                weekOptions = weekOptions,
                dateRange = "${monday.format(RANGE_FORMATTER)} - ${sunday.format(RANGE_FORMATTER)}",
                userName = userName,
                recordedDays = report.week.count { it.isDiary },
                counts =
                    listOf(
                        report.dailyQuestionAmount to MindRecordCategoryUi.DailyQuestion,
                        report.diaryAmount to MindRecordCategoryUi.Diary,
                        report.deepThoughtAmount to MindRecordCategoryUi.DeepThought,
                    ),
                weekDays = mapWeekDays(monday, report.week),
                emotionBubbles = mapEmotionBubbles(report.emotions),
                summaryText = report.summaryText,
                dailyQuestions = report.dailyQuestions.map { it.toUi() },
            )
        }

        companion object {
            private const val WEEK_OPTION_COUNT = 5
            private const val WEEK_LENGTH = 7

            private val API_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            private val RANGE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd.")

            private fun TodayMood.toEmoji(): String =
                when (this) {
                    TodayMood.HAPPY -> "😊"
                    TodayMood.SOSO -> "😐"
                    TodayMood.SAD -> "😢"
                }

            // 4개 슬롯의 크기/위치/색상은 디자인 시안에 맞춰 고정. API 의 emotions 를 percentage
            // 내림차순으로 정렬해 슬롯에 채워 넣어, 1위가 가장 크고 진한 버블이 되도록 배치.
            private val BUBBLE_SLOTS: List<BubbleSlot> =
                listOf(
                    BubbleSlot(size = 100.dp, bgColor = Color(0xFF212121), offsetX = 0.dp, offsetY = 30.dp),
                    BubbleSlot(size = 82.dp, bgColor = Color(0xFF424242), offsetX = 88.dp, offsetY = 0.dp),
                    BubbleSlot(size = 68.dp, bgColor = Color(0xFF616161), offsetX = 158.dp, offsetY = 68.dp),
                    BubbleSlot(size = 58.dp, bgColor = Color(0xFF9E9E9E), offsetX = 230.dp, offsetY = 42.dp),
                )

            private fun mapEmotionBubbles(emotions: List<WeeklyReportEmotion>): List<EmotionBubble> =
                emotions
                    .sortedByDescending { it.percentage }
                    .take(BUBBLE_SLOTS.size)
                    .mapIndexed { index, emotion ->
                        val slot = BUBBLE_SLOTS[index]
                        EmotionBubble(
                            keyword = emotion.keyword,
                            count = emotion.percentage,
                            size = slot.size,
                            bgColor = slot.bgColor,
                            offsetX = slot.offsetX,
                            offsetY = slot.offsetY,
                        )
                    }

            private fun WeeklyReportDailyQuestion.toUi(): DailyQuestion =
                DailyQuestion(
                    title = title,
                    date = parseLocalDate(date),
                    content = content,
                )

            // 서버는 "yyyy.MM.dd 요일" 또는 ISO 포맷으로 내려옴 — 둘 다 허용.
            private val DATE_FORMATTERS: List<DateTimeFormatter> =
                listOf(
                    DateTimeFormatter.ofPattern("yyyy.MM.dd"),
                    DateTimeFormatter.ISO_DATE,
                )

            private fun parseLocalDate(raw: String): LocalDate {
                val datePart = raw.substringBefore(' ').trim()
                for (formatter in DATE_FORMATTERS) {
                    runCatching { return LocalDate.parse(datePart, formatter) }
                }
                return LocalDate.now()
            }
        }

        private data class BubbleSlot(
            val size: Dp,
            val bgColor: Color,
            val offsetX: Dp,
            val offsetY: Dp,
        )
    }
