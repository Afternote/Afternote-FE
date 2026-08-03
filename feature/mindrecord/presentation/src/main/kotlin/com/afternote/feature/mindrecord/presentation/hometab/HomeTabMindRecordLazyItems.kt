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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.model.MindRecordCategory
import com.afternote.core.ui.AfternoteSectionHeader
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.presentation.R.string.mindrecord_home_tab_memories_section_click_label
import com.afternote.feature.mindrecord.presentation.R.string.mindrecord_home_tab_memories_section_title
import com.afternote.feature.mindrecord.presentation.component.MemoriesCard
import com.afternote.feature.mindrecord.presentation.component.TodayQuestionCard
import com.afternote.feature.mindrecord.presentation.component.hometab.RecordCategoryCard
import com.afternote.feature.mindrecord.presentation.model.MindRecordCategoryUi
import com.afternote.feature.mindrecord.presentation.util.htmlToPlainText
import com.afternote.feature.mindrecord.presentation.viewmodel.MemoriesCardViewModel
import com.afternote.core.ui.R as CoreUiR

/**
 * `:app`은 셸·애프터노트/주간 등 다른 섹션만 담당하고, 마인드레코드 UI는 이 모듈에 둔다.
 *
 * @param dateText TODAY'S QUESTION 카드에 표시할 오늘 날짜 (yyyy.MM.dd).
 * @param questionText 실제 오늘의 질문 본문. 로딩 중이거나 조회에 실패하면 null.
 * @param isQuestionLoading 질문 조회 중이면 true. 조회 실패(null + false)와 구분하기 위해 함께 받는다.
 */
fun LazyListScope.homeTabMindRecordQuestionAndCategories(
    dateText: String,
    categoryCounts: Map<MindRecordCategory, Int>,
    onAnswerClick: () -> Unit,
    onRecordCategoryClick: (MindRecordCategory) -> Unit,
    questionText: String? = null,
    isQuestionLoading: Boolean = false,
    isCategoryCountLoading: Boolean = false,
) {
    item(key = "mind_record_question") {
        TodayQuestionCard(
            dateText = dateText,
            questionText = questionText,
            isQuestionLoading = isQuestionLoading,
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
                title = stringResource(MindRecordCategoryUi.Diary.titleRes),
                subtitle = stringResource(MindRecordCategoryUi.Diary.descriptionRes),
                totalCount = categoryCounts[MindRecordCategory.DIARY] ?: 0,
                onClick = { onRecordCategoryClick(MindRecordCategory.DIARY) },
                useDiaryIconLayout = true,
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

/**
 * `:app` 은 이 섹션을 배치만 하고 내용은 mindrecord 가 소유한다 — 그래서 카드 데이터도
 * 여기서 [MemoriesCardViewModel] 로 직접 받는다. 홈 ViewModel 을 거치면 마인드레코드
 * 기록 조회가 `:app` 으로 새어 나온다.
 */
@Composable
private fun HomeTabMindRecordMemoriesItem(
    onMemoriesSectionClick: () -> Unit,
    viewModel: MemoriesCardViewModel = hiltViewModel(),
) {
    val memoriesClickLabel = stringResource(mindrecord_home_tab_memories_section_click_label)
    val interactionSource = remember { MutableInteractionSource() }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 기록을 쓰고 홈으로 돌아오면 카드가 그 기록을 가리켜야 한다.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshOnReturn()
    }

    MemoriesSectionContent(
        onMemoriesSectionClick = onMemoriesSectionClick,
        question = uiState.question,
        // 본문은 에디터가 HTML 로 직렬화해 저장한다 — 카드에는 태그를 걷어 낸 미리보기만.
        answer = uiState.answer?.htmlToPlainText()?.takeIf { it.isNotBlank() },
        clickLabel = memoriesClickLabel,
        interactionSource = interactionSource,
    )
}

@Composable
private fun MemoriesSectionContent(
    onMemoriesSectionClick: () -> Unit,
    clickLabel: String,
    interactionSource: MutableInteractionSource,
    question: String? = null,
    answer: String? = null,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClickLabel = clickLabel,
                        role = Role.Button,
                        onClick = onMemoriesSectionClick,
                    ),
        ) {
            AfternoteSectionHeader(title = stringResource(mindrecord_home_tab_memories_section_title))
            Spacer(modifier = Modifier.height(12.dp))
            MemoriesCard(
                question = question,
                answer = answer,
                // 시안에 목적지 프로토타입이 없어 섹션과 같은 추억 공간으로 보낸다 (#559).
                onReadAgainClick = onMemoriesSectionClick,
            )
        }
    }
}

@Preview(showBackground = true, name = "오늘의 질문 + 기록 카테고리")
@Composable
private fun HomeTabMindRecordQuestionAndCategoriesPreview() {
    AfternoteTheme {
        LazyColumn {
            homeTabMindRecordQuestionAndCategories(
                dateText = "2026.04.10",
                questionText = "오늘 내가 배운\n가장 작은 교훈은 무엇인가요?",
                categoryCounts =
                    mapOf(
                        MindRecordCategory.DIARY to 18,
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
        MemoriesSectionContent(
            onMemoriesSectionClick = {},
            clickLabel = "",
            interactionSource = remember { MutableInteractionSource() },
            question = "내 인생에서 가장 소중했던 순간은?",
            answer = "아이가 태어났을 때...",
        )
    }
}
