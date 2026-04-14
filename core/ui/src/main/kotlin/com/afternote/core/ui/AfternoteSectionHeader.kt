package com.afternote.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme

/**
 * 홈 등에서 쓰는 «제목 + 가로 구분선» 한 줄 헤더입니다.
 *
 * 문구는 호출 측에서 [stringResource] 등으로 넘기고, 스타일은 디자인 토큰으로 고정됩니다.
 */
@Composable
fun AfternoteSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = title,
            style = AfternoteDesign.typography.mono,
            color = AfternoteDesign.colors.gray6,
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = AfternoteDesign.colors.gray3,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AfternoteSectionHeaderPreview() {
    AfternoteTheme {
        AfternoteSectionHeader(title = "NEXT STEP")
    }
}
