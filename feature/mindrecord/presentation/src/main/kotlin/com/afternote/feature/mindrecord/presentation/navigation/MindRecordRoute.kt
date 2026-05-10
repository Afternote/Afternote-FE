package com.afternote.feature.mindrecord.presentation.navigation

import kotlinx.serialization.Serializable

sealed interface MindRecordRoute {
    @Serializable
    data object DailyQuestionWriteRoute : MindRecordRoute

    @Serializable
    data object DiaryWriteRoute : MindRecordRoute

    @Serializable
    data object DeepThoughtWriteRoute : MindRecordRoute
}
