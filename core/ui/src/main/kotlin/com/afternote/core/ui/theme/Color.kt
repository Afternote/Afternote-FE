package com.afternote.core.ui.theme

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

private val White = Color(0xFFFFFFFF)

internal val Black = Color(0xFF000000)

private val IconBk = Color(0xFF000000).copy(alpha = 0.6F)

private val Gray1 = Color(0xFFFAFAFA)

private val Gray2 = Color(0xFFEEEEEE)

private val Gray3 = Color(0xFFE0E0E0)

private val Gray4 = Color(0xFFBDBDBD)

private val Gray5 = Color(0xFF9E9E9E)

/**
 * 라이트 테마의 `gray5` 대체값. **[Gray5] 를 라이트에서 쓰면 본문 대비가 기준 미달이다.**
 *
 * 팔레트 반전에서 `gray5` 만 자기 자신으로 매핑되는데(아래 [darkColors]), 배경이 뒤집히므로
 * 같은 `#9E9E9E` 라도 두 테마의 대비가 갈린다. 갤럭시 S25(Android 16) 실기기 캡처 실측:
 *
 * | 테마 | 배경 | 대비 |
 * |---|---|---|
 * | 라이트 | `#FAFAFA` | 2.57:1 — WCAG 본문 최소 4.5:1 미달 |
 * | 다크 | `#212121` | 6.01:1 — 통과 |
 *
 * 그래서 라이트만 이 값(4.89:1)으로 낮추고 다크는 [Gray5] 를 그대로 둔다.
 * 시안도 `#9E9E9E` 라 값이 다른 것은 의도된 차이다 — 통일 금지.
 */
private val Gray5OnLight = Color(0xFF6E6E6E)

private val Gray6 = Color(0xFF757575)

private val Gray7 = Color(0xFF616161)

private val Gray8 = Color(0xFF424242)

private val Gray9 = Color(0xFF212121)

/** 브랜드 강조 블루 (디자인 토큰 B1). */
private val B1 = Color(0xFF328BFF)

/** 데일리 질문 액션 버튼 배경 컬러 팔레트. */
private val Accent1 = Color(0xFF4E5F4D)
private val Accent2 = Color(0xFF2C6E63)
private val Accent3 = Color(0xFF3F6B5B)
private val Accent4 = Color(0xFF2F4F4A)
private val Accent5 = Color(0xFF6E5A8D)
private val Accent6 = Color(0xFF2F6F73)
private val Accent7 = Color(0xFF4A657D)
private val Accent8 = Color(0xFF6E5A7F)
private val Accent9 = Color(0xFF24324A)
private val Accent10 = Color(0xFF3A4A8A)

/** 에러 문구 색. 검증 실패 등 인라인 에러 텍스트에 사용 (시안 에러 문구 6곳 정본). */
private val Error = Color(0xFFFF0C0C)

/**
 * 필수 입력 표시 점 색 (시안 필수 마커 32곳 정본).
 * 에러 문구([Error])와 값이 다른 것은 시안의 용도별 구분 의도 — 통일 금지.
 */
private val RequiredMark = Color(0xFFFF3647)

internal fun lightColors() =
    AfternoteColors(
        white = White,
        black = Black,
        iconBk = IconBk,
        gray1 = Gray1,
        gray2 = Gray2,
        gray3 = Gray3,
        gray4 = Gray4,
        gray5 = Gray5OnLight, // 본문 대비 기준 — 상세는 Gray5OnLight KDoc
        gray6 = Gray6,
        gray7 = Gray7,
        gray8 = Gray8,
        gray9 = Gray9,
        b1 = B1,
        accent1 = Accent1,
        accent2 = Accent2,
        accent3 = Accent3,
        accent4 = Accent4,
        accent5 = Accent5,
        accent6 = Accent6,
        accent7 = Accent7,
        accent8 = Accent8,
        accent9 = Accent9,
        accent10 = Accent10,
        error = Error,
        requiredMark = RequiredMark,
        isLightMode = true,
    )

internal fun darkColors() =
    AfternoteColors(
        white = Black, // 배경 계열 반전
        black = White, // 텍스트 계열 반전
        iconBk = White.copy(alpha = 0.6F),
        gray1 = Gray9, // 가장 밝은 ↔ 가장 어두운
        gray2 = Gray8,
        gray3 = Gray7,
        gray4 = Gray6,
        gray5 = Gray5, // 중간은 반전 없음 — 다크는 이 값으로 대비 6.01:1 통과
        gray6 = Gray4,
        gray7 = Gray3,
        gray8 = Gray2,
        gray9 = Gray1, // 가장 어두운 ↔ 가장 밝은
        b1 = B1,
        accent1 = Accent1,
        accent2 = Accent2,
        accent3 = Accent3,
        accent4 = Accent4,
        accent5 = Accent5,
        accent6 = Accent6,
        accent7 = Accent7,
        accent8 = Accent8,
        accent9 = Accent9,
        accent10 = Accent10,
        error = Error,
        requiredMark = RequiredMark,
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
    b1: Color,
    accent1: Color,
    accent2: Color,
    accent3: Color,
    accent4: Color,
    accent5: Color,
    accent6: Color,
    accent7: Color,
    accent8: Color,
    accent9: Color,
    accent10: Color,
    error: Color,
    requiredMark: Color,
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
    var b1 by mutableStateOf(b1)
        private set
    var accent1 by mutableStateOf(accent1)
        private set
    var accent2 by mutableStateOf(accent2)
        private set
    var accent3 by mutableStateOf(accent3)
        private set
    var accent4 by mutableStateOf(accent4)
        private set
    var accent5 by mutableStateOf(accent5)
        private set
    var accent6 by mutableStateOf(accent6)
        private set
    var accent7 by mutableStateOf(accent7)
        private set
    var accent8 by mutableStateOf(accent8)
        private set
    var accent9 by mutableStateOf(accent9)
        private set
    var accent10 by mutableStateOf(accent10)
        private set
    var error by mutableStateOf(error)
        private set
    var requiredMark by mutableStateOf(requiredMark)
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
        b1: Color = this.b1,
        accent1: Color = this.accent1,
        accent2: Color = this.accent2,
        accent3: Color = this.accent3,
        accent4: Color = this.accent4,
        accent5: Color = this.accent5,
        accent6: Color = this.accent6,
        accent7: Color = this.accent7,
        accent8: Color = this.accent8,
        accent9: Color = this.accent9,
        accent10: Color = this.accent10,
        error: Color = this.error,
        requiredMark: Color = this.requiredMark,
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
        b1 = b1,
        accent1 = accent1,
        accent2 = accent2,
        accent3 = accent3,
        accent4 = accent4,
        accent5 = accent5,
        accent6 = accent6,
        accent7 = accent7,
        accent8 = accent8,
        accent9 = accent9,
        accent10 = accent10,
        error = error,
        requiredMark = requiredMark,
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
        this.b1 = other.b1
        this.accent1 = other.accent1
        this.accent2 = other.accent2
        this.accent3 = other.accent3
        this.accent4 = other.accent4
        this.accent5 = other.accent5
        this.accent6 = other.accent6
        this.accent7 = other.accent7
        this.accent8 = other.accent8
        this.accent9 = other.accent9
        this.accent10 = other.accent10
        this.error = other.error
        this.requiredMark = other.requiredMark
        this.isLightMode = other.isLightMode
    }
}
