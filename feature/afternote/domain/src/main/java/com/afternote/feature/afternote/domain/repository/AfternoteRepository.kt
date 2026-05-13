package com.afternote.feature.afternote.domain.repository

import androidx.paging.PagingData
import com.afternote.feature.afternote.domain.model.author.AfternoteUpdatePayload
import com.afternote.feature.afternote.domain.model.author.CreateGalleryPayload
import com.afternote.feature.afternote.domain.model.author.CreatePlaylistPayload
import com.afternote.feature.afternote.domain.model.author.CreateSocialPayload
import com.afternote.feature.afternote.domain.model.author.Detail
import com.afternote.feature.afternote.domain.model.author.ListItem
import kotlinx.coroutines.flow.Flow

/**
 * 작성/수정 호출의 서버 검증 실패는
 * [com.afternote.feature.afternote.domain.error.AfternoteAuthoringValidationException]으로
 * Result.failure에 담겨 전달된다.
 */
interface AfternoteRepository {
    /**
     * ALL 카테고리는 null로 전달. CUD 성공 시 구현체가 PagingSource를 invalidate하므로
     * 호출자는 수동 새로고침 트리거를 관리하지 않는다.
     */
    fun getPagedAfternotes(category: String?): Flow<PagingData<ListItem>>

    suspend fun getDetail(id: Long): Result<Detail>

    suspend fun createSocial(payload: CreateSocialPayload): Result<Long>

    suspend fun createGallery(payload: CreateGalleryPayload): Result<Long>

    suspend fun createPlaylist(payload: CreatePlaylistPayload): Result<Long>

    suspend fun update(
        id: Long,
        payload: AfternoteUpdatePayload,
    ): Result<Long>

    suspend fun delete(id: Long): Result<Unit>
}
