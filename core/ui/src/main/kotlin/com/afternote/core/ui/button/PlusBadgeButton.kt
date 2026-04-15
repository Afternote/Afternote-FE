package com.afternote.core.ui.button

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.R
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme

/**
 * 다른 UI 요소(프로필 사진 등)에 붙어있는 뱃지형/인라인 원형 버튼.
 *
 * [FloatingActionButton][androidx.compose.material3.FloatingActionButton]과 달리
 * 그림자·최소 터치 영역 강제가 없어 작은 크기로 밀착 배치할 수 있습니다.
 */
@Composable
fun PlusBadgeButton(
    contentDescription: String,
    onClick: () -> Unit,
    paddingValues: PaddingValues,
    modifier: Modifier = Modifier,
    size: Dp = 30.dp,
) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(AfternoteDesign.colors.black)
            .clickable(onClick = onClick)
            .padding(paddingValues),
    ) {
        Icon(
            painter = painterResource(R.drawable.core_ui_circle_button_plus),
            contentDescription = contentDescription,
            tint = AfternoteDesign.colors.white,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PlusBadgeButtonPreview() {
    AfternoteTheme {
        PlusBadgeButton(
            contentDescription = "Plus Button",
            onClick = {},
            paddingValues = PaddingValues(8.dp),
        )
    }
}
