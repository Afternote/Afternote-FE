package com.afternote.feature.home.presentation.receiver

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.afternote.core.ui.popup.AfternoteErrorPopup
import com.afternote.core.ui.popup.NetworkErrorPopup
import com.afternote.core.ui.popup.ServerErrorPopup
import com.afternote.feature.home.presentation.R
import com.afternote.feature.home.presentation.receiver.model.ReceiverDownloadErrorPopup
import com.afternote.core.ui.R as CoreUiR

/**
 * 내려받기 실패 팝업 3종을 그린다 (#446 · #1737).
 *
 * 저장 실패만 시안 4종(네트워크·서버·업로드·권한, `3628:23827`)에 대응 항목이 없다.
 * 업로드 팝업을 돌려쓰면 화면에 「파일 업로드에 실패했습니다」 가 뜨는데, 이 경로는
 * 내려받은 파일을 로컬에 저장하는 중이라 사실과 다르다. 그래서 같은 골격
 * ([AfternoteErrorPopup])에 이 화면의 저장 실패 문구를 실어 그린다 — 시안 밖의 새 모양이
 * 아니라 4종이 공유하는 그 골격이다.
 */
@Composable
internal fun ReceiverDownloadErrorPopupHost(
    popup: ReceiverDownloadErrorPopup,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    when (popup) {
        ReceiverDownloadErrorPopup.NETWORK -> {
            NetworkErrorPopup(onRetry = onRetry, onDismiss = onDismiss)
        }

        ReceiverDownloadErrorPopup.SERVER -> {
            ServerErrorPopup(onRetry = onRetry, onDismiss = onDismiss)
        }

        ReceiverDownloadErrorPopup.SAVE -> {
            AfternoteErrorPopup(
                iconRes = CoreUiR.drawable.core_ui_ic_server,
                title = stringResource(R.string.home_receiver_download_save_error_title),
                description = stringResource(R.string.home_receiver_download_save_error_description),
                buttonText = stringResource(CoreUiR.string.core_ui_server_error_retry),
                onButtonClick = onRetry,
                onDismiss = onDismiss,
            )
        }
    }
}
