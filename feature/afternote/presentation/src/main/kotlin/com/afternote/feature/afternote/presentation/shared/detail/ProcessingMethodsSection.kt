package com.afternote.feature.afternote.presentation.shared.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.button.AfternoteCircularCheckbox
import com.afternote.core.ui.button.CheckboxState
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.R
import com.afternote.core.ui.R as CoreUiR

/**
 * "처리방법" 섹션.
 *
 * [DetailSection] 슬롯 안에 체크박스 리스트만 둔다.
 * 섹션 아래 외부 간격은 호출하는 부모 레이아웃에서 둔다.
 * [methods] 가 비어 있으면 아무것도 렌더링하지 않는다.
 */
@Composable
fun ProcessingMethodsSection(
    methods: List<String>,
    modifier: Modifier = Modifier,
) {
    DetailSection(
        iconResId = CoreUiR.drawable.core_ui_settings,
        label = stringResource(R.string.feature_afternote_detail_section_processing),
        modifier = modifier,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(17.dp)) {
            methods.forEach { method ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    AfternoteCircularCheckbox(
                        state = CheckboxState.Default,
                        size = 20.dp,
                    )
                    Text(
                        text = method,
                        style = AfternoteDesign.typography.bodySmallR,
                        color = AfternoteDesign.colors.gray9,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ProcessingMethodsSectionPreview() {
    AfternoteTheme {
        ProcessingMethodsSection(
            methods = listOf("계정 삭제", "게시물 유지", "메모리얼 계정 전환"),
        )
    }
}
