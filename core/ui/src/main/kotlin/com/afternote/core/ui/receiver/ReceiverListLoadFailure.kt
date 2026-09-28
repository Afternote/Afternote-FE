package com.afternote.core.ui.receiver

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.R
import com.afternote.core.ui.button.AfternoteButton
import com.afternote.core.ui.theme.AfternoteDesign

/**
 * 수신자 목록 조회 실패 안내 (#2045). 보여 줄 행이 없을 때 목록 자리를 통째로 대체한다.
 *
 * 애프터노트 에디터의 수신자 선택과 설정 수신자 목록(관리·사후 전달 조건 선택)이 같은 문구·구성을
 * 각자 갖고 있던 것을 올렸다. [ReceiverSelectScreen] 의 `listReplacement` 슬롯에 그대로 끼운다.
 *
 * @param onRetryClick «다시 시도» 클릭. 목록을 다시 조회하는 일은 소비 기능이 맡는다.
 */
@Composable
public fun ReceiverListLoadFailure(
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.core_ui_receiver_list_load_failed),
            style = AfternoteDesign.typography.captionLargeR,
            color = AfternoteDesign.colors.gray8,
            textAlign = TextAlign.Center,
        )
        AfternoteButton(
            text = stringResource(R.string.core_ui_receiver_list_retry),
            onClick = onRetryClick,
            modifier = Modifier.padding(top = 16.dp, start = 20.dp, end = 20.dp),
        )
    }
}
