package com.afternote.feature.afternote.presentation.author.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.R

/**
 * 애프터노트 에디터 섹션 라벨.
 *
 * 각 입력 섹션 상단에 위치하며 [isRequired]가 true일 때 우측에 필수 표시 점이 붙습니다.
 *
 * @param text 라벨 문구
 * @param modifier 외부 레이아웃 수정자
 * @param isRequired 필수 항목 여부 (true 시 우측 점 표시)
 * @param color 텍스트 색상 (기본: gray9)
 * @param style 텍스트 스타일 (기본: bodyBase)
 */
@Composable
fun EditorSectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    isRequired: Boolean = false,
    color: Color = AfternoteDesign.colors.gray9,
    style: TextStyle = AfternoteDesign.typography.bodyBase,
) {
    Row(
        modifier = modifier.semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = style,
            color = color,
        )

        if (isRequired) {
            val requiredMarkerDescription =
                stringResource(R.string.afternote_editor_semantics_required_field)
            Box(
                modifier =
                    Modifier
                        .size(4.dp)
                        .background(
                            color = Color(0xFFFF3647),
                            shape = CircleShape,
                        ).semantics {
                            contentDescription = requiredMarkerDescription
                        }.align(Alignment.Top),
            )
        }
    }
}

@Preview(showBackground = true, name = "기본 라벨")
@Composable
private fun EditorSectionLabelPreview() {
    AfternoteTheme {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
        ) {
            EditorSectionLabel(text = "이름")
        }
    }
}

@Preview(showBackground = true, name = "필수 라벨")
@Composable
private fun EditorSectionLabelRequiredPreview() {
    AfternoteTheme {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
        ) {
            EditorSectionLabel(
                text = "정보 처리 방법",
                isRequired = true,
            )
        }
    }
}

@Preview(showBackground = true, name = "커스텀 스타일 라벨")
@Composable
private fun EditorSectionLabelCustomStylePreview() {
    AfternoteTheme {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
        ) {
            EditorSectionLabel(
                text = "경고 항목",
                color = AfternoteDesign.colors.b1,
            )
        }
    }
}
