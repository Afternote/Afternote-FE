package com.afternote.feature.mindrecord.domain.repository

import com.afternote.feature.mindrecord.domain.model.Diary
import com.afternote.feature.mindrecord.domain.model.DiaryCreatePayload
import com.afternote.feature.mindrecord.domain.model.DiaryUpdatePayload

interface DiaryRepository {
    suspend fun getList(
        yearMonth: String,
        draftOnly: Boolean? = null,
    ): Result<List<Diary>>

    suspend fun create(payload: DiaryCreatePayload): Result<Unit>

    suspend fun update(
        id: Long,
        payload: DiaryUpdatePayload,
    ): Result<Unit>

    suspend fun delete(id: Long): Result<Unit>
}
