package com.afternote.feature.mindrecord.data.repositoryimpl

import com.afternote.core.common.result.runCatchingCancellable
import com.afternote.core.network.model.requireData
import com.afternote.core.network.model.requireStatus
import com.afternote.feature.mindrecord.data.api.DiaryApiService
import com.afternote.feature.mindrecord.data.mapper.toDomain
import com.afternote.feature.mindrecord.data.mapper.toRequest
import com.afternote.feature.mindrecord.domain.model.DiaryCreatePayload
import com.afternote.feature.mindrecord.domain.model.DiaryList
import com.afternote.feature.mindrecord.domain.model.DiaryUpdatePayload
import com.afternote.feature.mindrecord.domain.repository.DiaryRepository
import com.afternote.feature.mindrecord.domain.sync.MindRecordChangeTracker
import javax.inject.Inject

class DiaryRepositoryImpl
    @Inject
    constructor(
        private val api: DiaryApiService,
        private val changeTracker: MindRecordChangeTracker,
    ) : DiaryRepository {
        override suspend fun getList(
            yearMonth: String,
            draftOnly: Boolean?,
        ): Result<DiaryList> =
            runCatchingCancellable {
                api
                    .getDiaries(yearMonth = yearMonth, draftOnly = draftOnly)
                    .requireData()
                    .toDomain()
            }

        override suspend fun create(payload: DiaryCreatePayload): Result<Unit> =
            runCatchingCancellable {
                api.createDiary(payload.toRequest()).requireStatus()
            }.onSuccess { changeTracker.notifyChanged() }

        override suspend fun update(
            id: Long,
            payload: DiaryUpdatePayload,
        ): Result<Unit> =
            runCatchingCancellable {
                api.updateDiary(diaryId = id, request = payload.toRequest()).requireStatus()
            }.onSuccess { changeTracker.notifyChanged() }

        override suspend fun delete(id: Long): Result<Unit> =
            runCatchingCancellable {
                api.deleteDiary(diaryId = id).requireStatus()
            }.onSuccess { changeTracker.notifyChanged() }
    }
