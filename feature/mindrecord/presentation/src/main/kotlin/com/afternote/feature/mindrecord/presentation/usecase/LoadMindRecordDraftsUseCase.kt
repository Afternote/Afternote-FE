package com.afternote.feature.mindrecord.presentation.usecase

import com.afternote.core.common.result.runCatchingCancellable
import com.afternote.feature.mindrecord.domain.model.DailyQuestion
import com.afternote.feature.mindrecord.domain.model.Diary
import com.afternote.feature.mindrecord.domain.repository.DailyQuestionRepository
import com.afternote.feature.mindrecord.domain.repository.DiaryRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.time.YearMonth
import javax.inject.Inject

/**
 * 임시저장의 **조회 범위 정본** (#769 · #1693).
 *
 * 임시저장 목록 화면과 작성 툴바의 카운트가 같은 범위를 봐야 한다 — 목록에 5건이 뜨는데
 * 툴바가 3을 보이면 어느 쪽이 맞는지 알 수 없다. 두 화면이 각자 조회하면 범위가 갈라지므로
 * 여기 한 곳에만 둔다.
 *
 * 일기는 `yearMonth` 가 필수라 **이번 달**만, 데일리질문은 `date` 를 생략해 **전체 기간**을
 * 받는다. 서버 계약이 그렇게 갈려 있어 범위가 비대칭인 것이지 의도한 구분은 아니다.
 */
class LoadMindRecordDraftsUseCase
    @Inject
    constructor(
        private val diaryRepository: DiaryRepository,
        private val dailyQuestionRepository: DailyQuestionRepository,
    ) {
        /**
         * 임시저장 원본을 모아 돌려준다.
         *
         * 한쪽이라도 조회에 실패하면 실패로 올린다 — 빈 목록으로 흡수하면 "실패" 와 "0건" 이
         * 같은 화면이 되어 복구 수단이 사라진다.
         *
         * @param currentMonth 일기를 조회할 달. 종전에는 [YearMonth.now] 를 이 안에서 직접
         *   불렀는데, 그러면 **어느 달을 요청했는가를 벽시계 없이 단언할 수 없다** — 기대값을
         *   만들려고 테스트가 같은 `now()` 를 다시 부르면 구현과 기대가 같은 실수를 공유하고,
         *   달이 바뀌는 자정에는 판정 자체가 흔들린다. 기본값이 있어 호출부는 종전 그대로다 (#1693).
         */
        suspend fun load(currentMonth: YearMonth = YearMonth.now()): Result<Drafts> =
            runCatchingCancellable {
                coroutineScope {
                    val diariesDeferred =
                        async {
                            diaryRepository
                                .getList(yearMonth = currentMonth.toString(), draftOnly = true)
                                .getOrThrow()
                                .diaries
                        }
                    val dailyQuestionsDeferred =
                        async {
                            dailyQuestionRepository.getList(draftOnly = true).getOrThrow()
                        }
                    Drafts(
                        diaries = diariesDeferred.await(),
                        dailyQuestions = dailyQuestionsDeferred.await(),
                    )
                }
            }

        /** 툴바 카운트용 — 개수만 필요할 때. */
        suspend fun count(currentMonth: YearMonth = YearMonth.now()): Result<Int> = load(currentMonth).map { it.total }

        data class Drafts(
            val diaries: List<Diary>,
            val dailyQuestions: List<DailyQuestion>,
        ) {
            val total: Int get() = diaries.size + dailyQuestions.size
        }
    }
