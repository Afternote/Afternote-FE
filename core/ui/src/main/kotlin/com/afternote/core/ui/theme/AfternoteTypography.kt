package com.afternote.core.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.afternote.core.ui.R

private val nanumGothic =
    FontFamily(
        Font(R.font.nanumbarungothic, FontWeight.Normal),
        Font(R.font.nanumbarungothicbold, FontWeight.Bold),
    )

private val sfMono =
    FontFamily(
        Font(R.font.sf_mono_regular, FontWeight.Normal),
    )

data class AfternoteTypography(
    val h1: TextStyle =
        TextStyle(
            fontFamily = nanumGothic,
            fontWeight = FontWeight.Normal,
            fontSize = 28.sp,
            lineHeight = 36.sp,
            letterSpacing = (-0.0025).em,
        ),
    val h2: TextStyle =
        TextStyle(
            fontFamily = nanumGothic,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            lineHeight = 30.sp,
            letterSpacing = (-0.0025).em,
        ),
    val h3: TextStyle =
        TextStyle(
            fontFamily = nanumGothic,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            lineHeight = 26.sp,
            letterSpacing = (-0.0025).em,
        ),
    val bodyLargeB: TextStyle =
        TextStyle(
            fontFamily = nanumGothic,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            lineHeight = 24.sp,
        ),
    val bodyLargeR: TextStyle =
        TextStyle(
            fontFamily = nanumGothic,
            fontWeight = FontWeight.Normal,
            fontSize = 18.sp,
            lineHeight = 22.sp,
        ),
    val bodyBase: TextStyle =
        TextStyle(
            fontFamily = nanumGothic,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
        ),
    val bodySmallB: TextStyle =
        TextStyle(
            fontFamily = nanumGothic,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        ),
    val bodySmallR: TextStyle =
        TextStyle(
            fontFamily = nanumGothic,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        ),
    val primaryButton: TextStyle =
        TextStyle(
            fontFamily = nanumGothic,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = (-0.0025).em,
        ),
    val secondaryButton: TextStyle =
        TextStyle(
            fontFamily = nanumGothic,
            fontWeight = FontWeight.Normal,
            fontSize = 22.sp,
            lineHeight = 20.sp,
            letterSpacing = (-0.0025).em,
        ),
    val footnoteCaption: TextStyle =
        TextStyle(
            fontFamily = nanumGothic,
            fontWeight = FontWeight.Normal,
            fontSize = 10.sp,
            lineHeight = 16.sp,
        ),
    val captionLargeB: TextStyle =
        TextStyle(
            fontFamily = nanumGothic,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            lineHeight = 18.sp,
        ),
    val captionLargeR: TextStyle =
        TextStyle(
            fontFamily = nanumGothic,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 18.sp,
        ),
    val mono: TextStyle =
        TextStyle(
            fontFamily = sfMono,
            fontWeight = FontWeight.Normal,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.045.em,
        ),
    val textField: TextStyle =
        TextStyle(
            fontFamily = nanumGothic,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 22.sp,
        ),
)
