package com.afternote.core.ui.icon

import android.R.attr.height
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.R
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme

/**
 * Material Icons 기반 오른쪽 화살표 (RTL에서 자동 반전).
 * Material 아이콘은 1:1 비율이므로 [size] 단일 파라미터를 쓴다.
 *
 * @param modifier 외부 레이아웃 조정 (padding, offset 등)
 * @param size 아이콘 크기 (정사각형, 기본 12.dp)
 * @param tint 기본값은 상위 [LocalContentColor] (예: [androidx.compose.material3.Surface]의 contentColor).
 */
@Composable
fun RightArrowIcon(
    modifier: Modifier = Modifier,
    size: Dp = 12.dp,
    tint: Color = LocalContentColor.current,
) {
    Icon(
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = null,
        modifier = modifier.size(size),
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
            RightArrowIcon(tint = AfternoteDesign.colors.gray9)
            RightArrowIcon(tint = AfternoteDesign.colors.gray9, size = 16.dp)
            RightArrowIcon(tint = AfternoteDesign.colors.b1, size = 18.dp)
        }
    }
}

@Preview(showBackground = true, name = "Drawable (Custom Size)")
@Composable
private fun ArrowIconDrawablePreview() {
    AfternoteTheme {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ArrowIcon(
                iconRes = R.drawable.core_ui_arrow_left,
                contentDescription = "미리보기",
                tint = AfternoteDesign.colors.gray9,
                modifier = Modifier.size(20.dp),
            )
            ArrowIcon(
                iconRes = R.drawable.core_ui_arrow_left,
                contentDescription = null,
                modifier =
                    Modifier
                        .size(20.dp)
                        .offset(x = 2.dp),
                tint = AfternoteDesign.colors.b1,
            )
        }
    }
}
