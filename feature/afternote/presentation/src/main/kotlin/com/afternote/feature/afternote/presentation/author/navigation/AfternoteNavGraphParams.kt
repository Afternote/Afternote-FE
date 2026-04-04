package com.afternote.feature.afternote.presentation.author.navigation

import com.afternote.feature.afternote.domain.model.ListItem
import com.afternote.feature.afternote.presentation.author.editor.model.AfternoteEditorState
import com.afternote.feature.afternote.presentation.author.editor.model.MemorialPlaylistStateHolder
import com.afternote.feature.afternote.presentation.author.editor.provider.AfternoteEditorDataProvider

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

/** Home-related inputs for [afternoteNavGraph] (visibleItems + home refresh). */
data class AfternoteNavGraphHomeContext(
    val afternoteVisibleItems: List<ListItem>,
    val onVisibleItemsUpdated: (List<ListItem>) -> Unit,
    val homeRefresh: AfternoteHomeRefreshParams? = null,
)

/** Edit-flow shared dependencies (playlist, providers, hoisted edit state). */
data class AfternoteNavGraphEditContext(
    val playlistStateHolder: MemorialPlaylistStateHolder,
    val afternoteProvider: AfternoteEditorDataProvider,
    val editStateHandling: AfternoteEditorStateHandling,
    val onNavigateToSelectReceiver: () -> Unit = {},
)

/**
 * Parameters for [afternoteNavGraph].
 *
 * Split into [home] / [edit] to avoid a single “god” params object. For graph-scoped shared
 * ViewModel, see future refactor (Hilt + [androidx.navigation.NavBackStackEntry] scope).
 */
data class AfternoteNavGraphParams(
    val home: AfternoteNavGraphHomeContext,
    val edit: AfternoteNavGraphEditContext,
    val userName: String,
)
