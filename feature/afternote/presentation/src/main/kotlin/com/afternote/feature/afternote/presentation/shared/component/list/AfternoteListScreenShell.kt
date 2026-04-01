package com.afternote.feature.afternote.presentation.shared.component.list

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.scaffold.TopBar
import com.afternote.core.ui.scaffold.bottombar.BottomBar
import com.afternote.core.ui.scaffold.bottombar.BottomNavTab
import com.afternote.feature.afternote.presentation.shared.shell.ScaffoldContentWithOptionalFab

/**
 * Shared shell for 애프터노트 list screens (writer main and receiver list).
 * Provides Scaffold with TopBar, BottomNavigationBar, optional FAB, and content slot.
 */
@Composable
fun AfternoteListScreenShell(
    modifier: Modifier = Modifier,
    onNavTabSelected: (BottomNavTab) -> Unit,
    selectedNavTab: BottomNavTab = BottomNavTab.NOTE,
    showFab: Boolean = false,
    onFabClick: () -> Unit = {},
    content: @Composable (Modifier) -> Unit,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopBar()
        },
        bottomBar = {
            BottomBar(
                selectedNavTab = selectedNavTab,
                onTabClick = onNavTabSelected,
            )
        },
    ) { paddingValues ->
        ScaffoldContentWithOptionalFab(
            paddingValues = paddingValues,
            showFab = showFab,
            onFabClick = onFabClick,
            content = content,
        )
    }
}

@Preview
@Composable
private fun AfternoteListScreenShellPreview() {
    AfternoteListScreenShell(
        onNavTabSelected = {},
        content = { },
    )
}
