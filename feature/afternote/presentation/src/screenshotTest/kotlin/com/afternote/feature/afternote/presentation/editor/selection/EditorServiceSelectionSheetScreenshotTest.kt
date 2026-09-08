package com.afternote.feature.afternote.presentation.editor.selection

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.presentation.shared.util.AfternoteServiceCatalog
import com.android.tools.screenshot.PreviewTest

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun editorServiceSelectionSheetScreenshot() {
    AfternoteTheme {
        EditorServiceSelectionSheetContent(
            title = "소셜 네트워크 서비스 선택",
            type = AfternoteType.SOCIAL_NETWORK,
            services = AfternoteServiceCatalog.socialServices,
            searchQueryState = rememberTextFieldState(),
            onServiceSelected = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun editorServiceSelectionSheetEmptySearchScreenshot() {
    AfternoteTheme {
        EditorServiceSelectionSheetContent(
            title = "갤러리 및 파일 서비스 선택",
            type = AfternoteType.GALLERY_AND_FILES,
            services = AfternoteServiceCatalog.galleryServices,
            searchQueryState = rememberTextFieldState("없는 서비스"),
            onServiceSelected = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun editorServiceSelectionCustomPrefillFieldScreenshot() {
    AfternoteTheme {
        Box(modifier = Modifier.padding(20.dp)) {
            EditorServiceSelectionField(
                selectedService = "사내 레거시 서비스",
                placeholder = "소셜네트워크 선택하기",
                onClick = {},
            )
        }
    }
}
