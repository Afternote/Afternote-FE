package com.afternote.core.ui.navigation.model

import com.afternote.feature.dailyrecord.presentation.navigiation.RecordRoute

interface DailyRecordNavigator {
    fun gotoDailyRecord(route: RecordRoute)
}
