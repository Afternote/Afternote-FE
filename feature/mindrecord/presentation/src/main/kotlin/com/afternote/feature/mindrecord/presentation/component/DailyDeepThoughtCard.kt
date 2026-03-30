package com.afternote.feature.mindrecord.presentation.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.core.ui.theme.Gray2
import com.afternote.core.ui.theme.Gray3
import com.afternote.core.ui.theme.Gray8
import com.afternote.core.ui.theme.White
import com.afternote.feature.mindrecord.presentation.R
import java.time.LocalDate

@Composable
fun DailyDeepThoughtCard(modifier: Modifier = Modifier) {
    OutlinedCard(
        colors =
            CardDefaults.cardColors(
                containerColor = White,
            ),
        border = BorderStroke(1.dp, color = Gray2),
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp)),
    ) {
        Column(modifier = Modifier.fillMaxSize().drawWithCache{
            val brush = Brush.linearGradient(
                colorStops = arrayOf(
                    0.0f to Color(0xFFF5F5F4).copy(alpha = 0.8f),
                    1.0f to Color(0xFFFAFAF9).copy(alpha = 0.5f)
                )
            )
            onDrawBehind {
                drawRect(brush)
            }
        }.padding(25.dp)) {
            Icon(
                painter = painterResource(R.drawable.semicol),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = Gray3
            )

            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "생각은 깊이를 더할수록 진실에 가까워진다.",
                style = MaterialTheme.typography.titleMedium,
                color = Gray8
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.mindrecord_bulb),
                    contentDescription = null,
                    tint = Color(0xFF000000).copy(alpha = 0.3f),
                )
                Text(
                    text = LocalDate.now().toString(),
                    style = MaterialTheme.typography.displayMedium,
                    color = Color(0xFF000000).copy(0.4f),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DailyDeepThoughtCardPreview() {
    AfternoteTheme {
        DailyDeepThoughtCard()
    }
}