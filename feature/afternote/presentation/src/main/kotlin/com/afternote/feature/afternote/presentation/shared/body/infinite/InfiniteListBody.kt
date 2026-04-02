package com.afternote.feature.afternote.presentation.shared.body.infinite

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.afternote.feature.afternote.presentation.shared.AfternoteCategory
import com.afternote.feature.afternote.presentation.shared.body.infinite.content.AfternoteListContent

@Composable
fun InfiniteListBody(
    uiState: AfternoteBodyUiState,
    onCategorySelected: (AfternoteCategory) -> Unit,
    onListItemClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    onLoadMore: () -> Unit = {},
) {
    Column(
        modifier = modifier,
    ) {
        AfternoteHeader()
        AfternoteListContent(
            uiState = uiState,
            onCategorySelected = onCategorySelected,
            onListItemClick = onListItemClick,
            onLoadMore = onLoadMore,
        )
    }
}
