package com.afternote.feature.mindrecord.presentation.hometab

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.afternote.core.model.MindRecordCategory
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.feature.mindrecord.presentation.component.MemoriesCard
import com.afternote.feature.mindrecord.presentation.component.TodayQuestionCard
import com.afternote.feature.mindrecord.presentation.component.hometab.RecordCategoryCard
import com.afternote.feature.mindrecord.presentation.model.description
import com.afternote.feature.mindrecord.presentation.model.imageUrl
import com.afternote.feature.mindrecord.presentation.model.title

/**
 * 홈 탭([com.afternote.afternote_fe.screen.HomeTabScreen]) LazyColumn 안의 마인드레코드 구간.
 * `:app`은 셸·애프터노트/주간 등 다른 섹션만 담당하고, 마인드레코드 UI는 이 모듈에 둔다.
 */
fun LazyListScope.homeTabMindRecordQuestionAndCategories(
    categoryCounts: Map<MindRecordCategory, Int>,
    onAnswerClick: () -> Unit,
    onRecordCategoryClick: (MindRecordCategory) -> Unit,
) {
    item {
        TodayQuestionCard(
            onAnswerClick = onAnswerClick,
        )
        Spacer(modifier = Modifier.height(24.dp))
    }

    item {
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
    item {
        Column(
            modifier = Modifier.clickable(onClick = onMemoriesSectionClick),
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
            text = "MEMORIES",
            style = AfternoteDesign.typography.mono,
            color = AfternoteDesign.colors.black.copy(alpha = 0.4f),
        )
        HorizontalDivider(
            modifier = Modifier.padding(start = 12.dp),
            color = AfternoteDesign.colors.black.copy(alpha = 0.1f),
        )
    }
}
