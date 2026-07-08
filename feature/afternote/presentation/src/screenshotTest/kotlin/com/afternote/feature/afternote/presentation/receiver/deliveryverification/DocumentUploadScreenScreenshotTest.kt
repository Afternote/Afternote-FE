package com.afternote.feature.afternote.presentation.receiver.deliveryverification

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.theme.AfternoteTheme
import com.android.tools.screenshot.PreviewTest

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun documentUploadScreenEmptyScreenshot() {
    AfternoteTheme {
        DocumentUploadScreenContent(
            uiState = DocumentUploadUiState(),
            snackbarHostState = remember { SnackbarHostState() },
            onBackClick = {},
            onSlotClick = {},
            onFamilyFieldBottomChanged = {},
            onSubmitClick = {},
        )
    }
}

/** 한쪽 서류만 업로드된 상태 — 두 서류 중 하나만으로 "다음" 버튼이 활성화되는 것을 가드 (#380). */
@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun documentUploadScreenDeathOnlyScreenshot() {
    AfternoteTheme {
        DocumentUploadScreenContent(
            uiState =
                DocumentUploadUiState(
                    deathCertificate =
                        DocumentSlotState(displayName = "사망진단서.jpeg", fileUrl = "https://x"),
                ),
            snackbarHostState = remember { SnackbarHostState() },
            onBackClick = {},
            onSlotClick = {},
            onFamilyFieldBottomChanged = {},
            onSubmitClick = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun documentUploadScreenFilledScreenshot() {
    AfternoteTheme {
        DocumentUploadScreenContent(
            uiState =
                DocumentUploadUiState(
                    deathCertificate =
                        DocumentSlotState(displayName = "사망진단서.jpeg", fileUrl = "https://x"),
                    familyRelationCertificate =
                        DocumentSlotState(displayName = "가족관계증명서.pdf", fileUrl = "https://y"),
                ),
            snackbarHostState = remember { SnackbarHostState() },
            onBackClick = {},
            onSlotClick = {},
            onFamilyFieldBottomChanged = {},
            onSubmitClick = {},
        )
    }
}
