package com.afternote.feature.afternote.domain.repository

import com.afternote.feature.afternote.domain.model.author.AfternoteUpdatePayload
import com.afternote.feature.afternote.domain.model.author.CreateGalleryPayload
import com.afternote.feature.afternote.domain.model.author.CreatePlaylistPayload
import com.afternote.feature.afternote.domain.model.author.CreateSocialPayload
import com.afternote.feature.afternote.domain.model.author.Detail
import com.afternote.feature.afternote.domain.model.author.ListPage
import kotlinx.coroutines.flow.StateFlow

/**
 * Afternote 도메인 Repository 인터페이스.
 *
 * API spec 기준:
 * - GET /afternotes (목록)
 * - GET /afternotes/{afternoteId} (상세)
 * - POST /afternotes (SOCIAL / GALLERY / PLAYLIST 생성)
 * - PATCH /afternotes/{afternoteId} (수정)
 * - DELETE /afternotes/{afternoteId} (삭제)
 *
 * POST/PATCH 실패 시 구현체는 서버 검증 응답을
 * [com.afternote.feature.afternote.domain.error.AfternoteAuthoringValidationException]으로 올릴 수 있다.
 */
interface AfternoteRepository {
    /**
     * 작성자 애프터노트 목록을 다시 가져와야 할 때마다 증가합니다 (생성·수정·삭제 성공 시).
     * UI는 이 값을 구독해 목록 화면을 새로고침할 수 있습니다.
     */
    val authorAfternoteListRevision: StateFlow<Long>

    suspend fun getListPage(
        category: String?,
        pageNumber: Int,
        size: Int,
    ): Result<ListPage>

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
