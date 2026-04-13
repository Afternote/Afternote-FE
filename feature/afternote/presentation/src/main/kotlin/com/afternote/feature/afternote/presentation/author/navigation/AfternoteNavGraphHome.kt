package com.afternote.feature.afternote.presentation.author.navigation

import androidx.compose.runtime.Composable
import com.afternote.core.ui.bottombar.BottomNavTab
import com.afternote.feature.afternote.presentation.author.home.AfternoteHomeEntry
import com.afternote.feature.afternote.presentation.author.home.AfternoteHomeEntryActions
import com.afternote.feature.afternote.presentation.shared.AfternoteCategory

@Composable
internal fun AfternoteHomeNavigation(
    onNavigateToDetail: (itemId: String) -> Unit,
    onNavigateToGalleryDetail: (itemId: String) -> Unit,
    onNavigateToMemorialGuidelineDetail: (itemId: String) -> Unit,
    onNavigateToNewEditor: (initialCategory: String?) -> Unit,
    onNavTabSelected: (BottomNavTab) -> Unit = {},
) {
    AfternoteHomeEntry(
        actions =
            AfternoteHomeEntryActions(
                navigateToDetail = onNavigateToDetail,
                navigateToGalleryDetail = onNavigateToGalleryDetail,
                navigateToMemorialGuidelineDetail = onNavigateToMemorialGuidelineDetail,
                navigateToAdd = { selectedTab ->
                    val initialCategory =
                        if (selectedTab == AfternoteCategory.ALL) null else selectedTab.navKey
                    onNavigateToNewEditor(initialCategory)
                },
                onNavTabSelected = onNavTabSelected,
            ),
    )
}
