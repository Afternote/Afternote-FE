package com.afternote.feature.home.presentation.receiver.component

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
import com.afternote.feature.home.presentation.R

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
 * 개수 자리에 넣을 문자열 — 값이 있으면 숫자, 없으면 대시 (#952).
 *
 * 갈래를 문장이 아니라 **개수 자리 하나로** 좁힌다. 문장을 통째로 갈아 끼우면 섹션 높이와
 * 줄 구성이 바뀌고(시안 확정값은 레이아웃 유지), 호출부마다 같은 문장이 if/else 양쪽에
 * 두 번 적혀 한쪽만 고쳐질 자리가 된다.
 */
@Composable
fun countText(count: Int?): String = count?.toString() ?: stringResource(R.string.home_receiver_section_count_unavailable)
