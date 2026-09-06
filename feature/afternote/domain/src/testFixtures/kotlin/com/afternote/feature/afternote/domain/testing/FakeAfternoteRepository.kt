package com.afternote.feature.afternote.domain.testing

import androidx.paging.PagingData
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
import com.afternote.feature.afternote.domain.model.author.DraftDetail
import com.afternote.feature.afternote.domain.model.author.ListItem
import com.afternote.feature.afternote.domain.model.author.playlist.DetailSong
import com.afternote.feature.afternote.domain.model.author.playlist.MemorialMedia
import com.afternote.feature.afternote.domain.repository.author.AfternoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicLong

/**
 * [AfternoteRepository] fake 정본 (#1030, #1044).
 *
 * 목록·상세는 메모리에 보관하고 조회 종류를 계약대로 거른다. 생성 payload만으로는 서버가
 * 채우는 날짜·수신자 이름을 포함한 완전한 엔티티를 만들 수 없으므로 기본 생성은 ID를 발급하고
 * 호출을 기록한다. 실패·경합·스크립트 응답은 `onX` 람다로 기본 동작을 갈아끼운다.
 */
class FakeAfternoteRepository(
    initialItems: List<ListItem> = emptyList(),
    initialDetails: Map<Long, Detail> = emptyMap(),
    initialDraftDetails: Map<Long, DraftDetail> = emptyMap(),
    nextId: Long =
        (initialItems.map(ListItem::id) + initialDetails.keys)
            .maxOrNull()
            ?.plus(1L) ?: 1L,
    var onGetPagedAfternotes: ((AfternoteType?) -> Flow<PagingData<ListItem>>)? = null,
    var onGetPagedDrafts: ((AfternoteType?) -> Flow<PagingData<ListItem>>)? = null,
    var onGetDetail: (suspend (Long) -> Result<Detail>)? = null,
    var onGetDraftDetail: (suspend (Long) -> Result<DraftDetail>)? = null,
    var onCreateSocial: (suspend (CreateAccountPayload) -> Result<Long>)? = null,
    var onCreateBusiness: (suspend (CreateAccountPayload) -> Result<Long>)? = null,
    var onCreateGallery: (suspend (CreateGalleryPayload) -> Result<Long>)? = null,
    var onCreateMemorial: (suspend (CreateMemorialPayload) -> Result<Long>)? = null,
    var onUpdate: (suspend (Long, AfternoteUpdatePayload) -> Result<Long>)? = null,
    var onDelete: (suspend (Long) -> Result<Unit>)? = null,
) : AfternoteRepository {
    val items = CopyOnWriteArrayList(initialItems)
    val details = ConcurrentHashMap(initialDetails)
    val draftDetails = ConcurrentHashMap(initialDraftDetails)

    val requestedTypes = CopyOnWriteArrayList<AfternoteType?>()
    val requestedDetailIds = CopyOnWriteArrayList<Long>()
    val socialPayloads = CopyOnWriteArrayList<CreateAccountPayload>()
    val businessPayloads = CopyOnWriteArrayList<CreateAccountPayload>()
    val galleryPayloads = CopyOnWriteArrayList<CreateGalleryPayload>()
    val memorialPayloads = CopyOnWriteArrayList<CreateMemorialPayload>()
    val updateCalls = CopyOnWriteArrayList<Pair<Long, AfternoteUpdatePayload>>()
    val deletedIds = CopyOnWriteArrayList<Long>()

    val requestedDraftTypes = CopyOnWriteArrayList<AfternoteType?>()
    val requestedDraftDetailIds = CopyOnWriteArrayList<Long>()

    private val idCounter = AtomicLong(nextId)
    private val stateVersion = MutableStateFlow(0L)
    private val stateLock = Any()

    override fun getPagedAfternotes(type: AfternoteType?): Flow<PagingData<ListItem>> {
        requestedTypes += type
        onGetPagedAfternotes?.let { return it(type) }
        return stateVersion.map {
            // 서버와 같은 계약 — draftOnly 미전송은 발행분만 준다.
            PagingData.from(items.filter { !it.isDraft && (type == null || it.type == type) })
        }
    }

    override fun getPagedDrafts(type: AfternoteType?): Flow<PagingData<ListItem>> {
        requestedDraftTypes += type
        onGetPagedDrafts?.let { return it(type) }
        return stateVersion.map {
            PagingData.from(items.filter { it.isDraft && (type == null || it.type == type) })
        }
    }

    override suspend fun getDetail(id: Long): Result<Detail> {
        requestedDetailIds += id
        onGetDetail?.let { return it(id) }
        return runCatching {
            details[id] ?: throw NoSuchElementException("애프터노트 상세가 없다: id=$id")
        }
    }

    override suspend fun getDraftDetail(id: Long): Result<DraftDetail> {
        requestedDraftDetailIds += id
        onGetDraftDetail?.let { return it(id) }
        return runCatching {
            draftDetails[id] ?: throw NoSuchElementException("임시저장 상세가 없다: id=$id")
        }
    }

    override suspend fun createSocial(payload: CreateAccountPayload): Result<Long> {
        socialPayloads += payload
        onCreateSocial?.let { return it(payload) }
        return Result.success(idCounter.getAndIncrement())
    }

    override suspend fun createBusiness(payload: CreateAccountPayload): Result<Long> {
        businessPayloads += payload
        onCreateBusiness?.let { return it(payload) }
        return Result.success(idCounter.getAndIncrement())
    }

    override suspend fun createGallery(payload: CreateGalleryPayload): Result<Long> {
        galleryPayloads += payload
        onCreateGallery?.let { return it(payload) }
        return Result.success(idCounter.getAndIncrement())
    }

    override suspend fun createMemorial(payload: CreateMemorialPayload): Result<Long> {
        memorialPayloads += payload
        onCreateMemorial?.let { return it(payload) }
        return Result.success(idCounter.getAndIncrement())
    }

    override suspend fun update(
        id: Long,
        payload: AfternoteUpdatePayload,
    ): Result<Long> {
        updateCalls += id to payload
        onUpdate?.let { return it(id, payload) }

        synchronized(stateLock) {
            val itemIndex = items.indexOfFirst { it.id == id }
            val detail = details[id]
            if (itemIndex < 0 && detail == null) {
                return Result.failure(NoSuchElementException("수정할 애프터노트가 없다: id=$id"))
            }
            if (itemIndex >= 0) {
                items[itemIndex] = items[itemIndex].updatedWith(payload)
            }
            if (detail != null) {
                details[id] = detail.updatedWith(payload)
            }
        }
        stateVersion.update { it + 1L }
        return Result.success(id)
    }

    override suspend fun delete(id: Long): Result<Unit> {
        deletedIds += id
        onDelete?.let { return it(id) }
        synchronized(stateLock) {
            items.removeAll { it.id == id }
            details.remove(id)
        }
        stateVersion.update { it + 1L }
        return Result.success(Unit)
    }

    companion object {
        /** 모든 호출을 닫고 테스트가 실제로 쓰는 경로만 `onX` 로 연다. */
        fun strict(): FakeAfternoteRepository =
            FakeAfternoteRepository(
                onGetPagedAfternotes = { unexpectedCall("AfternoteRepository.getPagedAfternotes") },
                onGetPagedDrafts = { unexpectedCall("AfternoteRepository.getPagedDrafts") },
                onGetDetail = { unexpectedCall("AfternoteRepository.getDetail") },
                onGetDraftDetail = { unexpectedCall("AfternoteRepository.getDraftDetail") },
                onCreateSocial = { unexpectedCall("AfternoteRepository.createSocial") },
                onCreateBusiness = { unexpectedCall("AfternoteRepository.createBusiness") },
                onCreateGallery = { unexpectedCall("AfternoteRepository.createGallery") },
                onCreateMemorial = { unexpectedCall("AfternoteRepository.createMemorial") },
                onUpdate = { _, _ -> unexpectedCall("AfternoteRepository.update") },
                onDelete = { unexpectedCall("AfternoteRepository.delete") },
            )
    }
}

