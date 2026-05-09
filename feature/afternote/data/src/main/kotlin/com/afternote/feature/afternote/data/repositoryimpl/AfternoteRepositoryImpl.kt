package com.afternote.feature.afternote.data.repositoryimpl

import android.util.Log
import com.afternote.core.common.di.IoDispatcher
import com.afternote.core.network.model.requireData
import com.afternote.core.network.model.requireStatus
import com.afternote.feature.afternote.data.mapper.response.toDetailDomain
import com.afternote.feature.afternote.data.mapper.response.toListPage
import com.afternote.feature.afternote.data.mapper.toRequest
import com.afternote.feature.afternote.data.service.AfternoteApiService
import com.afternote.feature.afternote.domain.model.author.AfternoteUpdatePayload
import com.afternote.feature.afternote.domain.model.author.CreateGalleryPayload
import com.afternote.feature.afternote.domain.model.author.CreatePlaylistPayload
import com.afternote.feature.afternote.domain.model.author.CreateSocialPayload
import com.afternote.feature.afternote.domain.model.author.Detail
import com.afternote.feature.afternote.domain.model.author.ListPage
import com.afternote.feature.afternote.domain.repository.AfternoteRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Data layer: calls Afternote API, maps DTO → domain at boundary.
 *
 * API spec: GET/POST /afternotes, GET/PATCH/DELETE /afternotes/{id}.
 */
class AfternoteRepositoryImpl
    @Inject
    constructor(
        private val api: AfternoteApiService,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : AfternoteRepository {
        private val _authorAfternoteListRevision = MutableStateFlow(0L)
        override val authorAfternoteListRevision: StateFlow<Long> =
            _authorAfternoteListRevision.asStateFlow()

        override suspend fun getListPage(
            category: String?,
            pageNumber: Int,
            size: Int,
        ): Result<ListPage> =
            safeCall(ioDispatcher) {
                api
                    .getAfternotes(
                        category = category,
                        pageNumber = pageNumber,
                        size = size,
                    ).requireData()
                    .toListPage()
            }

        override suspend fun createSocial(payload: CreateSocialPayload): Result<Long> =
            safeCall(ioDispatcher, errorMapper = ::mapAuthoringFailure) {
                api.createAfternoteSocial(payload.toRequest()).requireData().afternoteId
            }.onSuccess { bumpAuthorListRevision() }

        override suspend fun createGallery(payload: CreateGalleryPayload): Result<Long> =
            safeCall(ioDispatcher, errorMapper = ::mapAuthoringFailure) {
                api.createAfternoteGallery(payload.toRequest()).requireData().afternoteId
            }.onSuccess { bumpAuthorListRevision() }

        override suspend fun getDetail(id: Long): Result<Detail> =
            safeCall(ioDispatcher) {
                api.getAfternoteDetail(afternoteId = id).requireData().toDetailDomain()
            }

        override suspend fun createPlaylist(payload: CreatePlaylistPayload): Result<Long> =
            safeCall(ioDispatcher, errorMapper = ::mapAuthoringFailure) {
                api.createAfternotePlaylist(payload.toRequest()).requireData().afternoteId
            }.onSuccess { bumpAuthorListRevision() }

        override suspend fun update(
            id: Long,
            payload: AfternoteUpdatePayload,
        ): Result<Long> =
            safeCall(ioDispatcher, errorMapper = ::mapAuthoringFailure) {
                api
                    .updateAfternote(afternoteId = id, request = payload.toRequest())
                    .requireData()
                    .afternoteId
            }.onSuccess { bumpAuthorListRevision() }

        override suspend fun delete(id: Long): Result<Unit> =
            safeCall(ioDispatcher) {
                api.deleteAfternote(afternoteId = id).requireStatus()
            }.onSuccess { bumpAuthorListRevision() }

        private fun bumpAuthorListRevision() {
            _authorAfternoteListRevision.update { it + 1L }
        }
    }

private suspend inline fun <T> safeCall(
    dispatcher: CoroutineDispatcher,
    crossinline errorMapper: (Throwable) -> Throwable = { it },
    crossinline block: suspend () -> T,
): Result<T> =
    withContext(dispatcher) {
        try {
            Result.success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e.logRepositoryFailure()
            Result.failure(errorMapper(e))
        }
    }

private fun Exception.logRepositoryFailure() {
    Log.e("AfternoteRepository", message ?: "Unknown Error", this)
}
