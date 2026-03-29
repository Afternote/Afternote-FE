package com.afternote.core.ui.navigation.navigator

import androidx.navigation.NavController
import com.afternote.core.ui.navigation.model.DailyRecordNavigator
import com.afternote.feature.dailyrecord.presentation.navigiation.RecordRoute

class DailyRecordNavigatorImpl(
    private val navController: NavController,
) : DailyRecordNavigator {
    override fun gotoDailyRecord(route: RecordRoute) {
        navController.navigate(route)
    }
}
