package com.afternote.feature.mindrecord.domain.repository

import com.afternote.feature.mindrecord.domain.model.MindRecordDetail
import com.afternote.feature.mindrecord.domain.model.MindRecordList

interface MindRecordReceiverRepository {
    suspend fun getList(): Result<MindRecordList>

    suspend fun getDetail(id: Long): Result<MindRecordDetail>
}
