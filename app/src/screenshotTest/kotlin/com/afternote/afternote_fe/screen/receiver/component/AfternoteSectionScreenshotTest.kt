package com.afternote.afternote_fe.screen.receiver.component

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.afternote_fe.screen.receiver.model.AfternoteSourceIcon
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
                    AfternoteSourceIcon(drawableResId = com.afternote.core.ui.R.drawable.core_ui_ic_tabler_search),
                    AfternoteSourceIcon(drawableResId = com.afternote.core.ui.R.drawable.core_ui_ic_link),
                ),
            onGoClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
