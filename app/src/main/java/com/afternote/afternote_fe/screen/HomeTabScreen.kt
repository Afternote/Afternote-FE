package com.afternote.afternote_fe.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.afternote_fe.R
import com.afternote.core.model.MindRecordCategory
import com.afternote.core.ui.AfternoteOutlinedCard
import com.afternote.core.ui.AfternoteSectionHeader
import com.afternote.core.ui.badge.RecipientDesignationBadge
import com.afternote.core.ui.badge.RecipientDesignationBadgeState
import com.afternote.core.ui.icon.RightArrowIcon
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.core.ui.topbar.HomeTopBar
import com.afternote.feature.mindrecord.presentation.hometab.homeTabMindRecordMemoriesSection
import com.afternote.feature.mindrecord.presentation.hometab.homeTabMindRecordQuestionAndCategories

sealed interface HomeTabUiState {
    data object Loading : HomeTabUiState

    @Immutable
    data class Success(
        val userName: String,
        val isRecipientDesignated: Boolean,
        val categoryCounts: Map<MindRecordCategory, Int>,
        val isRefreshing: Boolean = false,
    ) : HomeTabUiState

    data class Error(
        val throwable: Throwable,
    ) : HomeTabUiState
}

/** 홈 탭에서 발생하는 사용자 이벤트를 한곳에 모은다. */
interface HomeTabActions {
    fun onRecipientChipClick()

    fun onAnswerClick()

    fun onNextStepClick()

    fun onRecordCategoryClick(category: MindRecordCategory)

    fun onWeeklyImageClick()

    fun onWeeklyCountClick()

    fun onWeeklyRecentRecordClick()

    fun onMemoriesSectionClick()

    fun onSettingClick()

    fun onRetryLoad()
}

private object HomeTabActionsNoop : HomeTabActions {
    override fun onRecipientChipClick() {}

    override fun onAnswerClick() {}

    override fun onNextStepClick() {}

    override fun onRecordCategoryClick(category: MindRecordCategory) {}

    override fun onWeeklyImageClick() {}

    override fun onWeeklyCountClick() {}

    override fun onWeeklyRecentRecordClick() {}

    override fun onMemoriesSectionClick() {}

    override fun onSettingClick() {}

    override fun onRetryLoad() {}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTabScreen(
    modifier: Modifier = Modifier,
    uiState: HomeTabUiState = HomeTabUiState.Loading,
    actions: HomeTabActions = HomeTabActionsNoop,
) {
    Scaffold(
        modifier = modifier,
        topBar = { HomeTopBar(onSettingClick = actions::onSettingClick) },
        containerColor = Color.Transparent,
    ) { paddingValues ->
        when (uiState) {
            is HomeTabUiState.Loading -> {
                HomeTabScrollContent(
                    paddingValues = paddingValues,
                    userName = "\u2026",
                    isRecipientDesignated = false,
                    categoryCounts = MindRecordCategory.entries.associateWith { 0 },
                    categoryCountsLoading = true,
                    actions = actions,
                )
            }

            is HomeTabUiState.Success -> {
                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = actions::onRetryLoad,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                ) {
                    HomeTabScrollContent(
                        paddingValues = PaddingValues(0.dp),
                        userName = uiState.userName,
                        isRecipientDesignated = uiState.isRecipientDesignated,
                        categoryCounts = uiState.categoryCounts,
                        categoryCountsLoading = false,
                        actions = actions,
                    )
                }
            }

            is HomeTabUiState.Error -> {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.home_tab_error_message),
                            style = AfternoteDesign.typography.bodySmallR,
                            color = AfternoteDesign.colors.gray7,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        TextButton(onClick = actions::onRetryLoad) {
                            Text(
                                text = stringResource(R.string.home_tab_retry),
                                style = AfternoteDesign.typography.captionLargeB,
                                color = AfternoteDesign.colors.gray9,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeTabScrollContent(
    paddingValues: PaddingValues,
    userName: String,
    isRecipientDesignated: Boolean,
    categoryCounts: Map<MindRecordCategory, Int>,
    categoryCountsLoading: Boolean,
    actions: HomeTabActions,
) {
    LazyColumn(
        modifier = Modifier.padding(paddingValues),
        contentPadding = PaddingValues(horizontal = 20.dp),
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.home_tab_greeting, userName),
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

            RecipientDesignationBadge(
                state =
                    if (isRecipientDesignated) {
                        RecipientDesignationBadgeState.Completed
                    } else {
                        RecipientDesignationBadgeState.Incomplete(
                            onClick = actions::onRecipientChipClick,
                        )
                    },
            )

            Spacer(Modifier.height(32.dp))
        }

        homeTabMindRecordQuestionAndCategories(
            categoryCounts = categoryCounts,
            onAnswerClick = actions::onAnswerClick,
            onRecordCategoryClick = actions::onRecordCategoryClick,
            isCategoryCountLoading = categoryCountsLoading,
        )

        item {
            AfternoteSectionHeader(title = stringResource(R.string.home_tab_next_step_section_title))
            Spacer(modifier = Modifier.height(12.dp))

            AfternoteOutlinedCard(onClick = actions::onNextStepClick) {
                Column {
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

        item {
            WeeklySummaryGrid(
                onImageClick = actions::onWeeklyImageClick,
                onCountCardClick = actions::onWeeklyCountClick,
                onRecentRecordClick = actions::onWeeklyRecentRecordClick,
            )
            Spacer(modifier = Modifier.height(40.dp))
        }

        homeTabMindRecordMemoriesSection(
            onMemoriesSectionClick = actions::onMemoriesSectionClick,
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
