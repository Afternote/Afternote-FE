package com.afternote.feature.receiver.presentation.error

import androidx.compose.runtime.Composable
import com.afternote.core.ui.popup.NetworkErrorPopup
import com.afternote.core.ui.popup.ServerErrorPopup
import com.afternote.core.ui.popup.UploadErrorPopup
import com.afternote.feature.receiver.domain.error.ReceiverFailure

/**
 * 수신자 화면이 띄울 공통 오류 팝업의 갈래 (#446).
 *
 * 화면마다 `showXxxPopup: Boolean` 을 여러 개 두지 않고 단일 nullable 필드로 운반한다 — 불리언을
 * 늘리면 «서버 오류와 업로드 실패가 동시에 true» 같은 표현 불가능한 상태가 타입에 남는다.
 * 로그인 화면([com.afternote.feature.onboarding.presentation.login.LoginUiState])은 갈래가 하나뿐이라
 * 불리언으로 족했지만 여기는 셋이다.
 */
enum class ReceiverErrorPopup {
    /** 서버에 닿지도 못한 실패 — 안내는 "연결을 확인하라". */
    NETWORK,

    /** 서버가 응답했지만 처리하지 못한 실패 — 안내는 "잠시 후 다시". */
    SERVER,

    /** 파일 업로드 경로의 실패 — 재시도 대상이 화면이 아니라 그 첨부 하나다. */
    UPLOAD,
}

/**
 * 실패를 팝업 갈래로 옮긴다. `null` 은 «팝업으로 안내하지 않는다» 는 뜻이고, 그 자리는 호출처가
 * 기존대로 [toReceiverErrorUiText] 로 문구(스낵바)를 만든다.
 *
 * 사용자 거절을 팝업에서 제외하는 이유 — 서버가 이미 사람이 읽을 사유를 줬고, 그 사유는 사용자가
 * 입력을 고쳐 스스로 푸는 것이다. "다시 시도하기" 버튼을 주면 같은 입력으로 같은 거절을 반복하게
 * 만든다. 시안 4종에 그 갈래가 없는 것도 같은 이유다.
 *
 * @param uploadPath 실패가 파일 업로드 경로에서 났는지. 같은 [ReceiverFailure.UnexpectedServerFailure]
 *   라도 업로드 중이면 "업로드 실패" 가 사용자에게 더 정확하다.
 */
internal fun Throwable.toReceiverErrorPopupOrNull(uploadPath: Boolean = false): ReceiverErrorPopup? {
    val serverSide = if (uploadPath) ReceiverErrorPopup.UPLOAD else ReceiverErrorPopup.SERVER
    // 루트로 좁혀 `when` 을 exhaustive 하게 유지한다 — 수신자 실패 유형이 늘면 여기가 컴파일
    // 에러로 잡힌다. `else` 로 뭉개면 팝업으로 갈라야 할 새 유형이 조용히 스낵바로 흘러간다.
    return when (this as? ReceiverFailure) {
        is ReceiverFailure.NetworkUnavailable -> ReceiverErrorPopup.NETWORK

        is ReceiverFailure.UnexpectedServerFailure -> serverSide

        is ReceiverFailure.UserRejection -> null

        is ReceiverFailure.DeliveryConditionNotMet -> null

        // Data 계층이 번역하지 못한 실패(로컬 예외 등). 사유를 모르는 채 재시도 이상을 안내할
        // 근거가 없으므로 서버 쪽 갈래로 보낸다.
        null -> serverSide
    }
}

/** [ReceiverErrorPopup] 갈래를 시안 4종의 팝업으로 그린다. `null` 이면 아무것도 그리지 않는다. */
@Composable
internal fun ReceiverErrorPopupHost(
    popup: ReceiverErrorPopup?,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    when (popup) {
        ReceiverErrorPopup.NETWORK -> NetworkErrorPopup(onRetry = onRetry, onDismiss = onDismiss)
        ReceiverErrorPopup.SERVER -> ServerErrorPopup(onRetry = onRetry, onDismiss = onDismiss)
        ReceiverErrorPopup.UPLOAD -> UploadErrorPopup(onRetry = onRetry, onDismiss = onDismiss)
        null -> Unit
    }
}
