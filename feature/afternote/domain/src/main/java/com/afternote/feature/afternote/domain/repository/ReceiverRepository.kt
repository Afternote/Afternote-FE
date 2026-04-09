package com.afternote.feature.afternote.domain.repository

import com.afternote.feature.afternote.domain.model.receiver.AfterNotesListResult
import com.afternote.feature.afternote.domain.model.receiver.LoadCountResult
import com.afternote.feature.afternote.domain.model.receiver.ReceivedAfternoteDetail
import com.afternote.feature.afternote.domain.model.receiver.ReceivedExportBundle

/**
 * 수신자(auth code) 플로우의 데이터 접근. ViewModel은 이 인터페이스만 의존합니다.
 */
interface ReceiverRepository {
    fun currentAuthCode(): String?

    /** Persists the code the user entered (e.g. after successful verification). */
    fun saveAuthCode(code: String)

    /** Clears persisted code (logout, account switch, user reset). */
    fun clearAuthCode()

    suspend fun getAfterNotesByAuthCode(authCode: String): Result<AfterNotesListResult>

    suspend fun getAfternoteDetailByAuthCode(
        authCode: String,
        afternoteId: Long,
    ): Result<ReceivedAfternoteDetail>

    suspend fun downloadAllReceived(authCode: String): Result<ReceivedExportBundle>

    suspend fun saveReceivedExportToFile(bundle: ReceivedExportBundle): Result<Unit>

    suspend fun loadMindRecordsCount(authCode: String): Result<LoadCountResult>

    suspend fun loadTimeLettersCount(authCode: String): Result<LoadCountResult>

    suspend fun loadSenderMessage(authCode: String): Result<String?>
}
