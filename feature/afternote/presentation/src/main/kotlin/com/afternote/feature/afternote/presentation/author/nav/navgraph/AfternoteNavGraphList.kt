package com.afternote.feature.afternote.presentation.author.nav.navgraph

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.afternote.core.ui.scaffold.bottombar.BottomNavTab
import com.afternote.feature.afternote.domain.model.Item
import com.afternote.feature.afternote.presentation.author.edit.mapper.AfternoteItemMapper
import com.afternote.feature.afternote.presentation.author.edit.model.MemorialPlaylistStateHolder
import com.afternote.feature.afternote.presentation.author.edit.ui.provider.AfternoteEditDataProvider
import com.afternote.feature.afternote.presentation.author.list.AfternoteListRoute
import com.afternote.feature.afternote.presentation.author.list.AfternoteListRouteCallbacks
import com.afternote.feature.afternote.presentation.author.nav.model.AfternoteRoute
import com.afternote.feature.afternote.presentation.shared.list.AfternoteCategory

internal fun resolveListItems(
    afternoteItems: List<Item>,
    afternoteProvider: AfternoteEditDataProvider,
): List<Item> =
    afternoteItems.ifEmpty {
        AfternoteItemMapper.toAfternoteItemsWithStableIds(afternoteProvider.getDefaultAfternoteItems())
    }

@Composable
internal fun AfternoteListRouteContent(
    navController: NavController,
    onNavTabSelected: (BottomNavTab) -> Unit = {},
    onItemsUpdated: (List<Item>) -> Unit,
    editStateHandling: AfternoteEditStateHandling,
    playlistStateHolder: MemorialPlaylistStateHolder,
    listRefresh: AfternoteListRefreshParams? = null,
) {
    AfternoteListRoute(
        listRefreshRequested = listRefresh?.listRefreshRequestedProvider?.invoke() == true,
        onListRefreshConsumed = listRefresh?.onListRefreshConsumed ?: {},
        callbacks =
            AfternoteListRouteCallbacks(
                onNavigateToDetail = { itemId ->
                    navController.navigate(AfternoteRoute.DetailRoute(itemId = itemId))
                },
                onNavigateToGalleryDetail = { itemId ->
                    navController.navigate(AfternoteRoute.GalleryDetailRoute(itemId = itemId))
                },
                onNavigateToMemorialGuidelineDetail = { itemId ->
                    navController.navigate(AfternoteRoute.MemorialGuidelineDetailRoute(itemId = itemId))
                },
                onNavigateToAdd = { selectedTab ->
                    editStateHandling.onClear()
                    playlistStateHolder.clearAllSongs()
                    val initialCategory =
                        if (selectedTab == AfternoteCategory.ALL) null else selectedTab.label
                    Log.d(
                        "AfternoteNav",
                        "FAB onNavigateToAdd → navigate(EditRoute initialCategory=$initialCategory)",
                    )
                    navController.navigate(AfternoteRoute.EditRoute(initialCategory = initialCategory))
                },
                onBottomNavTabSelected = onNavTabSelected,
            ),
        initialItems = emptyList(),
        onItemsChanged = onItemsUpdated,
    )
}
