package com.afternote.feature.afternote.presentation.editor

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.presentation.editor.state.AfternoteTypeForm
import com.afternote.feature.afternote.presentation.editor.state.rememberAfternoteEditorState
import com.android.tools.screenshot.PreviewTest

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun accountPrefillSkeletonScreenshot() {
    EditorPrefillSkeletonScreenshotFixture(type = AfternoteType.SOCIAL_NETWORK)
}

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun galleryPrefillSkeletonScreenshot() {
    EditorPrefillSkeletonScreenshotFixture(type = AfternoteType.GALLERY_AND_FILES)
}

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun memorialPrefillSkeletonScreenshot() {
    EditorPrefillSkeletonScreenshotFixture(type = AfternoteType.MEMORIAL)
}

@Composable
private fun EditorPrefillSkeletonScreenshotFixture(type: AfternoteType) {
    AfternoteTheme {
        val state = rememberAfternoteEditorState()
        val form =
            state.currentForm().copy(
                typeForm = AfternoteTypeForm.pristineFor(type),
            )
        EditorContent(
            state = state,
            form = form,
            typeContent = {},
            isPrefillLoading = true,
        )
    }
}
