package com.afternote.feature.afternote.data.repositoryimpl.receiver

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.afternote.core.network.model.requireData
import com.afternote.feature.afternote.data.local.ReceiverAuthCodeDataSource
import com.afternote.feature.afternote.data.mapper.response.toDomain
import com.afternote.feature.afternote.data.paging.ReceiverAfternotePagingSource
import com.afternote.feature.afternote.data.service.ReceiverAfternoteApiService
import com.afternote.feature.afternote.domain.model.receiver.AfterNoteListItemDto
import com.afternote.feature.afternote.domain.model.receiver.AfterNotesListResult
import com.afternote.feature.afternote.domain.model.receiver.LoadCountResult
import com.afternote.feature.afternote.domain.model.receiver.ReceivedAfternoteDetail
import com.afternote.feature.afternote.domain.model.receiver.ReceivedExportBundle
import com.afternote.feature.afternote.domain.repository.receiver.ReceiverRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private const val PAGE_SIZE = 50

/**
 * 인증 코드는 [ReceiverAuthCodeDataSource]에서 읽고·쓰고·지우며, REST 요청에는 ReceiverAuthInterceptor가
 * `X-Auth-Code` 헤더를 자동 부착한다. 미연동 항목은 폴백 값을 반환한다.
 */
@Singleton
class ReceiverRepositoryImpl
    @Inject
    constructor(
        private val authCodeDataSource: ReceiverAuthCodeDataSource,
        private val api: ReceiverAfternoteApiService,
    ) : ReceiverRepository {
        override val authCodeFlow: Flow<String?> = authCodeDataSource.savedCodeFlow

        override suspend fun currentAuthCode(): String? = authCodeFlow.first()

        override suspend fun saveAuthCode(code: String) {
            authCodeDataSource.saveCode(code)
        }

        override suspend fun clearAuthCode() {
            authCodeDataSource.clearCode()
        }

        override fun getPagedReceivedAfternotes(): Flow<PagingData<AfterNoteListItemDto>> =
            Pager(
                config = PagingConfig(pageSize = PAGE_SIZE),
                pagingSourceFactory = { ReceiverAfternotePagingSource(api) },
            ).flow

        override suspend fun getReceivedAfterNotes(): Result<AfterNotesListResult> =
            Result.success(
                AfterNotesListResult(
                    items = emptyList(),
                    totalCount = 0,
                ),
            )

        override suspend fun getReceivedAfternoteDetail(afternoteId: Long): Result<ReceivedAfternoteDetail> =
            runCatching {
                api
                    .getReceiverAfternoteDetail(afternoteId = afternoteId)
                    .requireData()
                    .toDomain()
            }

        override suspend fun downloadAllReceived(): Result<ReceivedExportBundle> = Result.success(ReceivedExportBundle())

        override suspend fun saveReceivedExportToFile(bundle: ReceivedExportBundle): Result<Unit> = Result.success(Unit)

        override suspend fun loadMindRecordsCount(): Result<LoadCountResult> = Result.success(LoadCountResult(0))

        override suspend fun loadTimeLettersCount(): Result<LoadCountResult> = Result.success(LoadCountResult(0))

        override suspend fun loadSenderMessage(): Result<String?> = Result.success(null)
    }
