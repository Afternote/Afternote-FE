package com.afternote.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
// 기존 버전

// private val AfternoteLightColors =
//    lightColorScheme(
//        background = AfternoteDesign.colors.gray2,
//    )
//
// fun ProvideAfternoteTheme(content: @Composable () -> Unit) {
//    MaterialTheme(
//        colorScheme = AfternoteLightColors,
//        typography = afternoteTypography,
//        content = content,
//    )
// }

// 수정된 버전

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
    // 시스템 설정이 다크 모드인지 확인
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // remember는 이미 저장된 값이 있으면 블록을 실행하지 않기 때문에 darkTheme이 적용되지 않는다
//    val currentColor = remember { if (darkTheme) darkColors else colors }
    val currentColor = if (isDarkTheme) darkColors else colors
    // currentColors가 참조하는 원본의 변경에 따른 리컴포지션을 트리거하지 않고, 원본 내부를 update하지 않기 위해서 copy
    // copy를 리컴포지션마다 하지 않기 위해 remember
    val rememberedColors = remember { currentColor.copy() }.apply { update(currentColor) }

    // 데이터 제공자 역할
    CompositionLocalProvider(
        // provides 앞의 객체의 current 프로퍼티를 호출하면 provides 뒤의 객체를 제공하도록 current를 호출한 컴포저블부터 모든 하위 트리에 세팅
        LocalColors provides rememberedColors,
        LocalTypography provides typography,
    ) {
        // 별도의 style 지정이 없다면 value를 content 내부의 모든 Text 컴포저블의 style의 기본값으로 지정
        ProvideTextStyle(typography.bodyLargeR, content = content)
    }
}

// Typography: MaterialTheme.typography.* → AfternoteDesign.typography.* (see AfternoteTypography in Type.kt)
// e.g. headlineLarge→h1, headlineMedium→h2, headlineSmall→h3, bodyLarge→bodyLargeB, bodyMedium→bodyLargeR,
// titleMedium→bodyBase, bodySmall→bodySmallR, titleSmall→bodySmallB, labelLarge→primaryButton,
// labelMedium→secondaryButton, labelSmall→footnoteCaption, displayLarge→captionLargeB,
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
