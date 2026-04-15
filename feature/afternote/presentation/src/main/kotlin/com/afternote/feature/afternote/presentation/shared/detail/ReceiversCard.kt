package com.afternote.feature.afternote.presentation.shared.detail

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.shared.model.ReceiverUiModel
import com.afternote.core.ui.R as CoreUiR

/**
 * 수신자 목록 카드. 애프터노트 상세 화면(갤러리/소셜/추모 가이드라인)에서 공통 사용.
 *
 * [DetailSection] 뼈대를 재활용해 헤더(아이콘·라벨·가로선)와 아웃라인 카드 레이아웃을 맞춘다.
 * 수신자가 없으면 아무것도 표시하지 않음.
 */
@Composable
fun ReceiversCard(
    receivers: List<ReceiverUiModel>,
    modifier: Modifier = Modifier,
) {
    if (receivers.isEmpty()) return

    DetailSection(
        iconResId = CoreUiR.drawable.core_ui_user,
        label = stringResource(R.string.afternote_detail_extra_receivers_label),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            receivers.forEach { receiver ->
                ReceiverDetailItem(receiver = receiver)
            }
        }
    }
}

@Composable
private fun ReceiverDetailItem(
    receiver: ReceiverUiModel,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Image(
            painter = painterResource(CoreUiR.drawable.core_ui_ic_profile_placeholder),
            contentDescription = stringResource(R.string.feature_afternote_content_description_recipient_profile),
            modifier =
                Modifier
                    .size(50.dp)
                    .clip(CircleShape),
            contentScale = ContentScale.Crop,
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = receiver.name,
                style =
                    AfternoteDesign.typography.captionLargeR.copy(
                        fontWeight = FontWeight.Medium,
                        color = AfternoteDesign.colors.black,
                    ),
            )
            Text(
                text = receiver.label,
                style =
                    AfternoteDesign.typography.captionLargeR.copy(
                        color = AfternoteDesign.colors.gray6,
                    ),
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
private fun ReceiversCardPreview() {
    AfternoteTheme {
        ReceiversCard(
            receivers =
                listOf(
                    ReceiverUiModel(
                        id = "1",
                        name = "김지은",
                        label = "친구",
                    ),
                    ReceiverUiModel(
                        id = "2",
                        name = "김혜성",
                        label = "친구",
                    ),
                ),
        )
    }
}
