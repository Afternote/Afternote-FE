package com.afternote.feature.afternote.presentation.author.editor.selection

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * [SelectionDropdown]의 UI 상태(expanded 등).
 */
@Stable
class SelectionDropdownState(
    initialExpanded: Boolean = false,
) {
    var expanded by mutableStateOf(initialExpanded)
}

/**
 * [SelectionDropdownState]를 생성합니다.
 */
@Composable
fun rememberSelectionDropdownState(initialExpanded: Boolean = false): SelectionDropdownState =
    remember {
        SelectionDropdownState(initialExpanded = initialExpanded)
    }
