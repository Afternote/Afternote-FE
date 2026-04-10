package com.afternote.afternote_fe.screen

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.icon.AfternoteCircularCheckbox
import com.afternote.core.ui.icon.AfternoteCircularCheckboxState
import com.afternote.core.ui.icon.RightArrowIcon
import com.afternote.core.ui.scaffold.topbar.HomeTopBar
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.presentation.component.MemoriesCard
import com.afternote.feature.mindrecord.presentation.component.TodayQuestionCard
import com.afternote.feature.mindrecord.presentation.model.MindRecordCategory

@Immutable
data class HomeTabUiState(
    val userName: String = "박서연",
    val isRecipientDesignated: Boolean = false,
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
) {
    Scaffold(
        modifier = modifier,
        topBar = { HomeTopBar() },
        containerColor = Color.Transparent,
    ) { paddingValues ->
        LazyColumn(
            modifier =
                Modifier
                    .padding(paddingValues)
                    .padding(horizontal = 24.dp),
        ) {
            // 1. 헤더 영역 (인사말 & 수신인 지정 칩)
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "안녕하세요,\n${uiState.userName}님",
                    style = AfternoteDesign.typography.h1,
                )
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "오늘도 당신의 하루를 차분히 기록해보세요.",
                    style = AfternoteDesign.typography.captionLargeR,
                    color = AfternoteDesign.colors.gray5,
                )
                Spacer(modifier = Modifier.height(16.dp))

                RecipientDesignationChip(
                    isDesignated = uiState.isRecipientDesignated,
                    modifier = Modifier.padding(bottom = 32.dp),
                    onClick = onRecipientChipClick,
                )
            }

            // 2. 오늘의 질문 카드
            item {
                TodayQuestionCard(
                    onAnswerClick = onAnswerClick,
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            // 3. 기록 카테고리 (일기, 깊은 생각)
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
                                totalCount = 18, // TODO: ViewModel UiState에서 전달
                                onClick = { onRecordCategoryClick(category) },
                            )
                        }
                }
                Spacer(modifier = Modifier.height(40.dp))
            }

            // 4. AFTER NOTE NEXT STEP 섹션
            item {
                SectionHeader(title = "AFTER NOTE NEXT STEP")
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
                            text = "가족들의 '주거래 은행' 정보를\n입력하신 건 확인하셨나요?",
                            style = AfternoteDesign.typography.captionLargeR,
                            color = AfternoteDesign.colors.black,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "애프터노트 입력하러가기",
                                style = AfternoteDesign.typography.captionLargeR,
                                color = AfternoteDesign.colors.gray5,
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            RightArrowIcon(
                                tint = AfternoteDesign.colors.gray5,
                                size = 12.dp,
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

            // 6. MEMORIES 섹션
            item {
                SectionHeader(title = "MEMORIES")
                Spacer(modifier = Modifier.height(16.dp))
                MemoriesCard()
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

/**
 * 수신인 지정 상태 칩.
 * - 미지정: 배경 [AfternoteDesign.colors.gray2], 체크박스 미체크
 * - 지정 완료: 배경 [AfternoteDesign.colors.white], 체크박스 체크
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
                        AfternoteCircularCheckboxState.Checked
                    } else {
                        AfternoteCircularCheckboxState.Unchecked
                    },
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text =
                    if (isDesignated) {
                        "수신인 지정 완료"
                    } else {
                        "수신인 지정 미완료"
                    },
                style = AfternoteDesign.typography.captionLargeB,
                color = colors.gray7,
            )
            Spacer(modifier = Modifier.width(4.dp))
            RightArrowIcon(
                tint = colors.gray5,
                size = 12.dp,
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
            modifier = Modifier.padding(start = 12.dp),
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
