package com.afternote.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme

@Composable
fun AfternoteIconCountCard(
    icon: Painter,
    title: String,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = AfternoteDesign.colors.white,
        border = BorderStroke(1.dp, AfternoteDesign.colors.gray2),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = AfternoteDesign.colors.gray5,
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = title,
                style = AfternoteDesign.typography.bodyBase,
                color = AfternoteDesign.colors.gray9,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${count}건",
                style = AfternoteDesign.typography.bodySmallR.copy(
                    textDecoration = TextDecoration.Underline,
                ),
                color = AfternoteDesign.colors.gray7,
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                painter = painterResource(R.drawable.core_ui_right),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = AfternoteDesign.colors.gray7,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AfternoteIconCountCardPreview() {
    AfternoteTheme {
        AfternoteIconCountCard(
            icon = painterResource(R.drawable.core_ui_ic_diary),
            title = "나의 모든 기록",
            count = 8,
            onClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
