package com.afternote.core.ui.scaffold.bottombar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme

// TODO:검토

@Composable
fun BottomBar(
    selectedNavTab: BottomNavTab,
    onTabClick: (BottomNavTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(
        modifier = modifier,
        containerColor = AfternoteDesign.colors.white,
        tonalElevation = 0.dp,
    ) {
        BottomNavTab.entries.forEach { tab ->
            val selected = selectedNavTab == tab
            NavigationBarItem(
                selected = selected,
                onClick = { onTabClick(tab) },
                icon = {
                    Icon(
                        painter = painterResource(tab.iconRes),
                        contentDescription = null,
                    )
                },
                label = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(stringResource(tab.label))
                        Spacer(modifier = Modifier.height(4.dp))
                        if (selected) {
                            Box(
                                modifier =
                                    Modifier
                                        .size(4.dp)
                                        .background(color = AfternoteDesign.colors.black, shape = CircleShape),
                            )
                        }
                    }
                },
                colors =
                    NavigationBarItemDefaults.colors(
                        selectedIconColor = AfternoteDesign.colors.black,
                        selectedTextColor = AfternoteDesign.colors.black,
                        unselectedIconColor = AfternoteDesign.colors.black.copy(alpha = 0.25f),
                        unselectedTextColor = AfternoteDesign.colors.black.copy(alpha = 0.25f),
                        indicatorColor = Color.Transparent,
                    ),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun 테마안씌운거() {
    BottomBar(
        onTabClick = {},
        selectedNavTab = BottomNavTab.TIMELETTER,
    )
}

@Preview(showBackground = true)
@Composable
private fun 테마씌운거() {
    AfternoteTheme {
        BottomBar(
            onTabClick = {},
            selectedNavTab = BottomNavTab.TIMELETTER,
        )
    }
}
