package com.afternote.feature.afternote.presentation.shared.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.feature.afternote.presentation.R

/**
 * 목록을 이미 그려 둔 상태에서 새로고침이 실패했을 때 목록 위에 얹는 안내 (#705).
 *
 * Paging 은 refresh 가 실패해도 기존 페이지를 유지하므로, 화면이 말하지 않으면 «표시만 갱신되지
 * 않은» 무음 실패가 된다. 전면 오류([ErrorListBody])로 갈아끼우지 않는 이유는 이미 보고 있는
 * 목록이 여전히 유효해서다 — 목록은 남기고 실패와 복구 수단만 덧붙인다.
 *
 * 스낵바가 아니라 상주 배너인 이유: 스낵바는 스스로 사라져 놓친 사용자에게 복구 수단이 남지 않는다.
 * 이 배너는 다음 새로고침이 성공할 때까지 «다시 시도» 를 화면에 붙들어 둔다.
 *
 * 표시 방식 통일(#446) 결론이 나오면 본문 표현만 교체한다 — 호출부 배선은 유지.
 *
 * @param onRetry «다시 시도» 클릭. 보통 [androidx.paging.compose.LazyPagingItems.retry] 를 전달한다.
 */
@Composable
fun ListRefreshErrorBanner(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(AfternoteDesign.colors.gray2)
                .padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.afternote_home_refresh_error),
            style = AfternoteDesign.typography.captionLargeR,
            color = AfternoteDesign.colors.gray7,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onRetry) {
            Text(
                text = stringResource(R.string.afternote_home_retry),
                style = AfternoteDesign.typography.captionLargeB,
                color = AfternoteDesign.colors.gray9,
            )
        }
    }
}
