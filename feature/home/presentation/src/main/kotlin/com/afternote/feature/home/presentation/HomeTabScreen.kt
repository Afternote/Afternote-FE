package com.afternote.feature.home.presentation

import androidx.compose.foundation.layout.Arrangement
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
import com.afternote.core.ui.AfternoteOutlinedCard
import com.afternote.core.ui.AfternoteSectionHeader
import com.afternote.core.ui.badge.RecipientDesignationBadge
import com.afternote.core.ui.badge.RecipientDesignationBadgeState
import com.afternote.core.ui.icon.RightArrowIcon
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.core.ui.topbar.HomeTopBar
import com.afternote.feature.mindrecord.presentation.hometab.homeTabMindRecordMemoriesSection
import com.afternote.feature.mindrecord.presentation.hometab.homeTabMindRecordTodayQuestion
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** TODAY'S QUESTION 카드 날짜 표기 포맷. */
private val homeTabDateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")

sealed interface HomeTabUiState {
    /** 당겨서 새로고침 인디케이터를 띄운 채로 둘지. */
    val showsRefreshIndicator: Boolean

    /**
     * @property cachedUserName 마지막 성공 응답에서 디스크 캐시된 이름. 콜드스타트 시 GET /users/me
     * 응답이 도착하기 전 placeholder로 즉시 노출하기 위해 사용한다. 신규 로그인 등 캐시가 없으면 null.
     * @property showsRefreshIndicator 당겨서 새로고침·재시도로 진입한 로딩이면 true. Success 가 아닌
     * 상태에서 당기면 이 상태로 넘어오는데, 여기서 인디케이터를 내리면 당긴 직후 사라져 요청이 씹힌
     * 것처럼 보인다. 최초 진입은 사용자가 당긴 적이 없으므로 false.
     */
    data class Loading(
        val cachedUserName: String? = null,
        override val showsRefreshIndicator: Boolean = false,
    ) : HomeTabUiState

    @Immutable
    data class Success(
        val userName: String,
        val isRecipientDesignated: Boolean,
        val isRefreshing: Boolean = false,
        /** 오늘의 질문 본문. 조회 실패 시 null — 카드가 중립 문구를 표시한다. */
        val todayQuestionContent: String? = null,
        /** 이번 주 기록 수. 조회 실패 시 null — 그리드가 «–» 를 표시한다 (#562). */
        val weeklyRecordCount: Int? = null,
    ) : HomeTabUiState {
        override val showsRefreshIndicator: Boolean get() = isRefreshing
    }

    data class Error(
        val throwable: Throwable,
    ) : HomeTabUiState {
        override val showsRefreshIndicator: Boolean = false
    }
}

/** 홈 탭에서 발생하는 사용자 이벤트를 한곳에 모은다. */
interface HomeTabActions {
    fun onRecipientChipClick()

    fun onAnswerClick()

    fun onNextStepClick()

    /** 타임레터 NEXT STEP 카드 — 2026-08-09 확정 문구의 목적지 (#700). */
    fun onTimeLetterNextStepClick()

    fun onWeeklyImageClick()

    fun onWeeklyCountClick()

    fun onMemoriesSectionClick()

    /** MEMORIES 카드의 「그날의 기록 다시 읽기」 — 카드가 가리키는 그 기록의 상세 (#793). */
    fun onMemoriesRecordDetailClick(recordId: Long)

    fun onSettingClick()

    fun onRetryLoad()
}

private object HomeTabActionsNoop : HomeTabActions {
    override fun onRecipientChipClick() {}

    override fun onAnswerClick() {}

    override fun onNextStepClick() {}

    override fun onTimeLetterNextStepClick() {}

    override fun onWeeklyImageClick() {}

    override fun onWeeklyCountClick() {}

    override fun onMemoriesSectionClick() {}

    override fun onMemoriesRecordDetailClick(recordId: Long) {}

    override fun onSettingClick() {}

