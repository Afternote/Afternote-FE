package com.afternote.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/*
style 매핑 가이드
H1: headlineLarge
H2: headlineMedium
H3: headlineSmall
BodyLarge(B): bodyLarge
BodyLarge(R): bodyMedium
BodyBase: titleMedium
BodySmall(R): bodySmall
BodySmall(B): titleSmall
PrimaryButton: labelLarge
SecondaryButton: labelMedium
Footnote-caption: labelSmall
CaptionLarge(B): displayLarge
captionLarge(R): displayMedium
mono: displaySmall
 */

private val afternoteTypography =
    Typography(
        headlineLarge =
            TextStyle(
                fontFamily = naNumGothic,
                fontWeight = FontWeight.Normal,
                fontSize = 28.sp,
                lineHeight = 36.sp,
                letterSpacing = (-0.0025).em,
            ),
        headlineMedium =
            TextStyle(
                fontFamily = naNumGothic,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                lineHeight = 30.sp,
                letterSpacing = (-0.0025).em,
            ),
        headlineSmall =
            TextStyle(
                fontFamily = naNumGothic,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                lineHeight = 26.sp,
                letterSpacing = (-0.0025).em,
            ),
        bodyLarge =
            TextStyle(
                fontFamily = naNumGothic,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                lineHeight = 24.sp,
            ),
        bodyMedium =
            TextStyle(
                fontFamily = naNumGothic,
                fontWeight = FontWeight.Normal,
                fontSize = 18.sp,
                lineHeight = 22.sp,
            ),
        bodySmall =
            TextStyle(
                fontFamily = naNumGothic,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            ),
        titleSmall =
            TextStyle(
                fontFamily = naNumGothic,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            ),
        titleMedium =
            TextStyle(
                fontFamily = naNumGothic,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 24.sp,
            ),
        labelLarge =
            TextStyle(
                fontFamily = naNumGothic,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                lineHeight = 24.sp,
                letterSpacing = (-0.0025).em,
            ),
        labelMedium =
            TextStyle(
                fontFamily = naNumGothic,
                fontWeight = FontWeight.Normal,
//                fontSize = 12.sp, // 나브탭 크기가 커지는 문제 때문에 22에서 변경
                fontSize = 22.sp,
                lineHeight = 20.sp,
                letterSpacing = (-0.0025).em,
            ),
        labelSmall =
            TextStyle(
                fontFamily = naNumGothic,
                fontWeight = FontWeight.Normal,
                fontSize = 10.sp,
                lineHeight = 16.sp,
            ),
        displayLarge =
            TextStyle(
                fontFamily = naNumGothic,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                lineHeight = 18.sp,
            ),
        displayMedium =
            TextStyle(
                fontFamily = naNumGothic,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                lineHeight = 18.sp,
            ),
        displaySmall =
            TextStyle(
                fontFamily = naNumGothic,
                fontWeight = FontWeight.Normal,
                fontSize = 11.sp,
                lineHeight = 16.sp,
            ),
    )

// 기존 버전

// private val AfternoteLightColors =
//    lightColorScheme(
//        background = Gray2,
//    )
//
// fun AfternoteTheme(content: @Composable () -> Unit) {
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
val LocalColors =
    staticCompositionLocalOf {
        lightColors() // 제공자가 준 데이터 값 없을 때 사용하는 기본 값
    }

val LocalTypography =
    staticCompositionLocalOf {
        afternoteTypography
    }

@Composable
fun AfternoteTheme(
    // AfternoteTheme의 current를 호출해 봤자 CompositionLocalProvider 호출 전이기 때문에 제공자가 없음
    // 따라서 staticCompositionLocalOf의 기본 값(lightColors() 등)만 들어오기 때문에 의미가 없음
    // 그래서 기본값 lightColors() 등을 직접 전달
//    colors: AfternoteColors = AfternoteTheme.colors,
//    darkColors: AfternoteColors = AfternoteTheme.darkColors,
//    typography: Typography = AfternoteTheme.typography,
    colors: AfternoteColors = lightColors(),
    darkColors: AfternoteColors = darkColors(),
    typography: Typography = afternoteTypography,
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
        ProvideTextStyle(typography.bodyMedium, content = content)
    }
}

object AfternoteTheme {
    val colors: AfternoteColors
        // current는 컴포저블 함수이므로 이를 호출하는 게터 함수도 컴포저블이어야 하기 때문에 컴포저블 어노테이션 필요
        // 이 게터는 colors가 호출되는 시점에 LocalColors.current를 실행하는 함수
        // LocalColors.current를 매번 새로 실행하므로 최신 값을 반환
        @Composable
        // 게터는 상태를 저장하지 않는 컴포저블이므로 컴포지션 노드를 만들지 말라는 뜻
        @ReadOnlyComposable
        get() = LocalColors.current

    val typography: Typography
        @Composable
        @ReadOnlyComposable //
        get() = LocalTypography.current
}
