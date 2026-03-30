package com.afternote.feature.afternote.presentation.shared.compositionlocal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import com.afternote.core.ui.scaffold.bottombar.BottomBar
import com.afternote.core.ui.scaffold.bottombar.BottomNavTab

/**
 * When true, the app-level main tab [BottomBar] is shown by the root scaffold; feature screens must
 * not render a duplicate main tab bar.
 */
val LocalAfternoteUsesAppBottomBar = staticCompositionLocalOf { false }

@Composable
fun AfternoteEmbeddedMainBottomBar(
    selectedNavTab: BottomNavTab,
    onTabClick: (BottomNavTab) -> Unit,
) {
    if (!LocalAfternoteUsesAppBottomBar.current) {
        BottomBar(
            selectedNavTab = selectedNavTab,
            onTabClick = onTabClick,
        )
    }
}
