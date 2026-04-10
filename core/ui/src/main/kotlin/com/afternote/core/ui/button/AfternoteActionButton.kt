package com.afternote.core.ui.button

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.icon.RightArrowIcon
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme

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
            RightArrowIcon(size = 14.dp)
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
