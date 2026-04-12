package com.afternote.afternote_fe.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.afternote_fe.R
import com.afternote.core.model.MindRecordCategory
import com.afternote.core.ui.AfternoteSectionHeader
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
        containerColor = Color.Transparent,
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
                    modifier = Modifier.padding(start = 4.dp),
                )
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.home_tab_tagline),
                    style = AfternoteDesign.typography.captionLargeR,
                    color = AfternoteDesign.colors.gray5,
                    modifier = Modifier.padding(start = 4.dp),
                )
                Spacer(modifier = Modifier.height(8.dp))

                RecipientDesignationChip(
                    isDesignated = uiState.isRecipientDesignated,
                    onClick = onRecipientChipClick,
                )

                Spacer(Modifier.height(32.dp))
            }

            homeTabMindRecordQuestionAndCategories(
                categoryCounts = uiState.categoryCounts,
                onAnswerClick = onAnswerClick,
                onRecordCategoryClick = onRecordCategoryClick,
            )

            // 4. AFTER NOTE NEXT STEP 섹션
            item {
                AfternoteSectionHeader(title = stringResource(R.string.home_tab_next_step_section_title))
                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, AfternoteDesign.colors.gray3),
                    color = AfternoteDesign.colors.white,
                    onClick = onNextStepClick,
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.home_tab_next_step_body),
                            style = AfternoteDesign.typography.inter,
                            color = AfternoteDesign.colors.gray8,
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.home_tab_next_step_cta),
                                style = AfternoteDesign.typography.captionLargeR,
                                color = AfternoteDesign.colors.gray6,
                            )
                            RightArrowIcon(
                                modifier = Modifier.size(width = 4.dp, height = 7.dp),
                                tint = AfternoteDesign.colors.gray6,
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
    Row(
        modifier =
            modifier
                // 1. 테두리 (Border)
                .border(
                    width = 1.dp,
                    color = AfternoteDesign.colors.gray3,
                    shape = RoundedCornerShape(20.dp),
                )
                // 2. 자르기 (Clip): Ripple 효과와 배경색이 모서리 밖으로 나가지 않게 가둠
                .clip(RoundedCornerShape(20.dp))
                // 3. 배경색 (Background)
                .background(color = if (isDesignated) AfternoteDesign.colors.white else AfternoteDesign.colors.gray2)
                // 4. 클릭 (Clickable): 접근성 Role.Button 추가
                .clickable(
                    role = Role.Button,
                    onClick = onClick,
                )
                // 5. 내부 여백 (Padding): 기존 Row에 있던 패딩을 가장 마지막에 배치
                .padding(vertical = 9.dp)
                .padding(start = 15.dp, end = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AfternoteCircularCheckbox(
            state = if (isDesignated) CheckboxState.Default else CheckboxState.None,
            onClick = null,
            size = 12.dp,
        )

        Spacer(modifier = Modifier.width(4.dp))

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
            color = AfternoteDesign.colors.gray9,
        )

        Spacer(modifier = Modifier.width(10.dp))

        RightArrowIcon(
            modifier = Modifier.size(12.dp),
            tint = AfternoteDesign.colors.gray5,
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
