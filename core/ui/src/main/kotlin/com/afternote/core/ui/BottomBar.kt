package com.afternote.core.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.Black

data class BottomNavItem(
    @param:StringRes val label: Int,
    @param:DrawableRes val iconRes: Int,
    val route: Route,
)

@Composable
fun BottomBar(
    items: List<BottomNavItem>,
    isSelected: (BottomNavItem) -> Boolean,
    onItemClick: (BottomNavItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(modifier = modifier) {
        items.forEach { item ->
            val selected = isSelected(item)
            NavigationBarItem(
                selected = selected,
                onClick = { onItemClick(item) },
                icon = {
                    Icon(
                        painter = painterResource(item.iconRes),
                        contentDescription = item.label,
                    )
                },
                label = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(item.label)
                        Spacer(modifier = Modifier.height(4.dp))
                        if (selected) {
                            Box(
                                modifier =
                                    Modifier
                                        .size(4.dp)
                                        .background(color = Black, shape = CircleShape),
                            )
                        }
                    }
                },
                colors =
                    NavigationBarItemDefaults.colors(
                        selectedIconColor = Black,
                        selectedTextColor = Black,
                        unselectedIconColor = Black.copy(alpha = 0.25f),
                        unselectedTextColor = Black.copy(alpha = 0.25f),
                        indicatorColor = Color.Transparent,
                    ),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BottomBarPreview() {
    val items =
        listOf(
            BottomNavItem("홈", R.drawable.core_ui_ic_home, Route.Home),
            BottomNavItem("기록", R.drawable.core_ui_ic_mindrecord, Route.MindRecord),
            BottomNavItem("타임레터", R.drawable.core_ui_ic_mail, Route.TimeLetter),
            BottomNavItem("노트", R.drawable.core_ui_ic_note, Route.Afternote),
        )
    MaterialTheme {
        BottomBar(
            items = items,
            isSelected = { it.route == Route.TimeLetter },
            onItemClick = {},
        )
    }
}
