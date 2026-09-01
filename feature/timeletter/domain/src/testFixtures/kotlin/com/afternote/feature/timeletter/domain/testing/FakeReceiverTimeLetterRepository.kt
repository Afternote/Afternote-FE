package com.afternote.feature.timeletter.domain.testing

import com.afternote.feature.timeletter.domain.model.ReceivedTimeLetter
import com.afternote.feature.timeletter.domain.model.ReceivedTimeLetterList
import com.afternote.feature.timeletter.domain.repository.ReceiverTimeLetterRepository
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

/**
 * [ReceiverTimeLetterRepository] fake 정본 (#1030, #1043).
 *
 * 기본은 받은 편지 목록과 상세를 메모리에 보관한다. 실패·경합·스크립트 응답처럼 저장소
 * 상태로 표현하기 어려운 시나리오만 `onX` 람다로 갈아끼운다.
 */
class FakeReceiverTimeLetterRepository(
    var receivedLetters: ReceivedTimeLetterList = EMPTY_RECEIVED_LETTERS,
    details: Map<Long, ReceivedTimeLetter> = emptyMap(),
    var onGetReceivedTimeLetters: (suspend () -> ReceivedTimeLetterList)? = null,
    var onGetReceivedTimeLetterDetail: (suspend (Long) -> ReceivedTimeLetter)? = null,
) : ReceiverTimeLetterRepository {
    val details = ConcurrentHashMap(details)
    val requestedDetailIds = CopyOnWriteArrayList<Long>()

    private val listCounter = AtomicInteger()

    val getReceivedTimeLettersCalls: Int
        get() = listCounter.get()

    override suspend fun getReceivedTimeLetters(): ReceivedTimeLetterList {
        listCounter.incrementAndGet()
        onGetReceivedTimeLetters?.let { return it() }
        return receivedLetters
    }

    override suspend fun getReceivedTimeLetterDetail(timeLetterReceiverId: Long): ReceivedTimeLetter {
        requestedDetailIds += timeLetterReceiverId
        onGetReceivedTimeLetterDetail?.let { return it(timeLetterReceiverId) }
        return requireNotNull(details[timeLetterReceiverId]) {
            "받은 타임레터 상세가 없다: timeLetterReceiverId=$timeLetterReceiverId"
        }
    }

    companion object {
        private val EMPTY_RECEIVED_LETTERS = ReceivedTimeLetterList(emptyList(), 0)

        /** 모든 호출을 닫고 테스트가 실제로 쓰는 경로만 `onX` 로 연다. */
        fun strict(): FakeReceiverTimeLetterRepository =
            FakeReceiverTimeLetterRepository(
                onGetReceivedTimeLetters = {
                    unexpectedCall("ReceiverTimeLetterRepository.getReceivedTimeLetters")
                },
                onGetReceivedTimeLetterDetail = {
                    unexpectedCall("ReceiverTimeLetterRepository.getReceivedTimeLetterDetail")
                },
            )
    }
}
