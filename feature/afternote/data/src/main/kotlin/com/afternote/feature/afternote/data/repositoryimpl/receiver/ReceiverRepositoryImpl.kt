package com.afternote.feature.afternote.data.repositoryimpl.receiver

import com.afternote.feature.afternote.data.local.ReceiverAuthCodeLocalDataSource
import com.afternote.feature.afternote.domain.model.receiver.AfterNotesListResult
import com.afternote.feature.afternote.domain.model.receiver.LoadCountResult
import com.afternote.feature.afternote.domain.model.receiver.ReceivedAfternoteDetail
import com.afternote.feature.afternote.domain.model.receiver.ReceivedExportBundle
import com.afternote.feature.afternote.domain.repository.ReceiverRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 인증 코드는 [ReceiverAuthCodeLocalDataSource]에서 읽고·쓰고·지우며, 수신 REST는 아직 미연동이면 아래 폴백 값을 반환합니다.
 * 작성자 애프터노트 HTTP는 DEBUG에서 `AfternoteDebugMockNetworkInterceptor`가 가로챕니다.
 */
@Singleton
class ReceiverRepositoryImpl
    @Inject
    constructor(
        private val localDataSource: ReceiverAuthCodeLocalDataSource,
    ) : ReceiverRepository {
        override fun currentAuthCode(): String? = localDataSource.getSavedCode()

        override fun saveAuthCode(code: String) {
            localDataSource.saveCode(code)
        }

        override fun clearAuthCode() {
            localDataSource.clearCode()
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
