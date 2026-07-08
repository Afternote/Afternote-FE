package com.afternote.feature.mindrecord.presentation.viewmodel

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
import com.afternote.feature.mindrecord.presentation.model.DailyQuestion
import com.afternote.feature.mindrecord.presentation.model.DayBackground
import com.afternote.feature.mindrecord.presentation.model.DayContent
import com.afternote.feature.mindrecord.presentation.model.DayItem
import com.afternote.feature.mindrecord.presentation.model.EmotionKeyword
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
                            emoji != null && isDiary -> DayContent.EmojiWithDot(dayOfMonth, emoji)
                            emoji != null -> DayContent.EmojiOnly(dayOfMonth, emoji)
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
                emotionKeywords = mapEmotionKeywords(report.emotions),
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

            // 카드 측에서 키워드 개수(0~4)에 따라 슬롯(size·offset·color)을 결정하므로,
            // 여기선 percentage 내림차순으로 정렬해 최대 4건만 잘라 키워드·카운트만 노출한다.
            private const val MAX_EMOTION_KEYWORDS = 4

            private fun mapEmotionKeywords(emotions: List<WeeklyReportEmotion>): List<EmotionKeyword> =
                emotions
                    .sortedByDescending { it.percentage }
                    .take(MAX_EMOTION_KEYWORDS)
                    .map { EmotionKeyword(keyword = it.keyword, count = it.percentage) }

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
    }
