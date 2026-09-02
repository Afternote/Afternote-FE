package com.afternote.feature.afternote.presentation.shared.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.feature.afternote.presentation.R

/**
 * 목록 초기 로드(refresh)가 실패했을 때 표시하는 에러 바디.
 *
 * @param onRetry "다시 시도" 클릭 시 호출. 보통 [androidx.paging.compose.LazyPagingItems.retry] 를 전달한다.
 */
@Composable
fun ErrorListBody(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.afternote_home_load_error),
                style = AfternoteDesign.typography.bodySmallR,
                color = AfternoteDesign.colors.gray7,
            )
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = onRetry) {
                Text(
                    text = stringResource(R.string.afternote_home_retry),
                    style = AfternoteDesign.typography.captionLargeB,
                    color = AfternoteDesign.colors.gray9,
                )
            }
        }
    }
}
