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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme

/**
 * 서비스 전반에서 사용되는 공통 라벨 컴포넌트입니다.
 *
 * @param text 라벨에 표시될 문구
 * @param modifier 레이아웃 수정을 위한 모디파이어
 * @param isRequired 필수 항목 여부 (true일 경우 우측에 점 표시)
 * @param style 텍스트 스타일 (기본값: AfternoteDesign.typography.bodyBase)
 * @param color 텍스트 색상 (기본값: AfternoteDesign.colors.gray9)
 */
@Composable
fun Label(
    text: String,
    modifier: Modifier = Modifier,
    isRequired: Boolean = false,
    style: TextStyle = AfternoteDesign.typography.bodyBase,
    color: Color = AfternoteDesign.colors.gray9,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = style,
            color = color,
        )

        if (isRequired) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier =
                    Modifier
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
            )
        }
    }
}
