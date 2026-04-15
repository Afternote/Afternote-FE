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
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.afternote.core.ui.button.PlusBadgeButton
import com.afternote.core.ui.theme.AfternoteTheme

@Composable
fun ProfileImage(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isEditable: Boolean = true,
    displayImageUri: String? = null,
) {
    Box(
        modifier = modifier.size(134.dp),
        contentAlignment = Alignment.Center,
    ) {
        ProfileImageContent(
            displayImageUri = displayImageUri,
            modifier =
                Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
        )
        if (isEditable) {
            PlusBadgeButton(
                contentDescription = stringResource(R.string.core_ui_content_description_profile_edit),
                onClick = onClick,
                paddingValues = PaddingValues(14.dp),
                plusSize = 20.dp,
                modifier = Modifier.align(Alignment.BottomEnd),
            )
        }
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
private fun ProfileImageEditablePreview() {
    AfternoteTheme {
        ProfileImage(
            onClick = {},
            isEditable = true,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileImageNotEditablePreview() {
    AfternoteTheme {
        ProfileImage(
            onClick = {},
            isEditable = false,
        )
    }
}
