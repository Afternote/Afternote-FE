package com.afternote.feature.timeletter.presentation.screen.sender

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.AfternoteFab
import com.afternote.core.ui.BottomBar
import com.afternote.core.ui.BottomNavItem
import com.afternote.core.ui.R
import com.afternote.core.ui.TopBar
import com.afternote.feature.timeletter.domain.TimeLetters
import com.afternote.feature.timeletter.presentation.component.EmptyTimeLetterContent

@Composable
fun TimeletterScreen(
    letters: TimeLetters,
    bottomNavItems: List<BottomNavItem>,
    selectedNavItem: BottomNavItem,
    onNavItemClick: (BottomNavItem) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TopBar() },
        bottomBar = {
            BottomBar(
                items = bottomNavItems,
                isSelected = { it == selectedNavItem },
                onNavItemClick,
            )
        },
        floatingActionButton = { AfternoteFab(onClick = onAddClick) },
    ) { paddingValues ->
        TimeletterContent(
            letters = letters,
            modifier = Modifier.padding(paddingValues),
        )
    }
}

@Composable
private fun TimeletterContent(
    letters: TimeLetters,
    modifier: Modifier = Modifier,
) {
    if (letters.isEmpty()) {
        EmptyTimeLetterContent(modifier = modifier)
        return
    }
}

@Preview
@Composable
private fun TimeletterScreenEmptyPreview() {
    val items = listOf(
        BottomNavItem("홈", R.drawable.core_ui_ic_home, "home"),
        BottomNavItem("기록", R.drawable.core_ui_ic_mindrecord, "mindrecord"),
        BottomNavItem("타임레터", R.drawable.core_ui_ic_mail, "timeletter"),
        BottomNavItem("노트", R.drawable.core_ui_ic_note, "afternote"),
    )
    TimeletterScreen(
        letters = TimeLetters(emptyList()),
        bottomNavItems = items,
        selectedNavItem = items[2],
        onNavItemClick = {},
        onAddClick = {},
    )
}