    override fun onRetryLoad() {}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTabScreen(
    modifier: Modifier = Modifier,
    uiState: HomeTabUiState = HomeTabUiState.Loading(),
    actions: HomeTabActions = HomeTabActionsNoop,
    // 프리뷰·스크린샷 테스트가 고정 날짜를 주입할 수 있도록 파라미터로 노출한다.
    todayDateText: String = LocalDate.now().format(homeTabDateFormatter),
) {
    Scaffold(
        modifier = modifier,
        topBar = { HomeTopBar(onSettingClick = actions::onSettingClick) },
        containerColor = Color.Transparent,
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = uiState.showsRefreshIndicator,
            onRefresh = actions::onRetryLoad,
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
        ) {
            when (uiState) {
                is HomeTabUiState.Loading -> {
                    HomeTabScrollContent(
                        userName = uiState.cachedUserName ?: "\u2026",
                        // 조회 전이다 — 지정 여부를 결과로 확정하지 않는다 (#698).
                        recipientBadgeState = RecipientDesignationBadgeState.Unknown,
                        // 조회 전에는 아는 값이 없다 — 0 을 채워 넣지 않는다 (#700).
                        todayDateText = todayDateText,
                        todayQuestionContent = null,
                        isQuestionLoading = true,
                        // 조회 전이다 — 0 을 넣으면 «이번 주 기록 없음» 을 확정한다 (#562).
                        weeklyRecordCount = null,
                        actions = actions,
                    )
                }

                is HomeTabUiState.Success -> {
                    HomeTabScrollContent(
                        userName = uiState.userName,
                        recipientBadgeState =
                            if (uiState.isRecipientDesignated) {
                                RecipientDesignationBadgeState.Completed
                            } else {
                                RecipientDesignationBadgeState.Incomplete(onClick = actions::onRecipientChipClick)
                            },
                        todayDateText = todayDateText,
                        todayQuestionContent = uiState.todayQuestionContent,
                        isQuestionLoading = false,
                        weeklyRecordCount = uiState.weeklyRecordCount,
                        actions = actions,
                    )
                }

                is HomeTabUiState.Error -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        item {
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
    }
}

@Composable
private fun HomeTabScrollContent(
    userName: String,
    /**
     * 수신인 지정 배지 상태.
     *
     * `Boolean?` 로 좁혔다 다시 펼치지 않는다 — 「null = 미결정」이 주석으로만 유지되는
     * 약속이 되고, 「널+폴백 대신 값으로 명시」(#934) 와도 어긋난다 (#698 리뷰).
     */
    recipientBadgeState: RecipientDesignationBadgeState,
    todayDateText: String,
    todayQuestionContent: String?,
    isQuestionLoading: Boolean,
    /** null 이면 아직 모름 — 그리드가 «–» 를 그린다 (#562). */
    weeklyRecordCount: Int?,
    actions: HomeTabActions,
) {
    LazyColumn(
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

            RecipientDesignationBadge(state = recipientBadgeState)

            Spacer(Modifier.height(32.dp))
        }

        homeTabMindRecordTodayQuestion(
            dateText = todayDateText,
            questionText = todayQuestionContent,
            isQuestionLoading = isQuestionLoading,
            onAnswerClick = actions::onAnswerClick,
        )

        // 정본(4327:99103)의 순서는 TODAY'S QUESTION → 타임레터 → AFTER NOTE NEXT STEP 이다.
        // 문구는 2026-08-09 디자이너 확정분 (#700).
        item {
            NextStepCard(
                sectionTitle = stringResource(R.string.home_tab_timeletter_next_step_section_title),
                body = stringResource(R.string.home_tab_timeletter_next_step_body),
                cta = stringResource(R.string.home_tab_timeletter_next_step_cta),
                onClick = actions::onTimeLetterNextStepClick,
            )
            Spacer(modifier = Modifier.height(40.dp))
        }

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
                // base 가 비워 둔 자리(#207 리뷰)에 실값을 붙인다 — 조회 실패는 null 로 남아
                // 대시로 그려진다 (#562).
                recordedCount = weeklyRecordCount,
                onImageClick = actions::onWeeklyImageClick,
                onCountCardClick = actions::onWeeklyCountClick,
            )
            Spacer(modifier = Modifier.height(40.dp))
        }

        homeTabMindRecordMemoriesSection(
            onMemoriesSectionClick = actions::onMemoriesSectionClick,
            onRecordDetailClick = actions::onMemoriesRecordDetailClick,
        )
    }
}

/**
 * NEXT STEP 카드 한 장. 타임레터·애프터노트가 같은 모양을 쓰므로 문구와 목적지만 받는다.
 */
@Composable
private fun NextStepCard(
    sectionTitle: String,
    body: String,
    cta: String,
    onClick: () -> Unit,
) {
    AfternoteSectionHeader(title = sectionTitle)
    Spacer(modifier = Modifier.height(12.dp))
    AfternoteOutlinedCard(onClick = onClick) {
        Column {
            Text(
                text = body,
                style = AfternoteDesign.typography.inter,
                color = AfternoteDesign.colors.gray8,
            )
            Spacer(modifier = Modifier.height(18.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = cta,
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
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun HomeTabScreenPreview() {
    AfternoteTheme {
        HomeTabScreen(todayDateText = "2026.04.10")
    }
}
