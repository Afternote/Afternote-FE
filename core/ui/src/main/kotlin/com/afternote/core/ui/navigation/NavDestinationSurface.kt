package com.afternote.core.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey
import com.afternote.core.ui.theme.AfternoteDesign

/**
 * 네비게이션 목적지 하나를 불투명하게 칠하는 바탕 (#2145).
 *
 * predictive back 진행 중 앞 화면은 [PredictiveBackPopExit] 로 줄어들어 뒤 화면 위에 뜬다(#1869).
 * 그런데 화면 대부분이 `Scaffold(containerColor = Color.Transparent)` 로 그려 루트 셸의 바탕을 비쳐
 * 보이게 한다. 멈춰 있을 때는 그 아래가 루트 바탕 하나라 티가 안 나지만, 전환 중에는 그 아래에 뒤
 * 화면이 있어서 **축소된 앞 화면 사이로 뒤 화면 글자가 그대로 비친다.**
 *
 * 화면마다 `containerColor` 를 고치지 않고 목적지 자리에서 한 번 칠한다. 색은 루트 셸과 같은
 * [NavDestinationBackground] 라 멈춰 있을 때의 화면은 달라지지 않는다.
 *
 * - Nav3 로컬 스택은 [rememberStandardNavEntryDecorators] 가 모든 entry 에 씌운다.
 * - Nav2 루트 `NavHost` 에는 목적지 단위로 감쌀 자리가 없어서 목적지 등록부가 직접 감싼다.
 *   루트가 `NavDisplay` 로 바뀌면(#1702) 그 몫은 데코레이터가 받고, 등록부의 감싸기는 지운다.
 */
@Composable
public fun NavDestinationSurface(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(NavDestinationBackground)) {
        content()
    }
}

/**
 * 루트 셸의 바탕과 [NavDestinationSurface] 가 함께 쓰는 색.
 *
 * 둘이 갈리면 투명하게 그리는 화면의 멈춘 모습이 바뀌므로 한 곳에서 받는다.
 */
public val NavDestinationBackground: Color
    @Composable
    @ReadOnlyComposable
    get() = AfternoteDesign.colors.gray1

/** 로컬 스택의 모든 entry 를 [NavDestinationSurface] 로 감싼다. 상태가 없어 하나를 나눠 쓴다. */
internal val NavDestinationSurfaceDecorator: NavEntryDecorator<NavKey> =
    NavEntryDecorator(decorate = { entry -> NavDestinationSurface { entry.Content() } })
