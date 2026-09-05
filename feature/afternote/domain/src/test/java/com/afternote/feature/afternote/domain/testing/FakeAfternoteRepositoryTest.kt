package com.afternote.feature.afternote.domain.testing

import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.domain.model.author.Account
import com.afternote.feature.afternote.domain.model.author.AfternoteAccountCredentials
import com.afternote.feature.afternote.domain.model.author.AfternoteUpdatePayload
import com.afternote.feature.afternote.domain.model.author.CreateAccountPayload
import com.afternote.feature.afternote.domain.model.author.CreateGalleryPayload
import com.afternote.feature.afternote.domain.model.author.CreateMemorialPayload
import com.afternote.feature.afternote.domain.model.author.Detail
import com.afternote.feature.afternote.domain.model.author.DetailContent
import com.afternote.feature.afternote.domain.model.author.DetailCredentials
import com.afternote.feature.afternote.domain.model.author.DetailReceiver
import com.afternote.feature.afternote.domain.model.author.DetailTimestamps
import com.afternote.feature.afternote.domain.model.author.ListItem
import com.afternote.feature.afternote.domain.model.author.MemorialWritePayload
import com.afternote.feature.afternote.domain.model.author.ReceiverRefPayload
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeAfternoteRepositoryTest {
    @Test
    fun `기본 update는 표현 가능한 필드만 바꾸고 기존 메타데이터를 보존한다`() =
        runBlocking {
            val item = listItem()
            val detail = detail()
            val repository =
                FakeAfternoteRepository(
                    initialItems = listOf(item),
                    initialDetails = mapOf(detail.id to detail),
                )
            val payload =
                AfternoteUpdatePayload(
                    type = AfternoteType.SOCIAL_NETWORK,
                    title = "수정 서비스",
                    processingMethods = listOf("계정 보존"),
                    credentials = AfternoteAccountCredentials(id = "new@test.local"),
                    receivers = listOf(ReceiverRefPayload(7L)),
                )

            val result = repository.update(item.id, payload)

            assertEquals(item.id, result.getOrThrow())
            assertEquals("수정 서비스", repository.items.single().serviceName)
            assertEquals("2026.08.22", repository.items.single().date)
            assertEquals(
                "new@test.local",
                repository.items
                    .single()
                    .account.id,
            )
            assertEquals(
                "old-password",
                repository.items
                    .single()
                    .account.password,
            )
            val updated = repository.details.getValue(detail.id)
            assertEquals(detail.timestamps, updated.timestamps)
            assertEquals(detail.receivers, updated.receivers)
            val updatedContent = updated.content as DetailContent.SocialNetwork
            assertEquals("new@test.local", updatedContent.credentials.id)
            assertEquals("old-password", updatedContent.credentials.password)
            assertEquals(listOf(item.id to payload), repository.updateCalls)

            val missing = repository.update(999L, payload)
            assertTrue(missing.exceptionOrNull() is NoSuchElementException)
            assertEquals(listOf(item.id to payload, 999L to payload), repository.updateCalls)

            val missingDetail = repository.getDetail(999L)
            assertTrue(missingDetail.exceptionOrNull() is NoSuchElementException)
            assertEquals(listOf(999L), repository.requestedDetailIds)
        }

    @Test
    fun `기본 생성은 호출을 기록하며 충돌 없는 ID만 발급한다`() =
        runBlocking {
            val repository = FakeAfternoteRepository(initialItems = listOf(listItem()))
            val social = CreateAccountPayload(title = "소셜", processingMethods = emptyList())
            val business = CreateAccountPayload(title = "업무", processingMethods = emptyList())

            val socialId = repository.createSocial(social).getOrThrow()
            val businessId = repository.createBusiness(business).getOrThrow()

            assertEquals(74L, socialId)
            assertEquals(75L, businessId)
            assertEquals(listOf(social), repository.socialPayloads)
            assertEquals(listOf(business), repository.businessPayloads)
            assertEquals(listOf(73L), repository.items.map(ListItem::id))
        }

    @Test
    fun `onX는 호출 기록 뒤 기본 메모리 변경을 대체한다`() =
        runBlocking {
            val item = listItem()
            val detail = detail()
            val repository =
                FakeAfternoteRepository(
                    initialItems = listOf(item),
                    initialDetails = mapOf(detail.id to detail),
                    onDelete = { Result.success(Unit) },
                )

            repository.delete(item.id).getOrThrow()

            assertEquals(listOf(item.id), repository.deletedIds)
            assertEquals(listOf(item), repository.items)
            assertEquals(detail, repository.details.getValue(detail.id))
        }

    @Test
    fun `기본 delete는 목록과 상세를 함께 지운다`() =
        runBlocking {
            val item = listItem()
            val repository =
                FakeAfternoteRepository(
                    initialItems = listOf(item),
                    initialDetails = mapOf(item.id to detail()),
                )
            val pagingEmissions =
                async(start = CoroutineStart.UNDISPATCHED) {
                    withTimeout(1_000L) {
                        repository.getPagedAfternotes(null).take(2).toList()
                    }
                }

            repository.delete(item.id).getOrThrow()

            assertTrue(repository.items.isEmpty())
            assertTrue(repository.details.isEmpty())
            assertEquals(2, pagingEmissions.await().size)
        }

    @Test
    fun `strict는 모든 계약 경로를 닫되 호출은 기록한다`() {
        val repository = FakeAfternoteRepository.strict()
        val accountPayload = CreateAccountPayload(title = "계정", processingMethods = emptyList())
        val galleryPayload = CreateGalleryPayload(title = "사진", processingMethods = emptyList())
        val memorialPayload =
            CreateMemorialPayload(
                title = "추억",
                memorial = MemorialWritePayload(memorialPhotoUrl = null, songs = emptyList(), memorialVideo = null),
            )
        val updatePayload = AfternoteUpdatePayload(type = AfternoteType.ESTATE, title = "유산")

        assertUnexpected { repository.getPagedAfternotes(null) }
        assertUnexpected { repository.getDetail(1L) }
        assertUnexpected { repository.createSocial(accountPayload) }
        assertUnexpected { repository.createBusiness(accountPayload) }
        assertUnexpected { repository.createGallery(galleryPayload) }
        assertUnexpected { repository.createMemorial(memorialPayload) }
        assertUnexpected { repository.update(1L, updatePayload) }
        assertUnexpected { repository.delete(1L) }

        assertEquals(listOf(null), repository.requestedTypes)
        assertEquals(listOf(1L), repository.requestedDetailIds)
        assertEquals(listOf(accountPayload), repository.socialPayloads)
        assertEquals(listOf(accountPayload), repository.businessPayloads)
        assertEquals(listOf(galleryPayload), repository.galleryPayloads)
        assertEquals(listOf(memorialPayload), repository.memorialPayloads)
        assertEquals(listOf(1L to updatePayload), repository.updateCalls)
        assertEquals(listOf(1L), repository.deletedIds)
    }

    private fun listItem(): ListItem =
        ListItem(
            id = 73L,
            serviceName = "기존 서비스",
            date = "2026.08.22",
            type = AfternoteType.SOCIAL_NETWORK,
            account = Account(id = "old@test.local", password = "old-password"),
        )

    private fun detail(): Detail =
        Detail(
            id = 73L,
            serviceName = "기존 서비스",
            timestamps = DetailTimestamps(updatedAt = "2026.08.22"),
            receivers = listOf(DetailReceiver(receiverId = 7L, name = "김수신", relation = "가족")),
            leaveMessageBlocks = emptyList(),
            content =
                DetailContent.SocialNetwork(
                    credentials = DetailCredentials(id = "old@test.local", password = "old-password"),
                    processingMethods = listOf("계정 삭제"),
                ),
        )

    private fun assertUnexpected(block: suspend () -> Unit) {
        assertThrows(IllegalStateException::class.java) {
            runBlocking { block() }
        }
    }
}
