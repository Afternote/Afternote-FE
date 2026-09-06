package com.afternote.feature.mindrecord.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.ui.UiText
import com.afternote.feature.mindrecord.domain.model.EmotionAnalysisStatus
import com.afternote.feature.mindrecord.domain.model.TodayMood
import com.afternote.feature.mindrecord.domain.model.WeeklyReport
import com.afternote.feature.mindrecord.domain.model.WeeklyReportDailyQuestion
import com.afternote.feature.mindrecord.domain.model.WeeklyReportDay
import com.afternote.feature.mindrecord.domain.model.WeeklyReportEmotion
import com.afternote.feature.mindrecord.domain.sync.MindRecordChangeTracker
import com.afternote.feature.mindrecord.presentation.R
import com.afternote.feature.mindrecord.presentation.model.DailyQuestion
import com.afternote.feature.mindrecord.presentation.model.DayBackground
import com.afternote.feature.mindrecord.presentation.model.DayContent
import com.afternote.feature.mindrecord.presentation.model.DayItem
import com.afternote.feature.mindrecord.presentation.model.EmotionKeyword
import com.afternote.feature.mindrecord.presentation.model.MindRecordCategoryUi
import com.afternote.feature.mindrecord.presentation.reporting.MindRecordFailureStage
import com.afternote.feature.mindrecord.presentation.reporting.recordMindRecordFailure
import com.afternote.feature.mindrecord.presentation.usecase.ObserveWeeklyReportUseCase
import com.afternote.feature.mindrecord.presentation.usecase.analysisStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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
        private val observeWeeklyReport: ObserveWeeklyReportUseCase,
        private val changeTracker: MindRecordChangeTracker,
        private val errorReporter: ErrorReporter,
    ) : ViewModel() {
        private val weekOptions: List<WeekOption> =
            buildWeekOptions(today = LocalDate.now())

        private val internalState = MutableStateFlow(InternalState())

        /** 마지막으로 성공한 조회 시점의 데이터 버전 (#736). */
        private var loadedVersion: Long? = null

        /** 이 ViewModel 이 ON_RESUME 을 한 번이라도 받았는지 — init 로드와의 중복을 가른다. */
        private var hasSeenFirstResume: Boolean = false
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
         * 조회 실패 화면의 재시도 — **실패한 주차를 그대로** 다시 부른다 (#723).
         *
         * 이번 주로 되돌리면 사용자가 보려던 주차가 유실돼, 나갔다 들어와도 복구되지 않는다.
         * 로딩을 보여도 잃을 것이 없다 — 보고 있던 것이 오류 문구뿐이다.
         */
        fun retry() {
            val target =
                when (val phase = internalState.value.loadPhase) {
                    is LoadPhase.Failed -> phase.monday

                    is LoadPhase.Loaded -> phase.monday

                    // 아직 첫 조회가 끝나지 않았다 — 겹쳐 부르지 않는다.
                    LoadPhase.Loading -> return
                }
            load(target)
        }

        /**
         * 감정 분석 실패 상태에서 사용자가 누르는 재시도.
         *
         * FE 가 할 수 있는 것은 **재조회**뿐이다 — 서버 측 재분석 트리거는 Afternote-BE#117
         * 소관이다. 보고 있던 주를 그대로 다시 불러 상태를 갱신한다 (#725).
         */
        fun retryEmotionAnalysis() {
            // refreshOnReturn 에 위임하지 않는다 — 그쪽의 «데이터가 안 바뀌었으면 안 부른다»
            // 가드(#736)에 걸려 조회가 아예 안 나간다. 분석 실패는 조회 자체는 성공한
            // 상태(Loaded)라 changeTracker 버전이 그대로이기 때문이다.
            //
            // 이쪽은 **사용자가 누른** 갱신이라 진행 표시를 낸다 — BE#117 전까지는 재조회가
            // 같은 FAILED 를 돌려주는 것이 기본 경로이고, 그러면 데이터가 같아 StateFlow 가
            // 방출조차 하지 않아 눌러도 픽셀이 안 바뀐다.
            if (loadJob?.isActive == true) return
            load(targetMonday(), showsLoading = true, keepsStateOnFailure = true)
        }

        /**
         * 탭 전환·작성 화면 복귀 등 사용자가 요청하지 않은 자동 갱신.
         *
         * 화면이 살아 있는 채로 발화하므로 로딩을 방출하지 않고, 실패해도 보고 있던
         * 화면을 유지한다. 보고 있던 주를 그대로 다시 조회한다.
         */
        fun refreshOnReturn(showsLoading: Boolean = false) {
            // **이 ViewModel 이 받는 첫 ON_RESUME 은 init 로드가 이미 덮는다.**
            //
            // 이 상태를 화면의 rememberSaveable 로 두면 프로세스 사망 뒤 되살아난 «지나갔음»
            // 플래그가 새 ViewModel 의 init 과 겹친다 — 저장 상태는 복원되는데 ViewModel 은
            // 새로 만들어지기 때문이다. 그러면 이 PR 이 막으려는 같은 주차 GET 이 다시 두 번
            // 나간다. 그래서 ViewModel 수명에 묶는다 (#736 리뷰).
            //
            // Job 가드만으로는 부족하다 — 즉시 끝나는 응답(캐시·즉시 실패)에서는 init 로드가
            // 이미 끝나 있어 그냥 통과한다.
            if (!hasSeenFirstResume) {
                hasSeenFirstResume = true
                return
            }
            if (loadJob?.isActive == true) return
            // 성공해서 보고 있는 화면이라면, 데이터가 바뀌었을 때만 다시 부른다 (#736).
            // 실패 상태는 이 가드에 걸리지 않는다 — 실패한 주차를 다시 시도해야 한다 (#723).
            //
            // **분석 대기도 통과시킨다.** changeTracker 는 일기·데일리질문의 쓰기 성공에서만
            // 올라가는데, 감정 분석 완료는 서버가 비동기로 채우는 상태라 그 카운터가 모른다.
            // 폴링(8회 × 8초 ≈ 1분)이 소진된 뒤 대기가 남아 있으면 복귀 갱신이 유일한 복구
            // 경로인데, 그것까지 막으면 «분석 중» 이 앱 재시작까지 화면에 굳는다 — PENDING
            // 에는 재시도 버튼도 없다(카드는 FAILED 전용).
            val phase = internalState.value.loadPhase
            val awaitsAnalysis =
                // emotionAnalysis 가 null 이면 서버가 상태를 안 준 것(#725 UNKNOWN 경로)이라
                // «분석 대기» 로 치지 않는다 — 기다릴 근거가 없다.
                phase is LoadPhase.Loaded && phase.report.emotionAnalysis?.status == EmotionAnalysisStatus.PENDING
            if (phase is LoadPhase.Loaded && !awaitsAnalysis && loadedVersion == changeTracker.version) return
            load(targetMonday(), showsLoading = showsLoading, keepsStateOnFailure = true)
        }

        /**
         * 다시 부를 주. 실패 상태면 **실패한 주**를 그대로 다시 시도한다 — 이번 주로
         * 되돌아가면 사용자가 보려던 주차가 유실돼 나갔다 들어와도 복구되지 않는다 (#723).
         */
        private fun targetMonday(): LocalDate =
            when (val phase = internalState.value.loadPhase) {
                is LoadPhase.Loaded -> phase.monday
                is LoadPhase.Failed -> phase.monday
                LoadPhase.Loading -> weekOptions.first().monday
            }

        private fun load(
            monday: LocalDate,
            showsLoading: Boolean = true,
            keepsStateOnFailure: Boolean = false,
        ) {
            loadJob?.cancel()
            loadJob =
                viewModelScope.launch {
                    // 실패했을 때 되돌아갈 화면을 **로딩으로 덮기 전에** 잡아 둔다 (#723).
                    val previousLoaded = internalState.value.loadPhase.lastLoadedOrNull()
                    // **조회를 시작하기 직전**의 버전을 잡아 둔다.
                    //
                    // 끝난 시점에 읽으면 조회와 겹친 쓰기를 통째로 삼킨다 — GET 이 서버
                    // snapshot 을 읽은 뒤 응답이 오는 사이에 쓰기가 성공하면, 이 결과에는
                    // 그 변경이 없는데도 증가한 최신 버전을 «내가 본 버전» 으로 기록한다.
                    // 그러면 복귀 시 두 값이 같아 재조회를 건너뛴다 (#736 리뷰).
                    val versionAtLoadStart = changeTracker.version
                    if (showsLoading) {
                        internalState.update { it.copy(loadPhase = LoadPhase.Loading) }
                    }
                    // 조회·폴링 정책은 [ObserveWeeklyReportUseCase] 가 갖는다. 여기 남는 것은
                    // 「그 결과를 화면의 어떤 상태로 보일까」 뿐이다 (#1693).
                    //
                    // 첫 방출은 리포트·프로필 병렬 조회 결과이고, 이번 주에 분석이 진행 중이면
                    // 갱신된 리포트가 이어서 온다. 새 로드가 이 Job 을 취소하면 수집도 함께
                    // 끊기므로 «다른 주로 옮겨갔는데 옛 응답이 덮어쓰는» 일이 없다.
                    observeWeeklyReport.observe(monday).collect { result ->
                        ensureActive()
                        result
                            .onSuccess { snapshot ->
                                loadedVersion = versionAtLoadStart
                                // 방출이 첫 조회든 폴링이든 [Snapshot] 이 이름을 함께 싣는다 —
                                // 폴링은 `loaded.copy(report =)` 로 첫 조회의 이름을 그대로 들고 온다.
                                // 그래서 여기서 갈라 «이름은 첫 조회 것» 으로 두면, ON_RESUME
                                // 재조회(refreshOnReturn)가 방금 받은 새 이름을 버린다 — 설정에서
                                // 이름을 바꾸고 돌아오면 주를 옮기기 전까지 옛 이름이 남는다 (#1693 리뷰).
                                internalState.update { current ->
                                    current.copy(
                                        loadPhase = LoadPhase.Loaded(monday, snapshot.report, snapshot.profileName),
                                    )
                                }
                            }.onFailure { e ->
                                // 탭 전체가 오류 화면이 되는 자리다 — 무엇이 실패했는지 남긴다 (#1882).
                                errorReporter.recordMindRecordFailure(MindRecordFailureStage.WEEKLY_REPORT_LOAD, e)
                                internalState.update { current ->
                                    if (keepsStateOnFailure && current.loadPhase is LoadPhase.Loaded) {
                                        current
                                    } else {
                                        current.copy(
                                            loadPhase =
                                                LoadPhase.Failed(
                                                    // 예외 문구를 화면에 싣지 않는다. 원문은 바로 위
                                                    // 계측으로 남는다 (#1339 선례, #1882).
                                                    message =
                                                        UiText.Resource(R.string.mindrecord_error_weekly_report_failed),
                                                    monday = monday,
                                                    previous = previousLoaded,
                                                ),
                                        )
                                    }
                                }
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

                            // 보통은 배경을 주지 않는다 — 시안이 «좋음/나쁨» 두 끝만 색으로 가른다.
                            TodayMood.SOSO -> DayBackground.None

                            // 그날 기록이 없거나, 있어도 감정을 고르지 않은 날.
                            null -> DayBackground.None
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

            /**
             * 조회 실패.
             *
             * [monday] 는 **사용자가 고른 주**다. 이 값이 없으면 재진입 자동 갱신이 이번 주로
             * 되돌아가, 실패한 주를 다시 시도할 방법이 사라진다 (#723).
             *
             * [previous] 는 직전에 성공했던 화면이다. 남겨 두면 실패해도 리포트와 주차 선택
             * UI 를 유지할 수 있다 — 종전에는 화면 전체가 오류 문구 하나로 바뀌어, 재시도도
             * 다른 주차로 이동도 불가능했다.
             */
            data class Failed(
                val message: UiText,
                val monday: LocalDate,
                val previous: Loaded?,
            ) : LoadPhase
        }

        /** 실패 상태에 보존해 둔 직전 성공 화면(없으면 null). */
        private fun LoadPhase.lastLoadedOrNull(): LoadPhase.Loaded? =
            when (this) {
                is LoadPhase.Loaded -> this
                is LoadPhase.Failed -> previous
                LoadPhase.Loading -> null
            }

        private fun InternalState.toUiState(): WeeklyReportUiState =
            when (val phase = loadPhase) {
                LoadPhase.Loading -> {
                    WeeklyReportUiState.Loading
                }

                is LoadPhase.Loaded -> {
                    phase.toSuccessUiState()
                }

                is LoadPhase.Failed -> {
                    // 직전에 보던 리포트가 있으면 그 화면을 유지하고 실패는 배너로 알린다.
                    // 화면 전체를 오류로 바꾸면 주차 선택 UI 까지 사라져 복구 수단이 없어진다.
                    phase.previous?.toSuccessUiState()?.copy(
                        loadFailure =
                            WeeklyReportUiState.LoadFailure(
                                message = phase.message,
                                failedWeekLabel = phase.monday,
                            ),
                    ) ?: WeeklyReportUiState.Error(
                        message = phase.message,
                        weekOptions = weekOptions,
                        failedMonday = phase.monday,
                    )
                }
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
                emotionAnalysisStatus = report.analysisStatus,
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
            private const val TAG = "WeeklyReportViewModel"

            private const val MAX_EMOTION_KEYWORDS = 4

            private fun mapEmotionKeywords(emotions: List<WeeklyReportEmotion>): List<EmotionKeyword> =
                emotions
                    .sortedByDescending { it.percentage }
                    .take(MAX_EMOTION_KEYWORDS)
                    .map { EmotionKeyword(keyword = it.keyword, count = it.percentage) }

            /**
             * 날짜 해석은 이미 data 계층(`ServerDateParser`)이 끝냈다 — 여기서는 옮기기만 한다.
             *
             * 해석하지 못한 항목은 매퍼가 이미 제외하므로 이 자리에 실패 갈래가 없다 (#547).
             */
            private fun WeeklyReportDailyQuestion.toUi(): DailyQuestion =
                DailyQuestion(
                    title = title,
                    date = date,
                    content = content,
                )
        }
    }
