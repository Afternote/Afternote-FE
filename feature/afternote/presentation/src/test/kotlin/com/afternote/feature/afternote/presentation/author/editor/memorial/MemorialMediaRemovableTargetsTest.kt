package com.afternote.feature.afternote.presentation.author.editor.memorial

import com.afternote.feature.afternote.presentation.author.editor.state.AfternoteTypeForm
import com.afternote.feature.afternote.presentation.author.editor.state.EditableMemorialVideo
import com.afternote.feature.afternote.presentation.author.editor.state.EditorFormState
import com.afternote.feature.afternote.presentation.author.editor.state.MemorialVideoAttachment
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 소스 시트 삭제 항목의 노출 판정 (#1114).
 *
 * 핵심 경계는 "서버에 이미 저장된 미디어는 삭제 대상이 아니다" 이다 — BE 수정(PATCH) 계약이
 * null 필드를 "기존 값 유지" 로 해석해 삭제를 표현할 수 없으므로, 폼만 비우는 삭제 항목을
 * 원격 미디어에 노출하면 저장 후 되살아나는 거짓 삭제가 된다.
 */
class MemorialMediaRemovableTargetsTest {
    private fun memorialForm(
        pickedPhotoUri: String? = null,
        photoUrl: String? = null,
        pendingVideoUrl: String? = null,
        persistedVideoUrl: String? = null,
    ): EditorFormState =
        EditorFormState(
            typeForm =
                AfternoteTypeForm.Memorial(
                    pickedPhotoUri = pickedPhotoUri,
                    photoUrl = photoUrl,
                    video =
                        EditableMemorialVideo.restore(
                            pending = MemorialVideoAttachment.ofOrNull(pendingVideoUrl),
                            persisted = MemorialVideoAttachment.ofOrNull(persistedVideoUrl),
                        ),
                ),
        )

    @Test
    fun `첨부가 없으면 삭제할 슬롯도 없다`() {
        assertEquals(emptySet<MemorialMediaTarget>(), memorialForm().removableMemorialMediaTargets())
    }

    @Test
    fun `새로 고른 로컬 첨부는 슬롯별로 삭제 대상이 된다`() {
        assertEquals(
            setOf(MemorialMediaTarget.PHOTO),
            memorialForm(pickedPhotoUri = "content://photos/new").removableMemorialMediaTargets(),
        )
        assertEquals(
            setOf(MemorialMediaTarget.VIDEO),
            memorialForm(pendingVideoUrl = "content://videos/new").removableMemorialMediaTargets(),
        )
        assertEquals(
            setOf(MemorialMediaTarget.PHOTO, MemorialMediaTarget.VIDEO),
            memorialForm(
                pickedPhotoUri = "content://photos/new",
                pendingVideoUrl = "content://videos/new",
            ).removableMemorialMediaTargets(),
        )
    }

    @Test
    fun `서버에 저장된 미디어만 있으면 삭제 대상이 아니다`() {
        // 수정 진입 prefill — 사진은 photoUrl, 영상은 원격 URL 로 온다.
        assertEquals(
            emptySet<MemorialMediaTarget>(),
            memorialForm(
                photoUrl = "https://cdn.test/portrait.jpg",
                persistedVideoUrl = "https://cdn.test/farewell.mp4",
            ).removableMemorialMediaTargets(),
        )
    }

    @Test
    fun `서버 사진 위에 새로 고른 사진만 삭제 대상이다`() {
        assertEquals(
            setOf(MemorialMediaTarget.PHOTO),
            memorialForm(
                pickedPhotoUri = "content://photos/replacement",
                photoUrl = "https://cdn.test/portrait.jpg",
            ).removableMemorialMediaTargets(),
        )
    }

    @Test
    fun `추억 노트 폼이 아니면 삭제할 슬롯이 없다`() {
        assertEquals(
            emptySet<MemorialMediaTarget>(),
            EditorFormState(typeForm = AfternoteTypeForm.Social()).removableMemorialMediaTargets(),
        )
    }
}
