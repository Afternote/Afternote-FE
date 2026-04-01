package com.afternote.feature.afternote.presentation.author.nav.navgraph

import androidx.compose.runtime.MutableState
import com.afternote.feature.afternote.domain.model.Item
import com.afternote.feature.afternote.presentation.author.edit.model.AfternoteEditState
import com.afternote.feature.afternote.presentation.author.edit.model.MemorialPlaylistStateHolder
import com.afternote.feature.afternote.presentation.author.edit.provider.AfternoteEditDataProvider

/**
 * Holder and clear callback for hoisted edit state (keeps param count ≤7).
 */
data class AfternoteEditStateHandling(
    val holder: MutableState<AfternoteEditState?>,
    val onClear: () -> Unit,
)

/**
 * Triggers list refresh when an afternote is deleted so the list reflects the deletion.
 */
data class AfternoteListRefreshParams(
    val listRefreshRequestedProvider: () -> Boolean,
    val onListRefreshConsumed: () -> Unit,
    val onAfternoteDeleted: () -> Unit,
)

/**
 * Parameters for [afternoteNavGraph]. Groups arguments to keep function param count ≤7.
 */
data class AfternoteNavGraphParams(
    val afternoteItemsProvider: () -> List<Item>,
    val onItemsUpdated: (List<Item>) -> Unit,
    val playlistStateHolder: MemorialPlaylistStateHolder,
    val afternoteProvider: AfternoteEditDataProvider,
    val userNameProvider: () -> String,
    val editStateHandling: AfternoteEditStateHandling,
    val listRefresh: AfternoteListRefreshParams? = null,
    val onNavigateToSelectReceiver: () -> Unit = {},
)
