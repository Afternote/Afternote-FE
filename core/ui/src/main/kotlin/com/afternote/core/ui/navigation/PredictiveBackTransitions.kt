package com.afternote.core.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleOut

/**
 * predictive back 진행 중 두 화면을 그리는 전환 한 쌍 (#1869).
 *
 * 이 저장소는 네비게이션 엔진을 둘 쓴다 — 루트는 Navigation 2 `NavHost`, 각 피처의 로컬 스택은
 * Navigation 3 `NavDisplay` 다(#1698). **두 엔진의 기본값이 서로 다르다.**
 *
 * - Nav3 `NavDisplay` 는 `predictivePopTransitionSpec` 을 안 넘기면
 *   `defaultPredictivePopTransitionSpec()` 을 쓴다 — 아래 두 값과 같은 모양이다
 *   (navigation3-ui 1.1.6 `NavDisplay.android.kt:50`). 그래서 로컬 스택은 이미 앞 화면이 줄어든다.
 * - Nav2 `NavHost` 의 `popEnterTransition`·`popExitTransition` 기본값은 `enterTransition`·
 *   `exitTransition` 을 그대로 받아 `fadeIn`/`fadeOut(tween(700))` 이다
 *   (navigation-compose 2.9.8 `NavHost.kt:132`). 앞 화면이 줄지 않고 제자리에서 흐려지기만 해,
 *   진행 중에 두 화면이 같은 크기로 겹쳐 보인다 — #1869 가 실기기에서 잡은 증상이다.
 *
 * 그래서 **새 값을 정하지 않고 Nav3 기본값을 그대로 복제해** 두 엔진의 눈에 보이는 동작을 맞춘다.
 * 루트가 `NavDisplay` 로 바뀌면(#1702) 이 두 상수를 그대로
 * `predictivePopTransitionSpec = { ContentTransform(PredictiveBackPopEnter, PredictiveBackPopExit) }`
 * 에 넘겨, 이관 전후로 화면이 달라지지 않게 한다.
 *
 * ### 모서리 라운딩은 여기서 못 준다
 * `ExitTransition` 은 fade·scale·slide·shrink 만 표현한다 — clip 이나 shape 는 담을 수 없다.
 * Nav3 기본값도 `scaleOut` 하나뿐이라 라운딩은 어느 쪽에도 없다. 축소만으로 겹침이 풀린다는 것이
 * #1869 의 완료 조건이고, 라운딩이 따로 필요하면 별도 축으로 받는다.
 */
public val PredictiveBackPopEnter: EnterTransition =
    fadeIn(
        // material3 motionScheme.defaultEffectsSpec() 을 반영한 값 그대로다.
        animationSpec = spring(dampingRatio = 1.0f, stiffness = 1600.0f),
    )

/**
 * 뒤로 밀려나는 앞 화면. `targetScale = 0.7f` 로 줄어들어 뒤 화면 위에 떠 보인다.
 *
 * 배경은 [PredictiveBackPopEnter] 의 KDoc 에 함께 적었다.
 */
public val PredictiveBackPopExit: ExitTransition = scaleOut(targetScale = 0.7f)
