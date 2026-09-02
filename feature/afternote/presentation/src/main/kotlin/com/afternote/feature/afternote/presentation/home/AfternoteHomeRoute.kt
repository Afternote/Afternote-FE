package com.afternote.feature.afternote.presentation.home

import androidx.compose.runtime.Composable
import com.afternote.feature.afternote.domain.AfternoteType

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
