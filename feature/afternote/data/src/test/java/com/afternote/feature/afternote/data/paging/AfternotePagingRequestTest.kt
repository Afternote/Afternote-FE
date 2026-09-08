package com.afternote.feature.afternote.data.paging

import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.afternote.core.network.di.NetworkModule
import com.afternote.feature.afternote.data.service.AfternoteApiService
import com.afternote.feature.afternote.domain.model.author.ListItem
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.CopyOnWriteArrayList

/** 실제 Retrofit query를 page × size로 해석하는 서버 응답으로 페이지 경계의 중복·누락을 검증한다. */
class AfternotePagingRequestTest {
    @Test
    fun `초기 30개와 append 10개 힌트에도 draft와 발행 목록은 같은 서버 크기로 끝까지 읽는다`() =
        runBlocking {
            for (draftOnly in listOf(false, true)) {
                val server = PageServer()
                val source = AfternotePagingSource(server.api, category = "PLAYLIST", draftOnly = draftOnly, pageSize = 10)
                val loaded = mutableListOf<Long>()
                var page = source.loadPage(PagingSource.LoadParams.Refresh(null, 30, false))
                loaded += page.data.map { it.id }
                while (page.nextKey != null) {
                    page = source.loadPage(PagingSource.LoadParams.Append(requireNotNull(page.nextKey), 10, false))
                    loaded += page.data.map { it.id }
                }

                assertEquals("항목 중복이나 누락 없이 서버 순서를 보존해야 한다", (1L..42L).toList(), loaded)
                assertEquals((0..4).map { it to 10 }, server.requests.map { it.pageAndSize() })
                assertTrue(server.requests.all { it.queryParameter("draftOnly") == draftOnly.toString() })
                assertTrue(server.requests.all { it.queryParameter("category") == "PLAYLIST" })
                assertNull(page.nextKey)
                assertTrue(page.data.all { it.isDraft == draftOnly })
            }
        }

    @Test
    fun `중간 페이지 refresh의 30개 힌트도 앞뒤 10개 페이지와 연속된다`() =
        runBlocking {
            val server = PageServer()
            val original = AfternotePagingSource(server.api, category = null, draftOnly = true, pageSize = 10)
            val page = original.loadPage(PagingSource.LoadParams.Refresh(2, 10, false))
            val state =
                PagingState(
                    pages = listOf(page),
                    anchorPosition = 5,
                    config = PagingConfig(pageSize = 10),
                    leadingPlaceholderCount = 0,
                )
            val refreshKey = original.getRefreshKey(state)
            assertEquals(2, refreshKey)

            val refreshed = AfternotePagingSource(server.api, category = null, draftOnly = true, pageSize = 10)
            val middle = refreshed.loadPage(PagingSource.LoadParams.Refresh(refreshKey, 30, false))
            assertEquals((21L..30L).toList(), middle.data.map { it.id })
            val before = refreshed.loadPage(PagingSource.LoadParams.Prepend(requireNotNull(middle.prevKey), 10, false))
            val after = refreshed.loadPage(PagingSource.LoadParams.Append(requireNotNull(middle.nextKey), 10, false))

            assertEquals((11L..40L).toList(), (before.data + middle.data + after.data).map { it.id })
            assertEquals(listOf(2 to 10, 2 to 10, 1 to 10, 3 to 10), server.requests.map { it.pageAndSize() })
            assertTrue(server.requests.all { it.queryParameter("category") == null })
        }

    private suspend fun AfternotePagingSource.loadPage(params: PagingSource.LoadParams<Int>): PagingSource.LoadResult.Page<Int, ListItem> =
        load(params) as PagingSource.LoadResult.Page<Int, ListItem>

    private fun HttpUrl.pageAndSize(): Pair<Int, Int> =
        requireNotNull(queryParameter("page")).toInt() to requireNotNull(queryParameter("size")).toInt()

    private class PageServer {
        val requests = CopyOnWriteArrayList<HttpUrl>()
        val api: AfternoteApiService =
            Retrofit
                .Builder()
                .baseUrl("https://paging.test/")
                .client(
                    OkHttpClient
                        .Builder()
                        .addInterceptor { chain ->
                            val request = chain.request()
                            val url = request.url
                            requests += url
                            val page = requireNotNull(url.queryParameter("page")).toInt()
                            val size = requireNotNull(url.queryParameter("size")).toInt()
                            val draft = requireNotNull(url.queryParameter("draftOnly")).toBooleanStrict()
                            val ids = (1L..42L).drop(page * size).take(size)
                            val content =
                                ids.joinToString(",") { id ->
                                    """{"afternoteId":$id,"title":"item $id","category":"PLAYLIST","createdAt":"2026-09-08T00:00:00","isDraft":$draft}"""
                                }
                            val body =
                                """{"status":200,"code":200,"data":{"content":[$content],"page":$page,"size":$size,"hasNext":${(page + 1) * size < 42}}}"""
                            Response
                                .Builder()
                                .request(request)
                                .protocol(Protocol.HTTP_1_1)
                                .code(200)
                                .message("OK")
                                .body(body.toResponseBody("application/json".toMediaType()))
                                .build()
                        }.build(),
                ).addConverterFactory(NetworkModule.provideJson().asConverterFactory("application/json".toMediaType()))
                .build()
                .create(AfternoteApiService::class.java)
    }
}
