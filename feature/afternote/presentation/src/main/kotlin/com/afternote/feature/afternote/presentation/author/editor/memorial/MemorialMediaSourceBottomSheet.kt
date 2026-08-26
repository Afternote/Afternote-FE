package com.afternote.feature.afternote.presentation.author.editor.memorial

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.afternote.core.ui.modifierextention.bottomBorder
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.R
import com.afternote.core.ui.R as CoreUiR

/** 추억 노트 미디어 슬롯 종류. 시트 문구와 촬영 인텐트를 함께 가른다. */
internal enum class MemorialMediaTarget {
    /** 영정 사진 — 갤러리 이미지 선택 / 사진 촬영. */
    PHOTO,

    /** 장례식에 남길 영상 — 갤러리 영상 선택 / 영상 촬영. */
    VIDEO,
}

/**
 * 추억 노트 미디어 소스 선택 시트 (#369) — "갤러리에서 선택" / "촬영" 두 갈래.
 *
 * 이전에는 슬롯을 누르면 곧장 `PickVisualMedia` 가 떠서 *이미 찍어 둔* 사진·영상만 붙일 수 있었다.
 * 그 사이에 이 시트를 끼워 즉석 촬영 경로를 연다.
 *
 * 시각 규격은 수신자 모듈의 `DocumentSourceBottomSheet` 와 같은 패턴을 따른다 — 디자이너가 같은
 * "미디어 추가하기" 시트를 다른 화면에 이미 그려 두었고, 이 슬롯용 시안은 따로 없다.
 */
@Composable
internal fun MemorialMediaSourceBottomSheet(
    target: MemorialMediaTarget,
    onPickFromGallery: () -> Unit,
    onCapture: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(8.dp))
        Box(
            modifier =
                Modifier
                    .size(width = 40.dp, height = 4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(AfternoteDesign.colors.gray3),
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = stringResource(R.string.afternote_editor_media_source_header),
            style = AfternoteDesign.typography.bodyBase,
            color = AfternoteDesign.colors.gray6,
        )
        Spacer(Modifier.height(18.dp))
        MemorialMediaSourceOption(
            iconRes = CoreUiR.drawable.core_ui_ic_image,
            label = stringResource(R.string.afternote_editor_media_source_gallery),
            onClick = onPickFromGallery,
        )
        MemorialMediaSourceOption(
            iconRes =
                when (target) {
                    MemorialMediaTarget.PHOTO -> R.drawable.feature_afternote_ic_camera
                    MemorialMediaTarget.VIDEO -> R.drawable.feature_afternote_ic_videocam
                },
            label =
                when (target) {
                    MemorialMediaTarget.PHOTO -> stringResource(R.string.afternote_editor_media_source_take_photo)
                    MemorialMediaTarget.VIDEO -> stringResource(R.string.afternote_editor_media_source_take_video)
                },
            onClick = onCapture,
        )
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun MemorialMediaSourceOption(
    @DrawableRes iconRes: Int,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .bottomBorder(color = AfternoteDesign.colors.gray3, width = 1.dp)
                .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = AfternoteDesign.colors.iconBk,
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = label,
            style = AfternoteDesign.typography.bodyBase,
            color = AfternoteDesign.colors.gray9,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MemorialMediaSourceBottomSheetPhotoPreview() {
    AfternoteTheme {
        MemorialMediaSourceBottomSheet(
            target = MemorialMediaTarget.PHOTO,
            onPickFromGallery = {},
            onCapture = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MemorialMediaSourceBottomSheetVideoPreview() {
    AfternoteTheme {
        MemorialMediaSourceBottomSheet(
            target = MemorialMediaTarget.VIDEO,
            onPickFromGallery = {},
            onCapture = {},
        )
    }
}
