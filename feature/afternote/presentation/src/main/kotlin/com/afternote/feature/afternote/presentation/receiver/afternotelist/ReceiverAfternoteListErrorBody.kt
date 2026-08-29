package com.afternote.feature.afternote.presentation.receiver.afternotelist

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign

/**
 * 재시도 버튼. 재시도가 실제로 유효한 실패에서만 넘긴다 — 라벨과 동작을 한 타입에 묶어, 버튼은
 * 있는데 동작이 없거나 그 반대인 상태를 만들 수 없게 한다.
 */
internal data class ListErrorRetry(
    val label: String,
    val onClick: () -> Unit,
)

/**
 * 목록을 열지 못했을 때의 전면 바디.
 *
 * 팝업(`NetworkErrorPopup`)이 아니라 전면 바디인 이유 — 작성자 목록이 같은 상황에서
 * `ErrorListBody` 를 쓰고, Paging 의 `retry()` 는 화면 안 상태라 dismiss 뒤에 남는 상태가
 * 애매해진다. 문구는 사유별로 갈리고, 재시도 수단은 [retry] 가 있을 때만 그린다.
 *
 * @param title 사유 문구. 서버가 실어 보낸 안내가 있으면 그대로 들어온다.
 * @param description 무엇을 기다리거나 확인해야 하는지.
 * @param retry `null` 이면 재시도로 풀리지 않는 실패라 버튼을 그리지 않는다(#611).
 */
@Composable
internal fun ReceiverAfternoteListErrorBody(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    retry: ListErrorRetry? = null,
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
                text = title,
                style = AfternoteDesign.typography.bodySmallR,
                color = AfternoteDesign.colors.gray9,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                style = AfternoteDesign.typography.captionLargeR,
                color = AfternoteDesign.colors.gray7,
                textAlign = TextAlign.Center,
            )
            if (retry != null) {
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = retry.onClick) {
                    Text(
                        text = retry.label,
                        style = AfternoteDesign.typography.captionLargeB,
                        color = AfternoteDesign.colors.gray9,
                    )
                }
            }
        }
    }
}
