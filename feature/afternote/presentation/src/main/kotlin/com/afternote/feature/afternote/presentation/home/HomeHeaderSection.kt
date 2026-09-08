package com.afternote.feature.afternote.presentation.home

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.AfternoteOutlinedCard
import com.afternote.core.ui.AfternoteSectionHeader
import com.afternote.core.ui.icon.RightArrowIcon
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.feature.afternote.presentation.R

/**
 * NEXT STEP 카드에 필요한 값 묶음.
 *
 * 문구와 동작을 함께 받아 «카드는 떴는데 눌러도 아무 일 없는» 상태를 만들 수 없게 한다.
 * 카드를 띄우지 않는 화면(수신자 애프터노트 목록)은 null 을 넘긴다 (#777).
 */
@Immutable
data class NextStep(
    val text: String,
    val onClick: () -> Unit,
)

/**
 * 헤더 위 여백. 목록 상태([com.afternote.feature.afternote.presentation.shared.component.InfiniteListBody])와
 * 빈 목록 상태([EmptyHomeBody])가 헤더를 **같은 자리**에 두도록 한 곳에서 정의한다 (#1175).
 */
internal val HomeBodyTopSpacing = 8.dp

/** 헤더와 그 아래 본문 사이 여백. [HomeBodyTopSpacing] 과 같은 이유로 공유한다 (#1175). */
internal val HomeBodySectionSpacing = 16.dp

/**
 * 애프터노트 목록 상단 헤더.
 *
 * @param description 제목 아래 한 줄. 이 목록은 작성자와 수신자가 같은 화면을 공유하므로 기본값을
 *   두지 않는다 — 발신자 문구("남길 기록을 정리해 보세요")가 수신자에게 그대로 새던 것이 #620 이다.
 *   [nextStep] 이 디폴트를 걷은 것(#777)과 같은 이유다.
 */
@Composable
internal fun HomeHeaderSection(
    description: String,
    nextStep: NextStep?,
    modifier: Modifier = Modifier,
    onDraftListClick: (() -> Unit)? = null,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 25.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.afternote_home_title),
                style = AfternoteDesign.typography.h1,
                color = AfternoteDesign.colors.gray9,
                modifier = Modifier.weight(1f),
            )
            // 임시저장은 작성자에게만 있다 — 같은 화면을 쓰는 수신자 목록은 null 로 안 그린다(#620 과 같은 이유).
            if (onDraftListClick != null) {
                Text(
                    text = stringResource(R.string.afternote_home_draft_entry),
                    style = AfternoteDesign.typography.bodySmallR,
                    color = AfternoteDesign.colors.gray7,
                    modifier =
                        Modifier
                            .clickable(
                                onClickLabel = stringResource(R.string.afternote_home_draft_entry_description),
                                role = Role.Button,
                                onClick = onDraftListClick,
                            ),
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = description,
            style = AfternoteDesign.typography.captionLargeR,
            color = AfternoteDesign.colors.black.copy(alpha = 89f / 255f),
        )
        if (nextStep != null) {
            Spacer(modifier = Modifier.height(16.dp))
            NextStepCard(
                text = nextStep.text,
                onClick = nextStep.onClick,
            )
        }
    }
}

@Composable
private fun NextStepCard(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        AfternoteSectionHeader(title = stringResource(R.string.afternote_home_next_step_section_title))
        AfternoteOutlinedCard(
            onClick = onClick,
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = text,
                    style = AfternoteDesign.typography.inter,
                    color = AfternoteDesign.colors.gray9,
                    modifier = Modifier.weight(1f),
                )
                RightArrowIcon(
                    modifier = Modifier.size(width = 4.dp, height = 7.dp),
                    tint = AfternoteDesign.colors.gray6,
                )
            }
        }
    }
}
