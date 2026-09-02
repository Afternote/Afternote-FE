package com.afternote.feature.home.presentation.receiver.component

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.icon.AfternoteSourceIcon
import com.afternote.core.ui.theme.AfternoteTheme
import com.android.tools.screenshot.PreviewTest

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun afternoteSectionScreenshot() {
    AfternoteTheme {
        AfternoteSection(
            totalCount = 5,
            icons =
                listOf(
                    AfternoteSourceIcon.SocialNetwork,
                    AfternoteSourceIcon.GalleryAndFiles,
                ),
            onGoClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
