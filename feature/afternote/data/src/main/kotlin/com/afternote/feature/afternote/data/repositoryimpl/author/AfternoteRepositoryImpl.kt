package com.afternote.feature.afternote.data.repositoryimpl.author

import android.util.Log
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
            // 서버가 모르는 종류는 보내면 400 이므로 요청 자체를 만들지 않는다.
            if (type != null && category == null) return flowOf(PagingData.empty())

            return invalidationTrigger.flatMapLatest {
                Pager(
                    config = PagingConfig(pageSize = PAGE_SIZE),
                    pagingSourceFactory = { AfternotePagingSource(api, category) },
                ).flow
            }
        }

        override suspend fun getDetail(id: Long): Result<Detail> =
            safeCall {
                api.getAfternoteDetail(afternoteId = id).requireData().toDomain()
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

/**
 * 취소 보존은 [runCatchingCancellable] 에 맡기고, 이 계층 고유의 두 책임만 얹는다 —
 * 실패 로깅과 [errorMapper] 를 통한 도메인 예외 치환.
 *
 * 실패 처리를 `Result` 위에서 하는 건 취소를 건드리지 않기 위해서다: 블록 안에서 `Throwable` 을
 * 잡으면 [errorMapper] 가 취소까지 다른 예외로 바꿔 전파를 끊는다.
 */
private suspend inline fun <T> safeCall(
    crossinline errorMapper: (Throwable) -> Throwable = { it },
    crossinline block: suspend () -> T,
): Result<T> {
    val result = runCatchingCancellable { block() }
    val failure = result.exceptionOrNull() ?: return result
    failure.logRepositoryFailure()
    return Result.failure(errorMapper(failure))
}

private fun Throwable.logRepositoryFailure() {
    Log.e("AfternoteRepository", message ?: "Unknown Error", this)
}
