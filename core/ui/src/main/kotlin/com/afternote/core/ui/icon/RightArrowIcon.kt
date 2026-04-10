package com.afternote.core.ui.icon

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.R
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme

/**
 * 아이콘 관련 설정을 묶는 불변 데이터 클래스
 *
 * @param iconRes 화살표 아이콘 리소스 ID
 * @param contentDescription 접근성 설명
 * @param size 아이콘 크기 (기본: null, 아이콘 리소스의 원본 크기 사용)
 * @param offset 아이콘 오프셋 (기본: DpOffset.Zero, 중앙 정렬)
 */
@Immutable
data class ArrowIconSpec(
    @DrawableRes val iconRes: Int,
    val contentDescription: String? = null,
    val size: Dp? = null,
    val offset: DpOffset = DpOffset.Zero,
)

/**
 * Material Icons를 사용하는 간단한 오른쪽 화살표 아이콘
 *
 * @param color 아이콘 틴트 색상
 * @param size 전체 크기 (기본: 12.dp)
 */
@Composable
fun RightArrowIcon(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 12.dp,
) {
    Icon(
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = null,
        modifier = modifier.size(size),
        tint = color,
    )
}

/**
 * Drawable 리소스를 사용하는 화살표 아이콘 (배경 없음)
 *
 * @param iconSpec 아이콘 관련 설정 (필수)
 * @param modifier Modifier (기본: Modifier)
 * @param size [ArrowIconSpec.size]가 null일 때 적용되는 크기 (기본: 12.dp)
 */
@Composable
fun RightArrowIcon(
    iconSpec: ArrowIconSpec,
    modifier: Modifier = Modifier,
    size: Dp = 12.dp,
) {
    Image(
        painter = painterResource(iconSpec.iconRes),
        contentDescription = iconSpec.contentDescription,
        modifier =
            modifier
                .then(
                    if (iconSpec.size != null) {
                        Modifier.size(iconSpec.size)
                    } else {
                        Modifier.size(size)
                    },
                ).then(
                    if (iconSpec.offset != DpOffset.Zero) {
                        Modifier.offset(x = iconSpec.offset.x, y = iconSpec.offset.y)
                    } else {
                        Modifier
                    },
                ),
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
            RightArrowIcon(color = AfternoteDesign.colors.gray9)
            RightArrowIcon(color = AfternoteDesign.colors.gray9, size = 16.dp)
            RightArrowIcon(color = AfternoteDesign.colors.b1, size = 12.dp)
        }
    }
}

@Preview(showBackground = true, name = "Drawable")
@Composable
private fun RightArrowIconDrawablePreview() {
    AfternoteTheme {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RightArrowIcon(
                iconSpec =
                    ArrowIconSpec(
                        iconRes = R.drawable.core_ui_arrow_left,
                        contentDescription = "미리보기",
                        size = 8.dp,
                    ),
            )
            RightArrowIcon(
                iconSpec =
                    ArrowIconSpec(
                        iconRes = R.drawable.core_ui_arrow_left,
                        contentDescription = null,
                    ),
                size = 20.dp,
            )
        }
    }
}
