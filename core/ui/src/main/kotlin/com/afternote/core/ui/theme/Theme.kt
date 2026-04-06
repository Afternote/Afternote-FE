package com.afternote.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
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

private val AfternoteLightColors =
    lightColorScheme(
        background = Gray2,
    )

@Composable
fun Afternote_AndroidTheme(
    colors: AfternoteColors,
    darkColors: AfternoteColors? = null,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val currentColor = remember { if (darkColors != null && darkTheme) darkColors else colors }
    val rememberedColors = remember { currentColor.copy() }.apply { update(currentColor) }

    CompositionLocalProvider {
        LocalColors provides rememberedColors
        LocalTypography provides afternoteTypography
    } {
        ProvideTextStyle(afternoteTypography, content = content)
    }
}

// 기존
// fun AfternoteTheme(content: @Composable () -> Unit) {
//    MaterialTheme(
//        colorScheme = AfternoteLightColors,
//        typography = afternoteTypography,
//        content = content,
//    )
// }

val LocalColors = staticCompositionLocalOf { lightColors() }
val LocalTypography = staticCompositionLocalOf { afternoteTypography }

object AfternoteTheme {
    val colors: AfternoteColors
        @Composable
        @ReadOnlyComposable
        get() = LocalColors.current
    val typography: Typography
        @Composable
        @ReadOnlyComposable
        get() = LocalTypography.current
}
