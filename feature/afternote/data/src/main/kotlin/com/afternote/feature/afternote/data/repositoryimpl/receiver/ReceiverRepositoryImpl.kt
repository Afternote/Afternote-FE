package com.afternote.feature.afternote.data.repositoryimpl.receiver

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.afternote.feature.afternote.data.local.ReceiverAuthCodeDataSource
import com.afternote.feature.afternote.data.paging.ReceiverAfternotePagingSource
import com.afternote.feature.afternote.data.service.ReceiverAfternoteApiService
import com.afternote.feature.afternote.domain.model.receiver.AfterNoteListItemDto
import com.afternote.feature.afternote.domain.model.receiver.AfterNotesListResult
import com.afternote.feature.afternote.domain.model.receiver.LoadCountResult
import com.afternote.feature.afternote.domain.model.receiver.ReceivedAfternoteDetail
import com.afternote.feature.afternote.domain.model.receiver.ReceivedExportBundle
import com.afternote.feature.afternote.domain.repository.ReceiverRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject
import javax.inject.Singleton

private const val PAGE_SIZE = 10

/**
 * 인증 코드는 [ReceiverAuthCodeDataSource]에서 읽고·쓰고·지우며, 수신 REST 미연동 항목은 폴백 값을 반환합니다.
 * 작성자 애프터노트 HTTP는 DEBUG에서 `AfternoteDebugMockNetworkInterceptor`가 가로챕니다.
 */
@Singleton
class ReceiverRepositoryImpl
    @Inject
    constructor(
        private val authCodeDataSource: ReceiverAuthCodeDataSource,
        private val api: ReceiverAfternoteApiService,
    ) : ReceiverRepository {
        private val invalidationTrigger = MutableStateFlow(0L)

        override val authCodeFlow: Flow<String?> = authCodeDataSource.savedCodeFlow

        override suspend fun currentAuthCode(): String? = authCodeFlow.first()

        override suspend fun saveAuthCode(code: String) {
            authCodeDataSource.saveCode(code)
        }

        override suspend fun clearAuthCode() {
            authCodeDataSource.clearCode()
        }

        @OptIn(ExperimentalCoroutinesApi::class)
        override fun getPagedReceivedAfternotes(category: String?): Flow<PagingData<AfterNoteListItemDto>> =
            invalidationTrigger.flatMapLatest {
                Pager(
                    config = PagingConfig(pageSize = PAGE_SIZE),
                    pagingSourceFactory = { ReceiverAfternotePagingSource(api, category) },
                ).flow
            }

        override suspend fun getAfterNotesByAuthCode(authCode: String): Result<AfterNotesListResult> =
            Result.success(
                AfterNotesListResult(
                    items = emptyList(),
                    totalCount = 0,
                ),
            )

        override suspend fun getAfternoteDetailByAuthCode(
            authCode: String,
            afternoteId: Long,
        ): Result<ReceivedAfternoteDetail> = Result.failure(IllegalStateException("Receiver afternote detail not wired"))

        override suspend fun downloadAllReceived(authCode: String): Result<ReceivedExportBundle> = Result.success(ReceivedExportBundle())

        override suspend fun saveReceivedExportToFile(bundle: ReceivedExportBundle): Result<Unit> = Result.success(Unit)

        override suspend fun loadMindRecordsCount(authCode: String): Result<LoadCountResult> = Result.success(LoadCountResult(0))

        override suspend fun loadTimeLettersCount(authCode: String): Result<LoadCountResult> = Result.success(LoadCountResult(0))

        override suspend fun loadSenderMessage(authCode: String): Result<String?> = Result.success(null)
    }
