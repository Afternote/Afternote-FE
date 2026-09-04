package com.afternote.feature.mindrecord.presentation.usecase

import com.afternote.core.common.result.runCatchingCancellable
import com.afternote.core.domain.repository.MyProfileRepository
import com.afternote.feature.mindrecord.domain.model.EmotionAnalysisStatus
import com.afternote.feature.mindrecord.domain.model.WeeklyReport
import com.afternote.feature.mindrecord.domain.repository.WeeklyReportRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * 주간 리포트 **조회·제한 폴링 정책** (#725 · #1693).
 *
 * 감정 분석은 저장 직후 비동기로 돌고 **완료를 알리는 채널이 없다.** 탭에 머무는 동안 결과가
 * 반영되려면 제한된 재조회가 필요하다. 종전에는 이 규칙이 `WeeklyReportViewModel` 안에서 UI
 * 상태 갱신과 뒤섞여 있어, 「몇 번·언제 조회하는가」를 화면을 띄우지 않고는 검증할 수 없었다.
 *
 * ### 무한 폴링을 하지 않는다
 *
 * [POLL_ATTEMPTS] 회만 시도하고 그 뒤에는 화면 이탈·복귀나 사용자의 재시도에 맡긴다.
 * 그리고 **이번 주에만** 돈다 — 폴링의 근거가 「저장 직후 비동기 분석」이라 지난 주를 보는
 * 동안 8초마다 조회가 나갈 이유가 없다.
 *
 * ### 한 번 실패했다고 남은 시도를 버리지 않는다
 *
 * `PENDING` 화면에는 재시도 수단이 없어(카드는 `FAILED` 전용) 여기서 포기하면 화면에 머무는
 * 동안 복구할 길이 사라진다. 그 시도만 소모하고 다음 간격을 기다린다.
 *
 * ### 취소
 *
 * 사용자가 다른 주를 고르면 호출부가 수집을 취소하고, 그 취소가 대기까지 함께 끊는다.
 * 취소를 실패로 바꾸지 않는다 — 화면을 떠난 것뿐인데 오류가 뜨면 안 된다.
 */
class ObserveWeeklyReportUseCase
    @Inject
    constructor(
        private val repository: WeeklyReportRepository,
        private val userRepository: MyProfileRepository,
    ) {
        /**
         * [monday] 주의 리포트를 내보낸다.
         *
         * 첫 방출은 리포트·프로필 **병렬** 조회 결과다. 실패하면 그것으로 끝난다 — 조회가
         * 안 되는데 폴링할 이유가 없다.
         *
         * 성공했고 이번 주이고 분석이 `PENDING` 이면, 완료되거나 [POLL_ATTEMPTS] 를 소진할
         * 때까지 갱신된 리포트를 이어서 내보낸다.
         *
         * @param today 「이번 주」 판정 기준. 종전에는 [LocalDate.now] 를 구현이 직접 불러
         *   **지난 주에는 폴링하지 않는다는 규칙을 벽시계 없이 검증할 수 없었다** (#1693).
         */
        fun observe(
            monday: LocalDate,
            today: LocalDate = LocalDate.now(),
        ): Flow<Result<Snapshot>> =
            flow {
                val first =
                    runCatchingCancellable {
                        coroutineScope {
                            val reportDeferred = async { repository.getWeeklyReport(date = monday.apiDate()).getOrThrow() }
                            val profileDeferred = async { userRepository.getMyProfile() }
                            Snapshot(report = reportDeferred.await(), profileName = profileDeferred.await().name)
                        }
                    }
                emit(first)

                val loaded = first.getOrNull() ?: return@flow
                if (monday != today.with(DayOfWeek.MONDAY)) return@flow

                var status = loaded.report.analysisStatus
                repeat(POLL_ATTEMPTS) {
                    if (status != EmotionAnalysisStatus.PENDING) return@flow
                    delay(POLL_INTERVAL_MILLIS)
                    val report = repository.getWeeklyReport(date = monday.apiDate()).getOrNull()
                    currentCoroutineContext().ensureActive()
                    if (report == null) return@repeat
                    status = report.analysisStatus
                    emit(Result.success(loaded.copy(report = report)))
                }
            }

        private fun LocalDate.apiDate(): String = format(API_DATE_FORMATTER)

        /**
         * 한 주의 화면 재료. 이름은 첫 조회에서만 오고 폴링은 리포트만 갱신한다 —
         * 프로필은 8초마다 다시 받을 이유가 없다.
         */
        data class Snapshot(
            val report: WeeklyReport,
            val profileName: String,
        )

        private companion object {
            /** 화면에 머무는 동안만 시도하는 횟수. 무한 폴링을 막는 상한이다. */
            const val POLL_ATTEMPTS = 8
            const val POLL_INTERVAL_MILLIS = 8_000L

            private val API_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        }
    }

/**
 * 서버가 진행 상태를 주지 않았으면 «모른다» — 0 건으로 확정하지 않는다 (#725).
 *
 * 폴링을 계속할지 정하는 값이라 정책과 같은 자리에 둔다. 화면(`WeeklyReportViewModel`)도
 * 카드에 그대로 실어 보여 주므로 모듈 안에서 공유한다 (#1693).
 */
internal val WeeklyReport.analysisStatus: EmotionAnalysisStatus
    get() = emotionAnalysis?.status ?: EmotionAnalysisStatus.UNKNOWN
