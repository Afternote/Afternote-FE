package com.afternote.feature.afternote.presentation.editor.memorial

import com.afternote.feature.afternote.presentation.editor.state.AfternoteTypeForm
import com.afternote.feature.afternote.presentation.editor.state.EditableMemorialVideo
import com.afternote.feature.afternote.presentation.editor.state.EditorFormState
import com.afternote.feature.afternote.presentation.editor.state.MemorialVideoAttachment
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 소스 시트 삭제 항목의 노출 판정 (#1114).
 *
 * #1597부터 BE가 미디어 키 생략(유지)과 명시적 null(삭제)을 구분하고 FE도 null 키를 보존하므로,
 * 출처가 아니라 현재 슬롯의 표시값 존재 여부가 경계다.
 */
class MemorialMediaRemovableTargetsTest {
    private fun memorialForm(
        pickedPhotoUri: String? = null,
        photoUrl: String? = null,
        selectedVideoUrl: String? = null,
        persistedVideoUrl: String? = null,
    ): EditorFormState =
        EditorFormState(
            typeForm =
                AfternoteTypeForm.Memorial(
                    pickedPhotoUri = pickedPhotoUri,
                    photoUrl = photoUrl,
                    video =
                        EditableMemorialVideo
                            .fromPersisted(MemorialVideoAttachment.ofOrNull(persistedVideoUrl))
                            .let { video -> selectedVideoUrl?.let(video::withSelection) ?: video },
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
            memorialForm(selectedVideoUrl = "content://videos/new").removableMemorialMediaTargets(),
        )
        assertEquals(
            setOf(MemorialMediaTarget.PHOTO, MemorialMediaTarget.VIDEO),
            memorialForm(
                pickedPhotoUri = "content://photos/new",
                selectedVideoUrl = "content://videos/new",
            ).removableMemorialMediaTargets(),
        )
    }

    @Test
    fun `서버에 저장된 미디어만 있어도 슬롯별 삭제 대상이 된다`() {
        // 수정 진입 prefill — 사진은 photoUrl, 영상은 원격 URL 로 온다.
        assertEquals(
            setOf(MemorialMediaTarget.PHOTO, MemorialMediaTarget.VIDEO),
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
