package com.afternote.feature.afternote.presentation.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.feature.afternote.presentation.R

/**
 * 수정 진입 prefill 조회가 실패했을 때 폼 자리에 그리는 본문 (#705).
 *
 * 빈 폼을 대신 세우지 않는 것이 이 컴포저블의 존재 이유다 — 수정 저장(PATCH)은 보낸 값으로 기존
 * 기록을 덮으므로, 못 읽은 상태의 빈 폼이 저장되면 기록이 사라진다. 그래서 이 상태에서는 폼 대신
 * 사유와 «다시 시도» 만 노출하고, 화면 상단의 «등록» 도 함께 잠근다
 * ([AfternoteEditorScreen] 의 `isSubmitEnabled`).
 *
 * 표시 방식 통일(#446) 결론이 나오면 본문 표현만 교체한다 — 호출부 배선은 유지.
 *
 * @param onRetry «다시 시도» 클릭. 보통 [AfternoteEditorViewModel.retryPrefill] 을 전달한다.
 */
@Composable
internal fun EditorPrefillErrorBody(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.afternote_editor_prefill_load_failed),
                style = AfternoteDesign.typography.bodySmallR,
                color = AfternoteDesign.colors.gray7,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = onRetry) {
                Text(
                    text = stringResource(R.string.afternote_editor_prefill_retry),
                    style = AfternoteDesign.typography.captionLargeB,
                    color = AfternoteDesign.colors.gray9,
                )
            }
        }
    }
}
