package com.afternote.feature.afternote.presentation.editor.memorial

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.editor.fixture.sampleRecipientSection
import com.android.tools.screenshot.PreviewTest

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun memorialEditorContentScreenshot() {
    AfternoteTheme {
        MemorialEditorContent(
            displayMemorialPhotoUri = null,
            playlistAlbumCovers = emptyList(),
            memorialVideoUrl = null,
            memorialThumbnailUrl = null,
            editorMessages = emptyList(),
            recipientSection = sampleRecipientSection(),
            onSongAddClick = {},
            onPhotoAddClick = {},
            onVideoAddClick = {},
            onMessageRegisterClick = {},
            onMessageDeleteClick = {},
            onMessageAddClick = {},
            onThumbnailBytesReady = {},
            onThumbnailExtractionFailed = {},
            thumbnailRetryToken = 0,
            modifier = Modifier.padding(20.dp),
        )
    }
}
