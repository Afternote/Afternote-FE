package com.afternote.feature.mindrecord.presentation.navigation

import kotlinx.serialization.Serializable

sealed interface MindRecordRoute {
    @Serializable
    data object DailyQuestionWriteRoute : MindRecordRoute

    @Serializable
    data object DiaryWriteRoute : MindRecordRoute

    @Serializable
    data object DeepThoughtWriteRoute : MindRecordRoute

    /** 작성 화면 키보드 툴바의 "임시저장 N" 영역에서 진입하는 임시저장 목록 화면. */
    @Serializable
    data object DraftListRoute : MindRecordRoute
}
