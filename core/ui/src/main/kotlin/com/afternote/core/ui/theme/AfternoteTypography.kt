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

/**
 * 앱 타이포 정본.
 *
 * **이름에 `B` 가 붙은 스타일이 `FontWeight.Bold` 가 아닌 것은 의도다 — 되돌리지 말 것.**
 *
 * 나눔바른고딕은 가로획을 세로획의 81% 로 그리는 서체다. 홑자모 아웃라인 실측(upem 1000):
 *
 * | 페이스 | `ㅣ` 세로획 | `ㅡ` 가로획 | 가로/세로 |
 * |---|---|---|---|
 * | Regular | 90 | 73 | 0.81 |
 * | Bold | 125 | 103 | 0.82 |
 *
 * **두 페이스의 비율은 같다 — Bold 가 더 비대칭인 것이 아니라 획이 39% 굵을 뿐이다.**
 * 참고로 Apple SD 고딕 Neo 는 같은 비가 웨이트별로 0.88~0.92 라 나눔보다 대비가 작다.
 *
 * 문제는 렌더링에서 갈린다. 플랫폼이 획을 정수 픽셀로 맞추는데, **Bold 는 세로획이 가로획보다
 * 항상 1~2px 두껍게 떨어지고 Regular 은 작은 크기에서 둘이 같은 정수로 떨어진다.** 같은 기기
 * 같은 설정에서 `ㅣ`/`ㅡ` 만 띄워 크기별로 잰 결과(밀도 420 · 글꼴 배율 0.8, sp→px 2.1):
 *
 * | em px | Bold 세로/가로 | 비 | Regular 세로/가로 | 비 |
 * |---|---|---|---|---|
 * | 21 | 3 / 2 | 0.67 | 2 / 2 | **1.00** |
 * | 29 | 4 / 2 | **0.50** | 2 / 2 | **1.00** |
 * | 34 | 4 / 3 | 0.75 | 3 / 3 | **1.00** |
 * | 42 | 5 / 4 | 0.80 | 4 / 3 | 0.75 |
 * | 59 | 7 / 6 | 0.86 | 5 / 5 | **1.00** |
 *
 * 즉 Bold 가 불균일을 만드는 것이 아니라 원래 있던 대비를 픽셀 단위로 드러낸다. 본문 크기대에서
 * 가장 나쁘고(em 29 에서 0.50), Regular 은 바로 그 구간에서 1.00 이다.
 *
 * **Bold 를 쓸 수 있는 하한은 em 58px 이다** — 그래야 가로획이 6px 이 되어 1px 차이가 17% 로
 * 떨어진다. 기본 설정(밀도 480 · 글꼴 1.0)에서 약 19sp, 글꼴 배율을 0.8 로 줄인 기기에서는
 * 약 28sp 다. 이 파일의 스타일은 전부 그 아래라 Bold 를 쓸 자리가 없다.
 *
 * 시안도 `H2`·`H3`·`BodyLarge(B)`·`BodySmall(B)`·`CaptionLarge(B)`·`PrimaryButton` 을 전부
 * Regular 페이스로 쓴다 — 정본 페이지 TEXT 노드 1,789건 전량이 `NanumBarunGothic` 이고
 * `NanumBarunGothicBold` 는 0건이다.
 *
 * Bold 폰트 파일 등록([nanumBarunGothic])은 그대로 둔다 — 마음의 기록 편집기에서 사용자가
 * 직접 거는 볼드(`SpanStyle(fontWeight = FontWeight.Bold)`)가 그 파일을 쓴다.
 */
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
            fontWeight = FontWeight.Normal,
            fontSize = 24.sp,
            lineHeight = 30.sp,
            letterSpacing = (-0.0025).em,
            lineBreak = HeadingLineBreak,
            localeList = KoreanLocale,
        ),
    val h3: TextStyle =
        TextStyle(
            fontFamily = nanumBarunGothic,
            fontWeight = FontWeight.Normal,
            fontSize = 20.sp,
            lineHeight = 26.sp,
            letterSpacing = (-0.0025).em,
            lineBreak = HeadingLineBreak,
            localeList = KoreanLocale,
        ),
    val bodyLargeB: TextStyle =
        TextStyle(
            fontFamily = nanumBarunGothic,
            fontWeight = FontWeight.Normal,
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
    /**
     * **[bodySmallR] 과 여섯 필드가 전부 같다 — 지금은 이름만 다른 같은 값이다.** 합치지 않고
     * 남기는 이유는 [captionLargeB] 와 같다. 상세는 그쪽 KDoc.
     */
    val bodySmallB: TextStyle =
        TextStyle(
            fontFamily = nanumBarunGothic,
            fontWeight = FontWeight.Normal,
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
            fontWeight = FontWeight.Normal,
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
    /**
     * **[captionLargeR] 과 여섯 필드가 전부 같다 — 지금은 이름만 다른 같은 값이다.**
     * ([bodySmallB]/[bodySmallR] 도 마찬가지다. [bodyLargeB]/[bodyLargeR] 는 줄높이가 달라 해당 없다.)
     *
     * **중복을 만든 것은 이 코드가 아니라 시안이다.** 정본 페이지에서도 `CaptionLarge(B)` 와
     * `CaptionLarge-R` 의 값이 같다. 코드가 Bold 를 쓰던 동안에만 둘이 갈려 보였고, 위 KDoc 대로
     * 웨이트를 시안에 맞추면서 그 착시가 걷혔을 뿐이다.
     *
     * **그런데도 합치지 않고 남긴다.** 두 가지 때문이다.
     *
     * 1. 합치려면 호출부 94곳(`bodySmallB` 58 · `captionLargeB` 36)을 고쳐야 하는데 8개 모듈에
     *    걸쳐 있고 담당자가 셋이다. 웨이트를 맞추는 이 변경의 범위를 넘는다.
     * 2. 「값이 같으니 하나로 합친다」는 **시안이 두 이름을 계속 나눠 둘 것인지 확인한 뒤**에
     *    내릴 판정이다. 지금 합쳤다가 시안이 다시 갈리면 94곳을 두 번 고치게 된다.
     *
     * 그 확인과 통합은 #1862 로 뗐다.
     *
     * **그때까지 둘이 조용히 갈라지지 않도록 `AfternoteTypographyPairTest` 가 동일성을 잠근다** —
     * 한쪽만 고치면 그 테스트가 깨지므로, 갈라놓는 것도 합치는 것도 의식적인 판정이 된다.
     */
    val captionLargeB: TextStyle =
        TextStyle(
            fontFamily = nanumBarunGothic,
            fontWeight = FontWeight.Normal,
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
