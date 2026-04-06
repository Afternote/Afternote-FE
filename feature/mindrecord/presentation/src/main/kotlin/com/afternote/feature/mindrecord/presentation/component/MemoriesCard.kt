package com.afternote.feature.mindrecord.presentation.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.presentation.R

@Composable
fun MemoriesCard(modifier: Modifier = Modifier) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .border(
                    1.dp,
                    color = Color(0xFF000000).copy(alpha = 0.05f),
                    shape = RoundedCornerShape(8.dp),
                ),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(R.drawable.mindrecord_img),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            this.alpha = 0.7f
                        }.drawWithContent {
                            drawContent()
                            drawRect(
                                brush =
                                    Brush.verticalGradient(
                                        colorStops =
                                            arrayOf(
                                                0.0f to Color(0xFF000000).copy(alpha = 0.0f),
                                                0.5f to Color(0xFFFFFFFF).copy(alpha = 0.2f),
                                                1.0f to Color(0xFFF8F8F7),
                                            ),
                                    ),
                            )
                        },
            )
        }
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 15.dp),
        ) {
            Text(
                text = "\"내 인생에서 가장 소중했던 순간은?\"",
                style = AfternoteDesign.typography.bodySmallR,
                color = Color(0xFF000000).copy(alpha = 0.8f),
            )
            Text(
                text = "- 아이가 태어났을 때...",
                style = AfternoteDesign.typography.captionLargeR,
                color = Color(0xFF000000).copy(alpha = 0.3f),
            )

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {},
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF000000).copy(alpha = 0.03f),
                    ),
                border = BorderStroke(1.dp, Color(0xFF000000).copy(alpha = 0.05f)),
            ) {
                Text(
                    text = "그날의 기록 다시 읽기",
                    style =
                        TextStyle(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal,
                            lineHeight = 16.5.sp,
                            letterSpacing = 0.06.sp,
                        ),
                    color = Color(0xFF000000).copy(alpha = 0.6f),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MemoriesCardPreview() {
    AfternoteTheme {
        MemoriesCard()
    }
}
