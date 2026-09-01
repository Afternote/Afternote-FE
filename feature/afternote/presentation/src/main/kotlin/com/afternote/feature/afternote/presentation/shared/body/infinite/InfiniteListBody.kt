package com.afternote.feature.afternote.presentation.shared.body.infinite

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.presentation.author.home.HomeHeaderSection
import com.afternote.feature.afternote.presentation.author.home.NextStep
import com.afternote.feature.afternote.presentation.shared.body.infinite.content.AfternoteListContent
import com.afternote.feature.afternote.presentation.shared.body.infinite.content.list.item.ListItemUiModel

@Composable
fun InfiniteListBody(
    items: LazyPagingItems<ListItemUiModel>,
    selectedType: AfternoteType?,
    onTypeSelected: (AfternoteType?) -> Unit,
    onListItemClick: (id: Long, type: AfternoteType) -> Unit,
    headerDescription: String,
    nextStep: NextStep?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        HomeHeaderSection(
            description = headerDescription,
            nextStep = nextStep,
        )
        AfternoteListContent(
            items = items,
            selectedType = selectedType,
            onTypeSelected = onTypeSelected,
            onListItemClick = onListItemClick,
        )
    }
}
