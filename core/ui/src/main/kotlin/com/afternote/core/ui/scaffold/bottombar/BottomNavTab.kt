package com.afternote.core.ui.scaffold.bottombar

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.afternote.core.ui.R
import com.afternote.core.ui.Route

enum class BottomNavTab(
    @param:StringRes val label: Int,
    @param:DrawableRes val iconRes: Int,
    val route: Route,
) {
    HOME(
        R.string.core_ui_nav_item_home,
        R.drawable.core_ui_ic_home,
        Route.Home,
    ),
    RECORD(
        R.string.core_ui_nav_item_mindrecord,
        R.drawable.core_ui_ic_mindrecord,
        Route.MindRecord,
    ),
    TIMELETTER(
        R.string.core_ui_nav_item_timeletter,
        R.drawable.core_ui_ic_mail,
        Route.TimeLetter,
    ),
    NOTE(
        R.string.core_ui_nav_item_note,
        R.drawable.core_ui_ic_note,
        Route.Afternote,
    ),

//    companion object {
//        fun find(route: Route) = entries.find { it.route == route }
//    }
}
