package com.afternote.feature.mindrecord.presentation.hometab

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
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
import com.afternote.core.ui.AfternoteSectionHeader
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.presentation.R.string.mindrecord_home_tab_memories_section_click_label
import com.afternote.feature.mindrecord.presentation.R.string.mindrecord_home_tab_memories_section_title
import com.afternote.feature.mindrecord.presentation.component.MemoriesCard
import com.afternote.feature.mindrecord.presentation.component.TodayQuestionCard
import com.afternote.feature.mindrecord.presentation.util.htmlToPlainText
import com.afternote.feature.mindrecord.presentation.viewmodel.MemoriesCardViewModel

/**
 * `:app`은 셸·애프터노트/주간 등 다른 섹션만 담당하고, 마인드레코드 UI는 이 모듈에 둔다.
 *
 * **기록 카테고리 카드는 시안에 없다.** 정본(정리 Screen Design 페이지의 「홈」 섹션, 프레임
 * 3종)에는 `일기`·`깊은 생각` 카드 행 자체가 없고, 앱에 남아 있던 일기 카드 하나는 백업
 * 페이지 시안에서 이어진 잔재였다. 그래서 이 함수는 TODAY'S QUESTION 카드만 놓는다 (#700).
 *
 * @param dateText TODAY'S QUESTION 카드에 표시할 오늘 날짜 (yyyy.MM.dd).
 * @param questionText 실제 오늘의 질문 본문. 로딩 중이거나 조회에 실패하면 null.
 * @param isQuestionLoading 질문 조회 중이면 true. 조회 실패(null + false)와 구분하기 위해 함께 받는다.
 */
fun LazyListScope.homeTabMindRecordTodayQuestion(
    dateText: String,
    onAnswerClick: () -> Unit,
    questionText: String? = null,
    isQuestionLoading: Boolean = false,
) {
    item(key = "mind_record_question") {
        TodayQuestionCard(
            dateText = dateText,
            questionText = questionText,
            isQuestionLoading = isQuestionLoading,
            onAnswerClick = onAnswerClick,
        )
        Spacer(modifier = Modifier.height(40.dp))
    }
}

/**
 * @param onMemoriesSectionClick 카드·섹션을 누르면 갈 곳(추억 공간).
 * @param onRecordDetailClick 「그날의 기록 다시 읽기」가 열 **그 기록의 상세** (#793).
 */
fun LazyListScope.homeTabMindRecordMemoriesSection(
    onMemoriesSectionClick: () -> Unit,
    onRecordDetailClick: (recordId: Long) -> Unit,
) {
    item(key = "mind_record_memories") {
        HomeTabMindRecordMemoriesItem(
            onMemoriesSectionClick = onMemoriesSectionClick,
            onRecordDetailClick = onRecordDetailClick,
        )
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
    onRecordDetailClick: (recordId: Long) -> Unit,
    viewModel: MemoriesCardViewModel = hiltViewModel(),
) {
    val memoriesClickLabel = stringResource(mindrecord_home_tab_memories_section_click_label)
    val interactionSource = remember { MutableInteractionSource() }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 기록을 쓰고 홈으로 돌아오면 카드가 그 기록을 가리켜야 한다.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshOnReturn()
    }

    val recordId = uiState.recordId

    MemoriesSectionContent(
        onMemoriesSectionClick = onMemoriesSectionClick,
        // 「그날의 기록 다시 읽기」는 **카드가 가리키는 그 기록**으로 간다 (#793).
        // 0건이면 `null` 이라 버튼 자체가 안 그려진다 — 다른 곳으로 보내면 문구가 약속한
        // 「그날의 기록」이 아니게 된다 (리뷰 지적).
        onReadAgainClick = recordId?.let { id -> { onRecordDetailClick(id) } },
        question = uiState.question,
        // 본문은 에디터가 HTML 로 직렬화해 저장한다 — 카드에는 태그를 걷어 낸 미리보기만.
        answer = uiState.answer?.htmlToPlainText()?.takeIf { it.isNotBlank() },
        clickLabel = memoriesClickLabel,
        interactionSource = interactionSource,
    )
}

@VisibleForTesting
@Composable
internal fun MemoriesSectionContent(
    onMemoriesSectionClick: () -> Unit,
    onReadAgainClick: (() -> Unit)?,
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
            // 버튼 문구가 「**그날의** 기록 다시 읽기」라 목적지는 카드가 보여 주는 그 한 건의
            // 상세다 (#793). 카드·섹션 전체는 종전대로 추억 공간으로 간다 — 「그날」이 없는
            // 넓은 자리라 목록 성격의 목적지가 맞는다.
            MemoriesCard(
                question = question,
                answer = answer,
                onReadAgainClick = onReadAgainClick,
            )
        }
    }
}

@Preview(showBackground = true, name = "오늘의 질문")
@Composable
private fun HomeTabMindRecordTodayQuestionPreview() {
    AfternoteTheme {
        LazyColumn {
            homeTabMindRecordTodayQuestion(
                dateText = "2026.04.10",
                questionText = "오늘 내가 배운\n가장 작은 교훈은 무엇인가요?",
                onAnswerClick = {},
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
            onReadAgainClick = {},
            clickLabel = "",
            interactionSource = remember { MutableInteractionSource() },
            question = "내 인생에서 가장 소중했던 순간은?",
            answer = "아이가 태어났을 때...",
        )
    }
}
