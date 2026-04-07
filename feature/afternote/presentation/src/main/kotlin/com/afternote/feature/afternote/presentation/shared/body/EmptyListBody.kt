package com.afternote.feature.afternote.presentation.shared.body

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.feature.afternote.presentation.R

@Composable
fun EmptyListBody(modifier: Modifier = Modifier) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(start = 20.dp, top = 24.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.feature_afternote_ic_empty_logo),
            contentDescription = null,
            modifier =
                Modifier
                    .width(200.dp),
            tint = AfternoteDesign.colors.gray8,
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = stringResource(R.string.feature_afternote_empty_list_body),
            style = AfternoteDesign.typography.bodyLargeR,
            color = AfternoteDesign.colors.gray8,
        )
        Spacer(Modifier.weight(1f))
    }
}

@Preview(showBackground = true, heightDp = 600)
@Composable
private fun EmptyListBodyPreview() {
    EmptyListBody()
}
