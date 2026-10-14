package com.afternote.core.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleOut

/**
 * predictive back 진행 중 두 화면을 그리는 전환 한 쌍 (#1869).
 *
 * 이 저장소의 루트와 피처별 로컬 스택은 Navigation 3 `NavDisplay` 를 쓴다(#1702).
 * 루트는 이 공용 전환을 명시적으로 전달하고, 로컬 스택은 같은 기본 전환을 사용한다.
 *
 * - Nav3 `NavDisplay` 는 `predictivePopTransitionSpec` 을 안 넘기면
 *   `defaultPredictivePopTransitionSpec()` 을 쓴다 — 아래 두 값과 같은 모양이다
 *   (navigation3-ui 1.1.6 `NavDisplay.android.kt:50`). 그래서 로컬 스택은 이미 앞 화면이 줄어든다.
 * - Nav2 `NavHost` 의 `popEnterTransition`·`popExitTransition` 기본값은 `enterTransition`·
 *   `exitTransition` 을 그대로 받아 `fadeIn`/`fadeOut(tween(700))` 이다
 *   (navigation-compose 2.9.8 `NavHost.kt:132`). 앞 화면이 줄지 않고 제자리에서 흐려지기만 해,
 *   진행 중에 두 화면이 같은 크기로 겹쳐 보인다 — #1869 가 실기기에서 잡은 증상이다.
 *
 * 새 전환 값을 정하지 않고 Nav3 기본값과 같은 동작을 공유한다. 루트는
 * `predictivePopTransitionSpec = { ContentTransform(PredictiveBackPopEnter, PredictiveBackPopExit) }`
 * 를 전달해 이 전환을 명시적으로 유지한다.
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
