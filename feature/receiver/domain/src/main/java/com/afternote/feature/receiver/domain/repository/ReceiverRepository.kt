package com.afternote.feature.receiver.domain.repository

import androidx.paging.PagingData
import com.afternote.feature.receiver.domain.model.AfterNoteListItem
import com.afternote.feature.receiver.domain.model.AfterNotesListResult
import com.afternote.feature.receiver.domain.model.ReceivedAfternoteDetail
import com.afternote.feature.receiver.domain.model.ReceivedExportBundle
import com.afternote.feature.receiver.domain.model.SenderMessageInfo
import kotlinx.coroutines.flow.Flow

/**
 * 수신자(auth code) 플로우의 데이터 접근. ViewModel은 이 인터페이스만 의존합니다.
 *
 * `X-Auth-Code` 헤더는 네트워크 계층의 ReceiverAuthInterceptor가 자동 부착하므로
 * 호출자는 인증 코드를 메서드 인자로 들고 다닐 필요가 없습니다.
 *
 * **실패 계약은 [ReceiverAuthRepository] 와 같다(#1053)** — 서버를 부르는 조회는 단발이든 페이징이든
 * [com.afternote.feature.receiver.domain.error.ReceiverFailure] 로 실패를 돌려준다. 같은 서버 사유가
 * 경로에 따라 다른 타입으로 오면 화면이 «전달 조건 미충족»·«연결 없음» 을 한 기준으로 가를 수 없다.
 */
interface ReceiverRepository {
    /** 저장된 인증 코드 스트림(없거나 공백만 있으면 null 방출). */
    val masterKeyFlow: Flow<String?>

    /** 단발 조회; UI 스레드에서는 코루틴 안에서 호출하세요. */
    suspend fun currentMasterKey(): String?

    /** 사용자가 입력·검증한 코드를 저장합니다. 제거는 로그아웃 일괄 정리(SESSION scope, #912)가 담당합니다. */
    suspend fun saveMasterKey(code: String)

    /**
     * 수신 애프터노트 스트림. 서버는 페이지네이션 미지원이므로 단일 페이지로 받지만,
     * Paging 3 API(LoadState/refresh/cachedIn) 통일과 추후 페이지네이션 도입을 위해 PagingData로 노출한다.
     */
    fun getPagedReceivedAfternotes(): Flow<PagingData<AfterNoteListItem>>

    suspend fun getReceivedAfterNotes(): Result<AfterNotesListResult>

    suspend fun getReceivedAfternoteDetail(afternoteId: Long): Result<ReceivedAfternoteDetail>

    /** 서버 export 계약 도입 전에는 [com.afternote.feature.receiver.domain.error.ReceiverFailure.ExportNotSupported]. */
    suspend fun downloadReceivedExport(): Result<ReceivedExportBundle>

    /** export 저장 구현 도입 전에는 [com.afternote.feature.receiver.domain.error.ReceiverFailure.ExportNotSupported]. */
    suspend fun saveReceivedExportToFile(bundle: ReceivedExportBundle): Result<Unit>

    suspend fun loadSenderMessage(): Result<SenderMessageInfo?>
}
