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
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.ui.text.style.TextAlign
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
                // 이 화면은 Scaffold 없이 전체 화면 Box 위에 직접 얹히므로 시스템 인셋이 적용되지
                // 않는다. statusBarsPadding 을 빼면 "돌아가기" 가 상태바 아래로 들어가 터치가
                // 상태바에 먹혀 실제로 눌리지 않는다 (#559) — 아래 24.dp 는 그 위에 얹는 여백이다.
                .statusBarsPadding()
                // 좌우를 같이 준다. 종전에는 start 만 있어서 제목 블록이 오른쪽 화면 끝까지
                // 밀고 나갔다 — 360dp + 글자 1.5배 실측에서 부제가 x=356.5dp(폭 360dp)까지
                // 닿아 우측 여백이 0 이었다 (#1153).
                .padding(top = 24.dp, start = 24.dp, end = 24.dp),
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
        //
        // weight 를 준다 — 없으면 이 Column 이 자기 내용 폭을 그대로 요구해, 폭이 모자랄 때
        // 줄바꿈하지 않고 「돌아가기」 옆으로 계속 뻗는다. weight 가 있으면 남은 폭이 상한이
        // 되어 그 안에서 접히고, 버튼과 겹치는 것도 구조적으로 불가능해진다 (#1153).
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.mindrecord_memory_space_title),
                style = AfternoteDesign.typography.inter,
                color = AfternoteDesign.colors.gray6,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.mindrecord_memory_space_subtitle),
                style = AfternoteDesign.typography.inter.copy(fontSize = 9.sp),
                color = AfternoteDesign.colors.gray5,
                // Column 의 CenterHorizontally 는 Text 상자를 가운데 놓을 뿐이고, 접힌 줄을
                // 서로 가운데로 맞추는 것은 textAlign 이다 — 없으면 둘째 줄이 왼쪽에 붙는다.
                textAlign = TextAlign.Center,
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
