package com.afternote.feature.mindrecord.presentation.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * 마인드레코드 허브 내부 스택의 키 집합 (#924 Nav3 파일럿).
 *
 * 루트 NavHost(Nav2)에는 [com.afternote.core.ui.Route.MindRecord] 하나만 등록되고,
 * 여기 키들은 그 안의 로컬 `NavDisplay` 백스택에서만 산다. [NavKey] + [Serializable] 조합이라
 * `rememberNavBackStack` 이 프로세스 재생성을 넘겨 스택을 복원한다.
 */
sealed interface MindRecordRoute : NavKey {
    /** 허브(마음의 기록 탭 홈) — 로컬 스택의 항상 첫 키. */
    @Serializable
    data object HubRoute : MindRecordRoute

    @Serializable
    data object DailyQuestionWriteRoute : MindRecordRoute

    /**
     * 일기 작성 화면. [draftId] 가 있으면 임시저장 이어쓰기 모드 — 해당 달([draftYearMonth],
     * `yyyy-MM`)의 draft 목록에서 항목을 찾아 프리필하고, 저장 시 PATCH 로 수정한다.
     */
    @Serializable
    data class DiaryWriteRoute(
        val draftId: Long? = null,
        val draftYearMonth: String? = null,
    ) : MindRecordRoute

    /** 작성 화면 키보드 툴바의 "임시저장 N" 영역에서 진입하는 임시저장 목록 화면. */
    @Serializable
    data object DraftListRoute : MindRecordRoute
}
