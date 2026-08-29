package com.afternote.feature.afternote.domain.repository.author

import androidx.paging.PagingData
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.domain.model.author.AfternoteUpdatePayload
import com.afternote.feature.afternote.domain.model.author.CreateAccountPayload
import com.afternote.feature.afternote.domain.model.author.CreateGalleryPayload
import com.afternote.feature.afternote.domain.model.author.CreateMemorialPayload
import com.afternote.feature.afternote.domain.model.author.Detail
import com.afternote.feature.afternote.domain.model.author.ListItem
import kotlinx.coroutines.flow.Flow

interface AfternoteRepository {
    /**
     * 전체 목록은 [type] 을 null 로 전달. CUD 성공 시 구현체가 PagingSource를 invalidate하므로
     * 호출자는 수동 새로고침 트리거를 관리하지 않는다.
     *
     * 목록 조회가 아직 지원되지 않는 종류를 넘기면 빈 결과가 온다 — 구현체가 그 종류를 판별해
     * 요청 자체를 만들지 않는다. 호출자는 어떤 종류가 지원되는지 알 필요가 없다.
     */
    fun getPagedAfternotes(type: AfternoteType?): Flow<PagingData<ListItem>>

    suspend fun getDetail(id: Long): Result<Detail>

    suspend fun createSocial(payload: CreateAccountPayload): Result<Long>

    /** BUSINESS 생성. 바디 스키마가 SOCIAL 과 동일해 [CreateAccountPayload] 를 공유한다 (category 만 상이). */
    suspend fun createBusiness(payload: CreateAccountPayload): Result<Long>

    suspend fun createGallery(payload: CreateGalleryPayload): Result<Long>

    suspend fun createMemorial(payload: CreateMemorialPayload): Result<Long>

    suspend fun update(
        id: Long,
        payload: AfternoteUpdatePayload,
    ): Result<Long>

    suspend fun delete(id: Long): Result<Unit>
}
