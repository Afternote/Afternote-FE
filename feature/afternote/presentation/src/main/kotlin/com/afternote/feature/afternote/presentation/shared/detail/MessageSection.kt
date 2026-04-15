package com.afternote.feature.afternote.presentation.shared.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.R

/**
 * "남기신 말씀" 섹션.
 *
 * [DetailSection] 슬롯 안에 인용 부호·본문·로고 행만 둔다.
 * 섹션 아래 외부 간격은 호출하는 부모 레이아웃에서 둔다.
 * [message] 가 공백만 있거나 비어 있으면(`isBlank`) `feature_afternote_detail_no_message` 문구를 gray5 로 표시한다.
 */
@Composable
fun MessageSection(
    message: String,
    modifier: Modifier = Modifier,
) {
    val isMessageEmpty = message.isBlank()

    DetailSection(
        iconResId = R.drawable.afternote_ui_ic_leave_message_header,
        label = stringResource(R.string.feature_afternote_detail_section_message),
        modifier = modifier,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.afternote_ui_ic_leave_message_card),
                contentDescription = null,
                tint = AfternoteDesign.colors.gray4,
                modifier = Modifier.size(15.dp),
            )
            Text(
                text =
                    if (isMessageEmpty) {
                        stringResource(R.string.feature_afternote_detail_no_message)
                    } else {
                        message
                    },
                style = AfternoteDesign.typography.bodySmallR,
                color =
                    if (isMessageEmpty) {
                        AfternoteDesign.colors.gray5
                    } else {
                        AfternoteDesign.colors.gray8
                    },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MessageSectionPreview() {
    AfternoteTheme {
        MessageSection(
            message = "소중한 사람들에게 남기는 마지막 메시지입니다.",
        )
    }
}
