package com.afternote.feature.mindrecord.presentation.navigation

/**
 * NavHost 루트에서 마인드레코드 서브그래프로 넘기는 네비게이션 명령 모음.
 *
 * [com.afternote.feature.onboarding.presentation.navigation.OnboardingNavActions]·
 * [com.afternote.feature.afternote.presentation.author.navigation.AfternoteNavActions]와 동일한
 * “actions 객체 단일 전달” 패턴이다.
 */
interface MindRecordNavActions {
    fun onMemorySpaceBack()

    fun onWriteDailyQuestion()

    fun onWriteDiary()

    fun onWriteDeepThought()

    fun onWriteBack()

    fun onWriteSubmitSuccess()

    fun onNavigateToDraftList()

    fun onDraftListBack()

    /** 임시저장 목록에서 일기 draft 를 탭 — 일기 작성 화면을 이어쓰기 모드로 연다. */
    fun onEditDiaryDraft(
        draftId: Long,
        draftYearMonth: String,
    )
}
