package com.afternote.feature.mindrecord.data.repositoryimpl

import com.afternote.core.network.model.requireData
import com.afternote.feature.mindrecord.data.api.MindRecordReceiverApiService
import com.afternote.feature.mindrecord.data.mapper.toDomain
import com.afternote.feature.mindrecord.domain.model.MindRecordDetail
import com.afternote.feature.mindrecord.domain.model.MindRecordList
import com.afternote.feature.mindrecord.domain.repository.MindRecordReceiverRepository
import javax.inject.Inject

class MindRecordReceiverRepositoryImpl
    @Inject
    constructor(
        private val api: MindRecordReceiverApiService,
    ) : MindRecordReceiverRepository {
        override suspend fun getList(): Result<MindRecordList> =
            runCatching {
                api.getReceiverMindRecords().requireData().toDomain()
            }

        override suspend fun getDetail(id: Long): Result<MindRecordDetail> =
            runCatching {
                api.getReceiverMindRecordDetail(mindRecordId = id).requireData().toDomain()
            }
    }
