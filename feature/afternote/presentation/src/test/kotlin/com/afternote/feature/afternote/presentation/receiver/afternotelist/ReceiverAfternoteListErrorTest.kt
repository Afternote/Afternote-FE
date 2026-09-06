package com.afternote.feature.afternote.presentation.receiver.afternotelist

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
        val serverOutage = ReceiverFailure.UnexpectedServerFailure(CAUSE)
        val otherRejection = ReceiverFailure.UserRejection(reason = null, cause = CAUSE)

        assertNull(serverOutage.toListError())
        assertNull(otherRejection.toListError())
        assertNull(IllegalStateException("boom").toListError())
    }
}

/** 프로덕션에서는 원인 자리에 `ApiException` 이 들어오지만, presentation 테스트는 network 를 알지 않는다. */
private val CAUSE: Throwable = IOException("stub cause")
