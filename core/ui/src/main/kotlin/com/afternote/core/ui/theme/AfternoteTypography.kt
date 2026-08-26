package com.afternote.core.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.afternote.core.ui.R

private val nanumBarunGothic =
    FontFamily(
        Font(R.font.nanumbarungothic, FontWeight.Normal),
        Font(R.font.nanumbarungothicbold, FontWeight.Bold),
    )

private val sfMono =
    FontFamily(
        Font(R.font.sf_mono_regular, FontWeight.Normal),
    )

private val inter =
    FontFamily(
        Font(R.font.inter_variable, FontWeight.Medium),
    )

/**
 * 한글을 어절 단위로 끊기 위한 설정. **[BodyLineBreak] 과 [KoreanLocale] 은 둘 다 있어야 효과가 있다.**
 *
 * Compose 기본값 [LineBreak.Simple] 은 한글을 글자 단위로 끊어 "작성" 이 "작"/"성" 으로 갈린다.
 * 어절을 지키는 것은 [LineBreak.WordBreak.Phrase] 인데, 프리셋 중 이를 켜는 것은 [LineBreak.Heading]
 * 뿐이고 [LineBreak.Paragraph] 는 `WordBreak.Default` 라 그대로 쓰면 효과가 없다.
 *
 * 그리고 `Phrase` 만으로도 부족하다 — 텍스트 로케일이 한국어여야 플랫폼이 어절 분석을 한다.
 * API 35 에뮬레이터(로케일 en-US) 실측:
 *
 * | 조합 | 어절 중간 잘림 |
 * |---|---|
 * | `Paragraph` + `Phrase`, 로케일 무지정 | 2건 |
 * | `Paragraph` (원본), `ko-KR` | 2건 |
 * | `Paragraph` + `Phrase`, `ko-KR` | 0건 |
 *
 * 이 앱은 `values-*` 가 없는 한국어 전용이라 로케일을 고정한다. `Strategy` 는 이 축과 무관하다.
 *
 * 플랫폼이 이 설정을 받는 것은 Android 13(API 33) 부터다 — `StaticLayout.Builder.setLineBreakConfig`
 * 가 API 33 게이트 안에 있다. minSdk 26 이므로 API 26~32 기기에서는 기존 동작이 유지된다.
 */
private val KoreanLocale = LocaleList(Locale("ko-KR"))

private val BodyLineBreak = LineBreak.Paragraph.copy(wordBreak = LineBreak.WordBreak.Phrase)

/** 제목용. [LineBreak.Heading] 은 이미 `WordBreak.Phrase` 를 포함한다. */
private val HeadingLineBreak = LineBreak.Heading

data class AfternoteTypography(
    val h1: TextStyle =
        TextStyle(
            fontFamily = nanumBarunGothic,
            fontWeight = FontWeight.Normal,
            fontSize = 28.sp,
            lineHeight = 36.sp,
            letterSpacing = (-0.0025).em,
            lineBreak = HeadingLineBreak,
            localeList = KoreanLocale,
        ),
    val h2: TextStyle =
        TextStyle(
            fontFamily = nanumBarunGothic,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            lineHeight = 30.sp,
            letterSpacing = (-0.0025).em,
            lineBreak = HeadingLineBreak,
            localeList = KoreanLocale,
        ),
    val h3: TextStyle =
        TextStyle(
            fontFamily = nanumBarunGothic,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            lineHeight = 26.sp,
            letterSpacing = (-0.0025).em,
            lineBreak = HeadingLineBreak,
            localeList = KoreanLocale,
        ),
    val bodyLargeB: TextStyle =
        TextStyle(
            fontFamily = nanumBarunGothic,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            lineHeight = 24.sp,
            lineBreak = BodyLineBreak,
            localeList = KoreanLocale,
        ),
    val bodyLargeR: TextStyle =
        TextStyle(
            fontFamily = nanumBarunGothic,
            fontWeight = FontWeight.Normal,
            fontSize = 18.sp,
            lineHeight = 22.sp,
            lineBreak = BodyLineBreak,
            localeList = KoreanLocale,
        ),
    val bodyBase: TextStyle =
        TextStyle(
            fontFamily = nanumBarunGothic,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            lineBreak = BodyLineBreak,
            localeList = KoreanLocale,
        ),
    val bodySmallB: TextStyle =
        TextStyle(
            fontFamily = nanumBarunGothic,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            lineBreak = BodyLineBreak,
            localeList = KoreanLocale,
        ),
    val bodySmallR: TextStyle =
        TextStyle(
            fontFamily = nanumBarunGothic,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            lineBreak = BodyLineBreak,
            localeList = KoreanLocale,
        ),
    val primaryButton: TextStyle =
        TextStyle(
            fontFamily = nanumBarunGothic,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = (-0.0025).em,
            lineBreak = BodyLineBreak,
            localeList = KoreanLocale,
        ),
    val secondaryButton: TextStyle =
        TextStyle(
            fontFamily = nanumBarunGothic,
            fontWeight = FontWeight.Normal,
            fontSize = 22.sp,
            lineHeight = 20.sp,
            letterSpacing = (-0.0025).em,
            lineBreak = BodyLineBreak,
            localeList = KoreanLocale,
        ),
    val footnoteCaption: TextStyle =
        TextStyle(
            fontFamily = nanumBarunGothic,
            fontWeight = FontWeight.Normal,
            fontSize = 10.sp,
            lineHeight = 16.sp,
            lineBreak = BodyLineBreak,
            localeList = KoreanLocale,
        ),
    val captionLargeB: TextStyle =
        TextStyle(
            fontFamily = nanumBarunGothic,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            lineBreak = BodyLineBreak,
            localeList = KoreanLocale,
        ),
    val captionLargeR: TextStyle =
        TextStyle(
            fontFamily = nanumBarunGothic,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            lineBreak = BodyLineBreak,
            localeList = KoreanLocale,
        ),
    val mono: TextStyle =
        TextStyle(
            fontFamily = sfMono,
            fontWeight = FontWeight.Normal,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.045.em,
            lineBreak = BodyLineBreak,
            localeList = KoreanLocale,
        ),
    val textField: TextStyle =
        TextStyle(
            fontFamily = nanumBarunGothic,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 22.sp,
            lineBreak = BodyLineBreak,
            localeList = KoreanLocale,
        ),
    val inter: TextStyle =
        TextStyle(
            fontFamily = com.afternote.core.ui.theme.inter,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            lineHeight = 21.sp,
            letterSpacing = (-0.006).em,
            lineBreak = BodyLineBreak,
            localeList = KoreanLocale,
        ),
)
