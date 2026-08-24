package com.afternote.feature.mindrecord.domain.testing

import com.afternote.feature.mindrecord.domain.model.ReceiverMindRecords
import com.afternote.feature.mindrecord.domain.repository.MindRecordReceiverRepository

/**
 * [MindRecordReceiverRepository] fake 정본 (#1030).
 *
 * 기본은 [result] 를 그대로 돌려준다. 완료 시점을 테스트가 쥐어야 하는 경합 시나리오는
 * `onGetAll` 로 갈아끼운다.
 */
class FakeMindRecordReceiverRepository(
    var result: Result<ReceiverMindRecords> = Result.success(EMPTY_RECORDS),
    var onGetAll: (suspend () -> Result<ReceiverMindRecords>)? = null,
) : MindRecordReceiverRepository {
    var getAllCalls: Int = 0
        private set

    override suspend fun getAll(): Result<ReceiverMindRecords> {
        getAllCalls += 1
        onGetAll?.let { return it() }
        return result
    }

    companion object {
        private val EMPTY_RECORDS = ReceiverMindRecords(dailyQuestions = emptyList(), diaries = emptyList())
    }
}
