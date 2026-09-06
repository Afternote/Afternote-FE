package com.afternote.feature.afternote.presentation.navigation

import com.afternote.core.ui.bottombar.BottomNavTab
import com.afternote.feature.afternote.domain.AfternoteType

/**
 * NavHost 루트에서 Afternote 서브그래프로 넘기는 네비게이션 명령 모음.
 *
 * 작명 컨벤션 (#239): `navigateTo<Where>` / `popBack` / `popTo<Where>` /
 * `replace<X>With<Y>` / `proceedTo<Next>` / `on<Result>Succeeded|Failed`.
 *
 * Screen 콜백 인자(예: `onSongClick`)는 *도메인 이벤트* 자리로 본 인터페이스와 분리.
 * NavGraph 가 둘을 매핑한다.
 */
interface AfternoteNavActions {
    fun navigateToBottomTab(tab: BottomNavTab)

    fun popBack()

    fun navigateToAfternoteDetail(itemId: Long)

    fun navigateToNewEditor(initialType: AfternoteType)

    fun navigateToEditorForEdit(
        itemId: Long,
        initialType: AfternoteType,
    )

    fun navigateToMemorialPlaylist()

    /** 에디터 수신자 `+` → 수신자 선택 화면(#540). 에디터 위에 push 된다. */
    fun navigateToSelectReceiver()

    /**
     * 수신자 선택 완료 → 확정한 [receiverIds] 전체를 이전 엔트리(에디터)의 SavedStateHandle 에
     * `SELECTED_RECEIVER_IDS_KEY`([LongArray])로 쓰고 pop. 에디터가 복귀 시 읽어 폼에 반영한다 (#1426).
     */
    fun popBackWithSelectedReceivers(receiverIds: List<Long>)

    fun navigateToAddSong()

    /** 지문 인증 성공 → Afternote 홈으로 진입하며 지문 로그인 화면 자체를 stack 에서 제거 (replace). */
    fun replaceFingerprintLoginWithAfternoteHome()

    /**
     * 지문 인증 실패 통지 — *navigate 아닌 결과 알림* 이라 NavActions 자리에 두는 게 어색하지만,
     * 현재 호출 측이 NavActions 를 통해 Toast 표시를 위임받는 구조라 본 인터페이스에 남겨둠.
     * 향후 별도 ScreenCallback 으로 분리 (별 작업).
     */
    fun onFingerprintAuthFailed(message: String)

    /** Editor 저장 성공 → Afternote 홈 위 화면(에디터·미디어 등)만 pop. Home 자체는 유지. */
    fun popToAfternoteHome()

    /** Afternote 홈 TopBar 설정 기어 → 설정 화면(Route.Setting) 진입. */
    fun navigateToSetting()
}
