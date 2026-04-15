package com.afternote.feature.afternote.presentation.shared

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.button.CustomRadioButton
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme

/**
 * 선택 가능한 라디오 버튼 카드 컴포넌트 (Slot API 패턴)
 *
 * 공통 껍데기를 제공하고, 내용물(content)은 호출부에서 주입받는 방식입니다.
 * Material Design의 Slot API 패턴을 따릅니다.
 *
 * @param selected 선택 여부
 * @param onClick 클릭 이벤트
 * @param modifier Modifier
 */
@Composable
fun SelectableRadioCard(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier =
            modifier.selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
            ),
        shape = RoundedCornerShape(6.dp),
        border =
            BorderStroke(
                1.dp,
                if (selected) {
                    AfternoteDesign.colors.gray8
                } else {
                    AfternoteDesign.colors.gray2
                },
            ),
        color = AfternoteDesign.colors.white,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 내부 라디오 버튼은 클릭 이벤트 null 처리하여 부모 Surface 클릭과 충돌 방지
            CustomRadioButton(
                selected = selected,
                onClick = null,
                buttonSize = 24.dp,
                selectedColor = AfternoteDesign.colors.gray9,
                unselectedColor = AfternoteDesign.colors.gray4,
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(
                    text = "제목 텍스트",
                    style =
                        if (selected) {
                            AfternoteDesign.typography.primaryButton.copy(
                                color = AfternoteDesign.colors.gray9,
                            )
                        } else {
                            AfternoteDesign.typography.bodyBase.copy(
                                color = AfternoteDesign.colors.gray8,
                            )
                        },
                )
                Text(
                    text = "설명 텍스트",
                    style =
                        AfternoteDesign.typography.bodySmallR.copy(
                            color = AfternoteDesign.colors.gray6,
                        ),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SelectableRadioCardPreview() {
    AfternoteTheme {
        // 선택됨 - 제목 + 설명
        SelectableRadioCard(
            selected = true,
            onClick = {},
        )
    }
}
