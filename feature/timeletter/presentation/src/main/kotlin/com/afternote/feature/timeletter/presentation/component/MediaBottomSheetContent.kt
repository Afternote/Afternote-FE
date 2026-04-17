package com.afternote.feature.timeletter.presentation.component

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

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

        MediaItem(icon = Icons.Default.Image, text = "이미지 추가하기", onClick = onImageClick)
        HorizontalDivider()
        MediaItem(icon = Icons.Default.Mic, text = "음성 추가하기", onClick = onVoiceClick)
        HorizontalDivider()
        MediaItem(icon = Icons.Default.Description, text = "파일 추가하기", onClick = onFileClick)
        HorizontalDivider()
        MediaItem(icon = Icons.Default.Link, text = "링크 추가하기", onClick = onLinkClick)
        HorizontalDivider()

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun MediaItem(
    icon: ImageVector,
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
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.Gray,
        )
        Text(text = text)
    }
}
