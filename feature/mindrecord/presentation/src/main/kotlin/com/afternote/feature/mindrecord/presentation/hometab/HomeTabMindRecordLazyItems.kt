package com.afternote.feature.mindrecord.presentation.hometab

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.model.MindRecordCategory
import com.afternote.core.ui.AfternoteSectionHeader
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.presentation.R.string.mindrecord_home_tab_memories_section_click_label
import com.afternote.feature.mindrecord.presentation.R.string.mindrecord_home_tab_memories_section_title
import com.afternote.feature.mindrecord.presentation.component.MemoriesCard
import com.afternote.feature.mindrecord.presentation.component.TodayQuestionCard
import com.afternote.feature.mindrecord.presentation.component.hometab.RecordCategoryCard
import com.afternote.feature.mindrecord.presentation.model.MindRecordCategoryUi
import com.afternote.core.ui.R as CoreUiR

/**
 * `:app`은 셸·애프터노트/주간 등 다른 섹션만 담당하고, 마인드레코드 UI는 이 모듈에 둔다.
 */
fun LazyListScope.homeTabMindRecordQuestionAndCategories(
    categoryCounts: Map<MindRecordCategory, Int>,
    onAnswerClick: () -> Unit,
    onRecordCategoryClick: (MindRecordCategory) -> Unit,
    isCategoryCountLoading: Boolean = false,
) {
    item(key = "mind_record_question") {
        TodayQuestionCard(
            onAnswerClick = onAnswerClick,
        )
        Spacer(modifier = Modifier.height(8.dp))
    }

    item(key = "mind_record_categories") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RecordCategoryCard(
                modifier =
                    Modifier.weight(1f),
                iconResId = CoreUiR.drawable.core_ui_ic_diary,
                title = MindRecordCategoryUi.Diary.title,
                subtitle = MindRecordCategoryUi.Diary.description,
                totalCount = categoryCounts[MindRecordCategory.DIARY] ?: 0,
                onClick = { onRecordCategoryClick(MindRecordCategory.DIARY) },
                useDiaryIconLayout = true,
                isCountLoading = isCategoryCountLoading,
            )
            RecordCategoryCard(
                modifier =
                    Modifier.weight(1f),
                iconResId = CoreUiR.drawable.core_ui_ic_deep_thought,
                title = MindRecordCategoryUi.DeepThought.title,
                subtitle = MindRecordCategoryUi.DeepThought.description,
                totalCount = categoryCounts[MindRecordCategory.DEEP_THOUGHT] ?: 0,
                onClick = { onRecordCategoryClick(MindRecordCategory.DEEP_THOUGHT) },
                isCountLoading = isCategoryCountLoading,
            )
        }
        Spacer(modifier = Modifier.height(40.dp))
    }
}

fun LazyListScope.homeTabMindRecordMemoriesSection(onMemoriesSectionClick: () -> Unit) {
    item(key = "mind_record_memories") {
        HomeTabMindRecordMemoriesItem(onMemoriesSectionClick = onMemoriesSectionClick)
    }
}

@Composable
private fun HomeTabMindRecordMemoriesItem(onMemoriesSectionClick: () -> Unit) {
    val memoriesClickLabel = stringResource(mindrecord_home_tab_memories_section_click_label)
    val interactionSource = remember { MutableInteractionSource() }
    Column(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClickLabel = memoriesClickLabel,
                        role = Role.Button,
                        onClick = onMemoriesSectionClick,
                    ),
        ) {
            AfternoteSectionHeader(title = stringResource(mindrecord_home_tab_memories_section_title))
            Spacer(modifier = Modifier.height(12.dp))
            MemoriesCard()
        }
    }
}

@Preview(showBackground = true, name = "오늘의 질문 + 기록 카테고리")
@Composable
private fun HomeTabMindRecordQuestionAndCategoriesPreview() {
    AfternoteTheme {
        LazyColumn {
            homeTabMindRecordQuestionAndCategories(
                categoryCounts =
                    mapOf(
                        MindRecordCategory.DIARY to 18,
                        MindRecordCategory.DEEP_THOUGHT to 7,
                    ),
                onAnswerClick = {},
                onRecordCategoryClick = {},
            )
        }
    }
}

@Preview(showBackground = true, name = "MEMORIES 섹션 + 카드")
@Composable
private fun HomeTabMindRecordMemoriesItemPreview() {
    AfternoteTheme {
        HomeTabMindRecordMemoriesItem(onMemoriesSectionClick = {})
    }
}
