package com.afternote.feature.afternote.presentation.author.editor.memorial

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.ProfileImage
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.R

@Composable
fun MemorialPhotoUpload(
    modifier: Modifier = Modifier,
    label: String? = null,
    displayImageUri: String? = null,
    onAddPhotoClick: () -> Unit = {},
) {
    val labelText = label ?: stringResource(R.string.afternote_editor_memorial_photo_label)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(space = 16.dp),
    ) {
        Text(
            text = labelText,
            style =
                AfternoteDesign.typography.textField.copy(
                    fontWeight = FontWeight.Medium,
                    color = AfternoteDesign.colors.gray9,
                ),
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ProfileImage(
                onClick = onAddPhotoClick,
                displayImageUri = displayImageUri,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MemorialPhotoUploadPreview() {
    AfternoteTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // 이미지 없음
            MemorialPhotoUpload()
        }
    }
}
