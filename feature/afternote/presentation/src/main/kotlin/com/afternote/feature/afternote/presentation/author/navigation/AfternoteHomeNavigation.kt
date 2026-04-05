package com.afternote.feature.afternote.presentation.author.navigation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.afternote.core.ui.scaffold.bottombar.BottomNavTab
import com.afternote.feature.afternote.domain.model.ListItem
import com.afternote.feature.afternote.presentation.author.editor.AfternoteItemMapper
import com.afternote.feature.afternote.presentation.author.editor.provider.AfternoteEditorDataProvider
import com.afternote.feature.afternote.presentation.author.home.AfternoteHomeEntry
import com.afternote.feature.afternote.presentation.author.home.AfternoteHomeEntryActions
import com.afternote.feature.afternote.presentation.author.navigation.model.AfternoteRoute
import com.afternote.feature.afternote.presentation.shared.AfternoteCategory

internal fun resolveListItems(
    afternoteVisibleItems: List<ListItem>,
    afternoteProvider: AfternoteEditorDataProvider,
): List<ListItem> =
    afternoteVisibleItems.ifEmpty {
        AfternoteItemMapper.toAfternoteItemsWithStableIds(afternoteProvider.getDefaultAfternoteItems())
    }

@Composable
internal fun AfternoteHomeNavigation(
    navController: NavController,
    onNavTabSelected: (BottomNavTab) -> Unit = {},
    onVisibleItemsUpdated: (List<ListItem>) -> Unit,
    homeRefreshRequested: Boolean = false,
    onHomeRefreshConsumed: () -> Unit = {}, // 새로고침 실행했을 때 플래그 끔
) {
    AfternoteHomeEntry(
        homeRefreshRequested = homeRefreshRequested,
        onHomeRefreshConsumed = onHomeRefreshConsumed,
        actions =
            AfternoteHomeEntryActions(
                navigateToDetail = { itemId ->
                    navController.navigate(AfternoteRoute.DetailRoute(itemId = itemId))
                },
                navigateToGalleryDetail = { itemId ->
                    navController.navigate(AfternoteRoute.GalleryDetailRoute(itemId = itemId))
                },
                navigateToMemorialGuidelineDetail = { itemId ->
                    navController.navigate(AfternoteRoute.MemorialGuidelineDetailRoute(itemId = itemId))
                },
                navigateToAdd = { selectedTab ->
                    val initialCategory =
                        if (selectedTab == AfternoteCategory.ALL) null else selectedTab.label
                    Log.d(
                        "AfternoteNav",
                        "FAB navigateToAdd → navigate(EditorRoute initialCategory=$initialCategory)",
                    )
                    navController.navigate(AfternoteRoute.EditorRoute(initialCategory = initialCategory))
                },
                onNavTabSelected = onNavTabSelected,
            ),
        onVisibleItemsUpdated = onVisibleItemsUpdated,
    )
}
