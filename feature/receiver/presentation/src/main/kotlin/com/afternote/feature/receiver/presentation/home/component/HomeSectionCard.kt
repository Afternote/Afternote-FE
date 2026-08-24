package com.afternote.feature.receiver.presentation.home.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.feature.receiver.presentation.R

/**
 * 마음의 기록·타임레터·애프터노트 카드의 공통 컨테이너.
 *
 * 단순 둥근 배경이라 Material3 [androidx.compose.material3.Card] 대신
 * Column + clip/background/border 체이닝으로 구성한다.
 */
@Composable
fun HomeSectionCard(
    title: String,
    description: String,
    countLine: AnnotatedString,
    buttonText: String,
    onButtonClick: () -> Unit,
    modifier: Modifier = Modifier,
    middleContent: @Composable ColumnScope.() -> Unit = {},
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(
                    width = 1.dp,
                    color = AfternoteDesign.colors.gray2,
                    shape = RoundedCornerShape(16.dp),
                ).background(AfternoteDesign.colors.white)
                .padding(horizontal = 20.dp, vertical = 20.dp),
    ) {
        Text(
            text = title,
            style = AfternoteDesign.typography.bodyLargeB,
            color = AfternoteDesign.colors.gray9,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = description,
            style = AfternoteDesign.typography.bodySmallR,
            color = AfternoteDesign.colors.gray8,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = countLine,
            style = AfternoteDesign.typography.captionLargeR,
            color = AfternoteDesign.colors.gray8,
        )

        middleContent()

        Spacer(modifier = Modifier.height(16.dp))
        SectionGoButton(text = buttonText, onClick = onButtonClick)
    }
}

/**
 * 「150개 마음의 기록이 있습니다.」처럼 카운트만 [AfternoteDesign.colors.b1]로 강조한 한 줄 문장 빌드.
 */
@Composable
fun rememberCountLine(
    prefix: String,
    suffix: String,
): AnnotatedString =
    buildAnnotatedString {
        withStyle(SpanStyle(color = AfternoteDesign.colors.b1)) { append(prefix) }
        append(suffix)
    }

/**
 * 조회 실패·미상일 때의 개수 자리 표기 — 정상 0건과 구분되는 값이면서 **문장 구조는
 * 그대로**다 (#952).
 *
 * 종전에는 문장을 통째로 "기록 수를 불러오지 못했습니다." 로 갈아 끼워 섹션 높이와 줄
 * 구성이 바뀌었다. 시안 확정값(4309:19394)은 레이아웃 유지 + 숫자 자리만 대시다.
 */
@Composable
fun unavailableCountLine(
    suffix: String,
    countUnitSpacing: String = " ",
): AnnotatedString =
    rememberCountLine(
        prefix = "${stringResource(R.string.receiver_home_section_count_unavailable)}개$countUnitSpacing",
        suffix = suffix,
    )
