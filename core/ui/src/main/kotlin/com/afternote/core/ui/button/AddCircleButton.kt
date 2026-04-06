package com.afternote.core.ui.button

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.R

/**
 * 원형 추가 버튼 컴포넌트
 *
 * 리스트 하단에 아이템을 추가하기 위한 버튼
 * - 파란 원형 아이콘 (24dp)
 * - 클릭 가능
 *
 * @param modifier Modifier (기본: Modifier)
 * @param contentDescription 접근성을 위한 콘텐츠 설명
 * @param onClick 클릭 시 실행할 콜백
 */
@Composable
fun AddCircleButton(
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Image(
        painter = painterResource(R.drawable.core_ui_add_circle),
        contentDescription = contentDescription,
        modifier =
            modifier
                .size(24.dp)
                .clickable(onClick = onClick),
    )
}
