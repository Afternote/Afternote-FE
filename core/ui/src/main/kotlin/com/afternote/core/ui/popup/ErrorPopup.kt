package com.afternote.core.ui.popup

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.afternote.core.ui.R
import com.afternote.core.ui.button.AfternoteButton
import com.afternote.core.ui.button.AfternoteButtonType
import com.afternote.core.ui.theme.AfternoteDesign

/** 시안 아이콘 원 배경(#FEE2E2)의 파생 표현 — error 12% on white ≈ #FFE2E2 (R 채널 1/255 차 근사). */
private const val ICON_CONTAINER_ALPHA = 0.12f

/**
 * 오류 안내 팝업 — 아이콘 원 + 제목 + 본문 + 단일 액션. 시안의 오류 팝업 4종(네트워크 연결
 * 오류·서버 오류·업로드 실패·접근 권한 없음, `3628:23827`)이 공유하는 골격이다. 접근 권한 없음(403)은
 * 아직 생산자가 없어 세우지 않았다.
 *
 * [Popup] 과 별개인 이유 — 그쪽은 "메시지 + 버튼" 골격이라 아이콘·제목 슬롯이 없고,
 * [PopupType] 은 enum 이라 타입별 필수 값을 담을 수 없다.
 */
@Composable
fun AfternoteErrorPopup(
    @DrawableRes iconRes: Int,
    title: String,
    description: String,
    buttonText: String,
    onButtonClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Dialog(onDismissRequest = onDismiss) {
        AfternoteErrorPopupContent(
            iconRes = iconRes,
            title = title,
            description = description,
            buttonText = buttonText,
            onButtonClick = onButtonClick,
            modifier = modifier,
        )
    }
}

/**
 * 네트워크 연결 실패 안내(시안 `3628:23575`). 문구는 이 컴포넌트가 갖고 재시도 동작만 받는다 —
 * 전송 계층 실패 안내는 화면과 무관하게 동일해서다.
 */
@Composable
fun NetworkErrorPopup(
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AfternoteErrorPopup(
        iconRes = R.drawable.core_ui_ic_wifi_off,
        title = stringResource(R.string.core_ui_network_error_title),
        description = stringResource(R.string.core_ui_network_error_description),
        buttonText = stringResource(R.string.core_ui_network_error_retry),
        onButtonClick = onRetry,
        onDismiss = onDismiss,
        modifier = modifier,
    )
}

/**
 * 서버가 응답했지만 요청을 처리하지 못한 실패 안내(시안 `3628:23785`). 5xx 처럼 사용자가 고칠 수
 * 없는 장애가 대상이라 액션은 재시도 하나다 — 서버가 준 거절 문구는 이쪽이 아니라 호출처가
 * 자기 문구로 안내한다.
 */
@Composable
fun ServerErrorPopup(
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AfternoteErrorPopup(
        iconRes = R.drawable.core_ui_ic_server,
        title = stringResource(R.string.core_ui_server_error_title),
        description = stringResource(R.string.core_ui_server_error_description),
        buttonText = stringResource(R.string.core_ui_server_error_retry),
        onButtonClick = onRetry,
        onDismiss = onDismiss,
        modifier = modifier,
    )
}

/**
 * 파일 업로드 실패 안내(시안 `3628:23795`). [ServerErrorPopup] 과 갈라 둔 이유 — 사용자가 방금 고른
 * 파일이 올라가지 못했다는 사실이 "서버에 문제가 발생했다" 보다 구체적이고, 재시도의 대상도
 * 화면 전체가 아니라 그 첨부 하나다.
 */
@Composable
fun UploadErrorPopup(
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AfternoteErrorPopup(
        iconRes = R.drawable.core_ui_ic_upload_cloud,
        title = stringResource(R.string.core_ui_upload_error_title),
        description = stringResource(R.string.core_ui_upload_error_description),
        buttonText = stringResource(R.string.core_ui_upload_error_retry),
        onButtonClick = onRetry,
        onDismiss = onDismiss,
        modifier = modifier,
    )
}

/**
 * Dialog 래퍼 없이 카드 본체만 렌더링하는 [AfternoteErrorPopup] 내부 구현.
 *
 * 파일 밖으로 열지 않는다 — 공개 계약은 [AfternoteErrorPopup] 과 그 위에 선
 * [NetworkErrorPopup]·[ServerErrorPopup]·[UploadErrorPopup] 이고, 시각 회귀 baseline 도
 * 그 진입점을 그려서 잡는다 (#1672).
 */
@Composable
private fun AfternoteErrorPopupContent(
    @DrawableRes iconRes: Int,
    title: String,
    description: String,
    buttonText: String,
    onButtonClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AfternotePopupCardLayout(modifier = modifier) {
        Box(
            modifier =
                Modifier
                    .size(48.dp)
                    .background(
                        color = AfternoteDesign.colors.error.copy(alpha = ICON_CONTAINER_ALPHA),
                        shape = CircleShape,
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = AfternoteDesign.colors.error,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = title,
            style = AfternoteDesign.typography.h3.copy(textAlign = TextAlign.Center),
            color = AfternoteDesign.colors.gray9,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = description,
            style = AfternoteDesign.typography.bodySmallR.copy(textAlign = TextAlign.Center),
            color = AfternoteDesign.colors.gray6,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(24.dp))

        AfternoteButton(
            text = buttonText,
            onClick = onButtonClick,
            type = AfternoteButtonType.Default,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
