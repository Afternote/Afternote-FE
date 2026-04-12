package com.afternote.afternote_fe.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.afternote_fe.R
import com.afternote.core.model.MindRecordCategory
import com.afternote.core.ui.button.AfternoteCircularCheckbox
import com.afternote.core.ui.button.CheckboxState
import com.afternote.core.ui.icon.RightArrowIcon
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.core.ui.topbar.HomeTopBar
import com.afternote.feature.mindrecord.presentation.hometab.homeTabMindRecordMemoriesSection
import com.afternote.feature.mindrecord.presentation.hometab.homeTabMindRecordQuestionAndCategories

@Immutable
data class HomeTabUiState(
    val userName: String = "",
    val isRecipientDesignated: Boolean = false,
    val categoryCounts: Map<MindRecordCategory, Int> =
        MindRecordCategory.entries.associateWith { 0 },
    val isLoading: Boolean = false,
    val isError: Boolean = false,
)

@Composable
fun HomeTabScreen(
    modifier: Modifier = Modifier,
    uiState: HomeTabUiState = HomeTabUiState(),
    onRecipientChipClick: () -> Unit = {},
    onAnswerClick: () -> Unit = {},
    onNextStepClick: () -> Unit = {},
    onRecordCategoryClick: (MindRecordCategory) -> Unit = {},
    onWeeklyImageClick: () -> Unit = {},
    onWeeklyCountClick: () -> Unit = {},
    onWeeklyRecentRecordClick: () -> Unit = {},
    onMemoriesSectionClick: () -> Unit = {},
    onSettingClick: () -> Unit = {},
) {
    Scaffold(
        modifier = modifier,
        topBar = { HomeTopBar(onSettingClick = onSettingClick) },
        containerColor = AfternoteDesign.colors.gray1,
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 20.dp),
        ) {
            // 1. 헤더 영역 (인사말 & 수신인 지정 칩)
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.home_tab_greeting, uiState.userName),
                    style = AfternoteDesign.typography.h1,
                )
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.home_tab_tagline),
                    style = AfternoteDesign.typography.captionLargeR,
                    color = AfternoteDesign.colors.gray5,
                )
                Spacer(modifier = Modifier.height(8.dp))

                RecipientDesignationChip(
                    isDesignated = uiState.isRecipientDesignated,
                    modifier = Modifier.padding(bottom = 32.dp),
                    onClick = onRecipientChipClick,
                )
            }

            homeTabMindRecordQuestionAndCategories(
                categoryCounts = uiState.categoryCounts,
                onAnswerClick = onAnswerClick,
                onRecordCategoryClick = onRecordCategoryClick,
            )

            // 4. AFTER NOTE NEXT STEP 섹션
            item {
                SectionHeader(title = stringResource(R.string.home_tab_next_step_section_title))
                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, AfternoteDesign.colors.gray2),
                    color = AfternoteDesign.colors.white,
                    onClick = onNextStepClick,
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.home_tab_next_step_body),
                            style = AfternoteDesign.typography.captionLargeR,
                            color = AfternoteDesign.colors.black,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(R.string.home_tab_next_step_cta),
                                style = AfternoteDesign.typography.captionLargeR,
                                color = AfternoteDesign.colors.gray5,
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            RightArrowIcon(
                                modifier = Modifier.size(12.dp),
                                tint = AfternoteDesign.colors.gray5,
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(40.dp))
            }

            // 5. 주간 기록 요약
            item {
                WeeklySummaryGrid(
                    onImageClick = onWeeklyImageClick,
                    onCountCardClick = onWeeklyCountClick,
                    onRecentRecordClick = onWeeklyRecentRecordClick,
                )
                Spacer(modifier = Modifier.height(40.dp))
            }

            homeTabMindRecordMemoriesSection(
                onMemoriesSectionClick = onMemoriesSectionClick,
            )
        }
    }
}

/**
 * 수신인 지정 상태 칩.
 *
 * 터치·리플은 [Surface]가 담당하고, 체크박스는 상태 표시만 한다 ([onClick] null).
 */
@Composable
private fun RecipientDesignationChip(
    isDesignated: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AfternoteDesign.colors
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isDesignated) colors.white else colors.gray2,
        modifier = modifier,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AfternoteCircularCheckbox(
                state =
                    if (isDesignated) {
                        CheckboxState.Default
                    } else {
                        CheckboxState.None
                    },
                onClick = null,
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text =
                    stringResource(
                        if (isDesignated) {
                            R.string.home_tab_recipient_designated
                        } else {
                            R.string.home_tab_recipient_not_designated
                        },
                    ),
                style = AfternoteDesign.typography.captionLargeB,
                color = colors.gray7,
            )
            Spacer(modifier = Modifier.width(4.dp))
            RightArrowIcon(
                modifier = Modifier.size(12.dp),
                tint = colors.gray5,
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = AfternoteDesign.typography.mono,
            color = AfternoteDesign.colors.black.copy(alpha = 0.4f),
        )
        HorizontalDivider(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
            color = AfternoteDesign.colors.black.copy(alpha = 0.1f),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun HomeTabScreenPreview() {
    AfternoteTheme {
        HomeTabScreen()
    }
}
