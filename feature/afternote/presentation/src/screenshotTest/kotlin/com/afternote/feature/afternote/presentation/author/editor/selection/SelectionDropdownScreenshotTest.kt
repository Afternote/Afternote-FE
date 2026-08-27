package com.afternote.feature.afternote.presentation.author.editor.selection

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.R
import com.android.tools.screenshot.PreviewTest

// 서비스명 드롭다운 — 미선택(placeholder, gray5) 상태 (이슈 #468).
@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun selectionDropdownPlaceholderScreenshot() {
    AfternoteTheme {
        Box(modifier = Modifier.padding(24.dp)) {
            EditorSelectionDropdown(
                label = stringResource(R.string.afternote_editor_label_service_name),
                selectedValue = "",
                options = emptyList(),
                optionLabel = { it },
                onValueSelected = {},
                expanded = false,
                onExpandedChange = {},
                placeholder =
                    stringResource(
                        R.string.afternote_editor_service_placeholder,
                        stringResource(R.string.afternote_editor_category_social),
                    ),
            )
        }
    }
}

// 서비스명 드롭다운 — 선택 완료(gray8) 상태. placeholder 지정돼도 선택값이 우선한다.
@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun selectionDropdownSelectedScreenshot() {
    AfternoteTheme {
        Box(modifier = Modifier.padding(24.dp)) {
            EditorSelectionDropdown(
                label = stringResource(R.string.afternote_editor_label_service_name),
                selectedValue = "인스타그램",
                options = emptyList(),
                optionLabel = { it },
                onValueSelected = {},
                expanded = false,
                onExpandedChange = {},
                placeholder =
                    stringResource(
                        R.string.afternote_editor_service_placeholder,
                        stringResource(R.string.afternote_editor_category_social),
                    ),
            )
        }
    }
}
