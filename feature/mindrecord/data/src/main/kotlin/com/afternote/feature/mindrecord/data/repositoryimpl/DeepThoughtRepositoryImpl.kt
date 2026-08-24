package com.afternote.feature.mindrecord.data.repositoryimpl

import com.afternote.core.network.model.requireData
import com.afternote.feature.mindrecord.data.api.DeepThoughtApiService
import com.afternote.feature.mindrecord.data.mapper.toDomain
import com.afternote.feature.mindrecord.domain.model.DeepThought
import com.afternote.feature.mindrecord.domain.repository.DeepThoughtRepository
import javax.inject.Inject

class DeepThoughtRepositoryImpl
    @Inject
    constructor(
        private val api: DeepThoughtApiService,
    ) : DeepThoughtRepository {
        override suspend fun getList(draftOnly: Boolean?): Result<List<DeepThought>> =
            runCatching {
                api
                    .getDeepThoughts(draftOnly = draftOnly)
                    .requireData()
                    .deepThoughts
                    .map { it.toDomain() }
            }
    }
