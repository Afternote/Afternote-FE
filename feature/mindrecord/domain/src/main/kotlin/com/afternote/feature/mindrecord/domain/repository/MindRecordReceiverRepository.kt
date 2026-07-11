package com.afternote.feature.mindrecord.domain.repository

import com.afternote.feature.mindrecord.domain.model.ReceiverMindRecords

interface MindRecordReceiverRepository {
    /** 수신자용 데일리질문/일기/깊은생각 3개 엔드포인트를 병렬 조회해 하나로 묶는다. */
    suspend fun getAll(): Result<ReceiverMindRecords>
}
