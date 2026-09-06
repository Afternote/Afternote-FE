package com.afternote.feature.timeletter.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.feature.timeletter.presentation.R
import com.afternote.core.ui.R as CoreUiR

@Composable
fun TimeLetterBottomBar(
    draftCount: Int,
    textAlign: TextAlign,
    onMediaAddClick: () -> Unit,
    onTextStyleClick: () -> Unit,
    onAlignCenterClick: () -> Unit,
    onAlignLeftClick: () -> Unit,
    onAlignRightClick: () -> Unit,
    onDraftClick: () -> Unit,
    onDraftCountClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(56.dp)
                .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(24.dp)
                    .clickable(onClick = onMediaAddClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(CoreUiR.drawable.core_ui_ic_link),
                contentDescription = "링크 삽입",
                tint = AfternoteDesign.colors.gray7,
                modifier = Modifier.size(24.dp),
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier =
                Modifier
                    .size(24.dp)
                    .clickable(onClick = onTextStyleClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_text),
                contentDescription = "텍스트",
                tint = AfternoteDesign.colors.gray7,
                modifier = Modifier.size(24.dp),
            )
        }

        Row(
            modifier =
                Modifier
                    .width(106.dp)
                    .height(43.dp)
                    .background(
                        color = AfternoteDesign.colors.gray2,
                        shape = RoundedCornerShape(size = 20132640.dp),
                    ).padding(start = 6.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 정렬 아이콘 3종은 `core:ui` 승격본을 공유한다 — 종전에는 모듈마다 사본이 있어
            // 같은 아이콘이 화면에 따라 다르게 렌더됐다 (#1404 · #635 의 사본 수렴 방향).
            AlignButton(
                painter = painterResource(CoreUiR.drawable.core_ui_ic_align_center),
                selected = textAlign == TextAlign.Center, // 상태 반영
                onClick = onAlignCenterClick,
            )
            AlignButton(
                painter = painterResource(CoreUiR.drawable.core_ui_ic_align_left),
                selected = textAlign == TextAlign.Start,
                onClick = onAlignLeftClick,
            )
            AlignButton(
                painter = painterResource(CoreUiR.drawable.core_ui_ic_align_right),
                selected = textAlign == TextAlign.End,
                onClick = onAlignRightClick,
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        TextButton(onClick = onDraftClick) {
            Text(
                text = "임시저장",
                style = AfternoteDesign.typography.captionLargeR,
                color = AfternoteDesign.colors.gray7,
            )
        }
        Box(
            modifier =
                Modifier
                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    .clickable(
                        onClickLabel = "임시저장 목록 열기",
                        role = Role.Button,
                        onClick = onDraftCountClick,
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "$draftCount",
                style = AfternoteDesign.typography.captionLargeR,
                color = AfternoteDesign.colors.gray7,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TimeLetterBottomBarPreview() {
    TimeLetterBottomBar(
        draftCount = 3,
        textAlign = TextAlign.Center,
        onMediaAddClick = {},
        onTextStyleClick = {},
        onAlignCenterClick = {},
        onAlignLeftClick = {},
        onAlignRightClick = {},
        onDraftClick = {},
        onDraftCountClick = {},
    )
}
