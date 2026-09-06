package com.afternote.feature.afternote.data.repositoryimpl.author

import com.afternote.core.network.model.ApiException
import com.afternote.core.network.model.BaseResponse
import com.afternote.feature.afternote.data.dto.AfternoteCreateAccountRequestDto
import com.afternote.feature.afternote.data.dto.AfternoteCreateGalleryRequestDto
import com.afternote.feature.afternote.data.dto.AfternoteCreatePlaylistRequestDto
import com.afternote.feature.afternote.data.dto.AfternoteDetailDto
import com.afternote.feature.afternote.data.dto.AfternoteIdDto
import com.afternote.feature.afternote.data.dto.AfternotePageDto
import com.afternote.feature.afternote.data.dto.AfternoteUpdateRequestDto
import com.afternote.feature.afternote.data.service.AfternoteApiService
import com.afternote.feature.afternote.domain.error.AfternoteFailure
import com.afternote.feature.afternote.domain.model.author.CreateAccountPayload
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * 저장 경로의 실패 번역 계약 회귀 가드 (`mapAuthoringFailure`).
 *
 * 계약 — 전송 계층 실패([IOException])만 [AfternoteFailure.NetworkUnavailable] 로 치환해
 * presentation 이 타입으로 분기하게 하고, 서버가 응답한 실패는 원본 인스턴스를 유지해
 * 기존 일반 문구 경로로 흐르게 한다. 번역 함수가 리포지토리 파일 안 private 이므로
 * 계약은 호출부인 리포지토리를 통해 검증한다(`mapLoginFailure`·`AuthRepositoryImplTest` 와 같은 방식).
 */
class AfternoteRepositoryImplTest {
    private val payload = CreateAccountPayload(title = "제목", processingMethods = listOf("DELETE"))

    private fun repository(onCreateAccount: suspend () -> BaseResponse<AfternoteIdDto>) =
        AfternoteRepositoryImpl(FakeAfternoteApiService(onCreateAccount))

    @Test
    fun `createSocial - 서버가 응답한 실패는 그대로 흘려보낸다`() =
        runBlocking {
            val original = ApiException(status = 400, code = 400, serverMessage = null, fallbackMessage = "x")

            val result = repository { throw original }.createSocial(payload)

            assertSame(original, result.exceptionOrNull())
        }

    @Test
    fun `createSocial - 전송 계층 IO 실패는 NetworkUnavailable 로 치환한다`() =
        runBlocking {
            val result = repository { throw IOException("timeout") }.createSocial(payload)

            assertTrue(result.exceptionOrNull() is AfternoteFailure.NetworkUnavailable)
        }

    @Test
    fun `createSocial - 치환해도 원래 예외를 cause 로 남긴다`() =
        runBlocking {
            // 진단 정보를 버리지 않는다 — 화면에 쓰지 않을 뿐이다.
            val original = IOException("timeout")

            val result = repository { throw original }.createSocial(payload)

            assertSame(original, result.exceptionOrNull()?.cause)
        }

    @Test
    fun `createSocial - 서버·전송 어느 쪽도 아닌 예외는 손대지 않는다`() =
        runBlocking {
            val original = IllegalStateException("boom")

            val result = repository { throw original }.createSocial(payload)

            assertSame(original, result.exceptionOrNull())
        }

    @Test
    fun `createSocial - 성공은 그대로 통과한다`() =
        runBlocking {
            val result = repository { idResponse(afternoteId = 42L) }.createSocial(payload)

            assertEquals(42L, result.getOrNull())
        }

    @Test
    fun `getDetail - 매퍼를 거치지 않아 전송 계층 실패가 원본 그대로 올라온다`() =
        runBlocking {
            // 상세 조회는 화면 처리가 갈리는 서버 사유가 없어 번역을 붙이지 않기로 한 경로다 (#1508).
            val original = IOException("timeout")
            val repository = AfternoteRepositoryImpl(FakeAfternoteApiService(onGetDetail = { throw original }))

            assertSame(original, repository.getDetail(id = 1L).exceptionOrNull())
        }
}

private fun idResponse(afternoteId: Long) = BaseResponse(status = 200, code = 200, data = AfternoteIdDto(afternoteId = afternoteId))

private class FakeAfternoteApiService(
    private val onCreateAccount: suspend () -> BaseResponse<AfternoteIdDto> = { error("호출되지 않는다") },
    private val onGetDetail: suspend () -> BaseResponse<AfternoteDetailDto> = { error("호출되지 않는다") },
) : AfternoteApiService {
    override suspend fun getAfternotes(
        category: String?,
        pageNumber: Int?,
        size: Int?,
        draftOnly: Boolean?,
    ): BaseResponse<AfternotePageDto> = error("호출되지 않는다")

    override suspend fun getAfternoteDetail(afternoteId: Long): BaseResponse<AfternoteDetailDto> = onGetDetail()

    override suspend fun createAfternoteAccount(request: AfternoteCreateAccountRequestDto): BaseResponse<AfternoteIdDto> = onCreateAccount()

    override suspend fun createAfternoteGallery(request: AfternoteCreateGalleryRequestDto): BaseResponse<AfternoteIdDto> = error("호출되지 않는다")

    override suspend fun createAfternotePlaylist(request: AfternoteCreatePlaylistRequestDto): BaseResponse<AfternoteIdDto> =
        error("호출되지 않는다")

    override suspend fun updateAfternote(
        afternoteId: Long,
        request: AfternoteUpdateRequestDto,
    ): BaseResponse<AfternoteIdDto> = error("호출되지 않는다")

    override suspend fun deleteAfternote(afternoteId: Long): BaseResponse<Unit> = error("호출되지 않는다")
}
