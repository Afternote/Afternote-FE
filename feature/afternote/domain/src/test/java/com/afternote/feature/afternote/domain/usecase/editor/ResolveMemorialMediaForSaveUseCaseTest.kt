package com.afternote.feature.afternote.domain.usecase.editor

import com.afternote.feature.afternote.domain.repository.author.MemorialPhotoUploadRepository
import com.afternote.feature.afternote.domain.repository.author.MemorialVideoUploadRepository
import com.afternote.feature.afternote.domain.repository.author.PhotoUploadOutcome
import com.afternote.feature.afternote.domain.repository.author.VideoUploadOutcome
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [ResolveMemorialMediaForSaveUseCase] 비즈니스 로직 회귀 가드.
 *
 * 검증 핵심:
 * 1. 입력 인자를 각 Repository 에 그대로 전달하는지 (영상=URL 1개, 사진=existingUrl/pickedUri 2개)
 * 2. sealed [VideoUploadOutcome]/[PhotoUploadOutcome] 분기를 저장 페이로드 URL 로 매핑하는지
 *    (Empty→null, Existing→url, FreshlyUploaded→url)
 * 3. 영상 resolve 실패면 [MemorialVideoSaveException] 으로 wrap 하고 **사진 Repository 는 호출하지 않은 채**
 *    short-circuit 하는지 (non-local return)
 * 4. 사진 resolve 실패면 [MemorialPhotoSaveException] 으로 wrap 하고, 두 경우 모두 원본 예외를 cause 로 보존하는지
 *
 * 외부 라이브러리(mockk 등) 없이 호출 인자/횟수를 기록하는 직접 작성 fake를 사용한다.
 */
class ResolveMemorialMediaForSaveUseCaseTest {
    @Test
    fun `영상-사진 모두 성공 - FreshlyUploaded 영상과 Existing 사진을 각 URL 로 매핑`() {
        val videoRepo = FakeVideoUploadRepository().apply { result = Result.success(VideoUploadOutcome.FreshlyUploaded("v-url")) }
        val photoRepo = FakePhotoUploadRepository().apply { result = Result.success(PhotoUploadOutcome.Existing("p-url")) }

        val result =
            runBlocking {
                ResolveMemorialMediaForSaveUseCase(videoRepo, photoRepo)(
                    funeralVideoUrl = "content://video",
                    memorialPhotoUrl = "https://cdn/photo.jpg",
                    pickedMemorialPhotoUri = null,
                )
            }

        assertTrue(result.isSuccess)
        assertEquals("v-url", result.getOrThrow().resolvedVideoUrl)
        assertEquals("p-url", result.getOrThrow().resolvedMemorialPhotoUrl)
    }

    @Test
    fun `Empty Outcome 은 null 로 매핑 - 미첨부`() {
        val videoRepo = FakeVideoUploadRepository().apply { result = Result.success(VideoUploadOutcome.Empty) }
        val photoRepo = FakePhotoUploadRepository().apply { result = Result.success(PhotoUploadOutcome.Empty) }

        val resolved =
            runBlocking {
                ResolveMemorialMediaForSaveUseCase(videoRepo, photoRepo)(null, null, null)
            }.getOrThrow()

        assertNull(resolved.resolvedVideoUrl)
        assertNull(resolved.resolvedMemorialPhotoUrl)
    }

    @Test
    fun `Existing 영상과 FreshlyUploaded 사진도 각 URL 로 매핑`() {
        val videoRepo = FakeVideoUploadRepository().apply { result = Result.success(VideoUploadOutcome.Existing("existing-v")) }
        val photoRepo = FakePhotoUploadRepository().apply { result = Result.success(PhotoUploadOutcome.FreshlyUploaded("fresh-p")) }

        val resolved =
            runBlocking {
                ResolveMemorialMediaForSaveUseCase(videoRepo, photoRepo)(null, null, null)
            }.getOrThrow()

        assertEquals("existing-v", resolved.resolvedVideoUrl)
        assertEquals("fresh-p", resolved.resolvedMemorialPhotoUrl)
    }

    @Test
    fun `입력 인자를 각 Repository 에 그대로 전달`() {
        val videoRepo = FakeVideoUploadRepository()
        val photoRepo = FakePhotoUploadRepository()

        runBlocking {
            ResolveMemorialMediaForSaveUseCase(videoRepo, photoRepo)(
                funeralVideoUrl = "fv",
                memorialPhotoUrl = "mp",
                pickedMemorialPhotoUri = "pick",
            )
        }

        assertEquals("fv", videoRepo.resolveVideoArg)
        assertEquals("mp" to "pick", photoRepo.resolvePhotoArgs)
    }

    @Test
    fun `영상 resolve 실패면 MemorialVideoSaveException 으로 wrap 하고 사진 Repository 는 호출하지 않음`() {
        val videoError = IllegalStateException("video resolve failed")
        val videoRepo = FakeVideoUploadRepository().apply { result = Result.failure(videoError) }
        val photoRepo = FakePhotoUploadRepository()

        val result =
            runBlocking {
                ResolveMemorialMediaForSaveUseCase(videoRepo, photoRepo)("v", "p", null)
            }

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is MemorialVideoSaveException)
        assertSame(videoError, result.exceptionOrNull()?.cause) // 원본 예외를 cause 로 보존
        assertEquals(0, photoRepo.callCount) // short-circuit — 사진 Repository 미호출
    }

    @Test
    fun `사진 resolve 실패면 MemorialPhotoSaveException 으로 wrap`() {
        val photoError = IllegalStateException("photo resolve failed")
        val videoRepo = FakeVideoUploadRepository().apply { result = Result.success(VideoUploadOutcome.Empty) }
        val photoRepo = FakePhotoUploadRepository().apply { result = Result.failure(photoError) }

        val result =
            runBlocking {
                ResolveMemorialMediaForSaveUseCase(videoRepo, photoRepo)("v", "p", null)
            }

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is MemorialPhotoSaveException)
        assertSame(photoError, result.exceptionOrNull()?.cause)
    }

    private class FakeVideoUploadRepository : MemorialVideoUploadRepository {
        var resolveVideoArg: String? = null
        var callCount = 0
        var result: Result<VideoUploadOutcome> = Result.success(VideoUploadOutcome.Empty)

        override suspend fun resolveVideo(input: String?): Result<VideoUploadOutcome> {
            callCount++
            resolveVideoArg = input
            return result
        }
    }

    private class FakePhotoUploadRepository : MemorialPhotoUploadRepository {
        var resolvePhotoArgs: Pair<String?, String?>? = null
        var callCount = 0
        var result: Result<PhotoUploadOutcome> = Result.success(PhotoUploadOutcome.Empty)

        override suspend fun resolvePhoto(
            existingUrl: String?,
            pickedUri: String?,
        ): Result<PhotoUploadOutcome> {
            callCount++
            resolvePhotoArgs = existingUrl to pickedUri
            return result
        }
    }
}
