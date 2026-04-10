package com.afternote.feature.afternote.data.repositoryimpl

import android.util.Log
import com.afternote.core.network.model.requireData
import com.afternote.core.network.model.requireStatus
import com.afternote.feature.afternote.data.dto.AfternoteIdResponse
import com.afternote.feature.afternote.data.mapper.response.toDetailDomain
import com.afternote.feature.afternote.data.mapper.response.toListPage
import com.afternote.feature.afternote.data.mapper.toRequest
import com.afternote.feature.afternote.data.service.AfternoteApiService
import com.afternote.feature.afternote.domain.model.Detail
import com.afternote.feature.afternote.domain.model.ListPage
import com.afternote.feature.afternote.domain.model.author.AfternoteUpdatePayload
import com.afternote.feature.afternote.domain.model.author.CreateGalleryPayload
import com.afternote.feature.afternote.domain.model.author.CreatePlaylistPayload
import com.afternote.feature.afternote.domain.model.author.CreateSocialPayload
import com.afternote.feature.afternote.domain.repository.AfternoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
    ) : AfternoteRepository {
        private val _authorAfternoteListRevision = MutableStateFlow(0L)
        override val authorAfternoteListRevision: StateFlow<Long> =
            _authorAfternoteListRevision.asStateFlow()

        override suspend fun getListPage(
            category: String?,
            pageNumber: Int,
            size: Int,
        ): Result<ListPage> =
            runCatching {
                val response =
                    api.getAfternotes(
                        category = category,
                        pageNumber = pageNumber,
                        size = size,
                    )
                val data = response.requireData()
                data.toListPage()
            }.logFailure()

        override suspend fun createSocial(payload: CreateSocialPayload): Result<Long> =
            runCatching {
                val request = payload.toRequest()
                val response = api.createAfternoteSocial(request)
                val data = response.requireData()
                getAfternoteId(data)
            }.logFailure()
                .also { if (it.isSuccess) bumpAuthorListRevision() }

        override suspend fun createGallery(payload: CreateGalleryPayload): Result<Long> =
            runCatching {
                val request = payload.toRequest()
                val response = api.createAfternoteGallery(request)
                val data = response.requireData()
                getAfternoteId(data)
            }.logFailure()
                .also { if (it.isSuccess) bumpAuthorListRevision() }

        /**
         * GET /afternotes/{afternoteId} — 상세 조회. DTO → domain 매핑 포함.
         */
        override suspend fun getDetail(id: Long): Result<Detail> =
            runCatching {
                val response = api.getAfternoteDetail(afternoteId = id)
                val data = response.requireData()
                data.toDetailDomain()
            }.logFailure()

        /**
         * POST /afternotes (PLAYLIST category).
         */
        override suspend fun createPlaylist(payload: CreatePlaylistPayload): Result<Long> =
            runCatching {
                val request = payload.toRequest()
                val response = api.createAfternotePlaylist(request)
                val data = response.requireData()
                getAfternoteId(data)
            }.logFailure()
                .also { if (it.isSuccess) bumpAuthorListRevision() }

        /**
         * PATCH /afternotes/{afternoteId} — 부분 수정 (수정할 필드만 전송).
         */
        override suspend fun update(
            id: Long,
            payload: AfternoteUpdatePayload,
        ): Result<Long> {
            val request = payload.toRequest()
            return runCatching {
                val response = api.updateAfternote(afternoteId = id, request = request)
                val data = response.requireData()
                getAfternoteId(data)
            }.logFailure()
                .also { if (it.isSuccess) bumpAuthorListRevision() }
        }

        /**
         * DELETE /afternotes/{afternoteId}.
         */
        override suspend fun delete(id: Long): Result<Unit> =
            runCatching {
                val response = api.deleteAfternote(afternoteId = id)
                response.requireStatus()
            }.logFailure()
                .also { if (it.isSuccess) bumpAuthorListRevision() }

        private fun bumpAuthorListRevision() {
            _authorAfternoteListRevision.update { it + 1L }
        }
    }

private fun getAfternoteId(data: AfternoteIdResponse) = data.afternoteId

private fun <T> Result<T>.logFailure() =
    onFailure { error ->
        val message = error.message
        val msg = message.toString()
        Log.e("AfternoteRepository", msg)
    }
