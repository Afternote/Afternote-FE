package com.afternote.feature.afternote.presentation.shared.shell

import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun BottomNavigationBar(
    selectedItem: BottomNavItem,
    onItemSelected: (BottomNavItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(modifier = modifier) {
        BottomNavItem.entries.forEach { item ->
            NavigationBarItem(
                selected = item == selectedItem,
                onClick = { onItemSelected(item) },
                icon = { },
                label = {
                    Text(
                        text =
                            when (item) {
                                BottomNavItem.MIND_RECORD -> "기록"
                                BottomNavItem.TIME_LETTER -> "타임레터"
                                BottomNavItem.AFTERNOTE -> "애프터노트"
                            },
                    )
                },
            )
        }
    }
}
