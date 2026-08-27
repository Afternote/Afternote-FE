package com.afternote.feature.mindrecord.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.presentation.R
import com.afternote.core.ui.R as CoreUiR

/**
 * 키보드 툴바 링크(미디어) 버튼에서 호출되는 "미디어 추가하기" 메인 메뉴 바텀시트.
 *
 * Figma 노드 4327:72281 (media_select variant) — 헤더(드래그 핸들 + 제목) + 4개 항목
 * (이미지/음성/파일/링크 추가하기). 항목 사이 #E0E0E0 디바이더.
 *
 * "링크 추가하기" 를 누르면 [LinkBottomSheet] 가 후속으로 뜨도록 호출부에서 분기한다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaSelectBottomSheet(
    onDismiss: () -> Unit,
    onImageClick: () -> Unit,
    onVoiceClick: () -> Unit,
    onFileClick: () -> Unit,
    onLinkClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
        MediaSelectContent(
            onImageClick = onImageClick,
            onVoiceClick = onVoiceClick,
            onFileClick = onFileClick,
            onLinkClick = onLinkClick,
            modifier = modifier,
        )
    }
}

@Composable
private fun MediaSelectContent(
    onImageClick: () -> Unit,
    onVoiceClick: () -> Unit,
    onFileClick: () -> Unit,
    onLinkClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
    ) {
        // 헤더 — 가운데 정렬, py=6
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.mindrecord_media_sheet_title),
                style = AfternoteDesign.typography.bodyBase,
                color = AfternoteDesign.colors.gray6,
            )
        }

        // 4개 항목 — 각 row p=16 gap=10, 사이 #E0E0E0 디바이더
        MediaItem(
            icon = painterResource(CoreUiR.drawable.core_ui_ic_image),
            text = stringResource(R.string.mindrecord_media_sheet_image),
            onClick = onImageClick,
        )
        HorizontalDivider(thickness = 1.dp, color = AfternoteDesign.colors.gray3)
        MediaItem(
            icon = painterResource(R.drawable.mindrecord_ic_mic),
            text = stringResource(R.string.mindrecord_media_sheet_voice),
            onClick = onVoiceClick,
        )
        HorizontalDivider(thickness = 1.dp, color = AfternoteDesign.colors.gray3)
        MediaItem(
            icon = painterResource(CoreUiR.drawable.core_ui_ic_file),
            text = stringResource(R.string.mindrecord_media_sheet_file),
            onClick = onFileClick,
        )
        HorizontalDivider(thickness = 1.dp, color = AfternoteDesign.colors.gray3)
        MediaItem(
            icon = painterResource(CoreUiR.drawable.core_ui_ic_link),
            text = stringResource(R.string.mindrecord_media_sheet_link),
            onClick = onLinkClick,
        )
        HorizontalDivider(thickness = 1.dp, color = AfternoteDesign.colors.gray3)
    }
}

@Composable
private fun MediaItem(
    icon: Painter,
    text: String,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            painter = icon,
            contentDescription = null,
            tint = AfternoteDesign.colors.gray9,
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = text,
            style = AfternoteDesign.typography.bodyBase,
            color = AfternoteDesign.colors.gray9,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MediaSelectBottomSheetPreview() {
    AfternoteTheme {
        MediaSelectContent(
            onImageClick = {},
            onVoiceClick = {},
            onFileClick = {},
            onLinkClick = {},
        )
    }
}
