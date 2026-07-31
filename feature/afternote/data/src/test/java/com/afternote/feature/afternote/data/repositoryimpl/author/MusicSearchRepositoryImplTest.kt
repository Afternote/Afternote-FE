package com.afternote.feature.afternote.data.repositoryimpl.author

import com.afternote.feature.afternote.data.service.MusicApiService
import com.afternote.feature.afternote.domain.model.author.playlist.SearchedSong
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * `search()` 의 예외 → [Result] 변환 규약 회귀 가드.
 *
 * 취소를 Result 로 삼키면 타이핑 디바운스로 이전 검색이 취소될 때마다 AddSongViewModel 의
 * onFailure 가 돌아 유령 "검색 실패"가 화면에 남는다 — 취소만은 되던져야 한다.
 */
class MusicSearchRepositoryImplTest {
    @Test
    fun `search - in-flight 취소는 Result 로 삼키지 않고 CancellationException 을 전파`() =
        runBlocking {
            val repository = MusicSearchRepositoryImpl(MusicApiService { awaitCancellation() })

            var result: Result<List<SearchedSong>>? = null
            val job = launch { result = repository.search("아이유") }
            yield() // job 이 api 호출 지점(awaitCancellation)까지 진행하도록
            job.cancel()
            job.join()

            assertNull(result) // 취소가 Result 로 둔갑했다면 non-null 로 남는다
        }

    @Test
    fun `search - 취소가 아닌 예외는 여전히 Result failure 로 변환`() =
        runBlocking {
            val repository = MusicSearchRepositoryImpl(MusicApiService { throw IOException("boom") })

            val result = repository.search("아이유")

            assertTrue(result.exceptionOrNull() is IOException)
        }
}
