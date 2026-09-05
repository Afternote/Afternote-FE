package com.afternote.feature.receiver.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.common.result.runCatchingCancellable
import com.afternote.core.network.model.requireData
import com.afternote.feature.receiver.data.error.toReceiverFailure
import com.afternote.feature.receiver.data.mapper.toReceiverDomainList
import com.afternote.feature.receiver.data.service.ReceiverAfternoteApiService
import com.afternote.feature.receiver.domain.model.AfterNoteListItem

/**
 * 서버는 페이지네이션을 지원하지 않으므로 응답 전체를 한 페이지로 감싸서 반환한다.
 * Paging 3 API(LoadState/refresh/cachedIn) 통일을 위한 단일 페이지 구현이며,
 * 서버가 page/size를 도입하면 nextKey/getRefreshKey만 채워 넣으면 된다.
 *
 * `X-Auth-Code` 헤더는 [com.afternote.feature.receiver.data.network.ReceiverAuthInterceptor]가 부착한다.
 */
internal class ReceiverAfternotePagingSource(
    private val api: ReceiverAfternoteApiService,
    private val errorReporter: ErrorReporter,
) : PagingSource<Int, AfterNoteListItem>() {
    override fun getRefreshKey(state: PagingState<Int, AfterNoteListItem>): Int? = null

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, AfterNoteListItem> =
        // 타입 인자를 적는 건 prevKey·nextKey 가 둘 다 null 이라(단일 페이지) key 가 Nothing 으로
        // 좁혀지기 때문 — 반환 타입이 추론을 잡아 주던 try/catch 와 달리 여기선 블록이 먼저 추론된다.
        runCatchingCancellable<LoadResult<Int, AfterNoteListItem>> {
            val response = api.getReceiverAfternotes().requireData()
            LoadResult.Page(
                data = response.toReceiverDomainList(errorReporter),
                prevKey = null,
                nextKey = null,
            )
        }.getOrElse { cause ->
            // 인프라 타입을 그대로 흘리면 화면이 실패 사유를 가를 수 없어 «전달 조건 미충족»·«연결 없음»
            // 처럼 처리가 갈리는 실패까지 하나의 "다시 시도" 로 수렴한다(#611). 사유가 확인되는 것만
            // 도메인 어휘로 옮기고 나머지는 원본 그대로 둔다.
            LoadResult.Error(cause.toReceiverFailure())
        }
}
