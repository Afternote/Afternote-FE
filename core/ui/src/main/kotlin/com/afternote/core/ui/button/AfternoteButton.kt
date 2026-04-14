package com.afternote.core.ui.button

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.icon.RightArrowIcon
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme

enum class AfternoteButtonType {
    Default,
    Active,
    Plain,
    Un,
    Variant5,
}

@Composable
fun AfternoteButton(
    text: String,
    onClick: () -> Unit,
    type: AfternoteButtonType,
    modifier: Modifier = Modifier,
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

@Composable
fun AfternoteActionButton(
    text: String,
    containerColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentColor: Color = AfternoteDesign.colors.white,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = containerColor,
        contentColor = contentColor,
    ) {
        Row(
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 20.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = text,
                style = AfternoteDesign.typography.captionLargeB,
                color = contentColor,
            )
            Spacer(modifier = Modifier.width(6.dp))
            RightArrowIcon(modifier = Modifier.Companion.size(14.dp))
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFCCCCCC)
@Composable
private fun AfternoteActionButtonPreview() {
    AfternoteTheme {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AfternoteActionButton(
                text = "마음의 기록 남기기",
                containerColor = AfternoteDesign.colors.accent1,
                onClick = {},
            )
            AfternoteActionButton(
                text = "마음의 기록 남기기",
                containerColor = AfternoteDesign.colors.accent2,
                onClick = {},
            )
            AfternoteActionButton(
                text = "마음의 기록 남기기",
                containerColor = AfternoteDesign.colors.accent5,
                onClick = {},
            )
            AfternoteActionButton(
                text = "마음의 기록 남기기",
                containerColor = AfternoteDesign.colors.accent10,
                onClick = {},
            )
        }
    }
}
