package com.afternote.feature.afternote.data.repositoryimpl.receiver

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.afternote.core.common.result.runCatchingCancellable
import com.afternote.core.network.model.requireData
import com.afternote.feature.afternote.data.dto.toReceivedRecordBox
import com.afternote.feature.afternote.data.local.ReceiverAuthCodeDataSource
import com.afternote.feature.afternote.data.mapper.response.toDomain
import com.afternote.feature.afternote.data.paging.ReceiverAfternotePagingSource
import com.afternote.feature.afternote.data.service.ReceiverAfternoteApiService
import com.afternote.feature.afternote.data.service.ReceiverAuthApiService
import com.afternote.feature.afternote.domain.model.receiver.AfterNoteListItem
import com.afternote.feature.afternote.domain.model.receiver.AfterNotesListResult
import com.afternote.feature.afternote.domain.model.receiver.LoadCountResult
import com.afternote.feature.afternote.domain.model.receiver.ReceivedAfternoteDetail
import com.afternote.feature.afternote.domain.model.receiver.ReceivedExportBundle
import com.afternote.feature.afternote.domain.model.receiver.ReceivedRecordBox
import com.afternote.feature.afternote.domain.repository.receiver.ReceiverRepository
import com.afternote.feature.receiver.domain.model.SenderMessageInfo
import com.afternote.feature.receiver.domain.repository.ReceiverAuthRepository
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
        private val receiverAuthApi: ReceiverAuthApiService,
        private val receiverAuthRepository: ReceiverAuthRepository,
    ) : ReceiverRepository {
        override val authCodeFlow: Flow<String?> = authCodeDataSource.savedCodeFlow

        override suspend fun currentAuthCode(): String? = authCodeFlow.first()

        override suspend fun saveAuthCode(code: String) {
            authCodeDataSource.saveCode(code)
        }

        override suspend fun clearAuthCode() {
            authCodeDataSource.clearCode()
        }

        override suspend fun getReceivedRecordBoxes(): Result<List<ReceivedRecordBox>> =
            runCatchingCancellable {
                receiverAuthApi
                    .getReceivedRecordBoxes()
                    .requireData()
                    .recordBoxes
                    .map { it.toReceivedRecordBox() }
            }

        override fun getPagedReceivedAfternotes(): Flow<PagingData<AfterNoteListItem>> =
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
            runCatchingCancellable {
                api
                    .getReceiverAfternoteDetail(afternoteId = afternoteId)
                    .requireData()
                    .toDomain()
            }

        override suspend fun downloadReceivedExport(): Result<ReceivedExportBundle> = Result.success(ReceivedExportBundle())

        override suspend fun saveReceivedExportToFile(bundle: ReceivedExportBundle): Result<Unit> = Result.success(Unit)

        override suspend fun loadMindRecordsCount(): Result<LoadCountResult> = Result.success(LoadCountResult(0))

        override suspend fun loadTimeLettersCount(): Result<LoadCountResult> = Result.success(LoadCountResult(0))

        // sender 메시지 조회는 receiver-auth 영역 → ReceiverAuthRepository 가 책임.
        // 본 Repository 는 위임만 — 같은 API 호출이 두 곳에 중복되지 않게 (Multiple levels of repositories).
        override suspend fun loadSenderMessage(): Result<SenderMessageInfo?> = receiverAuthRepository.getSenderMessage().map { it }
    }
