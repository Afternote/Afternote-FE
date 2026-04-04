package com.afternote.feature.afternote.presentation.author.navigation

import com.afternote.feature.afternote.presentation.author.editor.model.AfternoteEditorState

/**
 * Hoisted edit state for navigation (UDF: value + events, not [androidx.compose.runtime.MutableState]).
 *
 * [state] is the latest snapshot; [onStateChanged] replaces the stored edit state; [onClear] clears it.
 */
data class AfternoteEditorStateHandling(
    val state: AfternoteEditorState?,
    val onStateChanged: (AfternoteEditorState?) -> Unit,
    val onClear: () -> Unit,
)

/**
 * Home refresh signals for the home route. [homeRefreshRequested] should be driven by observable
 * state upstream (e.g. [kotlinx.coroutines.flow.StateFlow]) so recompositions see changes.
 */
data class AfternoteHomeRefreshParams(
    val homeRefreshRequested: Boolean,
    val onHomeRefreshConsumed: () -> Unit,
    val onAfternoteDeleted: () -> Unit,
)
