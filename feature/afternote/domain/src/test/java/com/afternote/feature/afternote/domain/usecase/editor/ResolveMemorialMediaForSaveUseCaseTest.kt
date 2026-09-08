package com.afternote.feature.afternote.domain.usecase.editor

import com.afternote.feature.afternote.domain.error.AfternoteFailure
import com.afternote.feature.afternote.domain.repository.author.MediaInput
import com.afternote.feature.afternote.domain.repository.author.MediaKind
import com.afternote.feature.afternote.domain.repository.author.MemorialMediaUploadRepository
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
 * 1. 입력 [MediaInput] 과 [MediaKind] 를 Repository 에 그대로 전달하는지 (로컬/원격 확정은 호출부 책임)
 * 2. 매체별 resolve 결과를 서로 뒤바꾸지 않고 각 필드에 담는지 (null 은 미첨부)
 * 3. 영상 resolve 실패면 [AfternoteFailure.MediaSave] (media=VIDEO) 로 wrap 하고 **사진·음성은 resolve 하지 않은 채**
 *    short-circuit 하는지 (non-local return)
 * 4. 사진·음성 resolve 실패면 각각 media=PHOTO·AUDIO 로 wrap 하고, 모든 경우 원본 예외를 cause 로 보존하는지
 *
 * 외부 라이브러리(mockk 등) 없이 호출 인자/횟수를 기록하는 직접 작성 fake를 사용한다.
 */
class ResolveMemorialMediaForSaveUseCaseTest {
    @Test
    fun `영상-사진-음성 모두 성공 - 각 결과 URL 을 제 필드에 매핑`() {
        val repo =
            FakeMemorialMediaUploadRepository().apply {
                videoResult = Result.success("v-url")
                photoResult = Result.success("p-url")
                audioResult = Result.success("a-url")
            }

        val result =
            runBlocking {
                ResolveMemorialMediaForSaveUseCase(repo)(
                    video = MediaInput.Local("content://video"),
                    photo = MediaInput.Remote("https://cdn/photo.jpg"),
                    audio = MediaInput.Local("content://audio"),
                )
            }

        assertTrue(result.isSuccess)
        assertEquals("v-url", result.getOrThrow().resolvedVideoUrl)
        assertEquals("p-url", result.getOrThrow().resolvedMemorialPhotoUrl)
        assertEquals("a-url", result.getOrThrow().resolvedMemorialAudioUrl)
    }

    @Test
    fun `null 결과는 null 로 매핑 - 미첨부`() {
        val repo = FakeMemorialMediaUploadRepository()

        val resolved =
            runBlocking {
                ResolveMemorialMediaForSaveUseCase(repo)(MediaInput.None, MediaInput.None, MediaInput.None)
            }.getOrThrow()

        assertNull(resolved.resolvedVideoUrl)
        assertNull(resolved.resolvedMemorialPhotoUrl)
        assertNull(resolved.resolvedMemorialAudioUrl)
    }

    @Test
    fun `원격 영상과 로컬 사진 조합도 각 URL 로 매핑`() {
        val repo =
            FakeMemorialMediaUploadRepository().apply {
                videoResult = Result.success("existing-v")
                photoResult = Result.success("fresh-p")
            }

        val resolved =
            runBlocking {
                ResolveMemorialMediaForSaveUseCase(repo)(
                    video = MediaInput.Remote("https://cdn/existing-v.mp4"),
                    photo = MediaInput.Local("content://fresh-photo"),
                    audio = MediaInput.None,
                )
            }.getOrThrow()

        assertEquals("existing-v", resolved.resolvedVideoUrl)
        assertEquals("fresh-p", resolved.resolvedMemorialPhotoUrl)
        assertNull(resolved.resolvedMemorialAudioUrl)
    }

    @Test
    fun `수정 진입 음성은 원격 URL 그대로 다시 실린다`() {
        val repo = FakeMemorialMediaUploadRepository().apply { audioResult = Result.success("https://cdn/kept.m4a") }

        val resolved =
            runBlocking {
                ResolveMemorialMediaForSaveUseCase(repo)(
                    video = MediaInput.None,
                    photo = MediaInput.None,
                    audio = MediaInput.Remote("https://cdn/kept.m4a"),
                )
            }.getOrThrow()

        assertEquals("https://cdn/kept.m4a", resolved.resolvedMemorialAudioUrl)
    }

