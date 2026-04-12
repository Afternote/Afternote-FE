package com.afternote.feature.afternote.presentation.author.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.afternote.core.ui.bottombar.BottomNavTab
import com.afternote.feature.afternote.presentation.author.home.AfternoteHomeEntry
import com.afternote.feature.afternote.presentation.author.home.AfternoteHomeEntryActions
import com.afternote.feature.afternote.presentation.author.navigation.model.AfternoteRoute
import com.afternote.feature.afternote.presentation.shared.AfternoteCategory

@Composable
internal fun AfternoteHomeNavigation(
    navController: NavController,
    onNavTabSelected: (BottomNavTab) -> Unit = {},
) {
    AfternoteHomeEntry(
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
                    navController.navigate(AfternoteRoute.EditorRoute(initialCategory = initialCategory))
                },
                onNavTabSelected = onNavTabSelected,
            ),
    )
}
