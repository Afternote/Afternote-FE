package com.afternote.feature.afternote.presentation.shared.component.list

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.afternote.core.ui.scaffold.bottombar.BottomNavTab
import com.afternote.feature.afternote.presentation.shared.compositionlocal.AfternoteEmbeddedMainBottomBar
import com.afternote.feature.afternote.presentation.shared.shell.ScaffoldContentWithOptionalFab
import com.afternote.feature.afternote.presentation.shared.shell.TopBar

/**
 * Shared shell for 애프터노트 list screens (writer main and receiver list).
 * Provides Scaffold with TopBar, BottomNavigationBar, optional FAB, and content slot.
 */
@Composable
fun AfternoteListScreenShell(
    onNavTabSelected: (BottomNavTab) -> Unit,
    modifier: Modifier = Modifier,
    title: String = "애프터노트",
    selectedNavTab: BottomNavTab = BottomNavTab.NOTE,
    showFab: Boolean = false,
    onFabClick: () -> Unit = {},
    content: @Composable (Modifier) -> Unit,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopBar(title = title)
        },
        bottomBar = {
            AfternoteEmbeddedMainBottomBar(
                onTabClick = onNavTabSelected,
                selectedNavTab = selectedNavTab,
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
