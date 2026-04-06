package com.afternote.core.ui.button

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.afternote.core.ui.expand.dropShadow
import com.afternote.core.ui.theme.B3
import com.afternote.core.ui.theme.Gray3
import com.afternote.core.ui.theme.Gray9
import com.afternote.core.ui.theme.naNumGothic

@Composable
fun ClickButton(
    color: Color,
    onButtonClick: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onButtonClick,
        modifier =
            modifier
                .fillMaxWidth()
                .height(54.dp)
                .dropShadow(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.05f),
                    offsetX = 5.dp,
                    offsetY = 0.dp,
                    blur = 5.dp,
                    spread = 0.dp,
                ),
        shape = RoundedCornerShape(8.dp),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = color,
            ),
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            color = Gray9,
            fontFamily = naNumGothic,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
fun ClickButton(
    onButtonClick: () -> Unit,
    title: String,
    isTrue: Boolean,
    modifier: Modifier = Modifier,
    activeColor: Color = B3,
) {
    Button(
        enabled = isTrue,
        onClick = onButtonClick,
        modifier =
            modifier
                .fillMaxWidth()
                .height(54.dp)
                .dropShadow(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.05f),
                    offsetX = 5.dp,
                    offsetY = 0.dp,
                    blur = 5.dp,
                    spread = 0.dp,
                ),
        shape = RoundedCornerShape(8.dp),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = if (isTrue) activeColor else Gray3,
            ),
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            color = Gray9,
            fontFamily = naNumGothic,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Preview
@Composable
private fun ClickButtonPreview() {
    ClickButton(
        onButtonClick = {},
        title = "시작하기",
        color = B3,
    )
}
