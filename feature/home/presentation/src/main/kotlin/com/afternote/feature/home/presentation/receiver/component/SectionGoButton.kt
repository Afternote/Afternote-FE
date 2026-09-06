package com.afternote.feature.home.presentation.receiver.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.icon.RightArrowIcon
import com.afternote.core.ui.theme.AfternoteDesign

/**
 * 수신자 홈의 «… 확인하러 가기 ›» 작은 회색 알약 버튼.
 *
 * 단순한 둥근 배경이라 Material3 [androidx.compose.material3.Card] 대신 Column + clip/background 조합을 사용한다.
 */
@Composable
fun SectionGoButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .wrapContentWidth()
                .clip(RoundedCornerShape(50))
                .background(AfternoteDesign.colors.gray2)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(PaddingValues(horizontal = 16.dp, vertical = 8.dp)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = text,
            style = AfternoteDesign.typography.captionLargeR,
            color = AfternoteDesign.colors.gray8,
        )
        RightArrowIcon(
            modifier = Modifier.size(width = 4.dp, height = 7.dp),
            tint = AfternoteDesign.colors.gray7,
        )
    }
}
