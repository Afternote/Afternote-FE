package com.afternote.feature.afternote.presentation.shared.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.button.AfternoteCircularCheckbox
import com.afternote.core.ui.button.CheckboxState
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.R

/**
 * 상세 화면 섹션 뼈대(Slot): [DetailSectionHeader] + 고정 헤더-카드 간격 + [DetailCard] 본문.
 *
 * 반복되는 레이아웃만 캡슐화하고, 카드 안 알맹이는 [content] 로 주입한다.
 */
@Composable
fun DetailSection(
    iconResId: Int,
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        DetailSectionHeader(
            iconResId = iconResId,
            label = label,
        )
        DetailCard {
            content()
        }
    }
}

@Composable
fun DetailSectionHeader(
    iconResId: Int,
    label: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(
            painter = painterResource(iconResId),
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = AfternoteDesign.colors.gray6,
        )
        Text(
            text = label,
            style = AfternoteDesign.typography.mono,
            color = AfternoteDesign.colors.gray6,
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = AfternoteDesign.colors.gray3,
        )
    }
}

@Composable
fun DetailCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .background(AfternoteDesign.colors.white)
                .border(
                    width = 1.dp,
                    color = AfternoteDesign.colors.gray2,
                    shape = RoundedCornerShape(6.dp),
                ).padding(16.dp),
    ) {
        content()
    }
}

/**
 * 상세 화면용 정보 행: 원형 배경 아이콘 + 라벨·값 + (선택) 우측 슬롯.
 *
 * 비밀번호 표시 토글 등 도메인 동작은 [trailingContent] 로만 주입한다.
 */
@Composable
fun DetailInfoRow(
    iconResId: Int,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
//            .height(IntrinsicSize.Min)
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .clip(CircleShape)
                    .size(32.dp)
                    .background(AfternoteDesign.colors.gray2),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(iconResId),
                contentDescription = null,
                tint = AfternoteDesign.colors.gray9,
                modifier = Modifier.size(16.dp),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = label,
                style = AfternoteDesign.typography.footnoteCaption,
                color = AfternoteDesign.colors.gray6,
            )
            Text(
                text = value,
                style = AfternoteDesign.typography.bodySmallB,
                color = AfternoteDesign.colors.gray9,
            )
        }
        if (trailingContent != null) {
            Spacer(modifier = Modifier.weight(1f))
            trailingContent()
        }
    }
}

/**
 * "처리방법" 섹션.
 *
 * [DetailSection] 슬롯 안에 체크박스 리스트만 둔다.
 * 섹션 아래 외부 간격은 호출하는 부모 레이아웃에서 둔다.
 * [methods] 가 비어 있으면 아무것도 렌더링하지 않는다.
 */
@Composable
fun ProcessingMethodsSection(
    methods: List<String>,
    modifier: Modifier = Modifier,
) {
    DetailSection(
        iconResId = com.afternote.core.ui.R.drawable.core_ui_settings,
        label = stringResource(R.string.feature_afternote_detail_section_processing),
        modifier = modifier,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(17.dp)) {
            methods.forEach { method ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    AfternoteCircularCheckbox(
                        state = CheckboxState.Default,
                        size = 20.dp,
                    )
                    Text(
                        text = method,
                        style = AfternoteDesign.typography.bodySmallR,
                        color = AfternoteDesign.colors.gray9,
                    )
                }
            }
        }
    }
}

/**
 * "남기신 말씀" 섹션.
 *
 * [DetailSection] 슬롯 안에 인용 부호·본문·로고 행만 둔다.
 * 섹션 아래 외부 간격은 호출하는 부모 레이아웃에서 둔다.
 * [message] 가 비어 있으면 "남기신 말씀이 없습니다." 플레이스홀더가 gray5 로 표시된다.
 */
@Composable
fun MessageSection(
    message: String,
    modifier: Modifier = Modifier,
) {
    DetailSection(
        iconResId = R.drawable.afternote_ui_ic_leave_message_header,
        label = stringResource(R.string.feature_afternote_detail_section_message),
        modifier = modifier,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.afternote_ui_ic_leave_message_card),
                contentDescription = null,
                tint = AfternoteDesign.colors.gray4,
                modifier = Modifier.size(15.dp),
            )
            Text(
                text = message,
                style = AfternoteDesign.typography.bodySmallR,
                color = AfternoteDesign.colors.gray8,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ProcessingMethodsSectionPreview() {
    AfternoteTheme {
        ProcessingMethodsSection(
            methods = listOf("계정 삭제", "게시물 유지", "메모리얼 계정 전환"),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MessageSectionPreview() {
    AfternoteTheme {
        MessageSection(
            message = "소중한 사람들에게 남기는 마지막 메시지입니다.",
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DetailSectionPreview() {
    AfternoteTheme {
        DetailSection(
            iconResId = com.afternote.core.ui.R.drawable.core_ui_settings,
            label = "섹션 헤더",
        ) {
            Text(
                text = "카드 내부 콘텐츠입니다.",
                style = AfternoteDesign.typography.bodySmallR,
                color = AfternoteDesign.colors.gray9,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DetailInfoRowPreview() {
    AfternoteTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DetailInfoRow(
                iconResId = com.afternote.core.ui.R.drawable.core_ui_ic_mail,
                label = "이메일",
                value = "afternote@example.com",
            )
            DetailInfoRow(
                iconResId = com.afternote.core.ui.R.drawable.core_ui_settings,
                label = "상태",
                value = "활성",
                trailingContent = {
                    AfternoteCircularCheckbox(
                        state = CheckboxState.Default,
                        size = 20.dp,
                    ) {}
                },
            )
        }
    }
}
