package com.afternote.core.ui

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.tooling.preview.Preview
import coil3.annotation.ExperimentalCoilApi
import coil3.asImage
import coil3.compose.AsyncImagePreviewHandler
import coil3.compose.LocalAsyncImagePreviewHandler
import com.afternote.core.ui.theme.AfternoteTheme
import com.android.tools.screenshot.PreviewTest

/**
 * [ProfileImage]·[ProfileImagePicker] 에 가로가 긴 사진(4:3)이 들어왔을 때의 baseline (#2143).
 *
 * 휴대폰 카메라 사진은 대부분 4:3 이다. 사진이 원을 다 채우지 못하면 원 위아래가 평평하게
 * 잘린 모양이 된다. 사진은 한 가지 색으로 칠해 두어, 원이 꽉 찼는지가 윤곽만으로 드러난다.
 *
 * `AsyncImage` 는 프리뷰에서 네트워크를 타지 않고 [LocalAsyncImagePreviewHandler] 가 주는
 * 이미지를 그린다. URI 는 `AsyncImage` 분기를 타게 하는 자리표시일 뿐이다.
 */
@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun profileImageLandscapePhotoScreenshot() {
    LandscapePhotoPreview {
        ProfileImage(displayImageUri = LANDSCAPE_PHOTO_URI)
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun profileImagePickerLandscapePhotoScreenshot() {
    LandscapePhotoPreview {
        ProfileImagePicker(onPickClick = {}, displayImageUri = LANDSCAPE_PHOTO_URI)
    }
}

@OptIn(ExperimentalCoilApi::class)
@Composable
private fun LandscapePhotoPreview(content: @Composable () -> Unit) {
    val photo =
        Bitmap
            .createBitmap(LANDSCAPE_PHOTO_WIDTH, LANDSCAPE_PHOTO_HEIGHT, Bitmap.Config.ARGB_8888)
            .apply { eraseColor(LANDSCAPE_PHOTO_COLOR) }
            .asImage()
    CompositionLocalProvider(LocalAsyncImagePreviewHandler provides AsyncImagePreviewHandler { photo }) {
        AfternoteTheme {
            content()
        }
    }
}

private const val LANDSCAPE_PHOTO_URI = "content://preview/landscape-4x3"
private const val LANDSCAPE_PHOTO_WIDTH = 160
private const val LANDSCAPE_PHOTO_HEIGHT = 120
private const val LANDSCAPE_PHOTO_COLOR = 0xFF5B7DB1.toInt()
