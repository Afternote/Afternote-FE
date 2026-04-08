package com.afternote.feature.afternote.presentation.author.home

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.icon.RightArrowIcon
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme

@Composable
internal fun HomeHeaderSection(
    nextStepText: String,
    onNextStepClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 25.dp),
    ) {
        Text(
            text = "애프터노트",
            style = AfternoteDesign.typography.h1,
            color = AfternoteDesign.colors.gray9,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "애프터 노트 설명",
            style = AfternoteDesign.typography.captionLargeR,
            color = Color(0x59000000),
        )
        Spacer(modifier = Modifier.height(16.dp))
        NextStepCard(
            text = nextStepText,
            onClick = onNextStepClick,
        )
    }
}

@Composable
private fun NextStepCard(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "NEXT STEP",
                style = AfternoteDesign.typography.mono,
                color = AfternoteDesign.colors.gray6,
            )
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = AfternoteDesign.colors.gray3,
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = AfternoteDesign.colors.gray2,
                        shape = RoundedCornerShape(8.dp),
                    ).clickable(onClick = onClick)
                    .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = text,
                style = AfternoteDesign.typography.inter,
                color = AfternoteDesign.colors.gray9,
                modifier = Modifier.weight(1f),
            )
            RightArrowIcon(
                color = AfternoteDesign.colors.gray6,
                size = 12.dp,
            )
            Spacer(Modifier.width(4.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeHeaderSectionPreview() {
    AfternoteTheme {
        HomeHeaderSection(
            nextStepText = "가족들의 '주거래 은행' 정보를\n입력하신 건 확인하셨나요?",
            onNextStepClick = {},
        )
    }
}
