package com.afternote.feature.mindrecord.domain.repository

import com.afternote.feature.mindrecord.domain.model.DeepThought
import com.afternote.feature.mindrecord.domain.model.DeepThoughtCreatePayload
import com.afternote.feature.mindrecord.domain.model.DeepThoughtUpdatePayload
import com.afternote.feature.mindrecord.domain.model.RandomDeepThought

interface DeepThoughtRepository {
    suspend fun getList(
        date: String? = null,
        tag: String? = null,
        category: String? = null,
    ): Result<List<DeepThought>>

    suspend fun getRandom(): Result<RandomDeepThought>

    suspend fun create(payload: DeepThoughtCreatePayload): Result<Unit>

    suspend fun update(
        id: Long,
        payload: DeepThoughtUpdatePayload,
    ): Result<Unit>

    suspend fun delete(id: Long): Result<Unit>
}
