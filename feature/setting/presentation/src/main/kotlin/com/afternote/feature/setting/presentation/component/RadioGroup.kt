package com.afternote.feature.setting.presentation.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.button.AfternoteRadioGroup

@Composable
fun RadioGroup(
    items: List<RadioGroupItem>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val indexedItems = items.withIndex().toList()

    AfternoteRadioGroup(
        options = indexedItems,
        selectedValue = indexedItems.getOrNull(selectedIndex),
        onSelect = { onSelectIndex(it.index) },
        modifier = modifier,
        itemContentPadding = PaddingValues(16.dp),
        itemDecoration = { _, selected -> radioGroupCardDecoration(selected) },
    ) { indexedItem, selected ->
        RadioGroupCardContent(
            item = indexedItem.value,
            selected = selected,
        )
    }
}
