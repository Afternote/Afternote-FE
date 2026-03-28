package com.afternote.feature.timeletter.presentation.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteTheme

@Composable
fun EmptyTimeletterContent(modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(top = 24.dp),
    ) {
        Text(
            text = "아직 등록된 타임레터가 없어요.\n소중한 사람을 위해 작성해 보세요.",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptyTimeletterContentPreview() {
    AfternoteTheme {
        EmptyTimeletterContent()
    }
}
