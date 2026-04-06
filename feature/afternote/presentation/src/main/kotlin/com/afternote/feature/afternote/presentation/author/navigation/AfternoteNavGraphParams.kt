package com.afternote.feature.afternote.presentation.author.navigation

import androidx.navigation.NavController
import com.afternote.core.ui.scaffold.bottombar.BottomNavTab

/**
 * [afternoteNavGraph]에 전달되는 파라미터 묶음.
 *
 * Parameter Object Pattern으로 의존성/콜백을 하나의 객체에 묶어 시그니처를 단순화합니다.
 * [afternoteNavGraph]는 `NavGraphBuilder` extension(= non-@Composable)이므로
 * 데이터 클래스에 람다를 포함해도 리컴포지션 동등성 비교 이슈는 없습니다.
 *
 * 홈 새로고침 플래그와 애프터노트 삭제/저장 이벤트는 그래프 내부(`AfternoteHostViewModel`)에
 * 캡슐화되어 있으므로 외부에서 주입할 필요가 없습니다.
 */
data class AfternoteNavGraphParams(
    val navController: NavController,
    val userName: String = "",
    val onNavTabSelected: (BottomNavTab) -> Unit = {},
)
