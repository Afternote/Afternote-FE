package com.afternote.feature.mindrecord.presentation.usecase

import com.afternote.core.common.result.runCatchingCancellable
import com.afternote.feature.mindrecord.domain.model.DailyQuestion
import com.afternote.feature.mindrecord.domain.model.Diary
import com.afternote.feature.mindrecord.domain.model.MindRecordType
import com.afternote.feature.mindrecord.domain.repository.DailyQuestionRepository
import com.afternote.feature.mindrecord.domain.repository.DiaryRepository
import com.afternote.feature.mindrecord.presentation.mapper.toUi
import com.afternote.feature.mindrecord.presentation.model.memoryspace.MemoryRecord
import com.afternote.feature.mindrecord.presentation.model.memoryspace.MemoryRecordId
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.time.YearMonth
import javax.inject.Inject

/**
 * 추억 공간의 **집계 정본** (#1693).
 *
 * 서버에 "추억" 전용 계약이 없어 사용자의 실제 기록을 모아 카드로 만든다. 그 조립에 붙는
 * 정책이 넷인데 종전에는 전부 `MemorySpaceViewModel` 안에 있었다.
 *
 * 1. **비대칭 조회 범위** — 일기는 `yearMonth` 가 필수라 최근 [DIARY_MONTH_WINDOW] 개월만
 *    달마다 따로 조회하고, 데일리질문은 `date` 를 생략해 전체 기간을 한 번에 받는다.
 *    카드가 몇 장뿐이라 그보다 과거까지 훑으면 화면에 쓰지도 않을 요청만 늘어난다.
 * 2. **부분 실패** — 카드가 한 장이라도 채워지면 실패를 삼킨다. 장식 화면을 한 출처의
 *    실패로 통째 비우면 잃는 것이 더 크다. 반면 **합친 결과가 비었는데 실패한 출처가
 *    있으면** 그 실패를 올린다 — 실패한 쪽에 기록이 있었을 수 있어 0건으로 확정하면
 *    「아직 담긴 기록이 없어요」 로 오인하고 재시도 경로까지 사라진다.
 * 3. **초안 제외** — `GET /diary` 는 `draftOnly` 를 생략하면 그 달 전체(임시저장 포함)를
 *    내려주므로 클라가 거른다. 데일리질문은 생략 시 서버가 제출 완료만 주므로 필요 없다.
 *    두 API 의 기본값이 다르다.
 * 4. **정렬·상한** — 두 출처를 최신순 한 줄로 세워 [limit] 장까지 남긴다.
 *
 * 이 집계를 소비하는 프로덕션 코드는 같은 모듈의 [MemorySpaceViewModel] 뿐이라 `internal`
 * 이다 (docs/convention/production-visibility.md). 모듈 밖 계측 조립은 `src/testFixtures`
 * 의 `memorySpaceViewModel` 로 간다.
 */
internal class GetMemorySpaceUseCase
    @Inject
    constructor(
        private val diaryRepository: DiaryRepository,
        private val dailyQuestionRepository: DailyQuestionRepository,
    ) {
        /**
         * @param currentMonth 일기 조회의 기준 달. 종전에는 [YearMonth.now] 를 안에서 직접
         *   불러 **어느 달을 요청했는가를 벽시계 없이 단언할 수 없었다** — 기대값을 만들려고
         *   테스트가 같은 `now()` 를 부르면 구현과 기대가 같은 실수를 공유하고, 달이 바뀌는
         *   자정에는 판정 자체가 흔들린다. 기본값이 있어 호출부는 종전 그대로다
         *   ([LoadMindRecordDraftsUseCase] 와 같은 이유·같은 형태).
         * @param limit 카드 배치(`MemorySpaceCardField`)가 자리를 잡는 장수.
         */
        suspend fun invoke(
            currentMonth: YearMonth = YearMonth.now(),
            limit: Int = MEMORY_CARD_LIMIT,
        ): Result<List<MemoryRecord>> =
            runCatchingCancellable {
                coroutineScope {
                    val diaryDeferred =
                        recentMonths(currentMonth).map { month ->
                            async { diaryRepository.getList(yearMonth = month.toString()) }
                        }
                    val questionDeferred = async { dailyQuestionRepository.getList() }

                    val diaryResults = diaryDeferred.awaitAll()
                    val questionResult = questionDeferred.await()

                    val diaries =
                        diaryResults
                            .mapNotNull { it.getOrNull() }
                            .flatMap { it.diaries }
                            .filterNot { it.isDraft }
                    val questions = questionResult.getOrNull().orEmpty()

                    val records =
                        (diaries.mapNotNull { it.toMemoryRecord() } + questions.mapNotNull { it.toMemoryRecord() })
                            .sortedByDescending { it.date }
                            .take(limit)

                    if (records.isEmpty()) {
                        val failure =
                            diaryResults.firstNotNullOfOrNull { it.exceptionOrNull() }
                                ?: questionResult.exceptionOrNull()
                        if (failure != null) throw failure
                    }

                    records
                }
            }

        private fun recentMonths(currentMonth: YearMonth): List<YearMonth> =
            (0 until DIARY_MONTH_WINDOW).map { currentMonth.minusMonths(it.toLong()) }

        /** 날짜를 못 정한 일기는 정렬 키가 없어 카드로 만들지 않는다 ([toUi] 가 null 을 돌린다). */
        private fun Diary.toMemoryRecord(): MemoryRecord? {
            val ui = toUi() ?: return null
            return MemoryRecord(
                id = MemoryRecordId(type = MindRecordType.DIARY, value = ui.id),
                date = ui.date,
                title = ui.title,
                content = ui.content,
                imageUrl = ui.imageUrl,
                emotion = ui.emotion,
            )
        }

        /** 날짜를 못 정한 데일리질문도 같다 — 정렬 키가 없어 카드로 만들지 않는다 (#751). */
        private fun DailyQuestion.toMemoryRecord(): MemoryRecord? {
            val ui = toUi() ?: return null
            return MemoryRecord(
                id = MemoryRecordId(type = MindRecordType.DAILY_QUESTION, value = ui.id),
                date = ui.date,
                title = ui.title,
                content = ui.content,
                imageUrl = ui.imageUrl,
                // 데일리질문에는 오늘의 기분이 없다 — 시안에도 그 자리가 없다.
                emotion = null,
            )
        }

        private companion object {
            /** 카드 배치(`MemorySpaceCardField`)가 4장까지만 자리를 잡는다. */
            const val MEMORY_CARD_LIMIT = 4

            const val DIARY_MONTH_WINDOW = 3
        }
    }
