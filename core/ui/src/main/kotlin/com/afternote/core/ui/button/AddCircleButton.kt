package com.afternote.core.ui.button

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.R
import com.afternote.core.ui.theme.AfternoteTheme

/**
 * 원형 추가 버튼 컴포넌트
 *
 * 리스트 하단에 아이템을 추가하기 위한 버튼
 * - 다크 원형 아이콘 (24dp, gray9 배경 + 흰색 "+")
 *
 * [interactive]가 true일 때는 [IconButton]으로 감싸 최소 48×48dp 터치 영역과 원형 리플,
 * [Role.Button] 접근성 의미를 사용합니다. false일 때는 24dp 이미지만 그려 부모 클릭과 겹치지 않습니다.
 *
 * @param contentDescription 접근성용 설명 ([interactive]이 false면 이미지는 장식으로 처리)
 * @param onClick 클릭 콜백 ([interactive]이 true일 때만 사용)
 * @param modifier Modifier
 * @param interactive false면 시각만 표시하고 클릭·이미지 설명은 부모에 맡김 (카드 전체 탭 등)
 */
@Composable
fun AddCircleButton(
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    interactive: Boolean = true,
) {
    if (interactive) {
        IconButton(
            onClick = onClick,
            modifier = modifier,
        ) {
            Image(
                painter = painterResource(R.drawable.core_ui_add_circle_dark),
                contentDescription = contentDescription,
                modifier = Modifier.size(24.dp),
            )
        }
    } else {
        Image(
            painter = painterResource(R.drawable.core_ui_add_circle_dark),
            contentDescription = null,
            modifier = modifier.size(24.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AddCircleButtonPreview() {
    AfternoteTheme {
        AddCircleButton(
            contentDescription = "추가",
            onClick = {},
        )
    }
}
