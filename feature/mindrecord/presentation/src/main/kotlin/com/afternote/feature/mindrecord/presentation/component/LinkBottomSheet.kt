package com.afternote.feature.mindrecord.presentation.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.AfternoteTextField
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.presentation.R
import com.afternote.feature.mindrecord.presentation.util.isSupportedLinkUrl

/**
 * 키보드 툴바의 링크 버튼에서 호출되는 "링크 추가하기" 바텀시트.
 *
 * Figma 노드 533:16183 (link variant) — 헤더(드래그 핸들 + 제목 + 완료 버튼) + URL 입력 필드.
 * 완료 시 [onConfirm] 으로 입력된 URL 을 전달한다 (호출부에서 에디터에 삽입).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkBottomSheet(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // core 정본이 `TextFieldState` 를 받는다. 상태 소유자가 이 로컬 하나뿐이라 국소 변경이다 (#634).
    val urlState = rememberTextFieldState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = AfternoteDesign.colors.gray1,
        dragHandle = {
            Box(
                modifier =
                    Modifier
                        .padding(top = 12.dp, bottom = 8.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFCCCCCC)),
            )
        },
    ) {
        LinkSheetContent(
            urlState = urlState,
            onConfirm = { onConfirm(urlState.text.toString()) },
            modifier = modifier,
        )
    }
}

@Composable
private fun LinkSheetContent(
    urlState: TextFieldState,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // core 정본은 TextFieldState 를 들고 있다 — 입력값 판정은 그 상태에서 읽는다 (#634·#722).
    val url = urlState.text.toString()

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 헤더 — 좌(52dp placeholder) / 가운데 제목 / 우(완료 버튼). 좌우 균형으로 제목이 가운데 정렬.
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(modifier = Modifier.width(52.dp))

            Text(
                text = stringResource(R.string.mindrecord_link_sheet_title),
                style = AfternoteDesign.typography.bodyBase,
                color = AfternoteDesign.colors.gray6,
            )

            // 형식이 맞을 때만 완료가 눌린다. 종전에는 입력값과 무관하게 항상 활성이라
            // 임의 문자열이 그대로 본문 링크가 됐다 (#722).
            ConfirmPill(onClick = onConfirm, enabled = url.isSupportedLinkUrl())
        }

        // core 정본을 쓴다. 종전에는 흰 배경 + gray2 indicator + 8dp 코너를 M3 프리미티브로
        // 다시 적었고, 레포 전체에서 M3 OutlinedTextField 를 쓰는 마지막 자리였다 (#634).
        AfternoteTextField(
            state = urlState,
            placeholder = stringResource(R.string.mindrecord_link_sheet_placeholder),
            keyboardType = KeyboardType.Uri,
            modifier = Modifier.fillMaxWidth(),
        )

        // 왜 완료가 안 눌리는지 알려 준다 — 회색 버튼만으로는 고장과 구분되지 않는다.
        if (url.isNotBlank() && !url.isSupportedLinkUrl()) {
            Text(
                text = stringResource(R.string.mindrecord_link_sheet_invalid),
                style = AfternoteDesign.typography.captionLargeR,
                color = AfternoteDesign.colors.gray6,
            )
        }
    }
}

@Composable
private fun ConfirmPill(
    onClick: () -> Unit,
    enabled: Boolean,
) {
    Box(
        modifier =
            Modifier
                .clip(CircleShape)
                .background(if (enabled) AfternoteDesign.colors.gray2 else AfternoteDesign.colors.gray1)
                .border(
                    BorderStroke(1.dp, if (enabled) AfternoteDesign.colors.gray4 else AfternoteDesign.colors.gray3),
                    CircleShape,
                ).clickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = 15.dp, vertical = 9.dp),
    ) {
        Text(
            text = stringResource(R.string.mindrecord_link_sheet_confirm),
            style = AfternoteDesign.typography.captionLargeB,
            color = if (enabled) AfternoteDesign.colors.gray9 else AfternoteDesign.colors.gray4,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LinkBottomSheetPreview() {
    AfternoteTheme {
        LinkSheetContent(
            urlState = rememberTextFieldState(),
            onConfirm = {},
        )
    }
}
