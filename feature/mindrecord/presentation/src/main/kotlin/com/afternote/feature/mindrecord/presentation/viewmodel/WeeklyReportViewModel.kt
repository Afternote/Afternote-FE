package com.afternote.feature.mindrecord.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.domain.repository.UserRepository
import com.afternote.core.ui.UiText
import com.afternote.feature.mindrecord.domain.model.EmotionAnalysisStatus
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
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
        private var loadJob: Job? = null

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

        /**
         * 감정 분석 실패 상태에서 사용자가 누르는 재시도.
         *
         * FE 가 할 수 있는 것은 **재조회**뿐이다 — 서버 측 재분석 트리거는 Afternote-BE#117
         * 소관이다. 보고 있던 주를 그대로 다시 불러 상태를 갱신한다 (#725).
         */
        fun retryEmotionAnalysis() {
            val current = (internalState.value.loadPhase as? LoadPhase.Loaded)?.monday ?: return
            load(current, showsLoading = false, keepsStateOnFailure = true)
        }

        /**
         * 탭 전환·작성 화면 복귀 등 사용자가 요청하지 않은 자동 갱신.
         *
         * 화면이 살아 있는 채로 발화하므로 로딩을 방출하지 않고, 실패해도 보고 있던
         * 화면을 유지한다. 보고 있던 주를 그대로 다시 조회한다.
         */
        fun refreshOnReturn() {
            // 진입 직후의 ON_RESUME 은 init 로드와 겹친다 — 진행 중이면 건너뛴다.
            if (loadJob?.isActive == true) return
            val current =
                (internalState.value.loadPhase as? LoadPhase.Loaded)?.monday
                    ?: weekOptions.first().monday
            load(current, showsLoading = false, keepsStateOnFailure = true)
        }

        private fun load(
            monday: LocalDate,
            showsLoading: Boolean = true,
            keepsStateOnFailure: Boolean = false,
        ) {
            loadJob?.cancel()
            loadJob =
                viewModelScope.launch {
                    if (showsLoading) {
                        internalState.update { it.copy(loadPhase = LoadPhase.Loading) }
                    }
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
                    // runCatching 이 CancellationException 까지 실패로 잡는다.
                    // 새 로드가 이 Job 을 취소했다면 상태는 그쪽이 결정하므로 여기서 멈춘다.
                    ensureActive()
                    result
                        .onSuccess { (report, profile) ->
                            internalState.update {
                                it.copy(loadPhase = LoadPhase.Loaded(monday, report, profile.name))
                            }
                            awaitEmotionAnalysis(monday, report.emotionAnalysis.status)
                        }.onFailure { e ->
                            internalState.update { current ->
                                if (keepsStateOnFailure && current.loadPhase is LoadPhase.Loaded) {
                                    current
                                } else {
                                    current.copy(
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
        }

        /**
         * 감정 분석이 끝나기를 화면에 머무른 채 기다린다.
         *
         * 분석은 저장 직후 비동기로 돌고 완료 신호를 주는 채널이 없어서, 탭에 머무르는 동안
         * 결과가 반영되려면 제한된 재조회가 필요하다 (#725). 무한 폴링은 하지 않는다 —
         * [EMOTION_ANALYSIS_POLL_ATTEMPTS] 회만 시도하고, 그 뒤에는 화면 이탈·복귀
         * ([refreshOnReturn])나 사용자의 재시도에 맡긴다.
         *
         * 이 로직은 [load] 의 코루틴 안에서 돈다. 사용자가 다른 주를 고르면 그 `loadJob`
         * 취소가 이 대기까지 함께 끊는다.
         */
        private suspend fun awaitEmotionAnalysis(
            monday: LocalDate,
            initialStatus: EmotionAnalysisStatus,
        ) {
            var status = initialStatus
            repeat(EMOTION_ANALYSIS_POLL_ATTEMPTS) {
                if (status != EmotionAnalysisStatus.PENDING) return
                delay(EMOTION_ANALYSIS_POLL_INTERVAL_MILLIS)
                val report =
                    repository
                        .getWeeklyReport(date = monday.format(API_DATE_FORMATTER))
                        .getOrNull() ?: return
                currentCoroutineContext().ensureActive()
                status = report.emotionAnalysis.status
                internalState.update { current ->
                    val phase = current.loadPhase
                    // 그 사이 다른 주로 옮겨갔으면 덮어쓰지 않는다.
                    if (phase is LoadPhase.Loaded && phase.monday == monday) {
                        current.copy(loadPhase = phase.copy(report = report))
                    } else {
                        current
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

        /**
         * 월~일 7칸을 만든다.
         *
         * `week[]` 는 기록이 있는 날만 담겨 오는 sparse 배열이라 index 를 요일 오프셋으로
         * 쓰면 안 된다 — 일자(`day`)로 매칭한다 ([aggregateWeekRecordsByDate], #563). 칸에 찍는 날짜는
         * 언제나 달력이 계산한 [LocalDate.dayOfMonth] 다. 서버 원소의 `day` 를 그대로 쓰면
         * 매칭이 어긋난 순간 같은 날짜가 두 칸에 나온다.
         */
        private fun mapWeekDays(
            monday: LocalDate,
            week: List<WeeklyReportDay>,
        ): List<DayItem> {
            val recordByDate = aggregateWeekRecordsByDate(monday, week)
            return List(WEEK_LENGTH) { index ->
                val date = monday.plusDays(index.toLong())
                val record = recordByDate[date]
                val isDiary = record?.isDiary == true
                val emoji = record?.emotion?.toEmoji()
                DayItem(
                    dayOfWeek = date.dayOfWeek,
                    content =
                        when {
                            emoji != null && isDiary -> DayContent.EmojiWithDot(emoji)
                            emoji != null -> DayContent.EmojiOnly(emoji)
                            isDiary -> DayContent.NumberWithDot(date.dayOfMonth)
                            else -> DayContent.NumberOnly(date.dayOfMonth)
                        },
                    background =
                        when (record?.emotion) {
                            TodayMood.HAPPY -> DayBackground.Green
                            TodayMood.SAD -> DayBackground.Pink
                            else -> DayBackground.None
                        },
                )
            }
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
            val sunday = monday.plusDays(WEEK_LENGTH - 1L)
            // 주간 범위 방어를 **한 번만** 한다. 종전에는 집계만 범위를 걸고 HISTORY 는
            // 전량을 렌더해, 같은 화면에 범위가 적용된 수치와 적용되지 않은 목록이
            // 함께 있었다 (#547).
            val dailyQuestionsInWeek = report.dailyQuestions.filter { it.date in monday..sunday }
            return WeeklyReportUiState.Success(
                selectedMonday = monday,
                weekOptions = weekOptions,
                dateRange = "${monday.format(RANGE_FORMATTER)} - ${sunday.format(RANGE_FORMATTER)}",
                userName = userName,
                recordedDays =
                    countRecordedDays(
                        monday = monday,
                        week = report.week,
                        dailyQuestionDates = dailyQuestionsInWeek.map { it.date },
                    ),
                counts =
                    listOf(
                        // 서버 원본(`dailyQuestionAmount`) 이 아니라 **화면이 실제로 그리는 목록**을
                        // 센다. 원본을 쓰면 범위 밖 항목이나 날짜를 해석하지 못해 빠진 항목이
                        // 목록에는 없는데 수치에만 남는다 — 방향만 뒤집힌 같은 불일치다 (#547).
                        dailyQuestionsInWeek.size to MindRecordCategoryUi.DailyQuestion,
                        // 일기는 이 화면에 대응 목록이 없어 서버 수치를 그대로 쓴다.
                        report.diaryAmount to MindRecordCategoryUi.Diary,
                    ),
                emotionAnalysisStatus = report.emotionAnalysis.status,
                weekDays = mapWeekDays(monday, report.week),
                emotionKeywords = mapEmotionKeywords(report.emotions),
                summaryText = report.summaryText,
                dailyQuestions = dailyQuestionsInWeek.map { it.toUi() },
            )
        }

        companion object {
            private const val WEEK_OPTION_COUNT = 5

            // 분석 완료 신호를 주는 채널이 없어 재조회로 기다린다. 8초 × 8회 ≈ 1분 —
            // 그 뒤에는 화면 이탈·복귀나 사용자의 재시도에 맡긴다 (#725).
            private const val EMOTION_ANALYSIS_POLL_ATTEMPTS = 8
            private const val EMOTION_ANALYSIS_POLL_INTERVAL_MILLIS = 8_000L

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
                    date = date,
                    content = content,
                )
        }
    }
