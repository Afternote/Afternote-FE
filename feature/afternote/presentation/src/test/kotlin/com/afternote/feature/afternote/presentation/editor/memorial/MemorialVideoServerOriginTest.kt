package com.afternote.feature.afternote.presentation.editor.memorial

import com.afternote.feature.afternote.domain.repository.author.MediaInput
import com.afternote.feature.afternote.presentation.editor.model.EditorContentPrefill
import com.afternote.feature.afternote.presentation.editor.state.AfternoteTypeForm
import com.afternote.feature.afternote.presentation.editor.state.EditorFormState
import com.afternote.feature.afternote.presentation.editor.state.MemorialVideoAttachment
import com.afternote.feature.afternote.presentation.editor.state.withMemorialThumbnail
import com.afternote.feature.afternote.presentation.editor.state.withMemorialVideo
import com.afternote.feature.afternote.presentation.editor.state.withMemorialVideoRemoved
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 서버 영상을 로컬로 교체한 뒤 지우는 경로 (#1406).
 *
 * #1375 는 삭제 항목을 로컬 첨부에만 노출해 서버 미디어의 거짓 삭제를 막았다. 그런데 영상 축은 한
 * 칸이 로컬 픽과 원격 prefill 을 겸해, 서버 영상을 로컬 영상으로 **교체**하면 그 추론이 가드를
 * 통과시켰다 — 그걸 지우면 폼은 비지만 저장 뒤 서버 영상이 그대로 남는다(수정 계약이 삭제를
 * 표현하지 못한다).
 *
 * 지금은 하나의 편집 값 객체가 서버 원본(`persisted`)과 이번 세션의 교체분(`selection`)을 함께
 * 기억해 저장 출처를 URL 모양이 아니라 상태로 판정한다. 삭제는 두 층을 함께 비우고, 빈 폼은 저장 시
 * 명시적 `null` 로 나간다(#1597) — BE 가 그것을 삭제로 읽으므로 폼이 지운 척만 하는 상태가 없다.
 */
class MemorialVideoServerOriginTest {
    private val serverVideo = "https://cdn.test/farewell.mp4"
    private val serverThumbnail = "https://cdn.test/farewell-thumb.jpg"
    private val localVideo = "content://videos/replacement"
    private val serverAttachment = MemorialVideoAttachment(url = serverVideo, thumbnailUrl = serverThumbnail)

    private fun prefilledEditForm(): EditorFormState =
        EditorFormState(
            typeForm =
                AfternoteTypeForm.fromPrefill(
                    EditorContentPrefill.Memorial(
                        videoUrl = serverVideo,
                        thumbnailUrl = serverThumbnail,
                        photoUrl = null,
                        playlistSongs = emptyList(),
                    ),
                ),
        )

    @Test
    fun `수정 진입 prefill 은 서버 원본을 따로 기억한다`() {
        val state = prefilledEditForm()

        assertEquals(serverAttachment, state.displayedMemorialVideo)
        assertEquals(MediaInput.Remote(serverVideo), state.memorialVideo?.toMediaInput())
    }

    @Test
    fun `로컬 영상으로 교체해도 서버 원본은 남는다`() {
        val state = prefilledEditForm().withMemorialVideo(localVideo)

        // 교체한 영상의 썸네일은 아직 없다 — 원본 썸네일을 물려주면 다른 영상의 그림이 붙는다.
        assertEquals(MemorialVideoAttachment(url = localVideo), state.displayedMemorialVideo)
        assertEquals(MediaInput.Local(localVideo), state.memorialVideo?.toMediaInput())
    }

    @Test
    fun `교체분을 지우면 슬롯이 비고 저장 시 명시적 null 로 나간다`() {
        val removed =
            prefilledEditForm()
                .withMemorialVideo(localVideo)
                .withMemorialVideoRemoved()

        // #1406 은 서버 삭제가 불가능하던 동안 「삭제 시 서버 영상 표시로 복귀」 를 대체 수단으로 택했고
        // #1561 이 그것을 잠갔다. #1597 이 명시적 null 삭제를 열어 그 대체 수단은 근거를 잃었다 —
        // 삭제는 이름대로 슬롯을 비우고, 서버 삭제는 저장이 한다. 저장 없이 나가면 서버 영상은 그대로다.
        assertNull(removed.displayedMemorialVideo)
        assertEquals(MediaInput.None, removed.memorialVideo?.toMediaInput())
        assertEquals(emptySet<MemorialMediaTarget>(), removed.removableMemorialMediaTargets())
    }

    @Test
    fun `서버 원본이 없어도 삭제는 같은 모양으로 슬롯을 비운다`() {
        // 생성 모드 — 서버 원본 유무와 무관하게 삭제 결과는 하나다.
        val removed =
            EditorFormState(typeForm = AfternoteTypeForm.Memorial())
                .withMemorialVideo(localVideo)
                .withMemorialVideoRemoved()

        assertNull(removed.displayedMemorialVideo)
    }

    @Test
    fun `영상을 지운 뒤 도착한 썸네일은 버려진다`() {
        // 썸네일 업로드는 viewModelScope 라 삭제 뒤에 성공 콜백이 온다. 종전에는 두 칸이 따로 놀아
        // 「영상은 없는데 썸네일만 있는」 폼이 만들어졌다 — 그 상태는 이제 타입이 배제한다.
        val afterLateThumbnail =
            EditorFormState(typeForm = AfternoteTypeForm.Memorial())
                .withMemorialVideo(localVideo)
                .withMemorialVideoRemoved()
                .withMemorialThumbnail("https://cdn.test/late-thumb.jpg")

        assertFalse(afterLateThumbnail.canRemoveMemorialVideo)
        assertNull(afterLateThumbnail.displayedMemorialVideo)
    }
}
