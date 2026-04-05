package com.afternote.feature.afternote.presentation.author.navigation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.afternote.core.ui.scaffold.bottombar.BottomNavTab
import com.afternote.feature.afternote.domain.model.ListItem
import com.afternote.feature.afternote.presentation.author.home.AfternoteHomeEntry
import com.afternote.feature.afternote.presentation.author.home.AfternoteHomeEntryActions
import com.afternote.feature.afternote.presentation.author.navigation.model.AfternoteRoute
import com.afternote.feature.afternote.presentation.shared.AfternoteCategory

@Composable
internal fun AfternoteHomeNavigation(
    navController: NavController,
    onNavTabSelected: (BottomNavTab) -> Unit = {},
    onVisibleItemsUpdated: (List<ListItem>) -> Unit,
    homeRefreshRequested: Boolean = false,
    // 새로고침 플래그와 짝을 이루는 필수 콜백. 누락 시 플래그가 소비되지 않아 상태가 어긋나므로 디폴트 없이 강제.
    onHomeRefreshConsumed: () -> Unit,
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
