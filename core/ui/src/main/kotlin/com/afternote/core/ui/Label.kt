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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme

/**
 * 라벨 컴포넌트 (필수 표시 옵션 포함)
 *
 * 피그마 디자인 기반:
 * 텍스트 오른쪽 8.dp 간격, 세로는 [requiredDotOffsetY]만큼 아래로 내려 배치
 *
 * 레이아웃은 [Row]로 한 번에 측정·배치하여 이중 리컴포지션을 피합니다.
 *
 * @param text Label text
 * @param modifier Modifier for the component
 * @param isRequired Whether to show the required indicator (blue dot)
 * @param style Typography style for the label (default: theme textField)
 * @param color Text color (default: theme gray9)
 * @param requiredDotOffsetY Top padding of the required dot (default: 4.dp)
 */
@Composable
fun Label(
    text: String,
    modifier: Modifier = Modifier,
    isRequired: Boolean = false,
    style: TextStyle = AfternoteDesign.typography.bodyBase, // 핵심: 정의한 테마를 기본값으로 직접 사용
    color: Color = AfternoteDesign.colors.gray9, // 핵심: 정의한 색상을 기본값으로 직접 사용
    requiredDotOffsetY: Dp = 4.dp,
) {
    Row(
        modifier = modifier,
    ) {
        Text(
            text = text,
            style = style, // Text 컴포넌트가 style과 color를 분리해서 받으므로 copy 불필요
            color = color,
        )

        if (isRequired) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier =
                    Modifier
                        .padding(top = requiredDotOffsetY)
                        .size(4.dp)
                        .background(
                            color = AfternoteDesign.colors.b1, // 정의한 테마 직접 사용
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
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            Label(text = "이름")
        }
    }
}

@Preview(showBackground = true, name = "필수 라벨")
@Composable
private fun LabelRequiredPreview() {
    AfternoteTheme {
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            Label(
                text = "정보 처리 방법",
                isRequired = true,
            )
        }
    }
}

@Preview(showBackground = true, name = "작은 라벨 (Theme Typography 활용)")
@Composable
private fun LabelSmallPreview() {
    AfternoteTheme {
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            Label(
                text = "종류",
                // 하드코딩 대신 다른 테마 타이포그래피를 주입 (예: captionLargeR 등)
                // 만약 마땅한 테마가 없다면 copy를 통해 오버라이드 합니다.
                style =
                    AfternoteDesign.typography.textField.copy(
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
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
                style = AfternoteDesign.typography.textField.copy(fontSize = 12.sp),
                requiredDotOffsetY = 2.dp,
            )
        }
    }
}
