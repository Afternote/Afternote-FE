package com.afternote.feature.afternote.presentation.editor.memorial

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.afternote.core.ui.sheet.MediaSelectSheetContent
import com.afternote.core.ui.sheet.MediaSheetItem
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.editor.isLocalContentUri
import com.afternote.feature.afternote.presentation.editor.state.EditorFormState
import com.afternote.core.ui.R as CoreUiR

/** 추억 노트 미디어 슬롯 종류. 시트 문구와 촬영 인텐트를 함께 가른다. */
internal enum class MemorialMediaTarget {
    /** 영정 사진 — 갤러리 이미지 선택 / 사진 촬영. */
    PHOTO,

    /** 장례식에 남길 영상 — 갤러리 영상 선택 / 영상 촬영. */
    VIDEO,
}

/**
 * 시트에 "삭제" 를 노출할 슬롯 — 이 폼에서 새로 붙인 로컬 첨부(`content://`)만 (#1114).
 *
 * 서버에 이미 저장된 미디어(수정 진입 prefill 의 원격 URL)는 대상에서 뺀다: 수정(PATCH) 계약이
 * 삭제를 표현하지 못한다 — BE `AfternotePlaylist.update` 는 null 필드를 "기존 값 유지" 로
 * 해석하므로, 폼만 비워 두면 저장 후 서버 미디어가 되살아나는 거짓 삭제가 된다.
 * 그래서 실제로 지울 수 있는 것만 지우게 한다. 서버 미디어 삭제는 BE 계약 확장 후 후속.
 *
 * - 사진: [EditorFormState.pickedMemorialPhotoUri] 는 픽·촬영으로만 채워지는 로컬 전용 필드라
 *   값이 있으면 곧 로컬 첨부다. 지우면 표시가 서버 사진([EditorFormState.memorialPhotoUrl])으로
 *   돌아간다.
 * - 영상: [EditorFormState.memorialVideoUrl] 은 로컬 픽과 원격 prefill 이 한 필드를 공유하므로
 *   [isLocalContentUri] 로 가른다 — `AfternoteEditorViewModel.videoMediaInput` 과 같은 기준.
 *
 * 알려진 구멍(#1406): 수정 모드에서 서버 영상을 로컬 영상으로 교체하면 원격 URL 이 덮여 이 가드가
 * 못 가른다 — 그 로컬 영상을 지우면 폼은 비지만 저장 후 서버 영상이 남는 거짓 삭제가 된다.
 */
internal fun EditorFormState.removableMemorialMediaTargets(): Set<MemorialMediaTarget> =
    buildSet {
        if (!pickedMemorialPhotoUri.isNullOrBlank()) add(MemorialMediaTarget.PHOTO)
        if (memorialVideoUrl?.isLocalContentUri() == true) add(MemorialMediaTarget.VIDEO)
    }

/**
 * 추억 노트 미디어 소스 선택 시트 (#369) — "갤러리에서 선택" / "촬영" 두 갈래.
 *
 * 이전에는 슬롯을 누르면 곧장 `PickVisualMedia` 가 떠서 *이미 찍어 둔* 사진·영상만 붙일 수 있었다.
 * 그 사이에 이 시트를 끼워 즉석 촬영 경로를 연다.
 *
 * 시각 규격은 공용 [MediaSelectSheetContent] 가 정한다(시안 정본 4327:72281) — 이 슬롯용 시안은
 * 따로 없고, 디자이너가 같은 "미디어 추가하기" 시트를 다른 화면에 이미 그려 두었다 (#642).
 * 여기 남는 건 이 화면만의 것뿐이다 — 헤더 문구, 슬롯(사진/영상)에 따라 갈리는 항목, 삭제 노출 여부.
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
    val captureLabel =
        when (target) {
            MemorialMediaTarget.PHOTO -> stringResource(R.string.afternote_editor_media_source_take_photo)
            MemorialMediaTarget.VIDEO -> stringResource(R.string.afternote_editor_media_source_take_video)
        }
    val removeLabel =
        when (target) {
            MemorialMediaTarget.PHOTO -> stringResource(R.string.afternote_editor_media_source_remove_photo)
            MemorialMediaTarget.VIDEO -> stringResource(R.string.afternote_editor_media_source_remove_video)
        }

    MediaSelectSheetContent(
        items =
            listOfNotNull(
                MediaSheetItem(
                    iconRes = CoreUiR.drawable.core_ui_ic_image,
                    label = stringResource(R.string.afternote_editor_media_source_gallery),
                    onClick = onPickFromGallery,
                ),
                MediaSheetItem(
                    iconRes =
                        when (target) {
                            MemorialMediaTarget.PHOTO -> R.drawable.afternote_ic_camera
                            MemorialMediaTarget.VIDEO -> R.drawable.afternote_ic_videocam
                        },
                    label = captureLabel,
                    onClick = onCapture,
                ),
                onRemove?.let { remove ->
                    MediaSheetItem(
                        iconRes = R.drawable.afternote_ic_trash,
                        label = removeLabel,
                        onClick = remove,
                        // 되돌릴 수 없는 파괴적 동작임을 색으로 가른다 — 추가 갈래(갤러리·촬영)와 같은 회색이면
                        // 실수 탭을 유도한다.
                        iconTint = AfternoteDesign.colors.error,
                        labelColor = AfternoteDesign.colors.error,
                    )
                },
            ),
        modifier = modifier,
        title = stringResource(R.string.afternote_editor_media_source_header),
    )
}
