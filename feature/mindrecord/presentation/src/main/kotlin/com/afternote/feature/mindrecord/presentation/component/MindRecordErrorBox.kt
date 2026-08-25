package com.afternote.feature.mindrecord.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.presentation.R

/**
 * 마음의 기록 목록·리포트의 조회 실패 표시.
 *
 * 세 화면이 각자 들고 있던 `ErrorBox` 를 한 곳으로 모았다. 종전에는 문구만 그리고 재시도 수단이
 * 없어, 한 번 실패하면 화면을 나갔다 들어오기 전까지 복구할 수 없었다 (#716).
 *
 * @param onRetry `null` 이면 재시도 버튼을 그리지 않는다 — 재시도로 풀리지 않는 실패용.
 */
@Composable
fun MindRecordErrorBox(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = message,
            color = AfternoteDesign.colors.gray9,
            style = AfternoteDesign.typography.bodySmallR,
            textAlign = TextAlign.Center,
        )
        if (onRetry != null) {
            TextButton(onClick = onRetry) {
                Text(
                    text = stringResource(R.string.mindrecord_error_retry),
                    color = AfternoteDesign.colors.gray9,
                    style = AfternoteDesign.typography.bodySmallB,
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "조회 실패 - 재시도 있음")
@Composable
private fun MindRecordErrorBoxPreview() {
    AfternoteTheme {
        MindRecordErrorBox(message = "기록을 불러오지 못했어요.", onRetry = {})
    }
}

@Preview(showBackground = true, name = "조회 실패 - 재시도 없음")
@Composable
private fun MindRecordErrorBoxWithoutRetryPreview() {
    AfternoteTheme {
        MindRecordErrorBox(message = "기록을 불러오지 못했어요.")
    }
}
