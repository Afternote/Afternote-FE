package com.afternote.feature.mindrecord.presentation.component.memoryspace

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.presentation.R
import com.afternote.core.ui.R as CoreUiR

@Composable
fun MemorySpaceHeader(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                // 여기서 주는 패딩이 '유일한' 상단 여백이 됩니다.
                .padding(top = 24.dp, start = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        // 1. 뒤로가기 버튼 (Box로 변경)
        Box(
            modifier =
                Modifier
                    .shadow(2.dp, CircleShape) // 그림자 수동 적용
                    .background(AfternoteDesign.colors.white, CircleShape)
                    .clip(CircleShape)
                    .clickable(
                        onClick = onBackClick,
                        // 리플 효과가 원형 밖으로 나가지 않도록 설정
                    ),
        ) {
            Row(
                modifier =
                    Modifier
                        .padding(start = 20.dp, end = 15.dp)
                        .padding(vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    painter = painterResource(CoreUiR.drawable.core_ui_arrow_left),
                    contentDescription = stringResource(R.string.mindrecord_memory_space_back),
                    modifier = Modifier.size(width = 4.dp, height = 7.dp),
                    tint = AfternoteDesign.colors.gray7,
                )
                Text(
                    text = stringResource(R.string.mindrecord_memory_space_back),
                    style = AfternoteDesign.typography.inter,
                    color = AfternoteDesign.colors.gray7,
                )
            }
        }

        // 2. 타이틀 영역
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.mindrecord_memory_space_title),
                style = AfternoteDesign.typography.inter,
                color = AfternoteDesign.colors.gray6,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.mindrecord_memory_space_subtitle),
                style = AfternoteDesign.typography.inter.copy(fontSize = 9.sp),
                color = AfternoteDesign.colors.gray5,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MemorySpaceHeaderPreview() {
    AfternoteTheme {
        MemorySpaceHeader(
            onBackClick = {},
        )
    }
}
