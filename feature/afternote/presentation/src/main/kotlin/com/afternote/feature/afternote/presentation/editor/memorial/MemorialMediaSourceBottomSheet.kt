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
 * 시트에 "삭제" 를 노출할 슬롯 — 현재 폼에 표시 중인 로컬·서버 첨부 모두 (#1114, #1597).
 *
 * BE 수정 계약은 미디어 키 생략을 「기존 값 유지」, JSON `null` 을 「DB 참조 제거와 관리 S3 객체
 * 삭제 시도」로 구분한다(`PlaylistRequestDeserializer`, Afternote-BE `72fee63`). #1596이 폼의
 * `null` 을 요청에 명시적으로 싣기 때문에 서버 값도 거짓 삭제 없이 지울 수 있다.
 *
 * 삭제는 슬롯을 비운다. 새 선택과 서버 원본을 함께 걷고, 저장 시 `null` 을 보낸다. 노출 판정은
 * 출처가 아니라 표시값이 있는지만 본다. 서버 원본인지 새 선택인지, URL 스킴도 이 단에서는 판정하지
 * 않는다(#1406 이전에는 영상 한 칸이 두 출처를 겸해 [isLocalContentUri]로 추론했고, 로컬 영상으로
 * 덮는 순간 서버 원본을 잃었다).
 *
 * 음성은 로컬 선택 칸이 따로 없어 표시값 한 칸이 곧 판정이다 (#1118). 요청 DTO 의
 * `memorialAudioUrl` 에 기본값을 두지 않아 폼이 비면 JSON null 이 그대로 실리고, BE 가 그것을
 * 삭제로 읽는다(`PlaylistRequestDeserializer` → `AfternotePlaylist.update`, Afternote-BE `72fee63`).
 */
internal fun EditorFormState.removableMemorialMediaTargets(): Set<MemorialMediaTarget> =
    buildSet {
        if (!displayMemorialPhotoUri().isNullOrBlank()) add(MemorialMediaTarget.PHOTO)
        if (canRemoveMemorialVideo) add(MemorialMediaTarget.VIDEO)
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
