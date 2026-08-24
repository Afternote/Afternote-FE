package com.afternote.feature.receiver.presentation.home.component

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.receiver.presentation.R

/** 타임레터 섹션 — 단순 카드. */
@Composable
fun TimeLetterSection(
    totalCount: Int?,
    onGoClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    HomeSectionCard(
        modifier = modifier,
        title = stringResource(R.string.receiver_home_timeletter_section_title),
        description = stringResource(R.string.receiver_home_timeletter_section_desc),
        countLine =
            rememberCountLine(
                prefix = "${countText(totalCount)}개 ",
                suffix = "라이프 이벤트 레터가 있습니다.",
            ),
        buttonText = stringResource(R.string.receiver_home_timeletter_section_button),
        onButtonClick = onGoClick,
    )
}

@Preview(showBackground = true)
@Composable
private fun TimeLetterSectionPreview() {
    AfternoteTheme {
        TimeLetterSection(totalCount = 30, onGoClick = {}, modifier = Modifier.padding(20.dp))
    }
}
