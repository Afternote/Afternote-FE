package com.afternote.feature.afternote.presentation.shared.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.paging.compose.LazyPagingItems
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.presentation.home.HomeBodySectionSpacing
import com.afternote.feature.afternote.presentation.home.HomeBodyTopSpacing
import com.afternote.feature.afternote.presentation.home.HomeHeaderSection
import com.afternote.feature.afternote.presentation.home.NextStep
import com.afternote.feature.afternote.presentation.shared.component.AfternoteListContent

@Composable
fun InfiniteListBody(
    items: LazyPagingItems<ListItemUiModel>,
    selectedType: AfternoteType?,
    onTypeSelected: (AfternoteType?) -> Unit,
    onListItemClick: (id: Long, type: AfternoteType) -> Unit,
    headerDescription: String,
    nextStep: NextStep?,
    modifier: Modifier = Modifier,
    onDraftListClick: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(HomeBodySectionSpacing),
    ) {
        Spacer(Modifier.height(HomeBodyTopSpacing))
        HomeHeaderSection(
            description = headerDescription,
            nextStep = nextStep,
            onDraftListClick = onDraftListClick,
        )
        AfternoteListContent(
            items = items,
            selectedType = selectedType,
            onTypeSelected = onTypeSelected,
            onListItemClick = onListItemClick,
        )
    }
}
