package com.afternote.feature.afternote.presentation.detail.account

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.domain.AfternoteType
import com.android.tools.screenshot.PreviewTest

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun accountDetailScreenScreenshot() {
    AfternoteTheme {
        AccountDetailScreen(
            onBackClick = {},
            onEditClick = {},
            onDeleteConfirm = {},
            content =
                AccountDetailContent(
                    serviceName = "직접 입력한 소셜 서비스",
                    type = AfternoteType.SOCIAL_NETWORK,
                    finalWriteDate = "2026.08.28",
                ),
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun accountDetailBusinessFallbackScreenshot() {
    AfternoteTheme {
        AccountDetailScreen(
            onBackClick = {},
            onEditClick = {},
            onDeleteConfirm = {},
            content =
                AccountDetailContent(
                    serviceName = "직접 입력한 비즈니스 서비스",
                    type = AfternoteType.BUSINESS,
                    finalWriteDate = "2026.08.28",
                ),
        )
    }
}
