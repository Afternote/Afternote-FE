package com.afternote.feature.afternote.domain.repository.author

import androidx.paging.PagingData
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.domain.model.author.AfternoteUpdatePayload
import com.afternote.feature.afternote.domain.model.author.CreateAccountPayload
import com.afternote.feature.afternote.domain.model.author.CreateGalleryPayload
import com.afternote.feature.afternote.domain.model.author.CreateMemorialPayload
import com.afternote.feature.afternote.domain.model.author.Detail
import com.afternote.feature.afternote.domain.model.author.DraftDetail
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

    /**
     * 임시저장만 담은 목록. 서버는 발행분과 임시저장을 한 요청에 섞어 주지 않아
     * (`draftOnly` 미전송 = 발행분만) 목록도 화면도 따로 선다.
     */
    fun getPagedDrafts(type: AfternoteType?): Flow<PagingData<ListItem>>

    /** 발행 완료 상세 — 상세 화면용. 임시저장 id 를 넘기면 필수값 부재로 실패한다([getDraftDetail] 을 쓸 것). */
    suspend fun getDetail(id: Long): Result<Detail>

    /**
     * 임시저장 상세 — 에디터 이어쓰기용. 같은 `GET /afternotes/{id}` 를 타고 응답만 관용해서 읽는다.
     *
     * 서버가 상세에서 임시저장을 걸러 내지 않고 응답 형태로만 가르기 때문에(`AfternotedetailResponse`)
     * 엔드포인트는 하나이고, 갈라지는 곳은 여는 방향이다 — 목록의 `isDraft` 로 상세 화면과 에디터를 나눈다.
     */
    suspend fun getDraftDetail(id: Long): Result<DraftDetail>

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
