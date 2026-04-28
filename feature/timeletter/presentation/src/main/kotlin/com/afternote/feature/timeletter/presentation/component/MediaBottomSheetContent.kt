package com.afternote.feature.timeletter.presentation.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.timeletter.presentation.R

@Composable
fun MediaBottomSheetContent(
    onImageClick: () -> Unit,
    onVoiceClick: () -> Unit,
    onFileClick: () -> Unit,
    onLinkClick: () -> Unit,
) {
    Column {
        Text(
            text = "미디어 추가하기",
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
            textAlign = TextAlign.Center,
        )

        MediaItem(painter = painterResource(id = R.drawable.ic_image), text = "이미지 추가하기", onClick = onImageClick)
        HorizontalDivider()
        MediaItem(painter = painterResource(id = R.drawable.ic_mic), text = "음성 추가하기", onClick = onVoiceClick)
        HorizontalDivider()
        MediaItem(painter = painterResource(id = R.drawable.ic_file), text = "파일 추가하기", onClick = onFileClick)
        HorizontalDivider()
        MediaItem(painter = painterResource(id = R.drawable.ic_link), text = "링크 추가하기", onClick = onLinkClick)
        HorizontalDivider()

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Preview(showBackground = true)
@Composable
private fun MediaBottomSheetContentPreview() {
    AfternoteTheme {
        MediaBottomSheetContent(
            onImageClick = {},
            onVoiceClick = {},
            onFileClick = {},
            onLinkClick = {},
        )
    }
}

@Composable
private fun MediaItem(
    painter: androidx.compose.ui.graphics.painter.Painter,
    text: String,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Image(
            painter = painter,
            contentDescription = null,
        )
        Text(text = text)
    }
}
