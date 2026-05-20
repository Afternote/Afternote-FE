package com.afternote.core.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.afternote.core.ui.button.PlusBadgeButton
import com.afternote.core.ui.theme.AfternoteTheme

/**
 * 단순 표시 전용 프로필 이미지. URI 없으면 placeholder 로 fallback.
 *
 * 편집 picker 가 필요하면 [ProfileImagePicker] 사용.
 */
@Composable
fun ProfileImage(
    modifier: Modifier = Modifier,
    displayImageUri: String? = null,
    size: Dp = 134.dp,
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        ProfileImageContent(
            displayImageUri = displayImageUri,
            modifier =
                Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
        )
    }
}

/**
 * 편집 picker 형 프로필 이미지. 우하단의 [PlusBadgeButton] 클릭 시 [onPickClick] 호출.
 * picker 실행(예: photoPickerLauncher.launch) 책임은 호출자가 보유.
 */
@Composable
fun ProfileImagePicker(
    onPickClick: () -> Unit,
    modifier: Modifier = Modifier,
    displayImageUri: String? = null,
    size: Dp = 134.dp,
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        ProfileImageContent(
            displayImageUri = displayImageUri,
            modifier =
                Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
        )
        PlusBadgeButton(
            contentDescription = stringResource(R.string.core_ui_content_description_profile_edit),
            onClick = onPickClick,
            paddingValues = PaddingValues(17.dp),
            modifier = Modifier.align(Alignment.BottomEnd),
            size = 48.dp,
        )
    }
}

@Composable
private fun ProfileImageContent(
    displayImageUri: String?,
    modifier: Modifier = Modifier,
) {
    val placeholder = painterResource(R.drawable.core_ui_ic_profile_placeholder)
    val contentDescription = stringResource(R.string.core_ui_content_description_profile_image)

    if (!displayImageUri.isNullOrBlank()) {
        AsyncImage(
            model = displayImageUri,
            contentDescription = contentDescription,
            modifier = modifier,
            placeholder = placeholder,
            error = placeholder,
        )
    } else {
        Image(
            painter = placeholder,
            contentDescription = contentDescription,
            modifier = modifier,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileImagePickerPreview() {
    AfternoteTheme {
        ProfileImagePicker(
            onPickClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileImagePreview() {
    AfternoteTheme {
        ProfileImage()
    }
}
