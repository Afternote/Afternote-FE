package com.afternote.feature.afternote.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.afternote.core.network.model.requireData
import com.afternote.feature.afternote.data.mapper.toReceiverDomainList
import com.afternote.feature.afternote.data.service.ReceiverAfternoteApiService
import com.afternote.feature.afternote.domain.model.receiver.AfterNoteListItemDto
import kotlinx.coroutines.CancellationException

/**
 * 서버는 page/size/hasNext 기반 0-indexed 페이징을 사용한다 (GET /receiver-auth/afternotes).
 */
internal class ReceiverAfternotePagingSource(
    private val api: ReceiverAfternoteApiService,
    private val category: String?,
) : PagingSource<Int, AfterNoteListItemDto>() {
    override fun getRefreshKey(state: PagingState<Int, AfterNoteListItemDto>): Int? =
        state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, AfterNoteListItemDto> =
        try {
            val pageNumber = params.key ?: STARTING_PAGE_INDEX
            val response =
                api
                    .getReceiverAfternotes(
                        category = category,
                        pageNumber = pageNumber,
                        size = params.loadSize,
                    ).requireData()

            LoadResult.Page(
                data = response.content.toReceiverDomainList(),
                prevKey = if (pageNumber == STARTING_PAGE_INDEX) null else pageNumber - 1,
                nextKey = if (response.hasNext) pageNumber + 1 else null,
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LoadResult.Error(e)
        }

    private companion object {
        const val STARTING_PAGE_INDEX = 0
    }
}
