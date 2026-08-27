package com.afternote.feature.afternote.presentation.author.editor.memorial

import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.author.editor.message.EditorMessageSection
import com.afternote.feature.afternote.presentation.author.editor.message.LeaveMessageEditorItem
import com.afternote.feature.afternote.presentation.author.editor.receiver.RecipientDesignationSection
import com.afternote.feature.afternote.presentation.author.editor.receiver.model.AfternoteEditorReceiver
import com.afternote.feature.afternote.presentation.author.editor.receiver.model.AfternoteEditorReceiverSection
import com.afternote.feature.afternote.presentation.shared.MemorialContent
import com.afternote.feature.afternote.presentation.shared.detail.song.MemorialPlaylist
import com.afternote.feature.afternote.presentation.shared.model.AlbumCover

/**
 * 추억 노트 종류 선택 시 표시되는 콘텐츠 (편집 모드).
 * [MemorialContent] 공통 레이아웃에 편집용 섹션 컴포저블을 넘깁니다.
 */
@Composable
fun MemorialEditorContent(
    displayMemorialPhotoUri: String?,
    playlistAlbumCovers: List<AlbumCover>,
    memorialVideoUrl: String?,
    // null = 썸네일 추출 전/실패. 기본값은 두지 않는다 — 호출자가 "없음"을 명시적으로 선언해야 한다.
    memorialThumbnailUrl: String?,
    editorMessages: List<LeaveMessageEditorItem>,
    // 섹션·콜백엔 기본값을 두지 않는다 — no-op 디폴트가 미배선을 은폐한 전례(#466·#777) 재발 방지.
    recipientSection: AfternoteEditorReceiverSection,
    onSongAddClick: () -> Unit,
    onPhotoAddClick: () -> Unit,
    onVideoAddClick: () -> Unit,
    onMessageRegisterClick: (LeaveMessageEditorItem) -> Unit,
    onMessageDeleteClick: (LeaveMessageEditorItem) -> Unit,
    onMessageAddClick: () -> Unit,
    onThumbnailBytesReady: (ByteArray?) -> Unit,
    onThumbnailExtractionFailed: (Throwable) -> Unit,
    modifier: Modifier = Modifier,
) {
    MemorialContent(
        introContent = { LastMomentQuestion() },
        photoContent = {
            MemorialPhotoUpload(
                displayImageUri = displayMemorialPhotoUri,
                onAddPhotoClick = onPhotoAddClick,
            )
        },
        playlistContent = {
            MemorialPlaylist(
                songCount = playlistAlbumCovers.size,
                albumCovers = playlistAlbumCovers,
                onCardClick = onSongAddClick,
            )
        },
        modifier = modifier,
        sectionSpacing = 32.dp,
        messageContent = {
            EditorMessageSection(
                messages = editorMessages,
                onRegisterClick = onMessageRegisterClick,
                onDeleteClick = onMessageDeleteClick,
                onAddClick = onMessageAddClick,
            )
        },
        recipientContent = {
            RecipientDesignationSection(section = recipientSection)
        },
        videoContent = {
            MemorialVideoUpload(
                videoUrl = memorialVideoUrl,
                thumbnailUrl = memorialThumbnailUrl,
                onAddVideoClick = onVideoAddClick,
                onThumbnailBytesReady = onThumbnailBytesReady,
                onThumbnailExtractionFailed = onThumbnailExtractionFailed,
            )
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun MemorialEditorContentPreview() {
    AfternoteTheme {
        MemorialEditorContent(
            displayMemorialPhotoUri = null,
            playlistAlbumCovers = emptyList(),
            memorialVideoUrl = null,
            memorialThumbnailUrl = null,
            editorMessages =
                listOf(
                    LeaveMessageEditorItem(
                        titleState = rememberTextFieldState("가족들에게"),
                        contentState = rememberTextFieldState("항상 고마워요."),
                    ),
                ),
            recipientSection =
                AfternoteEditorReceiverSection(
                    afternoteEditReceivers =
                        listOf(
                            AfternoteEditorReceiver(id = "1", name = "홍길동", label = "가족"),
                        ),
                ),
            onSongAddClick = {},
            onPhotoAddClick = {},
            onVideoAddClick = {},
            onMessageRegisterClick = {},
            onMessageDeleteClick = {},
            onMessageAddClick = {},
            onThumbnailBytesReady = {},
            onThumbnailExtractionFailed = {},
        )
    }
}
