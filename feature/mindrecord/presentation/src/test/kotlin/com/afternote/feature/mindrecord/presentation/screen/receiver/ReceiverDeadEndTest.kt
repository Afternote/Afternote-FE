package com.afternote.feature.mindrecord.presentation.screen.receiver

import com.afternote.core.ui.UiText
import com.afternote.feature.mindrecord.domain.error.DeliveryNotReadyException
import com.afternote.feature.mindrecord.presentation.R
import com.afternote.feature.mindrecord.presentation.viewmodel.toDomainMessage
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

/**
 * 수신자 열람 실패 문구 가드 (#614).
 *
 * 종전에는 서버 응답의 `message` 를 그대로 화면에 실어 «아직 전달 조건이 충족되지
 * 않았습니다» 라는 원문이 그대로 노출됐다. 사용자는 "전달 조건" 이 무엇인지, 자기가
 * 무엇을 해야 하는지 알 수 없었다 — 실제로는 발신자가 설정해야 풀리는 상태라
 * 수신자가 할 수 있는 일이 없다.
 *
 * 화면은 **도메인 예외 타입만** 본다. 어떤 서버 코드가 그 타입이 되는지는 data 계층의
 * `mapReceiverFailure` 가 알고, 그쪽 계약은 `MindRecordReceiverRepositoryImplTest` 가 잡는다.
 */
class ReceiverDeadEndTest {
    @Test
    fun `전달 조건 미충족은 기다리는 상태임을 알리는 문구가 된다`() {
        assertEquals(
            UiText.Resource(R.string.mindrecord_receiver_delivery_not_ready),
            DeliveryNotReadyException().toDomainMessage(),
        )
    }

    @Test
    fun `서버 원문을 화면 문구로 쓰지 않는다`() {
        // 결과 타입이 UiText.Resource 라 원문이 들어갈 자리 자체가 없다.
        assertEquals(
            UiText.Resource(R.string.mindrecord_receiver_load_failed),
            IllegalStateException("서버 내부 오류가 발생했습니다.").toDomainMessage(),
        )
    }

    @Test
    fun `네트워크 예외도 같은 실패 문구로 수렴한다`() {
        assertEquals(
            UiText.Resource(R.string.mindrecord_receiver_load_failed),
            IOException("timeout").toDomainMessage(),
        )
    }
}
