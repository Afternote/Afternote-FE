package com.afternote.core.ui.icon

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.R
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme

/**
 * Material Icons 기반 오른쪽 화살표 (RTL에서 자동 반전).
 * 크기는 호출부에서 [Modifier.size] 등으로 지정한다.
 *
 * @param modifier 레이아웃·크기 ([Modifier.size]), padding, offset 등
 * @param tint 기본값은 상위 [LocalContentColor] (예: [androidx.compose.material3.Surface]의 contentColor).
 */
@Composable
fun RightArrowIcon(
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) {
    Icon(
        painter = painterResource(R.drawable.core_ui_right_arrow),
        contentDescription = null,
        modifier = modifier,
        tint = tint,
    )
}

/**
 * 커스텀 drawable 벡터 화살표(또는 아이콘). [RightArrowIcon]과 달리 에셋 ID를 받는다.
 * 디자인 에셋 비율이 1:1이 아닐 수 있어 [width]·[height]를 분리한다.
 *
 * 멀티 컬러·비틴트 에셋은 [tint]에 [Color.Unspecified]를 넘긴다.
 */
@Composable
fun ArrowIcon(
    @DrawableRes iconRes: Int,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    tint: Color = LocalContentColor.current,
) {
    Icon(
        painter = painterResource(id = iconRes),
        contentDescription = contentDescription,
        modifier = modifier,
        tint = tint,
    )
}

@Preview(showBackground = true, name = "Material arrow")
@Composable
private fun RightArrowIconMaterialPreview() {
    AfternoteTheme {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RightArrowIcon(
                modifier = Modifier.size(12.dp),
                tint = AfternoteDesign.colors.gray9,
            )
        }
    }
}