    @Test
    fun `입력 MediaInput 과 MediaKind 를 Repository 에 그대로 전달`() {
        val repo = FakeMemorialMediaUploadRepository()

        val videoInput = MediaInput.Local("content://fv")
        val photoInput = MediaInput.Remote("https://cdn/mp.jpg")
        val audioInput = MediaInput.Local("content://fa")
        runBlocking {
            ResolveMemorialMediaForSaveUseCase(repo)(videoInput, photoInput, audioInput)
        }

        assertEquals(
            listOf(
                videoInput to MediaKind.VIDEO,
                photoInput to MediaKind.PHOTO,
                audioInput to MediaKind.AUDIO,
            ),
            repo.calls,
        )
    }

    @Test
    fun `영상 resolve 실패면 MediaSave(VIDEO) 로 wrap 하고 사진-음성은 resolve 하지 않음`() {
        val videoError = IllegalStateException("video resolve failed")
        val repo = FakeMemorialMediaUploadRepository().apply { videoResult = Result.failure(videoError) }

        val result =
            runBlocking {
                ResolveMemorialMediaForSaveUseCase(repo)(
                    MediaInput.Local("content://v"),
                    MediaInput.None,
                    MediaInput.None,
                )
            }

        assertTrue(result.isFailure)
        val videoFailure = result.exceptionOrNull()
        assertTrue(videoFailure is AfternoteFailure.MediaSave)
        assertEquals(MediaKind.VIDEO, (videoFailure as AfternoteFailure.MediaSave).media)
        assertSame(videoError, result.exceptionOrNull()?.cause) // 원본 예외를 cause 로 보존
        // short-circuit — 사진·음성 resolve 미호출
        assertEquals(0, repo.calls.count { it.second == MediaKind.PHOTO })
        assertEquals(0, repo.calls.count { it.second == MediaKind.AUDIO })
    }

    @Test
    fun `사진 resolve 실패면 MediaSave(PHOTO) 로 wrap 하고 음성은 resolve 하지 않음`() {
        val photoError = IllegalStateException("photo resolve failed")
        val repo = FakeMemorialMediaUploadRepository().apply { photoResult = Result.failure(photoError) }

        val result =
            runBlocking {
                ResolveMemorialMediaForSaveUseCase(repo)(
                    MediaInput.None,
                    MediaInput.Remote("https://cdn/p.jpg"),
                    MediaInput.Local("content://a"),
                )
            }

        assertTrue(result.isFailure)
        val photoFailure = result.exceptionOrNull()
        assertTrue(photoFailure is AfternoteFailure.MediaSave)
        assertEquals(MediaKind.PHOTO, (photoFailure as AfternoteFailure.MediaSave).media)
        assertSame(photoError, result.exceptionOrNull()?.cause)
        assertEquals(0, repo.calls.count { it.second == MediaKind.AUDIO })
    }

    @Test
    fun `음성 resolve 실패면 MediaSave(AUDIO) 로 wrap`() {
        val audioError = IllegalArgumentException("unsupported memorial audio type: audio/amr")
        val repo = FakeMemorialMediaUploadRepository().apply { audioResult = Result.failure(audioError) }

        val result =
            runBlocking {
                ResolveMemorialMediaForSaveUseCase(repo)(
                    MediaInput.None,
                    MediaInput.None,
                    MediaInput.Local("content://a"),
                )
            }

        assertTrue(result.isFailure)
        val audioFailure = result.exceptionOrNull()
        assertTrue(audioFailure is AfternoteFailure.MediaSave)
        assertEquals(MediaKind.AUDIO, (audioFailure as AfternoteFailure.MediaSave).media)
        assertSame(audioError, result.exceptionOrNull()?.cause)
    }

    private class FakeMemorialMediaUploadRepository : MemorialMediaUploadRepository {
        val calls = mutableListOf<Pair<MediaInput, MediaKind>>()
        var videoResult: Result<String?> = Result.success(null)
        var photoResult: Result<String?> = Result.success(null)
        var audioResult: Result<String?> = Result.success(null)

        override suspend fun resolve(
            input: MediaInput,
            kind: MediaKind,
        ): Result<String?> {
            calls += input to kind
            return when (kind) {
                MediaKind.VIDEO -> videoResult
                MediaKind.PHOTO -> photoResult
                MediaKind.AUDIO -> audioResult
            }
        }
    }
}
