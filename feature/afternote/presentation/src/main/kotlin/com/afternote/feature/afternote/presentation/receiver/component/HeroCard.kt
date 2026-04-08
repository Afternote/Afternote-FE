package com.afternote.feature.afternote.presentation.receiver.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.feature.afternote.presentation.R

@Composable
fun HeroCard(
    modifier: Modifier = Modifier,
    leaveMessage: String = stringResource(R.string.receiver_hero_default_message),
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    brush =
                        Brush.linearGradient(
                            colors =
                                listOf(
                                    Color(0xFFD0E4FF), // Light Blue
                                    Color(0xFFF0F4F8), // White-ish
                                    Color(0xFFFFE0CC), // Light Peach
                                ),
                        ),
                ),
    ) {
        Image(
            painter = painterResource(R.drawable.feature_afternote_img_hero_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )

        Column(
            modifier =
                Modifier
                    .padding(20.dp)
                    .align(Alignment.CenterStart),
        ) {
            Text(
                text = leaveMessage,
                style = AfternoteDesign.typography.h2,
                color = AfternoteDesign.colors.gray8,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.feature_afternote_hero_last_greeting_caption),
                style =
                    AfternoteDesign.typography.bodySmallR.copy(
                        fontWeight = FontWeight.Medium,
                        color = AfternoteDesign.colors.gray5,
                    ),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewHeroCard() {
    HeroCard(leaveMessage = stringResource(R.string.receiver_hero_default_message))
}
