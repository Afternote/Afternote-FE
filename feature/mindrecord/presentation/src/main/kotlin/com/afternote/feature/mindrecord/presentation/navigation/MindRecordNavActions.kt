package com.afternote.feature.mindrecord.presentation.navigation

/**
 * NavHost 루트에서 마인드레코드 서브그래프로 넘기는 네비게이션 명령 모음.
 *
 * [com.afternote.feature.onboarding.presentation.navigation.OnboardingNavActions]·
 * [com.afternote.feature.afternote.presentation.navigation.AfternoteNavActions]와 동일한
 * “actions 객체 단일 전달” 패턴이다.
 */
interface MindRecordNavActions {
    /**
     * 한 칸 뒤로 — 화면별로 나뉘어 있던 단순 `popBackStack()` 5개를 하나로 합쳤다 (#1311).
     *
     * 종전에는 `onMemorySpaceBack`·`onReceiverMindRecordBack`·`onWriteBack`·
     * `onWriteSubmitSuccess`·`onDraftListBack` 이 **본문이 전부 같은 한 줄**이었다. 그때의
     * 근거는 「빌려 쓰면 한 화면만 다르게 바꾸려는 변경이 나머지까지 조용히 끌고 간다」였는데,
     * **그 걱정은 이 계약이 아니라 NavGraph 가 막는다.**
     *
     * 화면의 이벤트 이름(`onBackClick`·`onSubmitSuccess`)은 그대로 남고, 그것을 어느 명령에
     * 붙일지는 [mindRecordNavGraph] 가 화면마다 따로 정한다. 한 화면이 다른 뒤로가기를
     * 갖게 되면 그 한 줄의 매핑만 새 명령으로 바꾸면 되고, 나머지는 이 명령에 그대로 남는다 —
     * 분기점이 사라진 것이 아니라 **명령 쪽에서 매핑 쪽으로 옮겨졌다.**
     */
    fun popBack()

    fun onWriteDailyQuestion()

    fun onWriteDiary()

    fun onNavigateToDraftList()

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
