package com.afternote.feature.afternote.presentation.author.editor.memorial

import com.afternote.feature.afternote.presentation.author.editor.model.EditorContentPrefill
import com.afternote.feature.afternote.presentation.author.editor.state.AfternoteTypeForm
import com.afternote.feature.afternote.presentation.author.editor.state.EditorFormState
import com.afternote.feature.afternote.presentation.author.editor.state.withMemorialVideo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 서버 영상을 로컬로 교체한 뒤 지우는 경로 (#1406).
 *
 * #1375 는 삭제 항목을 로컬 첨부에만 노출해 서버 미디어의 거짓 삭제를 막았다. 그런데 서버 영상을
 * 로컬 영상으로 **교체**하면 `videoUrl` 이 `content://` 로 바뀌어 삭제 가능 슬롯이 되고, 그걸 지우면
 * 폼은 비지만 저장 뒤 서버 영상이 그대로 남는다 — 수정 계약이 삭제를 표현하지 못하기 때문이다.
 * 「서버에는 아직 영상이 있다」는 사실이 교체 순간 폼에서 사라진 것이 원인이었다.
 *
 * 그래서 삭제는 **로컬 교체분만** 걷어내고 서버 원본 표시로 되돌린다. 지울 수 없는 것을 지운 척하지
 * 않는 쪽이, 사후 전달이라 되돌릴 기회가 제한적인 이 도메인에서 안전하다.
 */
class MemorialVideoServerOriginTest {
    private val serverVideo = "https://cdn.test/farewell.mp4"
    private val serverThumbnail = "https://cdn.test/farewell-thumb.jpg"
    private val localVideo = "content://videos/replacement"

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

    private fun EditorFormState.memorial(): AfternoteTypeForm.Memorial = typeForm as AfternoteTypeForm.Memorial

    @Test
    fun `수정 진입 prefill 은 서버 원본을 따로 기억한다`() {
        val form = prefilledEditForm().memorial()

        assertEquals(serverVideo, form.videoUrl)
        assertEquals(serverVideo, form.serverVideoUrl)
        assertEquals(serverThumbnail, form.serverThumbnailUrl)
    }

    @Test
    fun `로컬 영상으로 교체해도 서버 원본은 남는다`() {
        val replaced = prefilledEditForm().withMemorialVideo(localVideo).memorial()

        assertEquals(localVideo, replaced.videoUrl)
        // 교체한 영상의 썸네일은 아직 없다 — 원본 썸네일을 그대로 두면 다른 영상의 그림이 붙는다.
        assertNull(replaced.thumbnailUrl)
        assertEquals(serverVideo, replaced.serverVideoUrl)
    }

    @Test
    fun `교체분을 지우면 빈 슬롯이 아니라 서버 영상으로 돌아간다`() {
        val removed =
            prefilledEditForm()
                .withMemorialVideo(localVideo)
                .withMemorialVideo(null)
                .memorial()

        // 종전에는 여기서 videoUrl 이 null 이 되어 폼이 빈 슬롯을 보여줬다. 저장하면 서버 영상이
        // 그대로 남아 재진입 시 되살아났다 — 폼이 서버 상태를 두고 거짓말을 한 자리다.
        assertEquals(serverVideo, removed.videoUrl)
        assertEquals(serverThumbnail, removed.thumbnailUrl)
    }

    @Test
    fun `서버 영상으로 돌아가면 삭제 항목도 다시 감춰진다`() {
        val removed =
            prefilledEditForm()
                .withMemorialVideo(localVideo)
                .withMemorialVideo(null)

        assertEquals(emptySet<MemorialMediaTarget>(), removed.removableMemorialMediaTargets())
    }

    @Test
    fun `서버 원본이 없으면 삭제는 종전대로 슬롯을 비운다`() {
        // 생성 모드 — 되돌아갈 서버 상태가 없으므로 지운 그대로가 사실이다.
        val removed =
            EditorFormState(typeForm = AfternoteTypeForm.Memorial())
                .withMemorialVideo(localVideo)
                .withMemorialVideo(null)
                .memorial()

        assertNull(removed.videoUrl)
        assertNull(removed.thumbnailUrl)
    }
}
