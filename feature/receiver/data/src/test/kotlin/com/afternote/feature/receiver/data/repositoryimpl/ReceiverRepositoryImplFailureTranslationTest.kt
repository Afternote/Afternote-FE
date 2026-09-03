package com.afternote.feature.receiver.data.repositoryimpl

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.network.model.ApiException
import com.afternote.core.network.model.BaseResponse
import com.afternote.feature.receiver.data.dto.ReceivedAfternoteDetailDto
import com.afternote.feature.receiver.data.dto.ReceivedAfternoteListDto
import com.afternote.feature.receiver.data.local.ReceiverMasterKeyDataSource
import com.afternote.feature.receiver.data.service.ReceiverAfternoteApiService
import com.afternote.feature.receiver.domain.error.ReceiverFailure
import com.afternote.feature.receiver.domain.testing.FakeReceiverAuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * 수신 목록·상세 조회가 실패를 도메인 어휘로 내보내는지의 회귀 가드(#1053).
 *
 * 같은 목록을 읽는 페이징 경로([com.afternote.feature.receiver.data.paging.ReceiverAfternotePagingSource])
 * 는 #611 에서 번역을 붙였지만 이 저장소의 단발 조회 두 개는 `ApiException`(HTTP status·BE
 * `ErrorCode` 번호·서버 문구)을 도메인 밖으로 그대로 흘렸다. 같은 서버 사유가 어느 경로로 들어왔는지에
 * 따라 화면 처리가 갈리면(«전달 조건 미충족» 이 한쪽에서만 재시도 없는 안내가 된다) 소비처는 그 차이를
 * 알 방법이 없다.
 *
 * code·status 는 2026-07-30 실기기 logcat 캡처 —
 * `<-- 403` `{"status":403,"code":2009,"message":"아직 전달 조건이 충족되지 않았습니다."}`.
 */
class ReceiverRepositoryImplFailureTranslationTest {
    @Test
    fun `목록 조회의 전달 조건 미충족 403 은 전용 도메인 타입으로 나온다`() {
        val original = deliveryConditionNotMet()
        val repository = repository(FailingReceiverAfternoteApiService { throw original })

        val exception = runBlocking { repository.getReceivedAfterNotes() }.exceptionOrNull()

        assertTrue("$exception", exception is ReceiverFailure.DeliveryConditionNotMet)
        assertEquals(original, exception?.cause)
    }

    @Test
    fun `목록 조회의 전송 계층 실패는 연결 불가로 나온다`() {
        val original = IOException("Unable to resolve host")
        val repository = repository(FailingReceiverAfternoteApiService { throw original })

        val exception = runBlocking { repository.getReceivedAfterNotes() }.exceptionOrNull()

        assertTrue("$exception", exception is ReceiverFailure.NetworkUnavailable)
        assertEquals(original, exception?.cause)
    }

    @Test
    fun `상세 조회의 전달 조건 미충족 403 은 전용 도메인 타입으로 나온다`() {
        val original = deliveryConditionNotMet()
        val repository = repository(FailingReceiverAfternoteApiService { throw original })

        val exception = runBlocking { repository.getReceivedAfternoteDetail(afternoteId = 7L) }.exceptionOrNull()

        assertTrue("$exception", exception is ReceiverFailure.DeliveryConditionNotMet)
        assertEquals(original, exception?.cause)
    }

    /** 매핑 실패처럼 도메인 어휘가 없는 실패는 원인 타입 그대로 나가야 리포팅에서 구분된다. */
    @Test
    fun `분류 대상이 아닌 실패는 원본 그대로 나온다`() {
        val original = IllegalStateException("boom")
        val repository = repository(FailingReceiverAfternoteApiService { throw original })

        val exception = runBlocking { repository.getReceivedAfterNotes() }.exceptionOrNull()

        assertEquals(original, exception)
    }

    private fun deliveryConditionNotMet(): ApiException =
        ApiException(
            status = 403,
            code = 2009,
            serverMessage = "아직 전달 조건이 충족되지 않았습니다.",
            fallbackMessage = "아직 전달 조건이 충족되지 않았습니다.",
        )

    private fun repository(api: ReceiverAfternoteApiService): ReceiverRepositoryImpl =
        ReceiverRepositoryImpl(
            masterKeyDataSource = ReceiverMasterKeyDataSource(EmptyPreferencesDataStore()),
            api = api,
            receiverAuthRepository = FakeReceiverAuthRepository(),
            errorReporter = SilentErrorReporter(),
        )
}

/** 목록·상세 두 endpoint 가 같은 실패를 던지는 fake. */
private class FailingReceiverAfternoteApiService(
    private val failure: suspend () -> Nothing,
) : ReceiverAfternoteApiService {
    override suspend fun getReceiverAfternotes(): BaseResponse<ReceivedAfternoteListDto> = failure()

    override suspend fun getReceiverAfternoteDetail(afternoteId: Long): BaseResponse<ReceivedAfternoteDetailDto> = failure()
}

/** 인증 코드는 이 테스트의 관심사가 아니다 — 저장된 코드가 없는 상태만 흉내 낸다. */
private class EmptyPreferencesDataStore : DataStore<Preferences> {
    override val data: Flow<Preferences> = flowOf(emptyPreferences())

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences = transform(emptyPreferences())
}

private class SilentErrorReporter : ErrorReporter {
    override fun writeFailure(
        throwable: Throwable,
        attributes: Map<String, String>,
    ) = Unit
}
