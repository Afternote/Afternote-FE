package com.afternote.feature.afternote.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.afternote.core.common.result.runCatchingCancellable
import com.afternote.core.network.model.requireData
import com.afternote.feature.afternote.data.mapper.toDomainList
import com.afternote.feature.afternote.data.service.AfternoteApiService
import com.afternote.feature.afternote.domain.model.author.ListItem

/**
 * 서버는 page/size/hasNext 기반 0-indexed 페이징을 사용한다 (GET /api/v1/afternotes).
 */
internal class AfternotePagingSource(
    private val api: AfternoteApiService,
    private val category: String?,
    private val draftOnly: Boolean,
) : PagingSource<Int, ListItem>() {
    override fun getRefreshKey(state: PagingState<Int, ListItem>): Int? =
        state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ListItem> =
        runCatchingCancellable {
            val pageNumber = params.key ?: STARTING_PAGE_INDEX
            val response =
                api
                    .getAfternotes(
                        category = category,
                        pageNumber = pageNumber,
                        size = params.loadSize,
                        draftOnly = draftOnly.takeIf { it },
                    ).requireData()

            LoadResult.Page(
                data = response.content.toDomainList(),
                prevKey = if (pageNumber == STARTING_PAGE_INDEX) null else pageNumber - 1,
                nextKey = if (response.hasNext) pageNumber + 1 else null,
            )
        }.getOrElse { LoadResult.Error(it) }

    private companion object {
        const val STARTING_PAGE_INDEX = 0
    }
}
