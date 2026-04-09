package com.afternote.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme

/**
 * Style configuration for Label component.
 *
 * @param fontSize Font size of the label text (default: 16.sp)
 * @param lineHeight Line height of the label text (default: 22.sp)
 * @param fontWeight Font weight of the label text (default: Medium)
 * @param color Text color (default: theme gray9)
 * @param requiredDotOffsetY Top padding of the required dot from the row baseline (default: 4.dp)
 */
data class LabelStyle(
    val fontSize: TextUnit = 16.sp,
    val lineHeight: TextUnit = 22.sp,
    val fontWeight: FontWeight = FontWeight.Medium,
    val color: Color? = null,
    val requiredDotOffsetY: Dp = 4.dp,
)

/**
 * 라벨 컴포넌트 (필수 표시 옵션 포함)
 *
 * 피그마 디자인 기반:
 * - 텍스트: 기본값 16sp, Medium, [AfternoteDesign.colors.gray9] ([LabelStyle]로 커스터마이징 가능)
 * - 필수 표시 ([isRequired]=true): 브랜드 블루([AfternoteDesign.colors.b1]) 점 4dp,
 *   텍스트 오른쪽 8.dp 간격, 세로는 [LabelStyle.requiredDotOffsetY]만큼 아래로 내려 배치
 *
 * 레이아웃은 [Row]로 한 번에 측정·배치하여 [onGloballyPositioned] 기반 이중 리컴포지션을 피합니다.
 *
 * @param modifier Modifier for the component
 * @param text Label text
 * @param isRequired Whether to show the required indicator (blue dot)
 * @param style Style configuration for the label
 */
@Composable
fun Label(
    text: String,
    modifier: Modifier = Modifier,
    isRequired: Boolean = false,
    style: LabelStyle = LabelStyle(),
) {
    val textColor = style.color ?: AfternoteDesign.colors.gray9

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = text,
            style =
                AfternoteDesign.typography.textField.copy(
                    fontSize = style.fontSize,
                    lineHeight = style.lineHeight,
                    fontWeight = style.fontWeight,
                    color = textColor,
                ),
        )

        if (isRequired) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier =
                    Modifier
                        .padding(top = style.requiredDotOffsetY)
                        .size(4.dp)
                        .background(
                            color = AfternoteDesign.colors.b1,
                            shape = CircleShape,
                        ),
            )
        }
    }
}

@Preview(showBackground = true, name = "기본 라벨")
@Composable
private fun LabelPreview() {
    AfternoteTheme {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
        ) {
            Label(text = "이름")
        }
    }
}

@Preview(showBackground = true, name = "필수 라벨")
@Composable
private fun LabelRequiredPreview() {
    AfternoteTheme {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
        ) {
            Label(
                text = "정보 처리 방법",
                isRequired = true,
            )
        }
    }
}

@Preview(showBackground = true, name = "작은 라벨 (12sp)")
@Composable
private fun LabelSmallPreview() {
    AfternoteTheme {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
        ) {
            Label(
                text = "종류",
                style =
                    LabelStyle(
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Normal,
                    ),
            )
        }
    }
}

@Preview(showBackground = true, name = "다양한 라벨 스타일")
@Composable
private fun LabelVariantsPreview() {
    AfternoteTheme {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Label(text = "일반 라벨")
            Label(text = "필수 라벨", isRequired = true)
            Label(
                text = "작은 필수 라벨",
                isRequired = true,
                style =
                    LabelStyle(
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Normal,
                        requiredDotOffsetY = 2.dp,
                    ),
            )
        }
    }
}
