package com.afternote.feature.afternote.data.paging

import androidx.paging.PagingSource
import com.afternote.core.network.model.BaseResponse
import com.afternote.feature.afternote.data.dto.AfternoteCreateAccountRequestDto
import com.afternote.feature.afternote.data.dto.AfternoteCreateGalleryRequestDto
import com.afternote.feature.afternote.data.dto.AfternoteCreatePlaylistRequestDto
import com.afternote.feature.afternote.data.dto.AfternoteDetailDto
import com.afternote.feature.afternote.data.dto.AfternoteIdDto
import com.afternote.feature.afternote.data.dto.AfternoteListItemDto
import com.afternote.feature.afternote.data.dto.AfternotePageDto
import com.afternote.feature.afternote.data.dto.AfternoteUpdateRequestDto
import com.afternote.feature.afternote.data.service.AfternoteApiService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 서버는 한 요청에 발행분과 임시저장을 섞어 주지 않는다 — `draftOnly` 미전송이 곧 «발행분만» 이다
 * (BE `AfternoteService.getAfternotes`). 그 계약을 와이어에서 지키는지 가드한다.
 */
class AfternotePagingSourceTest {
    @Test
    fun `발행 목록은 draftOnly 를 아예 보내지 않는다`() =
        runBlocking {
            val api = RecordingApi()

            AfternotePagingSource(api, category = null, draftOnly = false).load(refresh())

            assertNull(api.lastDraftOnly)
        }

    @Test
    fun `임시저장 목록은 draftOnly 를 true 로 보낸다`() =
        runBlocking {
            val api = RecordingApi()

            AfternotePagingSource(api, category = "PLAYLIST", draftOnly = true).load(refresh())

            assertEquals(true, api.lastDraftOnly)
            assertEquals("PLAYLIST", api.lastCategory)
        }

    @Test
    fun `응답의 isDraft 는 목록 항목에 실린다`() =
        runBlocking {
            val api = RecordingApi(isDraft = true)

            val page = AfternotePagingSource(api, category = null, draftOnly = true).load(refresh())

            val items = (page as PagingSource.LoadResult.Page).data
            assertTrue(items.single().isDraft)
        }

    private fun refresh() = PagingSource.LoadParams.Refresh<Int>(key = null, loadSize = 10, placeholdersEnabled = false)

    private class RecordingApi(
        private val isDraft: Boolean = false,
    ) : AfternoteApiService {
        var lastDraftOnly: Boolean? = null
        var lastCategory: String? = null

        override suspend fun getAfternotes(
            category: String?,
            pageNumber: Int?,
            size: Int?,
            draftOnly: Boolean?,
        ): BaseResponse<AfternotePageDto> {
            lastCategory = category
            lastDraftOnly = draftOnly
            return BaseResponse(
                status = 200,
                code = 200,
                data =
                    AfternotePageDto(
                        content =
                            listOf(
                                AfternoteListItemDto(
                                    afternoteId = 1L,
                                    title = "t",
                                    category = "PLAYLIST",
                                    createdAt = "2026-08-07T06:21:14.553567",
                                    isDraft = isDraft,
                                ),
                            ),
                        page = 0,
                        size = 10,
                        hasNext = false,
                    ),
            )
        }

        override suspend fun getAfternoteDetail(afternoteId: Long): BaseResponse<AfternoteDetailDto> = error("호출되지 않는다")

        override suspend fun createAfternoteAccount(request: AfternoteCreateAccountRequestDto): BaseResponse<AfternoteIdDto> =
            error("호출되지 않는다")

        override suspend fun createAfternoteGallery(request: AfternoteCreateGalleryRequestDto): BaseResponse<AfternoteIdDto> =
            error("호출되지 않는다")

        override suspend fun createAfternotePlaylist(request: AfternoteCreatePlaylistRequestDto): BaseResponse<AfternoteIdDto> =
            error("호출되지 않는다")

        override suspend fun updateAfternote(
            afternoteId: Long,
            request: AfternoteUpdateRequestDto,
        ): BaseResponse<AfternoteIdDto> = error("호출되지 않는다")

        override suspend fun deleteAfternote(afternoteId: Long): BaseResponse<Unit> = error("호출되지 않는다")
    }
}
