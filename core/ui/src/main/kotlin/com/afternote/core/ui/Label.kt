package com.afternote.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme

/**
 * 서비스 전반에서 사용되는 공통 라벨 컴포넌트입니다.
 *
 * 텍스트 스타일과 색상은 디자인 시스템에 의해 고정됩니다
 * (`AfternoteDesign.typography.bodyBase` / `AfternoteDesign.colors.gray9`).
 *
 * @param text 라벨에 표시될 문구
 * @param modifier 레이아웃 수정을 위한 모디파이어
 * @param isRequired 필수 항목 여부 (true일 경우 우측에 점 표시)
 */
@Composable
fun Label(
    text: String,
    modifier: Modifier = Modifier,
    isRequired: Boolean = false,
) {
    Row(
        modifier = modifier.semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = AfternoteDesign.typography.bodyBase,
            color = AfternoteDesign.colors.gray9,
        )

        if (isRequired) {
            val requiredMarkerDescription = stringResource(R.string.core_ui_semantics_required_field)
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier =
                    Modifier
                        .size(4.dp)
                        .background(
                            color = AfternoteDesign.colors.b1,
                            shape = CircleShape,
                        ).semantics {
                            contentDescription = requiredMarkerDescription
                        },
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
