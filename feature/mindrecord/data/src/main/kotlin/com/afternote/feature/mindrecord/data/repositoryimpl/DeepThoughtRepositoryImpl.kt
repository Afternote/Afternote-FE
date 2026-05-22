package com.afternote.feature.mindrecord.data.repositoryimpl

import com.afternote.core.network.model.requireData
import com.afternote.core.network.model.requireStatus
import com.afternote.feature.mindrecord.data.api.DeepThoughtApiService
import com.afternote.feature.mindrecord.data.mapper.toDomain
import com.afternote.feature.mindrecord.data.mapper.toRequest
import com.afternote.feature.mindrecord.domain.model.DeepThought
import com.afternote.feature.mindrecord.domain.model.DeepThoughtCategory
import com.afternote.feature.mindrecord.domain.model.DeepThoughtCategoryCreatePayload
import com.afternote.feature.mindrecord.domain.model.DeepThoughtCategoryUpdatePayload
import com.afternote.feature.mindrecord.domain.model.DeepThoughtCreatePayload
import com.afternote.feature.mindrecord.domain.model.DeepThoughtUpdatePayload
import com.afternote.feature.mindrecord.domain.model.RandomDeepThought
import com.afternote.feature.mindrecord.domain.repository.DeepThoughtRepository
import javax.inject.Inject

class DeepThoughtRepositoryImpl
    @Inject
    constructor(
        private val api: DeepThoughtApiService,
    ) : DeepThoughtRepository {
        override suspend fun getList(
            date: String?,
            tag: String?,
            category: String?,
        ): Result<List<DeepThought>> =
            runCatching {
                api
                    .getDeepThoughts(date = date, tag = tag, category = category)
                    .requireData()
                    .map { it.toDomain() }
            }

        override suspend fun getRandom(): Result<RandomDeepThought> =
            runCatching {
                api.getRandomDeepThought().requireData().toDomain()
            }

        override suspend fun create(payload: DeepThoughtCreatePayload): Result<Unit> =
            runCatching {
                api.createDeepThought(payload.toRequest()).requireStatus()
            }

        override suspend fun update(
            id: Long,
            payload: DeepThoughtUpdatePayload,
        ): Result<Unit> =
            runCatching {
                api.updateDeepThought(deepThoughtId = id, request = payload.toRequest()).requireStatus()
            }

        override suspend fun delete(id: Long): Result<Unit> =
            runCatching {
                api.deleteDeepThought(deepThoughtId = id).requireStatus()
            }

        override suspend fun getCategories(): Result<List<DeepThoughtCategory>> =
            runCatching {
                api.getDeepThoughtCategories().requireData().map { it.toDomain() }
            }

        override suspend fun createCategory(
            payload: DeepThoughtCategoryCreatePayload,
        ): Result<DeepThoughtCategory> =
            runCatching {
                api.createDeepThoughtCategory(request = payload.toRequest()).requireData().toDomain()
            }

        override suspend fun updateCategory(
            categoryId: Long,
            payload: DeepThoughtCategoryUpdatePayload,
        ): Result<DeepThoughtCategory> =
            runCatching {
                api
                    .updateDeepThoughtCategory(categoryId = categoryId, request = payload.toRequest())
                    .requireData()
                    .toDomain()
            }

        override suspend fun deleteCategory(categoryId: Long): Result<Unit> =
            runCatching {
                api.deleteDeepThoughtCategory(categoryId = categoryId).requireStatus()
            }
    }
