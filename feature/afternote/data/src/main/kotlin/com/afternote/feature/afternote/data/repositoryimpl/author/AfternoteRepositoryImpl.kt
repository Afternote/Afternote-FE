package com.afternote.feature.afternote.data.repositoryimpl.author

import android.util.Log
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.afternote.core.network.model.requireData
import com.afternote.core.network.model.requireStatus
import com.afternote.feature.afternote.data.mapper.response.toDetailDomain
import com.afternote.feature.afternote.data.mapper.toBusinessRequest
import com.afternote.feature.afternote.data.mapper.toRequest
import com.afternote.feature.afternote.data.mapper.toSocialRequest
import com.afternote.feature.afternote.data.paging.AfternotePagingSource
import com.afternote.feature.afternote.data.service.AfternoteApiService
import com.afternote.feature.afternote.domain.model.author.AfternoteUpdatePayload
import com.afternote.feature.afternote.domain.model.author.CreateAccountPayload
import com.afternote.feature.afternote.domain.model.author.CreateGalleryPayload
import com.afternote.feature.afternote.domain.model.author.CreateMemorialPayload
import com.afternote.feature.afternote.domain.model.author.Detail
import com.afternote.feature.afternote.domain.model.author.ListItem
import com.afternote.feature.afternote.domain.repository.author.AfternoteRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
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
        override fun getPagedAfternotes(category: String?): Flow<PagingData<ListItem>> =
            invalidationTrigger.flatMapLatest {
                Pager(
                    config = PagingConfig(pageSize = PAGE_SIZE),
                    pagingSourceFactory = { AfternotePagingSource(api, category) },
                ).flow
            }

        override suspend fun getDetail(id: Long): Result<Detail> =
            safeCall {
                api.getAfternoteDetail(afternoteId = id).requireData().toDetailDomain()
            }

        override suspend fun createSocial(payload: CreateAccountPayload): Result<Long> =
            safeCall(errorMapper = ::mapAuthoringFailure) {
                api.createAfternoteAccount(payload.toSocialRequest()).requireData().afternoteId
            }.onSuccess { invalidatePagedAfternotes() }

        override suspend fun createBusiness(payload: CreateAccountPayload): Result<Long> =
            safeCall(errorMapper = ::mapAuthoringFailure) {
                api.createAfternoteAccount(payload.toBusinessRequest()).requireData().afternoteId
            }.onSuccess { invalidatePagedAfternotes() }

        override suspend fun createGallery(payload: CreateGalleryPayload): Result<Long> =
            safeCall(errorMapper = ::mapAuthoringFailure) {
                api.createAfternoteGallery(payload.toRequest()).requireData().afternoteId
            }.onSuccess { invalidatePagedAfternotes() }

        override suspend fun createMemorial(payload: CreateMemorialPayload): Result<Long> =
            safeCall(errorMapper = ::mapAuthoringFailure) {
                api.createAfternotePlaylist(payload.toRequest()).requireData().afternoteId
            }.onSuccess { invalidatePagedAfternotes() }

        override suspend fun update(
            id: Long,
            payload: AfternoteUpdatePayload,
        ): Result<Long> =
            safeCall(errorMapper = ::mapAuthoringFailure) {
                api
                    .updateAfternote(afternoteId = id, request = payload.toRequest())
                    .requireData()
                    .afternoteId
            }.onSuccess { invalidatePagedAfternotes() }

        override suspend fun delete(id: Long): Result<Unit> =
            safeCall {
                api.deleteAfternote(afternoteId = id).requireStatus()
            }.onSuccess { invalidatePagedAfternotes() }

        private fun invalidatePagedAfternotes() {
            invalidationTrigger.value++
        }
    }

private suspend inline fun <T> safeCall(
    crossinline errorMapper: (Throwable) -> Throwable = { it },
    crossinline block: suspend () -> T,
): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        e.logRepositoryFailure()
        Result.failure(errorMapper(e))
    }

private fun Exception.logRepositoryFailure() {
    Log.e("AfternoteRepository", message ?: "Unknown Error", this)
}
