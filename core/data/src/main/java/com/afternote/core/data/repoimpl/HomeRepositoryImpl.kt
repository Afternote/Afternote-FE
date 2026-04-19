package com.afternote.core.data.repoimpl

import com.afternote.core.domain.repository.HomeRepository
import com.afternote.core.model.HomeSummary
import com.afternote.core.network.model.requireData
import com.afternote.core.network.service.HomeApiService
import javax.inject.Inject

class HomeRepositoryImpl
    @Inject
    constructor(
        private val api: HomeApiService,
    ) : HomeRepository {
        override suspend fun getHomeSummary(): Result<HomeSummary> =
            runCatching {
                val data = api.getHomeSummary().requireData()
                HomeSummary(
                    userName = data.userName,
                    isRecipientDesignated = data.isRecipientDesignated,
                    diaryCategoryCount = data.diaryCategoryCount,
                    deepThoughtCategoryCount = data.deepThoughtCategoryCount,
                )
            }
    }
