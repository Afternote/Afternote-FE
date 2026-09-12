package com.afternote.feature.timeletter.domain.testing

import com.afternote.feature.timeletter.domain.model.NewTimeLetterBlock
import com.afternote.feature.timeletter.domain.model.TimeLetter
import com.afternote.feature.timeletter.domain.model.TimeLetterBlock
import com.afternote.feature.timeletter.domain.model.TimeLetterDeliveryMode
import com.afternote.feature.timeletter.domain.model.TimeLetterList
import com.afternote.feature.timeletter.domain.model.TimeLetterStatus
import com.afternote.feature.timeletter.domain.repository.TimeLetterRepository
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * [TimeLetterRepository] fake 정본 (#1030, #1043).
 *
 * 기본은 보낸 편지·임시저장·상세를 메모리에 보관하고 모든 호출을 기록한다. 실패·경합·
 * 스크립트 응답처럼 저장소 상태로 표현하기 어려운 시나리오만 `onX` 람다로 갈아끼운다.
 */
class FakeTimeLetterRepository(
    registeredLetters: TimeLetterList = EMPTY_TIME_LETTERS,
    temporaryLetters: TimeLetterList = EMPTY_TIME_LETTERS,
    details: Map<Long, TimeLetter> =
        (registeredLetters.timeLetters + temporaryLetters.timeLetters).associateBy(TimeLetter::id),
    nextId: Long =
        (registeredLetters.timeLetters + temporaryLetters.timeLetters)
            .maxOfOrNull(TimeLetter::id)
            ?.plus(1L) ?: 1L,
    var onGetTimeLetters: (suspend () -> TimeLetterList)? = null,
    var onGetTemporaryTimeLetters: (suspend () -> TimeLetterList)? = null,
    var onGetTimeLetter: (suspend (Long) -> TimeLetter)? = null,
    var onCreateTimeLetter: (suspend (CreateCall) -> TimeLetter)? = null,
    var onUpdateTimeLetter: (suspend (UpdateCall) -> TimeLetter)? = null,
    var onDeleteTimeLetters: (suspend (List<Long>) -> Unit)? = null,
    var onDeleteAllTemporary: (suspend () -> Unit)? = null,
) : TimeLetterRepository {
    @Volatile
    var registeredLetters: TimeLetterList = registeredLetters

    @Volatile
    var temporaryLetters: TimeLetterList = temporaryLetters

    val details = ConcurrentHashMap(details)
    val requestedDetailIds = CopyOnWriteArrayList<Long>()
    val createCalls = CopyOnWriteArrayList<CreateCall>()
    val updateCalls = CopyOnWriteArrayList<UpdateCall>()
    val deleteCalls = CopyOnWriteArrayList<List<Long>>()

    private val idCounter = AtomicLong(nextId)
    private val registeredListCounter = AtomicInteger()
    private val temporaryListCounter = AtomicInteger()
    private val deleteAllTemporaryCounter = AtomicInteger()

    val getTimeLettersCalls: Int
        get() = registeredListCounter.get()

    val getTemporaryTimeLettersCalls: Int
        get() = temporaryListCounter.get()

    val deleteAllTemporaryCalls: Int
        get() = deleteAllTemporaryCounter.get()

    data class CreateCall(
        val title: String?,
        val blocks: List<NewTimeLetterBlock>,
        val sendAt: String?,
        val deliveryMode: TimeLetterDeliveryMode,
        val status: TimeLetterStatus,
        val receiverIds: List<Long>,
    )

    data class UpdateCall(
        val timeLetterId: Long,
        val title: String?,
        val blocks: List<NewTimeLetterBlock>,
        val sendAt: String?,
        val deliveryMode: TimeLetterDeliveryMode?,
        val status: TimeLetterStatus?,
    )

    override suspend fun getTimeLetters(): TimeLetterList {
        registeredListCounter.incrementAndGet()
        onGetTimeLetters?.let { return it() }
        return registeredLetters
    }

    override suspend fun getTemporaryTimeLetters(): TimeLetterList {
        temporaryListCounter.incrementAndGet()
        onGetTemporaryTimeLetters?.let { return it() }
        return temporaryLetters
    }

    override suspend fun getTimeLetter(timeLetterId: Long): TimeLetter {
        requestedDetailIds += timeLetterId
        onGetTimeLetter?.let { return it(timeLetterId) }
        return requireNotNull(details[timeLetterId]) { "타임레터 상세가 없다: timeLetterId=$timeLetterId" }
    }

    override suspend fun createTimeLetter(
        title: String?,
        blocks: List<NewTimeLetterBlock>,
        sendAt: String?,
        deliveryMode: TimeLetterDeliveryMode,
        status: TimeLetterStatus,
        receiverIds: List<Long>,
    ): TimeLetter {
        val call = CreateCall(title, blocks, sendAt, deliveryMode, status, receiverIds)
        createCalls += call
        onCreateTimeLetter?.let { return it(call) }

        val id = idCounter.getAndIncrement()
        val letter =
            TimeLetter(
                id = id,
                title = title,
                sendAt = sendAt,
                status = status,
                blocks = blocks.mapIndexed { index, block -> block.toStoredBlock(id, index) },
                receiverIds = receiverIds,
            )
        details[id] = letter
        if (status == TimeLetterStatus.DRAFT) {
            temporaryLetters = temporaryLetters.withAdded(letter)
        } else {
            registeredLetters = registeredLetters.withAdded(letter)
        }
        return letter
    }

    override suspend fun updateTimeLetter(
        timeLetterId: Long,
        title: String?,
        blocks: List<NewTimeLetterBlock>,
        sendAt: String?,
        deliveryMode: TimeLetterDeliveryMode?,
        status: TimeLetterStatus?,
    ): TimeLetter {
        val call = UpdateCall(timeLetterId, title, blocks, sendAt, deliveryMode, status)
        updateCalls += call
        onUpdateTimeLetter?.let { return it(call) }

        val existing = requireNotNull(details[timeLetterId]) { "수정할 타임레터가 없다: timeLetterId=$timeLetterId" }
        val updated =
            existing.copy(
                title = title,
                blocks = blocks.mapIndexed { index, block -> block.toStoredBlock(timeLetterId, index) },
                sendAt = sendAt,
                status = status ?: existing.status,
            )
        details[timeLetterId] = updated
        val wasListed =
            registeredLetters.timeLetters.any { it.id == timeLetterId } ||
                temporaryLetters.timeLetters.any { it.id == timeLetterId }
        if (wasListed) {
            registeredLetters = registeredLetters.without(listOf(timeLetterId))
            temporaryLetters = temporaryLetters.without(listOf(timeLetterId))
            if (updated.status == TimeLetterStatus.DRAFT) {
                temporaryLetters = temporaryLetters.withAdded(updated)
            } else {
                registeredLetters = registeredLetters.withAdded(updated)
            }
        }
        return updated
    }

    override suspend fun deleteTimeLetters(timeLetterIds: List<Long>) {
        deleteCalls += timeLetterIds
        onDeleteTimeLetters?.let { return it(timeLetterIds) }
        timeLetterIds.forEach(details::remove)
        registeredLetters = registeredLetters.without(timeLetterIds)
        temporaryLetters = temporaryLetters.without(timeLetterIds)
    }

    override suspend fun deleteAllTemporary() {
        deleteAllTemporaryCounter.incrementAndGet()
        onDeleteAllTemporary?.let { return it() }
        temporaryLetters.timeLetters.forEach { details.remove(it.id) }
        temporaryLetters = EMPTY_TIME_LETTERS
    }

    companion object {
        private val EMPTY_TIME_LETTERS = TimeLetterList(emptyList(), 0)

        /** 모든 호출을 닫고 테스트가 실제로 쓰는 경로만 `onX` 로 연다. */
        fun strict(): FakeTimeLetterRepository =
            FakeTimeLetterRepository(
                onGetTimeLetters = { unexpectedCall("TimeLetterRepository.getTimeLetters") },
                onGetTemporaryTimeLetters = {
                    unexpectedCall("TimeLetterRepository.getTemporaryTimeLetters")
                },
                onGetTimeLetter = { unexpectedCall("TimeLetterRepository.getTimeLetter") },
                onCreateTimeLetter = { unexpectedCall("TimeLetterRepository.createTimeLetter") },
                onUpdateTimeLetter = { unexpectedCall("TimeLetterRepository.updateTimeLetter") },
                onDeleteTimeLetters = { unexpectedCall("TimeLetterRepository.deleteTimeLetters") },
                onDeleteAllTemporary = { unexpectedCall("TimeLetterRepository.deleteAllTemporary") },
            )
    }
}

private fun NewTimeLetterBlock.toStoredBlock(
    timeLetterId: Long,
    index: Int,
): TimeLetterBlock =
    TimeLetterBlock(
        id = timeLetterId * 1_000 + index,
        blockType = blockType,
        blockOrder = blockOrder,
        textContent = textContent,
        url = url,
        mimeType = mimeType,
    )

private fun TimeLetterList.withAdded(letter: TimeLetter): TimeLetterList {
    val updated = timeLetters + letter
    return copy(timeLetters = updated, totalCount = updated.size)
}

private fun TimeLetterList.without(ids: List<Long>): TimeLetterList {
    val updated = timeLetters.filterNot { it.id in ids }
    return copy(timeLetters = updated, totalCount = updated.size)
}
