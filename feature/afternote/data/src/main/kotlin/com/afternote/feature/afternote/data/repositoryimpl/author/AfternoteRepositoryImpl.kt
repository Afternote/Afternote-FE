package com.afternote.feature.afternote.data.repositoryimpl.author

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.afternote.core.common.result.runCatchingCancellable
import com.afternote.core.network.model.requireData
import com.afternote.core.network.model.requireStatus
import com.afternote.feature.afternote.data.mapper.toBusinessRequest
import com.afternote.feature.afternote.data.mapper.toDomain
import com.afternote.feature.afternote.data.mapper.toRequest
import com.afternote.feature.afternote.data.mapper.toServerCategory
import com.afternote.feature.afternote.data.mapper.toSocialRequest
import com.afternote.feature.afternote.data.paging.AfternotePagingSource
import com.afternote.feature.afternote.data.service.AfternoteApiService
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.domain.model.author.AfternoteUpdatePayload
import com.afternote.feature.afternote.domain.model.author.CreateAccountPayload
import com.afternote.feature.afternote.domain.model.author.CreateGalleryPayload
import com.afternote.feature.afternote.domain.model.author.CreateMemorialPayload
import com.afternote.feature.afternote.domain.model.author.Detail
import com.afternote.feature.afternote.domain.model.author.ListItem
import com.afternote.feature.afternote.domain.repository.author.AfternoteRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

private const val PAGE_SIZE = 10

/** CUD 성공 시 활성 PagingSource를 재시작해 목록 SSOT를 유지한다. */
class AfternoteRepositoryImpl
    @Inject
    constructor(
        private val api: AfternoteApiService,
    ) : AfternoteRepository {
        private val invalidationTrigger = MutableStateFlow(0L)

        @OptIn(ExperimentalCoroutinesApi::class)
        override fun getPagedAfternotes(type: AfternoteType?): Flow<PagingData<ListItem>> {
            val category = type?.toServerCategory()
            // 서버 enum 에 없는 종류(ESTATE, #491)는 보내면 400 이라 요청 자체를 만들지 않는다.
            // BUSINESS 는 Afternote-BE 78ee857 부터 정식 값이라 여기서 걸리지 않는다 (#1048).
            if (type != null && category == null) return flowOf(PagingData.empty())

            return invalidationTrigger.flatMapLatest {
                Pager(
                    config = PagingConfig(pageSize = PAGE_SIZE),
                    pagingSourceFactory = { AfternotePagingSource(api, category) },
                ).flow
            }
        }

        override suspend fun getDetail(id: Long): Result<Detail> =
            runCatchingCancellable {
                api.getAfternoteDetail(afternoteId = id).requireData().toDomain()
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
