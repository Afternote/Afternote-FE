package com.afternote.feature.home.presentation.receiver.testing

import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.feature.home.presentation.receiver.ReceiverHomeViewModel
import com.afternote.feature.home.presentation.usecase.GetReceiverHomeSummaryUseCase
import com.afternote.feature.mindrecord.domain.repository.MindRecordReceiverRepository
import com.afternote.feature.receiver.domain.repository.ReceiverRepository
import com.afternote.feature.timeletter.domain.repository.ReceiverTimeLetterRepository

/**
 * 저장소 fake 만 쥔 테스트가 수신자 홈 ViewModel 을 조립하는 자리 (#1689).
 *
 * 집계 UseCase 와 결과 타입은 `feature:home:presentation` 안의 계약이라 모듈 밖으로 열지
 * 않는다 (docs/convention/production-visibility.md). 대신 조립만 fixture 로 내보낸다 —
 * 계측 테스트가 Hilt 그래프 없이 네 출처의 완료 순서를 쥐려면 손으로 조립해야 하는데,
 * 그 필요는 테스트의 것이지 프로덕션 API 의 이유가 아니다.
 *
 * Hilt 가 실제로 조립하는 그래프와 어긋나지 않도록 인자는 `@Inject` 생성자와 같은 것만 받는다.
 */
public fun receiverHomeViewModel(
    receiverRepository: ReceiverRepository,
    mindRecordReceiverRepository: MindRecordReceiverRepository,
    receiverTimeLetterRepository: ReceiverTimeLetterRepository,
    errorReporter: ErrorReporter,
): ReceiverHomeViewModel =
    ReceiverHomeViewModel(
        getReceiverHomeSummary =
            GetReceiverHomeSummaryUseCase(
                receiverRepository = receiverRepository,
                mindRecordReceiverRepository = mindRecordReceiverRepository,
                receiverTimeLetterRepository = receiverTimeLetterRepository,
            ),
        receiverRepository = receiverRepository,
        errorReporter = errorReporter,
    )
