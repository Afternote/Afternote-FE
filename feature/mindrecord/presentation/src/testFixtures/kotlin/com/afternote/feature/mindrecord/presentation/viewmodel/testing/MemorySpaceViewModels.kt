package com.afternote.feature.mindrecord.presentation.viewmodel.testing

import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.feature.mindrecord.domain.repository.DailyQuestionRepository
import com.afternote.feature.mindrecord.domain.repository.DiaryRepository
import com.afternote.feature.mindrecord.presentation.usecase.GetMemorySpaceUseCase
import com.afternote.feature.mindrecord.presentation.viewmodel.MemorySpaceViewModel

/**
 * 저장소 fake 만 쥔 테스트가 추억 공간 ViewModel 을 조립하는 자리 (#1693).
 *
 * 집계 UseCase 와 카드 모델은 `feature:mindrecord:presentation` 안의 계약이라 모듈 밖으로
 * 열지 않는다 (docs/convention/production-visibility.md). 대신 조립만 fixture 로 내보낸다.
 *
 * Hilt 가 실제로 조립하는 그래프와 어긋나지 않도록 인자는 `@Inject` 생성자가 받는 것만
 * 받는다 — UseCase 는 여기서 같은 저장소로 만든다.
 */
public fun memorySpaceViewModel(
    diaryRepository: DiaryRepository,
    dailyQuestionRepository: DailyQuestionRepository,
    errorReporter: ErrorReporter,
): MemorySpaceViewModel =
    MemorySpaceViewModel(
        getMemorySpace =
            GetMemorySpaceUseCase(
                diaryRepository = diaryRepository,
                dailyQuestionRepository = dailyQuestionRepository,
            ),
        errorReporter = errorReporter,
    )
