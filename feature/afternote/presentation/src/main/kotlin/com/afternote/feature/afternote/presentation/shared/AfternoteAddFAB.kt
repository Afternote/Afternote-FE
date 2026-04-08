package com.afternote.feature.afternote.presentation.shared

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.R
import com.afternote.core.ui.expand.dropShadow

/**
 * 추가 버튼 FAB 컴포넌트.
 *
 * 피그마 그림자: offset (0, 2), blur 40dp, spread 0, 투명도 15%.
 * 위치와 여백은 호출하는 쪽에서 결정합니다.
 */
@Composable
fun AfternoteAddFAB(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = "새 애프터노트 추가",
) {
    Box(
        modifier =
            modifier
                .dropShadow(
                    shape = CircleShape,
                    color = Color(0x26000000),
                    blur = 40.dp,
                    offsetX = 0.dp,
                    offsetY = 2.dp,
                    spread = 0.dp,
                ).background(Color.White, CircleShape)
                .clip(CircleShape)
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.core_ui_add_circle),
            contentDescription = contentDescription,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AfternoteAddFABPreview() {
    Box(modifier = Modifier.wrapContentSize()) {
        AfternoteAddFAB(onClick = {})
    }
}
