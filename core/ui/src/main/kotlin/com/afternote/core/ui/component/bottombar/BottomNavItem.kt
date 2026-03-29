package com.afternote.core.ui.component.bottombar

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.afternote.core.ui.Route

data class BottomNavItem(
    @param:StringRes val label: Int,
    @param:DrawableRes val iconRes: Int,
    val route: Route,
)
