package com.afternote.feature.afternote.presentation.shared.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme

/**
 * 상세 화면 **매크로 레이아웃** 세트: [DetailSectionHeader] + 고정 헤더-카드 간격 + [DetailCard] 본문.
 *
 * [DetailSection] 은 내부에서 헤더와 카드를 조립한다. 키-값 한 줄 UI는 [DetailInfoRow] 를 참고한다.
 *
 * 반복되는 레이아웃만 캡슐화하고, 카드 안 알맹이는 [content] 로 주입한다.
 * 도메인 전용 문자열·리소스는 이 파일에 두지 않는다.
 */
@Composable
fun DetailSection(
    iconResId: Int,
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        DetailSectionHeader(
            iconResId = iconResId,
            label = label,
        )
        DetailCard {
            content()
        }
    }
}

@Composable
fun DetailSectionHeader(
    iconResId: Int,
    label: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(
            painter = painterResource(iconResId),
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = AfternoteDesign.colors.gray6,
        )
        Text(
            text = label,
            style = AfternoteDesign.typography.mono,
            color = AfternoteDesign.colors.gray6,
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
                .background(AfternoteDesign.colors.white)
                .border(
                    width = 1.dp,
                    color = AfternoteDesign.colors.gray2,
                    shape = RoundedCornerShape(6.dp),
                ).padding(16.dp),
    ) {
        content()
    }
}

@Preview(showBackground = true)
@Composable
private fun DetailSectionPreview() {
    AfternoteTheme {
        DetailSection(
            iconResId = com.afternote.core.ui.R.drawable.core_ui_settings,
            label = "섹션 헤더",
        ) {
            Text(
                text = "카드 내부 콘텐츠입니다.",
                style = AfternoteDesign.typography.bodySmallR,
                color = AfternoteDesign.colors.gray9,
            )
        }
    }
}
