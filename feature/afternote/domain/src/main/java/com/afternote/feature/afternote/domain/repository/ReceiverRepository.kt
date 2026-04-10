package com.afternote.feature.afternote.domain.repository

import com.afternote.feature.afternote.domain.model.receiver.AfterNotesListResult
import com.afternote.feature.afternote.domain.model.receiver.LoadCountResult
import com.afternote.feature.afternote.domain.model.receiver.ReceivedAfternoteDetail
import com.afternote.feature.afternote.domain.model.receiver.ReceivedExportBundle
import kotlinx.coroutines.flow.Flow

/**
 * 수신자(auth code) 플로우의 데이터 접근. ViewModel은 이 인터페이스만 의존합니다.
 */
interface ReceiverRepository {
    /** 저장된 인증 코드 스트림(없거나 공백만 있으면 null 방출). */
    val authCodeFlow: Flow<String?>

    /** 단발 조회; UI 스레드에서는 코루틴 안에서 호출하세요. */
    suspend fun currentAuthCode(): String?

    /** 사용자가 입력·검증한 코드를 저장합니다. */
    suspend fun saveAuthCode(code: String)

    /** 로그아웃·계정 전환·초기화 시 저장 코드를 제거합니다. */
    suspend fun clearAuthCode()

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
