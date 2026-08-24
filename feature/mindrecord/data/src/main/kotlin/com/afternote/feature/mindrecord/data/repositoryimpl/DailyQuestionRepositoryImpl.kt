package com.afternote.feature.mindrecord.data.repositoryimpl

import com.afternote.core.common.result.runCatchingCancellable
import com.afternote.core.network.model.requireData
import com.afternote.core.network.model.requireStatus
import com.afternote.feature.mindrecord.data.api.DailyQuestionApiService
import com.afternote.feature.mindrecord.data.mapper.toDomain
import com.afternote.feature.mindrecord.data.mapper.toRequest
import com.afternote.feature.mindrecord.domain.model.DailyQuestion
import com.afternote.feature.mindrecord.domain.model.DailyQuestionCreatePayload
import com.afternote.feature.mindrecord.domain.model.DailyQuestionUpdatePayload
import com.afternote.feature.mindrecord.domain.model.TodayDailyQuestion
import com.afternote.feature.mindrecord.domain.repository.DailyQuestionRepository
import javax.inject.Inject

class DailyQuestionRepositoryImpl
    @Inject
    constructor(
        private val api: DailyQuestionApiService,
    ) : DailyQuestionRepository {
        override suspend fun getList(
            date: String?,
            draftOnly: Boolean?,
        ): Result<List<DailyQuestion>> =
            runCatchingCancellable {
                api
                    .getDailyQuestions(date = date, draftOnly = draftOnly)
                    .requireData()
                    .map { it.toDomain() }
            }

        override suspend fun getToday(): Result<TodayDailyQuestion> =
            runCatchingCancellable {
                api.getTodayDailyQuestion().requireData().toDomain()
            }

        override suspend fun create(payload: DailyQuestionCreatePayload): Result<Unit> =
            runCatchingCancellable {
                api.createDailyQuestion(payload.toRequest()).requireStatus()
            }

        override suspend fun update(
            id: Long,
            payload: DailyQuestionUpdatePayload,
        ): Result<Unit> =
            runCatchingCancellable {
                api
                    .updateDailyQuestion(userDailyQuestionId = id, request = payload.toRequest())
                    .requireStatus()
            }

        override suspend fun delete(id: Long): Result<Unit> =
            runCatchingCancellable {
                api.deleteDailyQuestion(userDailyQuestionId = id).requireStatus()
            }
    }
