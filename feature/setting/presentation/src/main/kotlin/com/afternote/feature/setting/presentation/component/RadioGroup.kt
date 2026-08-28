package com.afternote.feature.setting.presentation.component

import androidx.compose.foundation.layout.Arrangement.Absolute.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun RadioGroup(
    items: List<RadioGroupItem>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = spacedBy(12.dp),
    ) {
        items.forEachIndexed { index, item ->
            RadioGroupCard(
                item = item,
                selected = index == selectedIndex,
                onClick = { onSelectIndex(index) },
            )
        }
    }
}