private fun ListItem.updatedWith(payload: AfternoteUpdatePayload): ListItem =
    copy(
        serviceName = payload.title,
        type = payload.type,
        account = account.updatedWith(payload.credentials),
    )

private fun Account.updatedWith(credentials: AfternoteAccountCredentials?): Account =
    if (credentials == null) {
        this
    } else {
        copy(
            id = credentials.id ?: id,
            password = credentials.password ?: password,
        )
    }

private fun Detail.updatedWith(payload: AfternoteUpdatePayload): Detail =
    copy(
        serviceName = payload.title,
        receivers =
            payload.receivers?.map { ref ->
                receivers.firstOrNull { it.receiverId == ref.receiverId }
                    ?: DetailReceiver(receiverId = ref.receiverId, name = "", relation = "")
            } ?: receivers,
        leaveMessageBlocks = payload.leaveMessageBlocks,
        content = content.updatedWith(payload),
    )

private fun DetailContent.updatedWith(payload: AfternoteUpdatePayload): DetailContent =
    when (payload.type) {
        AfternoteType.SOCIAL_NETWORK -> {
            val previous = this as? DetailContent.SocialNetwork
            DetailContent.SocialNetwork(
                credentials = payload.credentials.toDetailCredentials(previous?.credentials),
                processingMethods = payload.processingMethods ?: previous?.processingMethods.orEmpty(),
            )
        }

        AfternoteType.BUSINESS -> {
            val previous = this as? DetailContent.Business
            DetailContent.Business(
                credentials = payload.credentials.toDetailCredentials(previous?.credentials),
                processingMethods = payload.processingMethods ?: previous?.processingMethods.orEmpty(),
            )
        }

        AfternoteType.GALLERY_AND_FILES -> {
            val previous = this as? DetailContent.Gallery
            DetailContent.Gallery(
                processingMethods = payload.processingMethods ?: previous?.processingMethods.orEmpty(),
            )
        }

        AfternoteType.MEMORIAL -> {
            val previous = this as? DetailContent.Memorial
            val memorial = payload.memorial
            if (memorial == null) {
                previous ?: DetailContent.Memorial(songs = emptyList(), media = MemorialMedia(null, null, null))
            } else {
                DetailContent.Memorial(
                    songs = memorial.songs.map { DetailSong(it.title, it.artist, it.coverUrl) },
                    media =
                        MemorialMedia(
                            photoUrl = memorial.memorialPhotoUrl,
                            videoUrl = memorial.memorialVideo?.videoUrl,
                            thumbnailUrl = memorial.memorialVideo?.thumbnailUrl,
                        ),
                )
            }
        }

        AfternoteType.ESTATE -> {
            DetailContent.Estate
        }
    }

private fun AfternoteAccountCredentials?.toDetailCredentials(previous: DetailCredentials?): DetailCredentials =
    DetailCredentials(
        id = this?.id ?: previous?.id.orEmpty(),
        password = this?.password ?: previous?.password.orEmpty(),
    )
