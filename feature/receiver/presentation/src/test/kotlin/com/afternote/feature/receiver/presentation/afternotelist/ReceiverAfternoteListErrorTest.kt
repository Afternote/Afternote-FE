package com.afternote.feature.receiver.presentation.afternotelist

import com.afternote.feature.receiver.domain.error.ReceiverFailure
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.IOException

/**
 * 목록 실패 → 화면 처리 판정 가드 (#611).
 *
 * 세 갈래가 각각 다른 화면을 부른다 — 재시도 없는 안내 / 연결 안내 + 재시도 / 종전 목록 화면.
 * 판정이 무너지면 «재시도로 풀리지 않는 실패에 재시도 버튼» 이 되돌아온다.
 */
class ReceiverAfternoteListErrorTest {
    @Test
    fun `전달 조건 미충족은 전용 갈래로 간다`() {
        val notDeliverable = ReceiverFailure.DeliveryConditionNotMet(CAUSE)

        assertEquals(ReceiverAfternoteListError.NotDeliverable, notDeliverable.toListError())
    }

    @Test
    fun `연결 실패는 전용 갈래로 간다`() {
        val offline = ReceiverFailure.NetworkUnavailable(IOException("timeout"))

        assertEquals(ReceiverAfternoteListError.NetworkUnavailable, offline.toListError())
    }

    /** null 은 «갈라 그릴 것이 없다» 다 — 재시도가 유효하다는 점에서 일반 실패와 처리가 같다. */
    @Test
    fun `그 밖의 실패는 종전 목록 화면 경로에 남는다`() {
        val serverOutage =
            ReceiverFailure.ServerRejection(status = 500, serverMessage = "internal error", serverCode = 1500, cause = CAUSE)
        val otherRejection =
            ReceiverFailure.ServerRejection(status = 403, serverMessage = "권한이 없습니다.", serverCode = 1903, cause = CAUSE)

        assertNull(serverOutage.toListError())
        assertNull(otherRejection.toListError())
        assertNull(IllegalStateException("boom").toListError())
    }
}

/**
 * ServerRejection 이 나르는 원인 예외 자리. 프로덕션에서는 `ApiException` 이 들어오지만, 도메인 계약이
 * 요구하는 것은 `Throwable` 뿐이라 이 테스트들은 core:network 를 끌어오지 않는다.
 */
private val CAUSE: Throwable = IOException("stub cause")
