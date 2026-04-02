package com.afternote.feature.afternote.presentation.author.navigation

import com.afternote.feature.afternote.domain.model.Item
import com.afternote.feature.afternote.presentation.author.edit.model.AfternoteEditState
import com.afternote.feature.afternote.presentation.author.edit.model.MemorialPlaylistStateHolder
import com.afternote.feature.afternote.presentation.author.edit.provider.AfternoteEditDataProvider

/**
 * Hoisted edit state for navigation (UDF: value + events, not [androidx.compose.runtime.MutableState]).
 *
 * [state] is the latest snapshot; [onStateChanged] replaces the stored edit state; [onClear] clears it.
 */
data class AfternoteEditStateHandling(
    val state: AfternoteEditState?,
    val onStateChanged: (AfternoteEditState?) -> Unit,
    val onClear: () -> Unit,
)

/**
 * List refresh signals for the list route. [listRefreshRequested] should be driven by observable
 * state upstream (e.g. [kotlinx.coroutines.flow.StateFlow]) so recompositions see changes.
 */
data class AfternoteListRefreshParams(
    val listRefreshRequested: Boolean,
    val onListRefreshConsumed: () -> Unit,
    val onAfternoteDeleted: () -> Unit,
)

/** List-related inputs for [afternoteNavGraph] (items + list refresh). */
data class AfternoteNavGraphListContext(
    val afternoteItems: List<Item>,
    val onItemsUpdated: (List<Item>) -> Unit,
    val listRefresh: AfternoteListRefreshParams? = null,
)

/** Edit-flow shared dependencies (playlist, providers, hoisted edit state). */
data class AfternoteNavGraphEditContext(
    val playlistStateHolder: MemorialPlaylistStateHolder,
    val afternoteProvider: AfternoteEditDataProvider,
    val editStateHandling: AfternoteEditStateHandling,
    val onNavigateToSelectReceiver: () -> Unit = {},
)

/**
 * Parameters for [afternoteNavGraph].
 *
 * Split into [list] / [edit] to avoid a single “god” params object. For graph-scoped shared
 * ViewModel, see future refactor (Hilt + [androidx.navigation.NavBackStackEntry] scope).
 */
data class AfternoteNavGraphParams(
    val list: AfternoteNavGraphListContext,
    val edit: AfternoteNavGraphEditContext,
    val userName: String,
)
