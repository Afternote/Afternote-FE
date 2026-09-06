package com.afternote.feature.afternote.data.repositoryimpl.author

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.afternote.core.common.result.runCatchingCancellable
import com.afternote.core.network.model.requireData
import com.afternote.core.network.model.requireStatus
import com.afternote.feature.afternote.data.mapper.toBusinessRequest
import com.afternote.feature.afternote.data.mapper.toDomain
import com.afternote.feature.afternote.data.mapper.toDraftDomain
import com.afternote.feature.afternote.data.mapper.toRequest
import com.afternote.feature.afternote.data.mapper.toServerCategory
import com.afternote.feature.afternote.data.mapper.toSocialRequest
import com.afternote.feature.afternote.data.paging.AfternotePagingSource
import com.afternote.feature.afternote.data.service.AfternoteApiService
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.domain.error.AfternoteFailure
import com.afternote.feature.afternote.domain.model.author.AfternoteUpdatePayload
import com.afternote.feature.afternote.domain.model.author.CreateAccountPayload
import com.afternote.feature.afternote.domain.model.author.CreateGalleryPayload
import com.afternote.feature.afternote.domain.model.author.CreateMemorialPayload
import com.afternote.feature.afternote.domain.model.author.Detail
import com.afternote.feature.afternote.domain.model.author.DraftDetail
import com.afternote.feature.afternote.domain.model.author.ListItem
import com.afternote.feature.afternote.domain.repository.author.AfternoteRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import java.io.IOException
import javax.inject.Inject

private const val PAGE_SIZE = 10

/** CUD 성공 시 활성 PagingSource를 재시작해 목록 SSOT를 유지한다. */
class AfternoteRepositoryImpl
    @Inject
    constructor(
        private val api: AfternoteApiService,
    ) : AfternoteRepository {
        private val invalidationTrigger = MutableStateFlow(0L)

        override fun getPagedAfternotes(type: AfternoteType?): Flow<PagingData<ListItem>> = pagedAfternotes(type, draftOnly = false)

        override fun getPagedDrafts(type: AfternoteType?): Flow<PagingData<ListItem>> = pagedAfternotes(type, draftOnly = true)

        @OptIn(ExperimentalCoroutinesApi::class)
        private fun pagedAfternotes(
            type: AfternoteType?,
            draftOnly: Boolean,
        ): Flow<PagingData<ListItem>> {
            val category = type?.toServerCategory()
            // 서버 enum 에 없는 종류(ESTATE, #491)는 보내면 400 이라 요청 자체를 만들지 않는다.
            // BUSINESS 는 Afternote-BE 78ee857 부터 정식 값이라 여기서 걸리지 않는다 (#1048).
            if (type != null && category == null) return flowOf(PagingData.empty())

            return invalidationTrigger.flatMapLatest {
                Pager(
                    config = PagingConfig(pageSize = PAGE_SIZE),
                    pagingSourceFactory = { AfternotePagingSource(api, category, draftOnly) },
                ).flow
            }
        }

        override suspend fun getDetail(id: Long): Result<Detail> =
            runCatchingCancellable {
                api.getAfternoteDetail(afternoteId = id).requireData().toDomain()
            }

        override suspend fun getDraftDetail(id: Long): Result<DraftDetail> =
            runCatchingCancellable {
                api.getAfternoteDetail(afternoteId = id).requireData().toDraftDomain()
            }

        override suspend fun createSocial(payload: CreateAccountPayload): Result<Long> =
            runCatchingCancellable {
                api.createAfternoteAccount(payload.toSocialRequest()).requireData().afternoteId
            }.mapAuthoringFailure()
                .onSuccess { invalidatePagedAfternotes() }

        override suspend fun createBusiness(payload: CreateAccountPayload): Result<Long> =
            runCatchingCancellable {
                api.createAfternoteAccount(payload.toBusinessRequest()).requireData().afternoteId
            }.mapAuthoringFailure()
                .onSuccess { invalidatePagedAfternotes() }

        override suspend fun createGallery(payload: CreateGalleryPayload): Result<Long> =
            runCatchingCancellable {
                api.createAfternoteGallery(payload.toRequest()).requireData().afternoteId
            }.mapAuthoringFailure()
                .onSuccess { invalidatePagedAfternotes() }

        override suspend fun createMemorial(payload: CreateMemorialPayload): Result<Long> =
            runCatchingCancellable {
                api.createAfternotePlaylist(payload.toRequest()).requireData().afternoteId
            }.mapAuthoringFailure()
                .onSuccess { invalidatePagedAfternotes() }

        override suspend fun update(
            id: Long,
            payload: AfternoteUpdatePayload,
        ): Result<Long> =
            runCatchingCancellable {
                api
                    .updateAfternote(afternoteId = id, request = payload.toRequest())
                    .requireData()
                    .afternoteId
            }.mapAuthoringFailure()
                .onSuccess { invalidatePagedAfternotes() }

        override suspend fun delete(id: Long): Result<Unit> =
            runCatchingCancellable {
                api.deleteAfternote(afternoteId = id).requireStatus()
            }.onSuccess { invalidatePagedAfternotes() }

        private fun invalidatePagedAfternotes() {
            invalidationTrigger.value++
        }
    }

/**
 * 저장 API 실패를 presentation 이 네트워크·서버 오류로 타입 분기할 수 있는 도메인 예외로 옮긴다
 * (`mapLoginFailure` 와 같은 자리·같은 이유 — 유일한 호출부인 이 파일 안에 둔다).
 *
 * 서버가 응답하며 거절한 실패는 원본 그대로 흘려보낸다 — 저장 경로에서 화면 처리가 달라지는
 * 서버 사유는 현재 없다.
 *
 * 취소는 여기 오지 않는다 — 호출부가 전부 `runCatchingCancellable`(#661) 이라
 * `CancellationException` 이 [Result] 에 담긴 채로 도달하지 않는다.
 */
private fun <T> Result<T>.mapAuthoringFailure(): Result<T> =
    when (val exception = exceptionOrNull()) {
        is IOException -> Result.failure(AfternoteFailure.NetworkUnavailable(exception))
        else -> this
    }
