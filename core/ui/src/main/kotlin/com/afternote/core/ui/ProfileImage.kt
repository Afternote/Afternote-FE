package com.afternote.core.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import com.afternote.core.ui.button.AddCircleButton
import com.afternote.core.ui.theme.AfternoteTheme

@Composable
fun ProfileImage(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isEditable: Boolean = true,
    displayImageUri: String? = null,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        ProfileImageContent(
            displayImageUri = displayImageUri,
        )
        if (isEditable) {
            AddCircleButton(
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
    val size = 134.dp
    if (!displayImageUri.isNullOrBlank()) {
        val context = LocalContext.current
        val imageRequest =
            remember(displayImageUri) {
                ImageRequest
                    .Builder(context)
                    .data(displayImageUri)
                    .httpHeaders(
                        NetworkHeaders
                            .Builder()
                            .apply {
                                this["User-Agent"] = "Afternote Android App"
                            }.build(),
                    ).build()
            }
        AsyncImage(
            model = imageRequest,
            contentDescription = stringResource(R.string.core_ui_content_description_profile_image),
            modifier = modifier.size(size),
            error = painterResource(R.drawable.core_ui_ic_profile_placeholder),
        )
    } else {
        Image(
            painter = painterResource(R.drawable.core_ui_ic_profile_placeholder),
            contentDescription = stringResource(R.string.core_ui_content_description_profile_image),
            modifier = modifier.size(size),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileImagePreview() {
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
