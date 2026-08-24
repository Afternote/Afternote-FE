package com.afternote.feature.mindrecord.presentation.screen.receiver

import com.afternote.core.network.model.ApiException
import com.afternote.core.ui.UiText
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
 */
class ReceiverDeadEndTest {
    @Test
    fun `전달 조건 미충족은 기다리는 상태임을 알리는 문구가 된다`() {
        val error = ApiException(status = 403, code = 2009, serverMessage = "아직 전달 조건이 충족되지 않았습니다.", message = "아직 전달 조건이 충족되지 않았습니다.")

        assertEquals(UiText.Resource(R.string.mindrecord_receiver_delivery_not_ready), error.toDomainMessage())
    }

    @Test
    fun `서버 원문을 화면 문구로 쓰지 않는다`() {
        // 어떤 코드가 오든 결과는 리소스 문구다 — 원문이 새어 나갈 자리가 없다.
        val message =
            ApiException(
                status = 500,
                code = 1004,
                serverMessage = "서버 내부 오류가 발생했습니다.",
                message = "서버 내부 오류가 발생했습니다.",
            ).toDomainMessage()

        assertEquals(UiText.Resource(R.string.mindrecord_receiver_load_failed), message)
    }

    @Test
    fun `네트워크 예외도 같은 실패 문구로 수렴한다`() {
        assertEquals(
            UiText.Resource(R.string.mindrecord_receiver_load_failed),
            IOException("timeout").toDomainMessage(),
        )
    }
}
