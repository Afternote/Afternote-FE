package com.afternote.feature.mindrecord.data.repositoryimpl

import com.afternote.core.network.model.requireData
import com.afternote.feature.mindrecord.data.api.MindRecordReceiverApiService
import com.afternote.feature.mindrecord.data.mapper.toDomain
import com.afternote.feature.mindrecord.domain.model.ReceiverMindRecords
import com.afternote.feature.mindrecord.domain.repository.MindRecordReceiverRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

class MindRecordReceiverRepositoryImpl
    @Inject
    constructor(
        private val api: MindRecordReceiverApiService,
    ) : MindRecordReceiverRepository {
        override suspend fun getAll(): Result<ReceiverMindRecords> =
            runCatching {
                coroutineScope {
                    val dailyQuestionsDeferred = async { api.getReceiverDailyQuestions().requireData() }
                    val diariesDeferred = async { api.getReceiverDiaries().requireData() }

                    ReceiverMindRecords(
                        dailyQuestions = dailyQuestionsDeferred.await().dailyQuestions.map { it.toDomain() },
                        diaries = diariesDeferred.await().diaries.map { it.toDomain() },
                    )
                }
            }
    }
