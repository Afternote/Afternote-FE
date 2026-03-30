package com.afternote.feature.afternote.presentation.shared.component.list
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.core.ui.theme.Gray4
import com.afternote.core.ui.theme.Sansneo
import com.afternote.feature.afternote.presentation.R

/**
 * Shared empty state for 애프터노트 list (writer main and receiver list).
 */
@Composable
fun EmptyAfternoteContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Spacer(modifier = Modifier.weight(12f))
            Image(
                painter = painterResource(R.drawable.img_empty_state),
                contentDescription = "빈 애프터노트",
                modifier =
                    Modifier
                        .width(106.dp)
                        .height(109.dp)
                        .alpha(0.6f),
            )
            Spacer(modifier = Modifier.weight(10f))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "아직 등록된 애프터노트가 없어요.\n등록하여 계정을 보호하세요.",
            style =
                TextStyle(
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontFamily = Sansneo,
                    fontWeight = FontWeight.Normal,
                    color = Gray4,
                    textAlign = TextAlign.Center,
                ),
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptyAfternoteContentPreview() {
    AfternoteTheme {
        EmptyAfternoteContent()
    }
}
