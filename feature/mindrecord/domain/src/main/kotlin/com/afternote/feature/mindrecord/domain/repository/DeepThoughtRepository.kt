package com.afternote.feature.mindrecord.domain.repository

import com.afternote.feature.mindrecord.domain.model.DeepThought

interface DeepThoughtRepository {
    /**
     * 깊은 생각 목록.
     *
     * @param draftOnly true 면 임시저장만, null 이면 서버 기본(임시저장 제외).
     */
    suspend fun getList(draftOnly: Boolean? = null): Result<List<DeepThought>>
}
