package com.afternote.feature.afternote.presentation.author.navigation

import androidx.compose.runtime.Composable
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.presentation.author.home.AfternoteHomeEntry

@Composable
internal fun AfternoteHomeNavigation(
    onNavigateToDetail: (itemId: Long) -> Unit,
    onNavigateToNewEditor: (initialType: AfternoteType) -> Unit,
    onNavigateToSetting: () -> Unit,
) {
    AfternoteHomeEntry(
        navigateToDetail = onNavigateToDetail,
        navigateToAdd = onNavigateToNewEditor,
        onSettingClick = onNavigateToSetting,
    )
}
