package com.afternote.feature.afternote.presentation.shared.detail

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.button.AfternoteCircularCheckbox
import com.afternote.core.ui.button.CheckboxState
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.R

@Composable
fun DetailSectionHeader(
    iconResId: Int,
    label: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            painter = painterResource(iconResId),
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = AfternoteDesign.colors.gray5,
        )
        Text(
            text = label,
            style = AfternoteDesign.typography.mono,
            color = AfternoteDesign.colors.gray5,
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = AfternoteDesign.colors.gray3,
        )
    }
}

@Composable
fun DetailCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = AfternoteDesign.colors.gray2,
                    shape = RoundedCornerShape(8.dp),
                ).padding(16.dp),
    ) {
        content()
    }
}

/**
 * "처리방법" 섹션.
 *
 * 헤더 + 체크박스 리스트 + 하단 Spacer 까지 완성된 섹션을 제공한다.
 * [methods] 가 비어 있으면 아무것도 렌더링하지 않는다.
 */
@Composable
fun ProcessingMethodsSection(
    methods: List<String>,
    modifier: Modifier = Modifier,
) {
    if (methods.isEmpty()) return
    Column(modifier = modifier) {
        DetailSectionHeader(
            iconResId = com.afternote.core.ui.R.drawable.core_ui_settings,
            label = stringResource(R.string.feature_afternote_detail_section_processing),
        )
        Spacer(modifier = Modifier.height(8.dp))
        DetailCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                methods.forEach { method ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        AfternoteCircularCheckbox(
                            state = CheckboxState.Default,
                            onClick = null,
                            size = 20.dp,
                        )
                        Text(
                            text = method,
                            style = AfternoteDesign.typography.bodySmallR,
                            color = AfternoteDesign.colors.gray9,
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

/**
 * "남기신 말씀" 섹션.
 *
 * 헤더 + 인용 부호와 로고가 포함된 메시지 카드 + 하단 Spacer 까지 완성된 섹션을 제공한다.
 * [message] 가 비어 있으면 "남기신 말씀이 없습니다." 플레이스홀더가 gray5 로 표시된다.
 */
@Composable
fun MessageSection(
    message: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        DetailSectionHeader(
            iconResId = com.afternote.core.ui.R.drawable.core_ui_ic_mail,
            label = stringResource(R.string.feature_afternote_detail_section_message),
        )
        Spacer(modifier = Modifier.height(8.dp))
        DetailCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
            ) {
                val hasMessage = message.isNotEmpty()
                val displayMessage =
                    if (hasMessage) message else stringResource(R.string.feature_afternote_detail_no_message)
                val textColor =
                    if (hasMessage) AfternoteDesign.colors.gray9 else AfternoteDesign.colors.gray5
                Row(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.feature_afternote_detail_quote_mark),
                        style = AfternoteDesign.typography.bodyLargeR,
                        color = AfternoteDesign.colors.gray4,
                    )
                    Text(
                        text = displayMessage,
                        style = AfternoteDesign.typography.bodySmallR,
                        color = textColor,
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Image(
                    painter = painterResource(R.drawable.feature_afternote_img_logo),
                    contentDescription = null,
                    modifier =
                        Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(18.dp)),
                    contentScale = ContentScale.Crop,
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Preview(showBackground = true)
@Composable
private fun ProcessingMethodsSectionPreview() {
    AfternoteTheme {
        ProcessingMethodsSection(
            methods = listOf("계정 삭제", "게시물 유지", "메모리얼 계정 전환"),
        )
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

@Preview(showBackground = true)
@Composable
private fun MessageSectionEmptyPreview() {
    AfternoteTheme {
        MessageSection(
            message = "",
        )
    }
}
