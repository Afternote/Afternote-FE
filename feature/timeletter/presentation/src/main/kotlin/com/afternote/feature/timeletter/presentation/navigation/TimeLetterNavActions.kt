package com.afternote.feature.timeletter.presentation.navigation

interface TimeLetterNavActions {
    /**
     * 한 칸 뒤로. 화면별로 나뉘어 있던 단순 `popBackStack()` 5개를 하나로 합쳤다 (#1301).
     *
     * 화면의 이벤트 이름(`onBackClick`·`onConfirmClick`·등록 완료)은 그대로 두고, 어느 명령에
     * 붙일지는 [timeLetterNavGraph] 가 화면마다 정한다. 한 화면이 다른 뒤로가기를 갖게 되면
     * 그 매핑 한 줄만 새 명령으로 바꾼다.
     */
    fun popBack()

    fun onSettingClick()

    fun onNavigateToWrite()

    fun onNavigateToEdit(timeLetterId: Long)

    fun onNavigateToDraft()

    fun onNavigateToRecipient()

    fun onNavigateToDetail(timeLetterId: Long)

    fun onNavigateToRecipientFilter()
}
