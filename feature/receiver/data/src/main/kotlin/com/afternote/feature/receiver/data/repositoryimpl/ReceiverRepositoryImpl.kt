package com.afternote.feature.receiver.data.repositoryimpl

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.common.result.runCatchingCancellable
import com.afternote.core.network.model.requireData
import com.afternote.feature.receiver.data.error.mapReceiverFailure
import com.afternote.feature.receiver.data.local.ReceiverMasterKeyDataSource
import com.afternote.feature.receiver.data.mapper.response.toDomain
import com.afternote.feature.receiver.data.mapper.toDomainResult
import com.afternote.feature.receiver.data.paging.ReceiverAfternotePagingSource
import com.afternote.feature.receiver.data.service.ReceiverAfternoteApiService
import com.afternote.feature.receiver.domain.model.AfterNoteListItem
import com.afternote.feature.receiver.domain.model.AfterNotesListResult
import com.afternote.feature.receiver.domain.model.ReceivedAfternoteDetail
import com.afternote.feature.receiver.domain.model.ReceivedExportBundle
import com.afternote.feature.receiver.domain.model.SenderMessageInfo
import com.afternote.feature.receiver.domain.repository.ReceiverAuthRepository
import com.afternote.feature.receiver.domain.repository.ReceiverRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private const val PAGE_SIZE = 50

/**
 * 인증 코드는 [ReceiverMasterKeyDataSource]에서 읽고·쓰고·지우며, REST 요청에는 ReceiverAuthInterceptor가
 * `X-Auth-Code` 헤더를 자동 부착한다. 아직 별도 기능 repository로 이관되지 않은 export 항목은
 * 폴백 값을 반환한다.
 *
 * 서버를 부르는 메서드는 [ReceiverAuthRepositoryImpl] 과 같은 규약으로
 * [com.afternote.feature.receiver.data.error.mapReceiverFailure] 를 거친다 — 목록·상세도 페이징 경로
 * ([com.afternote.feature.receiver.data.paging.ReceiverAfternotePagingSource]) 와 같은 도메인 어휘로
 * 실패를 내보내야 화면이 «전달 조건 미충족»·«연결 없음» 을 같은 기준으로 가른다.
 * [loadSenderMessage] 는 [ReceiverAuthRepository] 로 위임하므로 그쪽에서 이미 번역된 실패를 받는다.
 */
@Singleton
class ReceiverRepositoryImpl
    @Inject
    constructor(
        private val masterKeyDataSource: ReceiverMasterKeyDataSource,
        private val api: ReceiverAfternoteApiService,
        private val receiverAuthRepository: ReceiverAuthRepository,
        private val errorReporter: ErrorReporter,
    ) : ReceiverRepository {
        override val masterKeyFlow: Flow<String?> = masterKeyDataSource.savedCodeFlow

        override suspend fun currentMasterKey(): String? = masterKeyFlow.first()

        override suspend fun saveMasterKey(code: String) {
            masterKeyDataSource.saveCode(code)
        }

        override fun getPagedReceivedAfternotes(): Flow<PagingData<AfterNoteListItem>> =
            Pager(
                config = PagingConfig(pageSize = PAGE_SIZE),
                pagingSourceFactory = { ReceiverAfternotePagingSource(api, errorReporter) },
            ).flow

        override suspend fun getReceivedAfterNotes(): Result<AfterNotesListResult> =
            runCatchingCancellable {
                api
                    .getReceiverAfternotes()
                    .requireData()
                    .toDomainResult(errorReporter)
            }.mapReceiverFailure()

        override suspend fun getReceivedAfternoteDetail(afternoteId: Long): Result<ReceivedAfternoteDetail> =
            runCatchingCancellable {
                api
                    .getReceiverAfternoteDetail(afternoteId = afternoteId)
                    .requireData()
                    .toDomain()
            }.mapReceiverFailure()

        override suspend fun downloadReceivedExport(): Result<ReceivedExportBundle> = Result.success(ReceivedExportBundle())

        override suspend fun saveReceivedExportToFile(bundle: ReceivedExportBundle): Result<Unit> = Result.success(Unit)

        // sender 메시지 조회는 receiver-auth 영역 → ReceiverAuthRepository 가 책임.
        // 본 Repository 는 위임만 — 같은 API 호출이 두 곳에 중복되지 않게 (Multiple levels of repositories).
        override suspend fun loadSenderMessage(): Result<SenderMessageInfo?> = receiverAuthRepository.getSenderMessage().map { it }
    }
