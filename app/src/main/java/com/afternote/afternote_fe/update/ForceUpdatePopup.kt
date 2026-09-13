package com.afternote.afternote_fe.update

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.afternote.afternote_fe.R
import com.afternote.core.ui.popup.Popup
import com.afternote.core.ui.popup.PopupType

/**
 * 서버가 더 이상 허용하지 않는 버전임을 알리고 스토어로 보내는 팝업 (#1539).
 *
 * 닫는 길을 주지 않는다 — 뒤로가기·바깥 탭이 부르는 `onDismiss` 를 비워 두면 팝업이 사라지지
 * 않는다. 취소를 허용하면 관문이 관문이 아니게 된다. 사라지는 유일한 조건은 서버가 다시
 * 「업데이트 불필요」를 말하는 것뿐이고, 그건 앱을 다시 켤 때 확인한다.
 *
 * 시안이 없다(Figma 정본 페이지 전수 조회 0건). 새 화면을 그리는 대신 앱 공용 팝업
 * ([PopupType.Default]) 을 쓴다 — 디자이너 확정이 붙으면 그 자리에서 교체한다.
 */
@Composable
fun ForceUpdatePopup(onConfirm: () -> Unit) {
    Popup(
        type = PopupType.Default,
        message = stringResource(R.string.force_update_message),
        onConfirm = onConfirm,
        onDismiss = {},
        confirmText = stringResource(R.string.force_update_confirm),
    )
}
