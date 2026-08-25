package com.afternote.feature.receiver.presentation.afternotelist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.receiver.presentation.R

/**
 * 전달 조건이 아직 충족되지 않아 목록을 열 수 없을 때의 바디.
 *
 * 재시도 수단을 두지 않는 것이 이 화면의 요점이다 — 발신자가 세운 전달 조건이 충족돼야 열리는
 * 상태라 재시도로는 풀리지 않는데, 일반 실패와 같은 «다시 시도» 를 주면 사용자가 같은 실패를
 * 반복하게 된다(#611 실측: 403 code 2009 에 "다시 시도" 만 노출).
 *
 * @param message 사유 문구. 서버가 실어 보낸 안내를 그대로 쓰고, 미제공 시 폴백 리소스가 들어온다.
 */
@Composable
internal fun ReceiverAfternoteNotDeliverableBody(
    message: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp),
        ) {
            Text(
                text = message,
                style = AfternoteDesign.typography.bodySmallR,
                color = AfternoteDesign.colors.gray9,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.receiver_afternote_list_not_deliverable_description),
                style = AfternoteDesign.typography.captionLargeR,
                color = AfternoteDesign.colors.gray7,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ReceiverAfternoteNotDeliverableBodyPreview() {
    AfternoteTheme {
        ReceiverAfternoteNotDeliverableBody(message = "아직 전달 조건이 충족되지 않았습니다.")
    }
}
