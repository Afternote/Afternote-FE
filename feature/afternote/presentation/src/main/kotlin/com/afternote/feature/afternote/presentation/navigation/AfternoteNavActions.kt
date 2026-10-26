package com.afternote.feature.afternote.presentation.navigation

import com.afternote.core.ui.bottombar.BottomNavTab
import com.afternote.feature.afternote.domain.AfternoteType

/**
 * 애프터노트 작성자 로컬 스택 안의 네비게이션 명령 모음.
 *
 * 구현([AfternoteLocalNavActions])은 스택을 가진 [AfternoteNavHost] 안에 있고, 스택 밖으로 나가는
 * 이동은 [AfternoteExternalActions] 로 앱 셸에 넘긴다 (#1698).
 *
 * 작명 컨벤션 (#239): `navigateTo<Where>` / `popBack` / `popTo<Where>` /
 * `replace<X>With<Y>` / `proceedTo<Next>` / `on<Result>Succeeded|Failed`.
 *
 * Screen 콜백 인자(예: `onNavigateToDetail`)는 *도메인 이벤트* 자리로 본 인터페이스와 분리.
 * [AfternoteNavHost] 의 entry 가 둘을 매핑한다.
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

    /** 지문 인증 성공 → Afternote 홈으로 진입하며 지문 로그인 화면 자체를 stack 에서 제거 (replace). */
    fun replaceFingerprintLoginWithAfternoteHome()

    /**
     * 지문 인증 실패 통지 — *navigate 아닌 결과 알림* 이라 NavActions 자리에 두는 게 어색하지만,
     * [AfternoteNavHost] 의 지문 entry 가 화면의 `onShowError` 를 이 메서드로 잇는다. 구현은
     * [AfternoteExternalActions.onFingerprintAuthFailed] 로 앱 셸에 넘기고, 셸이 스낵바로 띄운다.
     * entry 에서 externalActions 로 바로 넘기면 본 인터페이스에서 뺄 수 있다 (별 작업).
     */
    fun onFingerprintAuthFailed(message: String)

    /** Editor 저장 성공 → Afternote 홈 위 화면(에디터·미디어 등)만 pop. Home 자체는 유지. */
    fun popToAfternoteHome()

    /** Afternote 홈 TopBar 설정 기어 → 설정 화면(Route.Setting) 진입. */
    fun navigateToSetting()
}
