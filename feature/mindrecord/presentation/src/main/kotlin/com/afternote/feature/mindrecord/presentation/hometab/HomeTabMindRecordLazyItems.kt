package com.afternote.feature.mindrecord.presentation.hometab

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.model.MindRecordCategory
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.presentation.R.string.mindrecord_home_tab_memories_section_click_label
import com.afternote.feature.mindrecord.presentation.R.string.mindrecord_home_tab_memories_section_title
import com.afternote.feature.mindrecord.presentation.component.MemoriesCard
import com.afternote.feature.mindrecord.presentation.component.TodayQuestionCard
import com.afternote.feature.mindrecord.presentation.component.hometab.RecordCategoryCard
import com.afternote.feature.mindrecord.presentation.model.description
import com.afternote.feature.mindrecord.presentation.model.imageUrl
import com.afternote.feature.mindrecord.presentation.model.title

/**
 * `:app`은 셸·애프터노트/주간 등 다른 섹션만 담당하고, 마인드레코드 UI는 이 모듈에 둔다.
 */
fun LazyListScope.homeTabMindRecordQuestionAndCategories(
    categoryCounts: Map<MindRecordCategory, Int>,
    onAnswerClick: () -> Unit,
    onRecordCategoryClick: (MindRecordCategory) -> Unit,
) {
    item(key = "mind_record_question") {
        TodayQuestionCard(
            onAnswerClick = onAnswerClick,
        )
        Spacer(modifier = Modifier.height(24.dp))
    }

    item(key = "mind_record_categories") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MindRecordCategory.entries
                .filter { it != MindRecordCategory.DAILY_QUESTION }
                .forEach { category ->
                    RecordCategoryCard(
                        modifier =
                            Modifier
                                .weight(1f)
                                .aspectRatio(1.1f),
                        iconResId = category.imageUrl,
                        title = category.title,
                        subtitle = category.description,
                        totalCount = categoryCounts[category] ?: 0,
                        onClick = { onRecordCategoryClick(category) },
                    )
                }
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
            HomeTabMemoriesSectionHeader()
            Spacer(modifier = Modifier.height(16.dp))
            MemoriesCard()
        }
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
private fun HomeTabMemoriesSectionHeader(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(mindrecord_home_tab_memories_section_title),
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

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF, name = "MEMORIES 섹션 + 카드")
@Composable
private fun HomeTabMindRecordMemoriesItemPreview() {
    AfternoteTheme {
        HomeTabMindRecordMemoriesItem(onMemoriesSectionClick = {})
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF, name = "오늘의 질문 + 기록 카테고리")
@Composable
private fun HomeTabMindRecordQuestionAndCategoriesPreview() {
    AfternoteTheme {
        LazyColumn(
            modifier =
                Modifier
                    .width(360.dp)
                    .padding(horizontal = 20.dp),
        ) {
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
