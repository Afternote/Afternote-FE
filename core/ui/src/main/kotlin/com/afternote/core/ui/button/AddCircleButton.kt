package com.afternote.core.ui.button

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
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
 * - 클릭 가능
 *
 * @param modifier Modifier (기본: Modifier)
 * @param contentDescription 접근성을 위한 콘텐츠 설명 ([interactive]이 false면 이미지는 장식으로 처리되어 무시됨)
 * @param onClick 클릭 시 실행할 콜백 ([interactive]이 true일 때만 적용)
 * @param interactive false면 시각만 표시하고 클릭·이미지 contentDescription은 부모에 맡김 (카드 전체 탭 등)
 */
@Composable
fun AddCircleButton(
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    interactive: Boolean = true,
) {
    Image(
        painter = painterResource(R.drawable.core_ui_add_circle_dark),
        contentDescription = if (interactive) contentDescription else null,
        modifier =
            modifier
                .size(24.dp)
                .then(
                    if (interactive) {
                        Modifier.clickable(onClick = onClick)
                    } else {
                        Modifier
                    },
                ),
    )
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
