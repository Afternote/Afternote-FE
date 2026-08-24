package com.afternote.feature.mindrecord.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.common.result.runCatchingCancellable
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
            buildWeekOptions(today = LocalDate.now())

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
            // 재조회 정책(로딩·실패 유지·중복 가드)이 한 곳에만 있도록 위임한다.
            // 다만 이쪽은 **사용자가 누른** 갱신이라 진행 표시를 낸다 — BE#117 전까지는
            // 재조회가 같은 FAILED 를 돌려주는 것이 기본 경로이고, 그러면 데이터가 같아
            // StateFlow 가 방출조차 하지 않아 눌러도 픽셀이 안 바뀐다.
            refreshOnReturn(showsLoading = true)
        }

        /**
         * 탭 전환·작성 화면 복귀 등 사용자가 요청하지 않은 자동 갱신.
         *
         * 화면이 살아 있는 채로 발화하므로 로딩을 방출하지 않고, 실패해도 보고 있던
         * 화면을 유지한다. 보고 있던 주를 그대로 다시 조회한다.
         */
        fun refreshOnReturn(showsLoading: Boolean = false) {
            // 진입 직후의 ON_RESUME 은 init 로드와 겹친다 — 진행 중이면 건너뛴다.
            if (loadJob?.isActive == true) return
            val current =
                (internalState.value.loadPhase as? LoadPhase.Loaded)?.monday
                    ?: weekOptions.first().monday
            load(current, showsLoading = showsLoading, keepsStateOnFailure = true)
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
                        runCatchingCancellable {
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
                    // 새 로드가 이 Job 을 취소했다면 상태는 그쪽이 결정하므로 여기서 멈춘다.
                    // `runCatchingCancellable` 이 취소를 다시 던지므로 위에서 이미 빠져나가지만,
                    // `await()` 사이에 취소가 들어온 경우를 위해 남겨 둔다.
                    ensureActive()
                    result
                        .onSuccess { (report, profile) ->
                            internalState.update {
                                it.copy(loadPhase = LoadPhase.Loaded(monday, report, profile.name))
                            }
                            awaitEmotionAnalysis(monday, report.analysisStatus)
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
            // 폴링 근거는 "저장 직후 비동기 분석" 이라 이번 주에만 성립한다. 지난 주를
            // 골라 보는 동안 8초마다 조회가 나갈 이유가 없다.
            if (monday != LocalDate.now().with(DayOfWeek.MONDAY)) return

            var status = initialStatus
            repeat(EMOTION_ANALYSIS_POLL_ATTEMPTS) {
                if (status != EmotionAnalysisStatus.PENDING) return
                delay(EMOTION_ANALYSIS_POLL_INTERVAL_MILLIS)
                val report =
                    repository
                        .getWeeklyReport(date = monday.format(API_DATE_FORMATTER))
                        .getOrNull()
                currentCoroutineContext().ensureActive()
                // 한 번 실패했다고 남은 시도를 전부 버리지 않는다 — PENDING 화면에는
                // 재시도 수단이 없어(카드는 FAILED 전용) 화면에 머무는 동안 복구할 길이
                // 사라진다. 이번 시도만 소모하고 다음 간격을 기다린다.
                if (report == null) return@repeat
                status = report.analysisStatus
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
                            // 이모지와 점은 배타적이다 (#749). 감정을 고른 날은 이모지만 그린다 —
                            // 종전에는 `emoji != null && isDiary` 가 먼저 걸려, 일기를 쓰고 감정까지
                            // 고른 가장 흔한 경우에 점이 함께 붙었다.
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
            val dailyQuestionDates = report.dailyQuestions.mapNotNull { parseLocalDateOrNull(it.date) }
            return WeeklyReportUiState.Success(
                selectedMonday = monday,
                weekOptions = weekOptions,
                dateRange = "${monday.format(RANGE_FORMATTER)} - ${sunday.format(RANGE_FORMATTER)}",
                userName = userName,
                recordedDays =
                    countRecordedDays(
                        monday = monday,
                        week = report.week,
                        dailyQuestionDates = dailyQuestionDates,
                    ),
                counts =
                    listOf(
                        report.dailyQuestionAmount to MindRecordCategoryUi.DailyQuestion,
                        report.diaryAmount to MindRecordCategoryUi.Diary,
                    ),
                emotionAnalysisStatus = report.analysisStatus,
                weekDays = mapWeekDays(monday, report.week),
                emotionKeywords = mapEmotionKeywords(report.emotions),
                summaryText = report.summaryText,
                dailyQuestions = report.dailyQuestions.mapNotNull { it.toUi() },
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
            private const val TAG = "WeeklyReportViewModel"

            private const val MAX_EMOTION_KEYWORDS = 4

            private fun mapEmotionKeywords(emotions: List<WeeklyReportEmotion>): List<EmotionKeyword> =
                emotions
                    .sortedByDescending { it.percentage }
                    .take(MAX_EMOTION_KEYWORDS)
                    .map { EmotionKeyword(keyword = it.keyword, count = it.percentage) }

            /**
             * 날짜를 못 정하면 `null` 을 돌린다 — 호출부가 그 항목을 목록에서 뺀다.
             *
             * 오늘 날짜로 메우면 지난 주 답변이 오늘로 표시되고 실패 신호가 남지 않는다 (#751).
             * 서버 명세에 `date` 형식이 정의돼 있지 않아 언제든 어긋날 수 있다.
             */
            private fun WeeklyReportDailyQuestion.toUi(): DailyQuestion? {
                val resolvedDate =
                    parseLocalDateOrNull(date) ?: run {
                        Log.w(TAG, "주간리포트 데일리질문 날짜를 해석하지 못해 목록에서 제외한다: raw=$date")
                        return null
                    }
                return DailyQuestion(
                    title = title,
                    date = resolvedDate,
                    content = content,
                )
            }

            // 서버는 "yyyy.MM.dd 요일" 또는 ISO 포맷으로 내려옴 — 둘 다 허용.
            private val DATE_FORMATTERS: List<DateTimeFormatter> =
                listOf(
                    DateTimeFormatter.ofPattern("yyyy.MM.dd"),
                    DateTimeFormatter.ISO_DATE,
                )

            private fun parseLocalDateOrNull(raw: String): LocalDate? {
                val datePart = raw.substringBefore(' ').trim()
                for (formatter in DATE_FORMATTERS) {
                    runCatching { return LocalDate.parse(datePart, formatter) }
                }
                return null
            }
        }
    }

/** 서버가 진행 상태를 주지 않았으면 «모른다» — 0 건으로 확정하지 않는다 (#725). */
private val WeeklyReport.analysisStatus: EmotionAnalysisStatus
    get() = emotionAnalysis?.status ?: EmotionAnalysisStatus.UNKNOWN
