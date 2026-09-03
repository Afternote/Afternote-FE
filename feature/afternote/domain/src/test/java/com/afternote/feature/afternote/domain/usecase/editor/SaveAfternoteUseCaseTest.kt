package com.afternote.feature.afternote.domain.usecase.editor

import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.domain.model.author.AfternoteUpdatePayload
import com.afternote.feature.afternote.domain.model.author.CreateAccountPayload
import com.afternote.feature.afternote.domain.model.author.CreateAfternoteInput
import com.afternote.feature.afternote.domain.model.author.CreateGalleryPayload
import com.afternote.feature.afternote.domain.model.author.CreateMemorialPayload
import com.afternote.feature.afternote.domain.model.author.MemorialWritePayload
import com.afternote.feature.afternote.domain.model.author.SaveAfternoteCommand
import com.afternote.feature.afternote.domain.testing.FakeAfternoteRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [SaveAfternoteUseCase] 가 소유하는 것 — [SaveAfternoteCommand] 5갈래의 해석 경계.
 *
 * 고정하는 계약:
 * 1. Create 4종(Social·Business·Gallery·Memorial)이 각각 *정확히 하나의* Repository 메서드로만 간다.
 *    (기록 목록을 전부 세어, 목표 외 메서드가 0회임을 같이 단언한다 — 라우팅이 옆칸으로 새는 회귀를 잡는다)
 * 2. payload 를 가공하지 않고 그대로 넘긴다. Update 는 id 와 payload 를 둘 다 바꾸지 않는다.
 * 3. Repository 의 성공·실패 [Result] 를 감싸지 않고 그대로 돌려준다.
 * 4. `runCatching` 을 두르지 않아 코루틴 취소를 [Result.failure] 로 삼키지 않는다.
 */
class SaveAfternoteUseCaseTest {
    private val accountPayload =
        CreateAccountPayload(
            title = "인스타그램",
            processingMethods = listOf("계정 삭제"),
        )

    private val galleryPayload =
        CreateGalleryPayload(
            title = "사진첩",
            processingMethods = listOf("전달"),
        )

    private val memorialPayload =
        CreateMemorialPayload(
            title = "장례식",
            memorial =
                MemorialWritePayload(
                    memorialPhotoUrl = null,
                    songs = emptyList(),
                    memorialVideo = null,
                ),
        )

    @Test
    fun `Social 생성은 createSocial 만 호출하고 payload 를 그대로 넘긴다`() {
        val repository = FakeAfternoteRepository()

        val result = repository.save(SaveAfternoteCommand.Create(CreateAfternoteInput.Social(accountPayload)))

        assertTrue(result.isSuccess)
        assertEquals(listOf(accountPayload), repository.socialPayloads)
        repository.assertOnlyCalled(SOCIAL)
    }

    @Test
    fun `Business 생성은 createBusiness 만 호출한다 - 같은 스키마의 createSocial 로 새지 않는다`() {
        val repository = FakeAfternoteRepository()

        val result = repository.save(SaveAfternoteCommand.Create(CreateAfternoteInput.Business(accountPayload)))

        assertTrue(result.isSuccess)
        assertEquals(listOf(accountPayload), repository.businessPayloads)
        repository.assertOnlyCalled(BUSINESS)
    }

    @Test
    fun `Gallery 생성은 createGallery 만 호출한다`() {
        val repository = FakeAfternoteRepository()

        val result = repository.save(SaveAfternoteCommand.Create(CreateAfternoteInput.Gallery(galleryPayload)))

        assertTrue(result.isSuccess)
        assertEquals(listOf(galleryPayload), repository.galleryPayloads)
        repository.assertOnlyCalled(GALLERY)
    }

    @Test
    fun `Memorial 생성은 createMemorial 만 호출한다`() {
        val repository = FakeAfternoteRepository()

        val result = repository.save(SaveAfternoteCommand.Create(CreateAfternoteInput.Memorial(memorialPayload)))

        assertTrue(result.isSuccess)
        assertEquals(listOf(memorialPayload), repository.memorialPayloads)
        repository.assertOnlyCalled(MEMORIAL)
    }

    @Test
    fun `Update 는 id 와 payload 를 바꾸지 않고 update 로만 보낸다`() {
        val repository = FakeAfternoteRepository()
        val payload = AfternoteUpdatePayload(type = AfternoteType.SOCIAL_NETWORK, title = "바뀐 제목")

        // 기본 update 는 대상이 없으면 실패하므로 성공 Result 를 열어 라우팅만 본다.
        repository.onUpdate = { _, _ -> Result.success(42L) }
        val result = repository.save(SaveAfternoteCommand.Update(id = 42L, payload = payload))

        assertEquals(42L, result.getOrNull())
        assertEquals(listOf(42L to payload), repository.updateCalls)
        repository.assertOnlyCalled(UPDATE)
    }

    @Test
    fun `Repository 성공 id 를 그대로 돌려준다`() {
        val repository = FakeAfternoteRepository().apply { onCreateGallery = { Result.success(7L) } }

        val result = repository.save(SaveAfternoteCommand.Create(CreateAfternoteInput.Gallery(galleryPayload)))

        assertEquals(7L, result.getOrNull())
    }

    @Test
    fun `Repository 실패는 감싸지 않고 같은 예외로 돌려준다`() {
        val failure = IllegalStateException("저장 실패")
        val repository = FakeAfternoteRepository().apply { onCreateSocial = { Result.failure(failure) } }

        val result = repository.save(SaveAfternoteCommand.Create(CreateAfternoteInput.Social(accountPayload)))

        assertTrue(result.isFailure)
        assertSame(failure, result.exceptionOrNull())
    }

    @Test
    fun `취소는 실패 Result 로 삼키지 않고 그대로 전파한다`() {
        val repository =
            FakeAfternoteRepository().apply {
                onCreateMemorial = { throw CancellationException("저장 중 화면 이탈") }
            }

        // 삼켰다면 Result.failure 로 정상 반환돼 assertThrows 가 실패한다.
        assertThrows(CancellationException::class.java) {
            repository.save(SaveAfternoteCommand.Create(CreateAfternoteInput.Memorial(memorialPayload)))
        }
    }

    private fun FakeAfternoteRepository.save(command: SaveAfternoteCommand): Result<Long> =
        runBlocking { SaveAfternoteUseCase(this@save)(command) }

    /** [only] 로 지목한 저장 메서드만 1회 호출되고 나머지 4개는 0회임을 단언한다. */
    private fun FakeAfternoteRepository.assertOnlyCalled(only: String) {
        val calls =
            mapOf(
                SOCIAL to socialPayloads.size,
                BUSINESS to businessPayloads.size,
                GALLERY to galleryPayloads.size,
                MEMORIAL to memorialPayloads.size,
                UPDATE to updateCalls.size,
            )
        assertEquals(calls.keys.associateWith { if (it == only) 1 else 0 }, calls)
    }

    private companion object {
        const val SOCIAL = "createSocial"
        const val BUSINESS = "createBusiness"
        const val GALLERY = "createGallery"
        const val MEMORIAL = "createMemorial"
        const val UPDATE = "update"
    }
}
