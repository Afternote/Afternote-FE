package com.afternote.feature.home.presentation.usecase

import com.afternote.core.common.result.runCatchingCancellable
import com.afternote.feature.mindrecord.domain.model.ReceiverMindRecords
import com.afternote.feature.mindrecord.domain.repository.MindRecordReceiverRepository
import com.afternote.feature.receiver.domain.model.AfterNotesListResult
import com.afternote.feature.receiver.domain.model.SenderMessageInfo
import com.afternote.feature.receiver.domain.repository.ReceiverRepository
import com.afternote.feature.timeletter.domain.repository.ReceiverTimeLetterRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

/**
 * 수신자 홈이 필요로 하는 네 출처를 병렬 조회하고 **부분 실패 정책까지** 소유한다 (#1689).
 *
 * 서버에 수신자 홈 전용 엔드포인트가 없어 클라이언트가 합성한다. 합성에 붙는 판정이 둘이고
 * 종전에는 `ReceiverHomeViewModel` 안에 있었다.
 *
 * 1. **전부 실패해야 전체 실패다.** 하나라도 성공하면 그 출처만으로 화면을 그린다 — 애프터노트
 *    한 건이 실패했다고 잘 온 타임레터·마음기록까지 오류 화면으로 덮으면 잃는 것이 더 크다.
 * 2. **실패한 출처의 이름을 남긴다.** 부분 실패는 0·빈 값으로 덮여 화면에도 콘솔에도 흔적이
 *    남지 않던 구간이라, 무엇이 빠졌는지는 이 결과만 안다.
 *
 * 아이콘 선택·문자열·화면 상태·telemetry 기록은 **여기 없다** — 그것들은 화면의 몫이라
 * ViewModel 에 남는다. 이 결과는 Android 리소스도 UI 모델도 참조하지 않는다.
 *
 * [GetHomeSummaryUseCase] 와 같은 자리에 두는 이유도 같다 — 여러 feature 의 domain 을 함께
 * 끌어와야 해서 domain 패키지에 두면 레이어 가드에 걸린다.
 *
 * 이 집계를 소비하는 프로덕션 코드는 같은 모듈의 [ReceiverHomeViewModel] 뿐이라 `internal`
 * 이다 (docs/convention/production-visibility.md). 모듈 밖 계측 조립은
 * `src/testFixtures` 의 `receiverHomeViewModel` 로 간다.
 */
internal class GetReceiverHomeSummaryUseCase
    @Inject
    constructor(
        private val receiverRepository: ReceiverRepository,
        private val mindRecordReceiverRepository: MindRecordReceiverRepository,
        private val receiverTimeLetterRepository: ReceiverTimeLetterRepository,
    ) {
        suspend operator fun invoke(): ReceiverHomeSummaryResult =
            coroutineScope {
                val afternotes = async { receiverRepository.getReceivedAfterNotes() }
                val mindRecords = async { mindRecordReceiverRepository.getAll() }
                // 이 저장소만 Result 가 아니라 값을 던진다 — 취소를 실패로 바꾸지 않도록 감싼다.
                val timeLetters =
                    async {
                        runCatchingCancellable { receiverTimeLetterRepository.getReceivedTimeLetters() }
                    }
                val message = async { receiverRepository.loadSenderMessage() }

                val afternotesResult = afternotes.await()
                val mindRecordsResult = mindRecords.await()
                val timeLettersResult = timeLetters.await()
                val messageResult = message.await()

                val failedSources =
                    buildList {
                        if (afternotesResult.isFailure) add(SOURCE_AFTERNOTES)
                        if (mindRecordsResult.isFailure) add(SOURCE_MIND_RECORDS)
                        if (timeLettersResult.isFailure) add(SOURCE_TIME_LETTERS)
                        if (messageResult.isFailure) add(SOURCE_SENDER_MESSAGE)
                    }
                val firstCause =
                    afternotesResult.exceptionOrNull()
                        ?: mindRecordsResult.exceptionOrNull()
                        ?: timeLettersResult.exceptionOrNull()
                        ?: messageResult.exceptionOrNull()

                val failure = firstCause?.let { SourceFailure(firstCause = it, failedSources = failedSources) }
                if (failure != null && failedSources.size == SOURCE_COUNT) {
                    return@coroutineScope ReceiverHomeSummaryResult.AllFailed(failure)
                }

                ReceiverHomeSummaryResult.Loaded(
                    summary =
                        ReceiverHomeSummary(
                            afternotes = afternotesResult.getOrNull(),
                            mindRecords = mindRecordsResult.getOrNull(),
                            timeLetterTotalCount = timeLettersResult.getOrNull()?.totalCount,
                            senderMessage = messageResult.getOrNull(),
                        ),
                    failure = failure,
                )
            }

        private companion object {
            const val SOURCE_AFTERNOTES = "afternotes"
            const val SOURCE_MIND_RECORDS = "mind_records"
            const val SOURCE_TIME_LETTERS = "time_letters"
            const val SOURCE_SENDER_MESSAGE = "sender_message"

            /** 홈 한 화면을 그리려고 던지는 요청 수 — 전부 실패해야 전체 실패다. */
            const val SOURCE_COUNT = 4
        }
    }

/** 네 출처에서 온 값. 실패한 출처는 `null` 이고, 무엇이 실패했는지는 [SourceFailure] 가 안다. */
internal data class ReceiverHomeSummary(
    val afternotes: AfterNotesListResult?,
    val mindRecords: ReceiverMindRecords?,
    val timeLetterTotalCount: Int?,
    val senderMessage: SenderMessageInfo?,
)

/**
 * 실패한 출처들과 그중 첫 원인.
 *
 * 첫 원인을 따로 드는 이유는 telemetry 가 예외 하나를 요구해서다 — 네 개를 다 올리면
 * 보관 한도(최근 8건)를 한 번의 오프라인이 채운다.
 */
internal data class SourceFailure(
    val firstCause: Throwable,
    val failedSources: List<String>,
)

internal sealed interface ReceiverHomeSummaryResult {
    /**
     * 화면을 그릴 수 있는 상태. [failure] 가 `null` 이 아니면 **부분 실패** 다 — 성공한 출처는
     * [summary] 에 들어 있고 실패한 자리는 `null` 이다.
     */
    data class Loaded(
        val summary: ReceiverHomeSummary,
        val failure: SourceFailure?,
    ) : ReceiverHomeSummaryResult

    /** 네 출처가 모두 실패해 그릴 것이 없는 상태. */
    data class AllFailed(
        val failure: SourceFailure,
    ) : ReceiverHomeSummaryResult
}
