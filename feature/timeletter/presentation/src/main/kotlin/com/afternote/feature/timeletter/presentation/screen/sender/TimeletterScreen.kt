package com.afternote.feature.timeletter.presentation.screen.sender

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.AfternoteFab
import com.afternote.core.ui.BottomBar
import com.afternote.core.ui.BottomNavItem
import com.afternote.core.ui.R
import com.afternote.core.ui.TopBar
import com.afternote.feature.timeletter.domain.LetterIdentity
import com.afternote.feature.timeletter.domain.LetterSchedule
import com.afternote.feature.timeletter.domain.OpenDate
import com.afternote.feature.timeletter.domain.TimeLetter
import com.afternote.feature.timeletter.domain.TimeLetters
import com.afternote.feature.timeletter.presentation.component.TimeLetterContent
import com.afternote.feature.timeletter.presentation.component.ViewMode

@Composable
fun TimeletterScreen(
    letters: TimeLetters,
    bottomNavItems: List<BottomNavItem>,
    selectedNavItem: BottomNavItem,
    onNavItemClick: (BottomNavItem) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var viewMode by remember { mutableStateOf(ViewMode.List) }
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
        TimeLetterContent(
            letters = letters,
            viewMode = viewMode,
            onViewModeChange = { viewMode = it },
            modifier = Modifier.padding(paddingValues),
        )
    }
}

private val previewItems =
    listOf(
        BottomNavItem("홈", R.drawable.core_ui_ic_home, "home"),
        BottomNavItem("기록", R.drawable.core_ui_ic_mindrecord, "mindrecord"),
        BottomNavItem("타임레터", R.drawable.core_ui_ic_mail, "timeletter"),
        BottomNavItem("노트", R.drawable.core_ui_ic_note, "afternote"),
    )

private val previewLetters =
    TimeLetters(
        listOf(
            TimeLetter(
                identity =
                    LetterIdentity(
                        id = 1L,
                        title = "미래의 나에게",
                        body = "지금 이 순간을 잊지 마. 열심히 살고 있는 너를 응원해.",
                    ),
                schedule = LetterSchedule(recipientName = "박경민", openDate = OpenDate("2026-12-31")),
            ),
            TimeLetter(
                identity = LetterIdentity(id = 2L, title = "10년 후의 나에게", body = "지금보다 더 행복하길 바라."),
                schedule =
                    LetterSchedule(
                        recipientName = "미래의 나",
                        openDate = OpenDate("2035-01-01"),
                    ),
            ),
        ),
    )

@Preview(showBackground = true)
@Composable
private fun TimeletterScreenEmptyPreview() {
    TimeletterScreen(
        letters = TimeLetters(emptyList()),
        bottomNavItems = previewItems,
        selectedNavItem = previewItems[2],
        onNavItemClick = {},
        onAddClick = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun TimeletterScreenListPreview() {
    TimeletterScreen(
        letters = previewLetters,
        bottomNavItems = previewItems,
        selectedNavItem = previewItems[2],
        onNavItemClick = {},
        onAddClick = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun TimeletterScreenBlockPreview() {
    TimeLetterContent(
        letters = previewLetters,
        viewMode = ViewMode.Block,
        onViewModeChange = {},
    )
}
