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
import com.afternote.feature.afternote.domain.model.input.CreateGalleryInput
import com.afternote.feature.afternote.domain.model.input.CreatePlaylistInput
import com.afternote.feature.afternote.domain.model.input.CreateSocialInput
import com.afternote.feature.afternote.domain.model.input.GetListPageInput
import com.afternote.feature.afternote.domain.model.input.UpdateInput
import com.afternote.feature.afternote.domain.repository.AfternoteRepository
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
        override suspend fun getListPage(input: GetListPageInput): Result<ListPage> =
            runCatching {
                val response =
                    api.getAfternotes(
                        category = input.category,
                        pageNumber = input.pageNumber,
                        size = input.size,
                    )
                val data = response.requireData()
                data.toListPage()
            }.logFailure()

        override suspend fun createSocial(input: CreateSocialInput): Result<Long> =
            runCatching {
                val request = input.toRequest()
                val response = api.createAfternoteSocial(request)
                val data = response.requireData()
                getAfternoteId(data)
            }.logFailure()

        override suspend fun createGallery(input: CreateGalleryInput): Result<Long> =
            runCatching {
                val request = input.toRequest()
                val response = api.createAfternoteGallery(request)
                val data = response.requireData()
                getAfternoteId(data)
            }.logFailure()

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
        override suspend fun createPlaylist(input: CreatePlaylistInput): Result<Long> =
            runCatching {
                val request = input.toRequest()
                val response = api.createAfternotePlaylist(request)
                val data = response.requireData()
                getAfternoteId(data)
            }.logFailure()

        /**
         * PATCH /afternotes/{afternoteId} — 부분 수정 (수정할 필드만 전송).
         */
        override suspend fun update(
            id: Long,
            input: UpdateInput,
        ): Result<Long> {
            val request = input.toRequest()
            return runCatching {
                val response = api.updateAfternote(afternoteId = id, request = request)
                val data = response.requireData()
                getAfternoteId(data)
            }.logFailure()
        }

        /**
         * DELETE /afternotes/{afternoteId}.
         */
        override suspend fun delete(id: Long): Result<Unit> =
            runCatching {
                val response = api.deleteAfternote(afternoteId = id)
                response.requireStatus()
            }.logFailure()
    }

private fun getAfternoteId(data: AfternoteIdResponse) = data.afternoteId

private fun <T> Result<T>.logFailure() =
    onFailure { error ->
        val message = error.message
        val msg = message.toString()
        Log.e("AfternoteRepository", msg)
    }
