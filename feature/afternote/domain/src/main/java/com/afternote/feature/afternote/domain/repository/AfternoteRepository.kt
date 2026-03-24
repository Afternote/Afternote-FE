package com.afternote.feature.afternote.domain.repository

import com.afternote.feature.afternote.domain.model.Detail
import com.afternote.feature.afternote.domain.model.ListPage
import com.afternote.feature.afternote.domain.model.input.CreateGalleryInput
import com.afternote.feature.afternote.domain.model.input.CreatePlaylistInput
import com.afternote.feature.afternote.domain.model.input.CreateSocialInput
import com.afternote.feature.afternote.domain.model.input.GetListPageInput
import com.afternote.feature.afternote.domain.model.input.UpdateInput

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
    suspend fun getListPage(input: GetListPageInput): Result<ListPage>

    suspend fun createSocial(input: CreateSocialInput): Result<Long>

    suspend fun createGallery(input: CreateGalleryInput): Result<Long>

    suspend fun getDetail(id: Long): Result<Detail>

    suspend fun createPlaylist(input: CreatePlaylistInput): Result<Long>

    suspend fun update(
        id: Long,
        input: UpdateInput,
    ): Result<Long>

    suspend fun delete(id: Long): Result<Unit>
}
