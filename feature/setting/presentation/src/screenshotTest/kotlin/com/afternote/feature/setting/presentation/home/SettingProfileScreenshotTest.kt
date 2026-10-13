package com.afternote.feature.setting.presentation.home

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.tooling.preview.Preview
import coil3.annotation.ExperimentalCoilApi
import coil3.asImage
import coil3.compose.AsyncImagePainter
import coil3.compose.AsyncImagePreviewHandler
import coil3.compose.LocalAsyncImagePreviewHandler
import coil3.request.ErrorResult
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.setting.presentation.home.component.SettingProfile
import com.android.tools.screenshot.PreviewTest
import java.io.IOException

@PreviewTest
@Preview(showBackground = true, widthDp = 360)
@Composable
internal fun settingProfileDefaultScreenshot() {
    SettingProfileScreenshotContent(profileImageUrl = null)
}

@OptIn(ExperimentalCoilApi::class)
@PreviewTest
@Preview(showBackground = true, widthDp = 360)
@Composable
internal fun settingProfilePhotoScreenshot() {
    val photo =
        Bitmap
            .createBitmap(160, 120, Bitmap.Config.ARGB_8888)
            .apply { eraseColor(0xFF5B7DB1.toInt()) }
            .asImage()
    CompositionLocalProvider(LocalAsyncImagePreviewHandler provides AsyncImagePreviewHandler { photo }) {
        SettingProfileScreenshotContent(profileImageUrl = "content://preview/profile-photo")
    }
}

@OptIn(ExperimentalCoilApi::class)
@PreviewTest
@Preview(showBackground = true, widthDp = 360)
@Composable
internal fun settingProfilePhotoFailureScreenshot() {
    val failedRequest =
        AsyncImagePreviewHandler { _, request ->
            AsyncImagePainter.State.Error(null, ErrorResult(null, request, IOException("missing image")))
        }
    CompositionLocalProvider(LocalAsyncImagePreviewHandler provides failedRequest) {
        SettingProfileScreenshotContent(profileImageUrl = "content://preview/missing-photo")
    }
}

@Composable
private fun SettingProfileScreenshotContent(profileImageUrl: String?) {
    AfternoteTheme {
        SettingProfile(
            name = "홍길동",
            email = "user@example.com",
            profileImageUrl = profileImageUrl,
            onInquiryClick = {},
            onNoticeClick = {},
            onRecipientListClick = {},
        )
    }
}
