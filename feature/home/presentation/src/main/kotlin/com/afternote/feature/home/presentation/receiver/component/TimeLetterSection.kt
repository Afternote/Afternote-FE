package com.afternote.feature.home.presentation.receiver.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.afternote.feature.home.presentation.R

/** 타임레터 섹션 — 단순 카드. */
@Composable
fun TimeLetterSection(
    totalCount: Int?,
    onGoClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    HomeSectionCard(
        modifier = modifier,
        title = stringResource(R.string.home_receiver_timeletter_section_title),
        description = stringResource(R.string.home_receiver_timeletter_section_desc),
        countLine =
            rememberCountLine(
                prefix =
                    stringResource(R.string.home_receiver_timeletter_count_prefix, countText(totalCount)),
                suffix = stringResource(R.string.home_receiver_timeletter_count_suffix),
            ),
        buttonText = stringResource(R.string.home_receiver_timeletter_section_button),
        onButtonClick = onGoClick,
    )
}
