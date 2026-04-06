package com.afternote.core.ui.theme

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

val White = Color(0xFFFFFFFF)

val Black = Color(0xFF000000)

val IconBk = Color(0xFF000000).copy(alpha = 0.6F)

val Gray1 = Color(0xFFFAFAFA)

val Gray2 = Color(0xFFEEEEEE)

val Gray3 = Color(0xFFE0E0E0)

val Gray4 = Color(0xFFBDBDBD)

val Gray5 = Color(0xFF9E9E9E)

val Gray6 = Color(0xFF757575)

val Gray7 = Color(0xFF616161)

val Gray8 = Color(0xFF424242)

val Gray9 = Color(0xFF212121)

fun lightColors() =
    AfternoteColors(
        white = White,
        black = Black,
        iconBk = IconBk,
        gray1 = Gray1,
        gray2 = Gray2,
        gray3 = Gray3,
        gray4 = Gray4,
        gray5 = Gray5,
        gray6 = Gray6,
        gray7 = Gray7,
        gray8 = Gray8,
        gray9 = Gray9,
        isLightMode = true,
    )

fun darkColors() =
    AfternoteColors(
        white = Black, // 배경 계열 반전
        black = White, // 텍스트 계열 반전
        iconBk = White.copy(alpha = 0.6F),
        gray1 = Gray9, // 가장 밝은 ↔ 가장 어두운
        gray2 = Gray8,
        gray3 = Gray7,
        gray4 = Gray6,
        gray5 = Gray5, // 중간은 그대로
        gray6 = Gray4,
        gray7 = Gray3,
        gray8 = Gray2,
        gray9 = Gray1, // 가장 어두운 ↔ 가장 밝은
        isLightMode = false,
    )

// 컴포저블이 리컴포지션될 때 그 내부 객체는 Stable/Unstable에 따라 리컴포지션이 결정
// Stable하다면 그 객체의 상태 변화를 확인 후 리컴포지션 결정
// Unstable하다면 그 객체의 상태를 확인할 것도 없이 무조건 리컴포지션
// 클래스의 경우 var 프로퍼티가 있으면 Unstable하므로 @Stable을 붙여 불필요한 리컴포지션 방지
@Stable
class AfternoteColors(
    white: Color,
    black: Color,
    iconBk: Color,
    gray1: Color,
    gray2: Color,
    gray3: Color,
    gray4: Color,
    gray5: Color,
    gray6: Color,
    gray7: Color,
    gray8: Color,
    gray9: Color,
    isLightMode: Boolean,
) {
    var white by mutableStateOf(white)
        private set
    var black by mutableStateOf(black)
        private set
    var iconBk by mutableStateOf(iconBk)
        private set
    var gray1 by mutableStateOf(gray1)
        private set
    var gray2 by mutableStateOf(gray2)
        private set
    var gray3 by mutableStateOf(gray3)
        private set
    var gray4 by mutableStateOf(gray4)
        private set
    var gray5 by mutableStateOf(gray5)
        private set
    var gray6 by mutableStateOf(gray6)
        private set
    var gray7 by mutableStateOf(gray7)
        private set
    var gray8 by mutableStateOf(gray8)
        private set
    var gray9 by mutableStateOf(gray9)
        private set
    var isLightMode by mutableStateOf(isLightMode)
        private set

    fun copy(
        white: Color = this.white,
        black: Color = this.black,
        iconBk: Color = this.iconBk,
        gray1: Color = this.gray1,
        gray2: Color = this.gray2,
        gray3: Color = this.gray3,
        gray4: Color = this.gray4,
        gray5: Color = this.gray5,
        gray6: Color = this.gray6,
        gray7: Color = this.gray7,
        gray8: Color = this.gray8,
        gray9: Color = this.gray9,
        isLightMode: Boolean = this.isLightMode,
    ) = AfternoteColors(
        white = white,
        black = black,
        iconBk = iconBk,
        gray1 = gray1,
        gray2 = gray2,
        gray3 = gray3,
        gray4 = gray4,
        gray5 = gray5,
        gray6 = gray6,
        gray7 = gray7,
        gray8 = gray8,
        gray9 = gray9,
        isLightMode = isLightMode,
    )

    fun update(other: AfternoteColors) {
        this.white = other.white
        this.black = other.black
        this.iconBk = other.iconBk
        this.gray1 = other.gray1
        this.gray2 = other.gray2
        this.gray3 = other.gray3
        this.gray4 = other.gray4
        this.gray5 = other.gray5
        this.gray6 = other.gray6
        this.gray7 = other.gray7
        this.gray8 = other.gray8
        this.gray9 = other.gray9
        this.isLightMode = other.isLightMode
    }
}

// 이전 레포
// val Purple80 = Color(0xFFD0BCFF)
// val PurpleGrey80 = Color(0xFFCCC2DC)
// val Pink80 = Color(0xFFEFB8C8)
//
// val Purple40 = Color(0xFF6650a4)
// val PurpleGrey40 = Color(0xFF625b71)
// val Pink40 = Color(0xFF7D5260)
// val TextPrimary = Color(0xFF2D2722) // 다이얼로그 텍스트 등 주요 텍스트 색상
//
// val LightBlue = Color(0xFFE3F2FD) // 아이콘 배경용
// val ShadowBlack = Color.Black.copy(alpha = 0.05f) // 그림자용

val Red = Color(0xFFFF0C0C) // 에러 메시지용 빨간색
