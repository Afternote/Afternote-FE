package com.afternote.core.ui.theme

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf

// (static)compositionLocalOf는 부모에서 자식 컴포저블로 데이터를 전달할 때 파라미터로 일일이 넘기지(Prop Drilling) 않고 데이터를 사용할 수 있게 함
// LocalColors.current의 형태로 현재 전달된 데이터를 꺼내 쓸 수 있음
// staticCompositionLocalOf는 데이터 값이 변경되면 데이터 제공자와 그 모든 자식 컴포저블을 리컴포지션
// compositionLocalOf는 데이터 값이 변경되면 데이터를 참조하는 컴포저블만 리컴포지션
private val LocalColors =
    staticCompositionLocalOf {
        lightColors() // 제공자가 준 데이터 값 없을 때 사용하는 기본 값
    }

private val LocalTypography =
    staticCompositionLocalOf {
        AfternoteTypography()
    }

// ripple()은 컴포저블이 아니라서 remember가 필요 없고, 여러 컴포넌트가 함께 써도 되므로 할당을 아끼려 top-level로 추출
private val AfternoteRipple = ripple()

@Composable
fun AfternoteTheme(
    // AfternoteTheme의 current를 호출해 봤자 CompositionLocalProvider 호출 전이기 때문에 제공자가 없음
    // 따라서 staticCompositionLocalOf의 기본 값(lightColors() 등)만 들어오기 때문에 의미가 없음
    // 그래서 기본값 lightColors() 등을 직접 전달
//    colors: AfternoteColors = ProvideAfternoteTheme.colors,
//    darkColors: AfternoteColors = ProvideAfternoteTheme.darkColors,
//    typography: Typography = ProvideAfternoteTheme.typography,
    colors: AfternoteColors = lightColors(),
    darkColors: AfternoteColors = darkColors(),
    typography: AfternoteTypography = AfternoteTypography(),
    /**
     * 다크 팔레트를 물릴지. **기본값은 라이트 고정이다** (#1719).
     *
     * 배선만 있고 화면이 안 맞춰진 중간 상태가 양쪽 끝보다 나빴다 — 실측에서 「마음의 기록」
     * 헤더가 검정 배경에 검정 글자로 거의 보이지 않았고, 주간 요약 카드는 배경만 밝은 채로
     * 남아 라벨이 저대비로 흐려졌다. 토큰을 쓰지 않고 색을 박은 자리가 반전을 따라가지
     * 못해서다(`feature/mindrecord/presentation` main 에만 29곳).
     *
     * 시안의 다크 팔레트가 확정되고 하드코딩 색이 토큰으로 옮겨지면 기본값을
     * [isSystemInDarkTheme] 으로 되돌린다. [darkColors] 와 이 파라미터는 그때를 위해 남겨
     * 둔다 — 값을 넘기면 지금도 다크로 그릴 수 있다.
     *
     * **되돌릴 때 `app` 의 `enableLightEdgeToEdge()` 도 같이 되돌린다.** 이 잠금만 풀면
     * 시스템바 아이콘이 라이트로 고정된 채 배경만 다크가 되어 반대 방향으로 안 보인다.
     */
    isDarkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    val currentColor = if (isDarkTheme) darkColors else colors
    // currentColors가 참조하는 원본의 변경에 따른 리컴포지션을 트리거하지 않고, 원본 내부를 update하지 않기 위해서 copy
    // copy를 리컴포지션마다 하지 않기 위해 remember
    val rememberedColors = remember { currentColor.copy() }.apply { update(currentColor) }

    // 데이터 제공자 역할
    CompositionLocalProvider(
        // provides 앞의 객체의 current 프로퍼티를 호출하면 provides 뒤의 객체를 제공하도록 current를 호출한 컴포저블부터 모든 하위 트리에 세팅
        LocalColors provides rememberedColors,
        LocalTypography provides typography,
        // 제공하지 않으면 clickable의 눌림 피드백이 foundation 기본값(DefaultDebugIndication)으로 떨어져
        // 노드 전체를 덮는 검정 사각형이 그려짐 — MaterialTheme도 같은 방식으로 ripple을 제공함
        LocalIndication provides AfternoteRipple,
    ) {
        // 별도의 style 지정이 없다면 value를 content 내부의 모든 Text 컴포저블의 style의 기본값으로 지정
        ProvideTextStyle(typography.bodyLargeR, content = content)
    }
}

// Typography: MaterialTheme.typography.* → AfternoteDesign.typography.* (see AfternoteTypography in AfternoteTypography.kt)
// e.g. headlineLarge→h1, headlineMedium→h2, headlineSmall→h3, bodyLarge→bodyLargeB, bodyMedium→bodyLargeR,
// titleMedium→bodyBase, bodySmall→bodySmallR, titleSmall→bodySmallB, labelLarge→primaryButton,
// labelSmall→footnoteCaption, displayLarge→captionLargeB,
// displayMedium→captionLargeR, displaySmall→mono
object AfternoteDesign {
    val colors: AfternoteColors
        // current는 컴포저블 함수이므로 이를 호출하는 게터 함수도 컴포저블이어야 하기 때문에 컴포저블 어노테이션 필요
        // 이 게터는 colors가 호출되는 시점에 LocalColors.current를 실행하는 함수
        // LocalColors.current를 매번 새로 실행하므로 최신 값을 반환
        @Composable
        // 게터는 상태를 저장하지 않는 컴포저블이므로 컴포지션 노드를 만들지 말라는 뜻
        @ReadOnlyComposable
        get() = LocalColors.current

    val typography: AfternoteTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalTypography.current
}
