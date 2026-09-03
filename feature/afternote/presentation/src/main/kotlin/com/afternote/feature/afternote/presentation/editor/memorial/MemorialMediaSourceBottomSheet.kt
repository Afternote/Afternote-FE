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

    /** 추모 음성 — 파일(SAF)에서 선택 / 즉석 녹음 (#1118). */
    AUDIO,
}

/**
 * 시트에 "삭제" 를 노출할 슬롯 — 이 폼에서 새로 붙인 로컬 첨부(`content://`)만 (#1114).
 *
 * 서버에 이미 저장된 미디어(수정 진입 prefill 의 원격 URL)는 대상에서 뺀다: 수정(PATCH) 계약이
 * 삭제를 표현하지 못한다 — BE `AfternotePlaylist.update` 는 null 필드를 "기존 값 유지" 로
 * 해석하므로, 폼만 비워 두면 저장 후 서버 미디어가 되살아나는 거짓 삭제가 된다.
 * 그래서 실제로 지울 수 있는 것만 지우게 한다. 서버 미디어 삭제는 BE 계약 확장 후 후속.
 *
 * 사진은 `picked` 칸을 확인하지만, 영상 UI는 편집 값에 "새 선택을 걷어낼 수 있는가"만 묻는다. 그래서
 * 이 단에서는 서버 원본인지 새 선택인지도, URL 스킴도 판정하지 않는다(#1406 이전에는 영상 한 칸이
 * 두 출처를 겸해 [isLocalContentUri]로 추론했고, 로컬 영상으로 덮는 순간 서버 원본을 잃었다).
 *
 * **음성만 원격도 삭제 대상이다** (#1118). 요청 DTO 의 `memorialAudioUrl` 에 기본값을 두지 않아
 * 폼이 비면 JSON null 이 그대로 실리고, BE 가 그것을 삭제로 읽는다
 * (`PlaylistRequestDeserializer` → `AfternotePlaylist.update`, Afternote-BE `72fee63`).
 * 즉 음성은 「거짓 삭제」가 되지 않는다. 사진·영상을 같은 모양으로 옮기는 것은 #1596·#1597 몫이다.
 */
internal fun EditorFormState.removableMemorialMediaTargets(): Set<MemorialMediaTarget> =
    buildSet {
        if (!pickedMemorialPhotoUri.isNullOrBlank()) add(MemorialMediaTarget.PHOTO)
        if (canDiscardMemorialVideoSelection) add(MemorialMediaTarget.VIDEO)
        if (!memorialAudioUrl.isNullOrBlank()) add(MemorialMediaTarget.AUDIO)
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
            MemorialMediaTarget.AUDIO -> stringResource(R.string.afternote_editor_media_source_record_audio)
        }
    val removeLabel =
        when (target) {
            MemorialMediaTarget.PHOTO -> stringResource(R.string.afternote_editor_media_source_remove_photo)
            MemorialMediaTarget.VIDEO -> stringResource(R.string.afternote_editor_media_source_remove_video)
            MemorialMediaTarget.AUDIO -> stringResource(R.string.afternote_editor_media_source_remove_audio)
        }

    MediaSelectSheetContent(
        items =
            listOfNotNull(
                MediaSheetItem(
                    // 음성은 갤러리(사진 선택기)의 대상이 아니라 문서 선택기로 고른다 — 아이콘·문구도 그에 맞춘다.
                    iconRes =
                        when (target) {
                            MemorialMediaTarget.PHOTO, MemorialMediaTarget.VIDEO -> CoreUiR.drawable.core_ui_ic_image
                            MemorialMediaTarget.AUDIO -> CoreUiR.drawable.core_ui_ic_file
                        },
                    label =
                        when (target) {
                            MemorialMediaTarget.PHOTO, MemorialMediaTarget.VIDEO -> {
                                stringResource(R.string.afternote_editor_media_source_gallery)
                            }

                            MemorialMediaTarget.AUDIO -> {
                                stringResource(R.string.afternote_editor_media_source_pick_audio)
                            }
                        },
                    onClick = onPickFromGallery,
                ),
                MediaSheetItem(
                    iconRes =
                        when (target) {
                            MemorialMediaTarget.PHOTO -> R.drawable.afternote_ic_camera
                            MemorialMediaTarget.VIDEO -> R.drawable.afternote_ic_videocam
                            MemorialMediaTarget.AUDIO -> CoreUiR.drawable.core_ui_ic_mic
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
