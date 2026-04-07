package com.afternote.feature.afternote.presentation.shared.body

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.feature.afternote.presentation.R

@Composable
fun EmptyListBody(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        Spacer(Modifier.weight(224f))

        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Spacer(modifier = Modifier.weight(6f))
            Image(
                painter = painterResource(R.drawable.img_empty_state),
                contentDescription = null,
                modifier =
                    Modifier
                        .width(106.dp)
                        .height(109.dp)
                        .alpha(0.6f),
            )
            Spacer(modifier = Modifier.weight(5f))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.feature_afternote_empty_list_body),
            style =
                AfternoteDesign.typography.bodySmallR.copy(
                    color = AfternoteDesign.colors.gray4,
                    textAlign = TextAlign.Center,
                ),
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Spacer(Modifier.weight(269f))
    }
}

@Preview(showBackground = true, heightDp = 600)
@Composable
private fun EmptyListBodyPreview() {
    EmptyListBody()
}
