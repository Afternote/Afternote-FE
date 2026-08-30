package com.afternote.feature.afternote.presentation.author.navigation

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
     * 수신자 선택 화면의 등록 진입(0건 CTA · 목록 하단 행) → 설정의 수신자 등록 화면(#1427).
     *
     * 선택 화면 **위로** push 한다 — 등록을 마치거나 취소하면 pop 되어 선택 화면으로 돌아오고,
     * 그 아래 에디터도 백스택에 남아 작성 중 내용이 유지된다. 등록 화면 자체는 setting 소유라
     * 목적지 라우트만 참조하고 화면은 건드리지 않는다.
     */
    fun navigateToReceiverRegister()

    /**
     * 수신자 선택 완료 → 선택한 [receiverId] 를 이전 엔트리(에디터)의 SavedStateHandle 에
     * `SELECTED_RECEIVER_ID_KEY`([Long])로 쓰고 pop. 에디터가 복귀 시 읽어 폼에 반영한다.
     */
    fun popBackWithSelectedReceiver(receiverId: Long)

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
