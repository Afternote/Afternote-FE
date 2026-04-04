package com.afternote.feature.afternote.presentation.author.navigation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.afternote.core.ui.scaffold.bottombar.BottomNavTab
import com.afternote.feature.afternote.domain.model.Item
import com.afternote.feature.afternote.presentation.author.editor.AfternoteItemMapper
import com.afternote.feature.afternote.presentation.author.editor.provider.AfternoteEditorDataProvider
import com.afternote.feature.afternote.presentation.author.home.AfternoteHomeRoute
import com.afternote.feature.afternote.presentation.author.home.AfternoteHomeRouteActions
import com.afternote.feature.afternote.presentation.author.navigation.model.AfternoteRoute
import com.afternote.feature.afternote.presentation.shared.AfternoteCategory

internal fun resolveListItems(
    afternoteItems: List<Item>,
    afternoteProvider: AfternoteEditorDataProvider,
): List<Item> =
    afternoteItems.ifEmpty {
        AfternoteItemMapper.toAfternoteItemsWithStableIds(afternoteProvider.getDefaultAfternoteItems())
    }

@Composable
internal fun AfternoteHomeDestination(
    navController: NavController,
    onNavTabSelected: (BottomNavTab) -> Unit = {},
    onItemsChanged: (List<Item>) -> Unit,
    homeRefresh: AfternoteHomeRefreshParams? = null,
) {
    AfternoteHomeRoute(
        homeRefreshRequested = homeRefresh?.homeRefreshRequested == true,
        onHomeRefreshConsumed = homeRefresh?.onHomeRefreshConsumed ?: {},
        actions =
            AfternoteHomeRouteActions(
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
                        "FAB navigateToAdd → navigate(EditRoute initialCategory=$initialCategory)",
                    )
                    navController.navigate(AfternoteRoute.EditRoute(initialCategory = initialCategory))
                },
                onNavTabSelected = onNavTabSelected,
            ),
        onItemsChanged = onItemsChanged,
    )
}
