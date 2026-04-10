package com.afternote.core.ui.button

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme

/**
 * 피그마 Variants에 대응하는 버튼 타입.
 *
 * - [Default]: 기본 상태 (gray9 배경, 흰색 텍스트)
 * - [Active]: 활성 상태 (gray6 배경, 흰색 텍스트)
 * - [Plain]: 배경만 있는 상태 (gray2 배경, gray9 텍스트, gray3 1dp 외곽선)
 * - [Un]: 비활성 상태 (gray2 배경, gray5 텍스트, gray3 1dp 외곽선, 클릭 불가)
 * - [Variant5]: 텍스트 두 개 + 구분선 (gray9 배경, 흰색 텍스트)
 */
enum class AfternoteButtonType {
    Default,
    Active,
    Plain,
    Un,
    Variant5,
}

/**
 * 애프터노트 공통 버튼.
 *
 * 피그마의 Button Variants를 하나의 컴포넌트로 커버합니다.
 * Material3 Button 대신 Surface를 사용하여 강제 여백 없이 피그마 디자인을 정확히 반영합니다.
 *
 * @param text 버튼 텍스트.
 * @param onClick 클릭 콜백.
 * @param modifier Modifier.
 * @param type 버튼 타입 (피그마 Variant에 대응).
 * @param secondaryText [AfternoteButtonType.Variant5] 전용 두 번째 텍스트.
 */
@Composable
fun AfternoteButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    type: AfternoteButtonType = AfternoteButtonType.Default,
    secondaryText: String? = null,
) {
    CompositionLocalProvider(
        LocalMinimumInteractiveComponentSize provides androidx.compose.ui.unit.Dp.Unspecified,
    ) {
        Surface(
            onClick = onClick,
            modifier =
                modifier.fillMaxWidth(),
            enabled = type != AfternoteButtonType.Un,
            shape = RoundedCornerShape(6.dp),
            color =
                when (type) {
                    AfternoteButtonType.Default -> AfternoteDesign.colors.gray9
                    AfternoteButtonType.Active -> AfternoteDesign.colors.gray6
                    AfternoteButtonType.Plain -> AfternoteDesign.colors.gray2
                    AfternoteButtonType.Un -> AfternoteDesign.colors.gray2
                    AfternoteButtonType.Variant5 -> AfternoteDesign.colors.gray9
                },
            contentColor =
                when (type) {
                    AfternoteButtonType.Plain -> AfternoteDesign.colors.gray9
                    AfternoteButtonType.Un -> AfternoteDesign.colors.gray5
                    else -> AfternoteDesign.colors.white
                },
            border =
                if (type == AfternoteButtonType.Plain || type == AfternoteButtonType.Un) {
                    BorderStroke(
                        1.dp,
                        AfternoteDesign.colors.gray3,
                    )
                } else {
                    null
                },
        ) {
            Row(
                modifier = Modifier.padding(vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                if (type == AfternoteButtonType.Variant5 && secondaryText != null) {
                    Text(
                        text = text,
                        style = AfternoteDesign.typography.captionLargeB,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(24.dp))
                    VerticalDivider(
                        modifier = Modifier.height(12.dp),
                        color = AfternoteDesign.colors.gray2,
                    )
                    Spacer(modifier = Modifier.width(24.dp))
                    Text(
                        text = secondaryText,
                        style = AfternoteDesign.typography.captionLargeB,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    Text(
                        text = text,
                        style = AfternoteDesign.typography.captionLargeB,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Default")
@Composable
private fun AfternoteButtonDefaultPreview() {
    AfternoteTheme {
        Column {
            AfternoteButton(
                text = "시작하기",
                onClick = {},
                type = AfternoteButtonType.Default,
            )
            AfternoteButton(
                text = "활성",
                onClick = {},
                type = AfternoteButtonType.Active,
            )
            AfternoteButton(
                text = "일반",
                onClick = {},
                type = AfternoteButtonType.Plain,
            )
            AfternoteButton(
                text = "비활성",
                onClick = {},
                type = AfternoteButtonType.Un,
            )
            AfternoteButton(
                text = "로그인",
                onClick = {},
                type = AfternoteButtonType.Variant5,
                secondaryText = "회원가입",
            )
        }
    }
}
