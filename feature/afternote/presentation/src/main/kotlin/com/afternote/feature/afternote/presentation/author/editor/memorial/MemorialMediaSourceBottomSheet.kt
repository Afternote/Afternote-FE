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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.modifierextention.bottomBorder
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.author.editor.state.EditorFormState
import com.afternote.core.ui.R as CoreUiR

/** 추억 노트 미디어 슬롯 종류. 시트 문구와 촬영 인텐트를 함께 가른다. */
internal enum class MemorialMediaTarget {
    /** 영정 사진 — 갤러리 이미지 선택 / 사진 촬영. */
    PHOTO,

    /** 장례식에 남길 영상 — 갤러리 영상 선택 / 영상 촬영. */
    VIDEO,
}

/**
 * 시트에 "삭제" 를 노출할 슬롯 — 현재 폼에 표시 중인 로컬·서버 첨부 모두 (#1114, #1597).
 *
 * BE 수정 계약은 미디어 키 생략을 「기존 값 유지」, JSON `null` 을 「DB 참조 제거와 관리 S3 객체
 * 삭제 시도」로 구분한다(`PlaylistRequestDeserializer`, Afternote-BE `72fee63`). #1596이 폼의
 * `null` 을 요청에 명시적으로 싣기 때문에 서버 값도 거짓 삭제 없이 지울 수 있다.
 *
 * 삭제는 현재 표시된 한 층에만 적용한다. 새 선택이 있으면 그것을 걷어 서버 원본으로 돌아가고, 서버
 * 원본만 있으면 서버 축을 비워 저장 시 `null` 을 보낸다. 노출 판정은 출처가 아니라 표시값이 있는지만
 * 본다 — 서버 원본인지 새 선택인지, URL 스킴도 이 단에서는 판정하지 않는다(#1406 이전에는 영상 한
 * 칸이 두 출처를 겸해 [isLocalContentUri]로 추론했고, 로컬 영상으로 덮는 순간 서버 원본을 잃었다).
 */
internal fun EditorFormState.removableMemorialMediaTargets(): Set<MemorialMediaTarget> =
    buildSet {
        if (!displayMemorialPhotoUri().isNullOrBlank()) add(MemorialMediaTarget.PHOTO)
        if (canRemoveMemorialVideo) add(MemorialMediaTarget.VIDEO)
    }

/**
 * 추억 노트 미디어 소스 선택 시트 (#369) — "갤러리에서 선택" / "촬영" 두 갈래.
 *
 * 이전에는 슬롯을 누르면 곧장 `PickVisualMedia` 가 떠서 *이미 찍어 둔* 사진·영상만 붙일 수 있었다.
 * 그 사이에 이 시트를 끼워 즉석 촬영 경로를 연다.
 *
 * 시각 규격은 수신자 모듈의 `DocumentSourceBottomSheet` 와 같은 패턴을 따른다 — 디자이너가 같은
 * "미디어 추가하기" 시트를 다른 화면에 이미 그려 두었고, 이 슬롯용 시안은 따로 없다.
 *
 * @param onRemove 슬롯의 첨부를 지우는 항목 (#1114). `null` 이면 항목 자체를 그리지 않는다 —
 *   지울 수 있는 첨부가 있을 때만 노출하며, 그 판정은 [removableMemorialMediaTargets] 가 정한다.
 */
@Composable
internal fun MemorialMediaSourceBottomSheet(
    target: MemorialMediaTarget,
    onPickFromGallery: () -> Unit,
    onCapture: () -> Unit,
    onRemove: (() -> Unit)?,
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
        if (onRemove != null) {
            MemorialMediaSourceOption(
                iconRes = R.drawable.feature_afternote_ic_trash,
                label =
                    when (target) {
                        MemorialMediaTarget.PHOTO -> stringResource(R.string.afternote_editor_media_source_remove_photo)
                        MemorialMediaTarget.VIDEO -> stringResource(R.string.afternote_editor_media_source_remove_video)
                    },
                onClick = onRemove,
                // 되돌릴 수 없는 파괴적 동작임을 색으로 가른다 — 추가 갈래(갤러리·촬영)와 같은 회색이면
                // 실수 탭을 유도한다.
                iconTint = AfternoteDesign.colors.error,
                labelColor = AfternoteDesign.colors.error,
            )
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun MemorialMediaSourceOption(
    @DrawableRes iconRes: Int,
    label: String,
    onClick: () -> Unit,
    iconTint: Color = AfternoteDesign.colors.iconBk,
    labelColor: Color = AfternoteDesign.colors.gray9,
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
            tint = iconTint,
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = label,
            style = AfternoteDesign.typography.bodyBase,
            color = labelColor,
        )
    }
}
