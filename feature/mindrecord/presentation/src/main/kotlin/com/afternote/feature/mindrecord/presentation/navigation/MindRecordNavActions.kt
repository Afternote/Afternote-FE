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

    /**
     * 수신자 기록 화면에서 나간다.
     *
     * 지금 동작은 추억 공간과 같은 `popBackStack()` 이지만 이름을 빌리지 않는다 — 이
     * 인터페이스는 화면별로 액션을 나눠 두는 곳이고, 빌려 쓰면 «추억 공간만 다르게
     * 처리하자» 는 변경이 이 화면까지 조용히 끌고 간다.
     */
    fun onReceiverMindRecordBack()

    fun onWriteDailyQuestion()

    fun onWriteDiary()

    fun onWriteBack()

    fun onWriteSubmitSuccess()

    fun onNavigateToDraftList()

    fun onDraftListBack()

    /** 목록 항목 탭 — 저장된 기록 본문을 여는 상세 화면 (#759). */
    fun onOpenRecordDetail(
        recordId: Long,
        isDiary: Boolean,
        yearMonth: String?,
    )

    /** 목록의 "수정하기" — 정식 데일리질문 답변을 프리필한 작성 화면으로 연다 (#582). */
    fun onEditDailyQuestion(answerId: Long)

    /** 목록의 "수정하기" — 정식 일기를 프리필한 작성 화면으로 연다 (#582). */
    fun onEditDiary(
        diaryId: Long,
        yearMonth: String,
    )

    /** 임시저장 목록에서 데일리질문 draft 를 탭 — 작성 화면을 이어쓰기 모드로 연다 (#770). */
    fun onEditDailyQuestionDraft(draftId: Long)

    /** 임시저장 목록에서 일기 draft 를 탭 — 일기 작성 화면을 이어쓰기 모드로 연다. */
    fun onEditDiaryDraft(
        draftId: Long,
        draftYearMonth: String,
    )
}
