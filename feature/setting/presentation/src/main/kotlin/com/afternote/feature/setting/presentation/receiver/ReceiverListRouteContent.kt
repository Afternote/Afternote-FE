package com.afternote.feature.setting.presentation.receiver

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.afternote.core.model.setting.ReceiverListItem
import com.afternote.core.ui.button.AfternoteButton
import com.afternote.core.ui.loading.LoadingBody
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.feature.setting.presentation.R

/**
 * 설정 수신자 목록 라우트의 조회 상태 분기 (#1281). 관리·선택 두 진입이 같은 규칙을 쓴다.
 *
 * 보여 줄 행이 없을 때만 목록 자리를 로딩이나 실패 안내로 바꾼다. 행이 있으면 조회 중에도, 다시 불러오기가
 * 실패해도 행과 검색·선택을 그대로 둔다. 0건 안내와 등록 버튼은 조회에 성공해 실제로 0건일 때만 나온다.
 *
 * 선택 진입은 공용 `ReceiverSelectScreen` 의 `listReplacement` 로만 넘긴다. 목록을 남긴 채 얹는 갱신 실패
 * 배너는 그 슬롯으로 표현할 수 없어 관리 진입에만 있다.
 */
@Composable
internal fun ReceiverListRouteContent(
    uiState: ReceiverListUiState,
    selectForDeliveryConditions: Boolean,
    onBackClick: () -> Unit,
    onRetryClick: () -> Unit,
    onConfirmClick: (ReceiverListItem) -> Unit,
    onReceiverClick: (Long) -> Unit,
    onRegisterClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listReplacement: (@Composable () -> Unit)? =
        when {
            uiState.receivers.isNotEmpty() -> null
            uiState.loadState == ReceiverListLoadState.Loading -> ({ LoadingBody() })
            uiState.loadState == ReceiverListLoadState.Failure -> ({ ReceiverListLoadFailure(onRetryClick = onRetryClick) })
            else -> null
        }

    if (selectForDeliveryConditions) {
        ReceiverListScreen(
            receivers = uiState.receivers,
            onBackClick = onBackClick,
            onConfirmClick = onConfirmClick,
            modifier = modifier,
            listReplacement = listReplacement,
        )
    } else {
        ReceiverManageScreen(
            receivers = uiState.receivers,
            onBackClick = onBackClick,
            onReceiverClick = onReceiverClick,
            onRegisterClick = onRegisterClick,
            modifier = modifier,
            listReplacement = listReplacement,
            listHeader =
                if (uiState.loadState == ReceiverListLoadState.RefreshFailure) {
                    { ReceiverListRefreshFailureBanner(onRetryClick = onRetryClick) }
                } else {
                    null
                },
        )
    }
}

/**
 * 목록 조회 실패 안내. 같은 공용 선택 화면 슬롯에 끼우는 애프터노트 수신자 선택의 실패 안내
 * (`SelectReceiverLoadFailed`)와 문구·구성이 같다.
 */
@Composable
private fun ReceiverListLoadFailure(onRetryClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.setting_recipient_list_load_failed),
            style = AfternoteDesign.typography.captionLargeR,
            color = AfternoteDesign.colors.gray8,
            textAlign = TextAlign.Center,
        )
        AfternoteButton(
            text = stringResource(R.string.setting_recipient_list_retry),
            onClick = onRetryClick,
            modifier = Modifier.padding(top = 16.dp, start = 20.dp, end = 20.dp),
        )
    }
}

/**
 * 행을 남긴 채 다시 불러오기만 실패했을 때 목록 위에 얹는 안내. 애프터노트 목록의 갱신 실패 배너
 * (`ListRefreshErrorBanner`, #705)와 문구·구성이 같다. 다음 조회가 성공할 때까지 다시 시도를 붙들어 둔다.
 */
@Composable
private fun ReceiverListRefreshFailureBanner(onRetryClick: () -> Unit) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(AfternoteDesign.colors.gray2)
                .padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.setting_recipient_list_refresh_failed),
            style = AfternoteDesign.typography.captionLargeR,
            color = AfternoteDesign.colors.gray7,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onRetryClick) {
            Text(
                text = stringResource(R.string.setting_recipient_list_retry),
                style = AfternoteDesign.typography.captionLargeB,
                color = AfternoteDesign.colors.gray9,
            )
        }
    }
}
