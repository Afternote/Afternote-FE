package com.afternote.feature.afternote.presentation.author.navigation

import androidx.navigation.NavController
import com.afternote.core.ui.bottombar.BottomNavTab

/**
 * [afternoteNavGraph]에 전달되는 파라미터 묶음.
 *
 * Parameter Object Pattern으로 의존성/콜백을 하나의 객체에 묶어 시그니처를 단순화합니다.
 * [afternoteNavGraph]는 `NavGraphBuilder` extension(= non-@Composable)이므로
 * 데이터 클래스에 람다를 포함해도 리컴포지션 동등성 비교 이슈는 없습니다.
 *
 * 작성자 목록 갱신은 [com.afternote.feature.afternote.domain.repository.AfternoteRepository.authorAfternoteListRevision]을
 * 홈 ViewModel이 구독하여 처리합니다.
 */
data class AfternoteNavGraphParams(
    val navController: NavController,
    val userName: String = "",
    val onNavTabSelected: (BottomNavTab) -> Unit = {},
)
