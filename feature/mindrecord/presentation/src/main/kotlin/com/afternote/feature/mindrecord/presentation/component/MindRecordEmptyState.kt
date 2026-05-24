package com.afternote.feature.mindrecord.presentation.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.common.R
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.presentation.R as MindRecordR

@Composable
fun MindRecordEmptyState(
    modifier: Modifier = Modifier,
    title: String = stringResource(MindRecordR.string.mindrecord_empty_state_title),
    description: String = stringResource(MindRecordR.string.mindrecord_empty_state_description),
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.core_common_logo),
            contentDescription = null,
            modifier =
                Modifier
                    .width(228.dp)
                    .height(56.dp)
                    .alpha(0.3f),
        )
        Column {
            Text(
                text = title,
                style = AfternoteDesign.typography.bodyLargeR,
                color = AfternoteDesign.colors.gray8,
            )
            Text(
                text = description,
                style = AfternoteDesign.typography.bodyLargeR,
                color = AfternoteDesign.colors.gray8,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MindRecordEmptyStatePreview() {
    AfternoteTheme {
        MindRecordEmptyState()
    }
}
