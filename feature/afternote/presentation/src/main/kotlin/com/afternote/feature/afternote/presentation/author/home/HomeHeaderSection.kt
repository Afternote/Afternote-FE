package com.afternote.feature.afternote.presentation.author.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.AfternoteOutlinedCard
import com.afternote.core.ui.AfternoteSectionHeader
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
            color = AfternoteDesign.colors.black.copy(alpha = 89f / 255f),
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
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        AfternoteSectionHeader(title = "NEXT STEP")
        AfternoteOutlinedCard(
            onClick = onClick,
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = text,
                    style = AfternoteDesign.typography.inter,
                    color = AfternoteDesign.colors.gray9,
                    modifier = Modifier.weight(1f),
                )
                RightArrowIcon(
                    modifier = Modifier.size(width = 4.dp, height = 7.dp),
                    tint = AfternoteDesign.colors.gray6,
                )
            }
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
