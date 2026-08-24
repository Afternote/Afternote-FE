package com.afternote.feature.mindrecord.domain.usecase

import com.afternote.feature.mindrecord.domain.model.DeepThought
import com.afternote.feature.mindrecord.domain.repository.DeepThoughtRepository
import javax.inject.Inject

/**
 * 가장 최근 깊은 생각 1건 — 홈 `WeeklySummaryGrid` 의 "최근 깊은생각" 카드 (#207).
 *
 * 한 건도 없으면 `null` 을 돌린다. **빈 목록과 조회 실패는 다르다** — 실패는 `Result` 의
 * 실패로 남겨 호출부가 목데이터나 빈 카드로 덮지 않게 한다.
 *
 * 서버 정렬을 믿지 않고 `id` 최대값으로 고른다. 목록 순서는 명세에 없어서, 서버가 정렬을
 * 바꾸면 "최근" 이 조용히 바뀐다.
 */
class GetRecentDeepThoughtUseCase
    @Inject
    constructor(
        private val deepThoughtRepository: DeepThoughtRepository,
    ) {
        suspend operator fun invoke(): Result<DeepThought?> =
            deepThoughtRepository
                .getList()
                .map { list -> list.maxByOrNull { it.id } }
    }
