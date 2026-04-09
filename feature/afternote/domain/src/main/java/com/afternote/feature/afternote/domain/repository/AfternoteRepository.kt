package com.afternote.feature.afternote.domain.repository

import com.afternote.feature.afternote.domain.model.Detail
import com.afternote.feature.afternote.domain.model.ListPage
import com.afternote.feature.afternote.domain.model.author.AfternoteUpdatePayload
import com.afternote.feature.afternote.domain.model.author.CreateGalleryPayload
import com.afternote.feature.afternote.domain.model.author.CreatePlaylistPayload
import com.afternote.feature.afternote.domain.model.author.CreateSocialPayload

/**
 * Afternote 도메인 Repository 인터페이스.
 *
 * API spec 기준:
 * - GET /afternotes (목록)
 * - GET /afternotes/{afternoteId} (상세)
 * - POST /afternotes (SOCIAL / GALLERY / PLAYLIST 생성)
 * - PATCH /afternotes/{afternoteId} (수정)
 * - DELETE /afternotes/{afternoteId} (삭제)
 */
interface AfternoteRepository {
    suspend fun getListPage(
        category: String?,
        pageNumber: Int,
        size: Int,
    ): Result<ListPage>

    suspend fun createSocial(payload: CreateSocialPayload): Result<Long>

    suspend fun createGallery(payload: CreateGalleryPayload): Result<Long>

    suspend fun getDetail(id: Long): Result<Detail>

    suspend fun createPlaylist(payload: CreatePlaylistPayload): Result<Long>

    suspend fun update(
        id: Long,
        payload: AfternoteUpdatePayload,
    ): Result<Long>

    suspend fun delete(id: Long): Result<Unit>
}
